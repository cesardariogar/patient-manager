package org.metalworkshop;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LocalStack extends Stack {

    private final static String APP_BUILD_OUTPUT_PATH = "./cdk.out";
    private final static String VPC_NAME = "PatientManagerVPC";
    private final static String AUTH_DATABASE_NAME = "auth_service_db";
    private final static String AUTH_DATABASE_USERNAME = "auth_admin";
    private final static String PATIENT_DATABASE_NAME = "patient_service_db";
    private final static String PATIENT_DATABASE_USERNAME = "patient_admin";
    private final static String LOCALSTACK_ID = "localstack";

    private final Vpc vpc;
    private final Cluster ecsCluster;
    private final CfnCluster mskCluster;


    public LocalStack(Construct scope,
                      String id,
                      StackProps props) {

        super(scope, id, props);

        this.vpc = createVpc();
        DatabaseInstance authServiceDb = createDatabase("AuthServiceDb", AUTH_DATABASE_NAME, AUTH_DATABASE_USERNAME);
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDb", PATIENT_DATABASE_NAME, PATIENT_DATABASE_USERNAME);
        //CfnHealthCheck authDbHealthCheck = createDatabaseHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
        //CfnHealthCheck patientDbHealthCheck = createDatabaseHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");
        this.mskCluster = createMskCluster("MskCluster");
        this.ecsCluster = createEscCluster("PatientManagerCluster");


        // TODO: INJECT JWT_SECRET HARDCODED CRAP
        FargateService authService = createFargateService("AuthService",
                "auth-service",
                List.of(4005),
                authServiceDb,
                Map.of("JWT_SECRET", "1e51f616b850a7a2e862377982029f2d26780da03bdd9eafebc31e7852950ed4238fad533e" +
                        "30caa498546bd64cffe46c677b5065343727dd844eaf048545882717dcd626d84f33749272af389c85dd2491c4ee625" +
                        "7a029cf5c34b226bf8298b3a3a45992395c815bf8977d54dbaaddbe1679790b8788533478a8ead550445e39355b9a1f" +
                        "966b200c07a65921dc12b58b326034227e0d1e92f785d0f5521688dfc471718c495a528dd7ce2541576e5621289fd1e" +
                        "f614f0192777c234ee5f2c9369e7d74cb7f91087a91febb0bb12dba38fa53c15be0e1f28f572ed9559f4603f1031c7e" +
                        "369bf29aaba535698f36c1b937af97578ee6fc3e4e738eace3c1649f871d0cde9296fe3406"));
        //authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);


        FargateService billingService = createFargateService("BillingService",
                "billing-service",
                List.of(4001, 9001),
                null,
                null);


        FargateService analyticsService = createFargateService("AnalyticsService",
                "analytics-service",
                List.of(4002),
                null,
                null);
        analyticsService.getNode().addDependency(mskCluster);


        FargateService patientService = createFargateService("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of("BILLING_SERVICE_NAME", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001",
                        "SPRING_KAFKA_BOOTSTRAP_SERVERS", ""));
        //patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        createApiGatewayService("ApiGatewayService", "api-gateway", List.of(4004));

    }

    private Vpc createVpc() {
        return Vpc.Builder
                .create(this, VPC_NAME)
                .vpcName(VPC_NAME)
                .maxAzs(2)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName, String dbUserName) {
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_18)
                        .build()))
                .vpc(this.vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret(dbUserName))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDatabaseHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
    }

    private CfnCluster createMskCluster(String id) {
        return CfnCluster.Builder.create(this, id)
                .clusterName("Kafka-Cluster")
                .kafkaVersion("4.3.1")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster
                        .BrokerNodeGroupInfoProperty.builder()
                        .instanceType("Kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets()
                                .stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }

    private Cluster createEscCluster(String id) {
        return Cluster.Builder.create(this, id)
                .vpc(this.vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();
    }

    private FargateService createFargateService(String id,
                                                String imageName,
                                                List<Integer> ports,
                                                DatabaseInstance db,
                                                Map<String, String> additionalEnvVars) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, id + "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder
                                        .create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()));

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");
        if (Objects.nonNull(additionalEnvVars)) {
            envVars.putAll(additionalEnvVars);
        }
        if (Objects.nonNull(db)) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName
            ));
            String datasourceUsername = (imageName.contains("auth")) ? "auth_admin" : "patient_admin";
            System.out.println("imageName: " + imageName);
            System.out.println("datasource user name: " + datasourceUsername + "\n");
            envVars.put("SPRING_DATASOURCE_DATASOURCE_USERNAME", datasourceUsername);
            envVars.put("SPRING_DATASOURCE_DATASOURCE_PASSWORD",
                    db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }
        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder
                .create(this, id)
                .cluster(this.ecsCluster)
                .taskDefinition(taskDefinition)
                .build();
    }

    // 4004
    // id: APIGatewayService
    // image: api-gateway
    private void createApiGatewayService(String id,
                                         String imageName,
                                         List<Integer> ports) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, id + "APIGatewayDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                        ))
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder
                                        .create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()))
                        .build();

        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway = ApplicationLoadBalancedFargateService.Builder
                .create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                //.healthCheckGracePeriod(Duration.seconds(60))
                .build();
    }

    static void main() {
        App app = new App(AppProps.builder().outdir(APP_BUILD_OUTPUT_PATH).build());

        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();
        new LocalStack(app, LOCALSTACK_ID, props);

        app.synth();
        System.out.println("App synthesizing in progress...");
    }


}
