package org.metalworkshop;

import com.fasterxml.jackson.core.JsonProcessingException;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedEc2Service;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * CDK stack for patient-manager infrastructure on floci (local AWS emulator).
 * <p>
 * Uses EC2-backed ECS with bridge networking because floci (like LocalStack) does not
 * emulate awsvpc networking. Bridge mode puts all containers on the host's Docker
 * network, making CloudMap service discovery and direct TCP connectivity work reliably.
 * <p>
 * RDS containers expose dynamic host ports. Find the actual ports with:
 * docker ps | grep postgres
 * Then update AUTH_DATABASE_HOST_PORT and PATIENT_DATABASE_HOST_PORT constants below.
 * <p>
 * Start floci before deploying:
 * docker compose -f docker-compose.floci.yml up -d
 *
 * Notes:
 * Billing-Service (and modules using gRPC):
 * The Java gRPC client (Netty) tries IPv6 first → NoRouteToHostException. WebClient handles this differently (retries on IPv4).
 * The fix: add JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true to the container env vars.
 *
 * Kafka (Msk):
 * Use Kafka via docker-compose not MSK: floci MSK doesn't provision real brokers; real Kafka container on floci_default network resolves kafka:9092
 *
 */
public class CloudFormationFlocciEc2 extends Stack {

    private static final String STACK_ID = "flocci-ec2";
    private static final String APP_BUILD_OUTPUT_PATH = "./cdk.out";
    private static final String VPC_NAME = "PatientManagerVPC";

    private static final String AUTH_DATABASE_NAME = "auth_service_db";
    private static final String AUTH_DATABASE_USERNAME = "auth_admin";
    private static final String PATIENT_DATABASE_NAME = "patient_service_db";
    private static final String PATIENT_DATABASE_USERNAME = "patient_admin";
    private static final String SERVICE_DISCOVERY_NAMESPACE = "patient-management.local";
    // Testing only variables
    private static final String AUTH_DATABASE_PASSWORD = "auth_admin_pwd_123!@#";
    private static final String PATIENT_DATABASE_PASSWORD = "patient_admin_pwd_456!@#";

    // floci proxies RDS at its own IP on the floci_default network. All ECS task
    // containers are also on floci_default, so they can reach the proxy directly.
    // The actual RDS containers have no host port mapping — only floci's proxy is
    // accessible. Find the floci proxy ports with: AWS_ENDPOINT_URL=http://localhost:4566 aws rds describe-db-instances
    private static final String FLOCI_RDS_PROXY = "172.18.0.3";
    private static final int AUTH_DATABASE_HOST_PORT = 7001;
    private static final int PATIENT_DATABASE_HOST_PORT = 7002;

    private final Vpc vpc;
    private final Cluster ecsCluster;
    private final SecurityGroup backendSecurityGroup;
    private final AsgCapacityProvider capacityProvider;

    public CloudFormationFlocciEc2(Construct scope, String id, StackProps props) throws JsonProcessingException {
        super(scope, id, props);

        this.vpc = createVpc();

        this.backendSecurityGroup = SecurityGroup.Builder.create(this, "BackendSecurityGroup")
                .vpc(this.vpc)
                .description("Allows all traffic between EC2 instances and RDS instances")
                .allowAllOutbound(true)
                .build();
        this.backendSecurityGroup.getConnections()
                .allowInternally(Port.allTraffic(), "allow all traffic within the backend security group");

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDb", AUTH_DATABASE_NAME, AUTH_DATABASE_USERNAME);
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDb", PATIENT_DATABASE_NAME, PATIENT_DATABASE_USERNAME);
        patientServiceDb.getNode().addDependency(authServiceDb);

        CfnHealthCheck authDbHealthCheck = createDatabaseHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createDatabaseHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

        // Kafka is managed outside this template
        //this.mskCluster = createMskCluster("MskCluster");
        this.capacityProvider = createCapacityProvider("EcsCapacityProvider");
        this.ecsCluster = createEcsCluster("PatientManagerCluster");

        // --- Services ---------------------------------------------------------------

        // TODO: INJECT JWT_SECRET HARDCODED CRAP, someday maybe
        Ec2Service authService = createEc2Service("AuthService",
                "auth-service",
                List.of(4005),
                authServiceDb,
                AUTH_DATABASE_NAME,
                AUTH_DATABASE_USERNAME,
                Map.of("JWT_SECRET", "1e51f616b850a7a2e862377982029f2d26780da03bdd9eafebc31e7852950ed4238fad533e" +
                        "30caa498546bd64cffe46c677b5065343727dd844eaf048545882717dcd626d84f33749272af389c85dd2491c4ee625" +
                        "7a029cf5c34b226bf8298b3a3a45992395c815bf8977d54dbaaddbe1679790b8788533478a8ead550445e39355b9a1f" +
                        "966b200c07a65921dc12b58b326034227e0d1e92f785d0f5521688dfc471718c495a528dd7ce2541576e5621289fd1e" +
                        "f614f0192777c234ee5f2c9369e7d74cb7f91087a91febb0bb12dba38fa53c15be0e1f28f572ed9559f4603f1031c7e" +
                        "369bf29aaba535698f36c1b937af97578ee6fc3e4e738eace3c1649f871d0cde9296fe3406"));
        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        Ec2Service billingService = createEc2Service("BillingService",
                "billing-service",
                List.of(4001, 9001),
                null, null, null,
                Map.of("GRPC_SERVER_PORT", "9001"));

        Ec2Service analyticsService = createEc2Service("AnalyticsService",
                "analytics-service",
                List.of(4002),
                null, null, null, null);

        Ec2Service patientService = createEc2Service("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                PATIENT_DATABASE_NAME,
                PATIENT_DATABASE_USERNAME,
                Map.of("BILLING_SERVICE_NAME", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001",
                        "SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(billingService);
        //patientService.getNode().addDependency(mskCluster);

        createApiGatewayService("ApiGatewayService", "api-gateway", List.of(4004));
    }

    // ---------------------------------------------------------------------------
    // Infrastructure factories
    // ---------------------------------------------------------------------------

    private Vpc createVpc() {
        return Vpc.Builder
                .create(this, VPC_NAME)
                .vpcName(VPC_NAME)
                .ipAddresses(IpAddresses.cidr("10.0.0.0/16"))
                .maxAzs(2)
                .natGateways(1)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName, String dbUserName) throws JsonProcessingException {
        String dbPassword = dbUserName.contains("auth")
                ? AUTH_DATABASE_PASSWORD
                : PATIENT_DATABASE_PASSWORD;

        CredentialsFromUsernameOptions options = CredentialsFromUsernameOptions.builder()
                .password(SecretValue.unsafePlainText(dbPassword))
                .build();

        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.of("13.13", "13"))
                        .build()))
                .vpc(this.vpc)
                .instanceType(software.amazon.awscdk.services.ec2.InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromUsername(dbUserName, options))
                .databaseName(dbName)
                .securityGroups(List.of(this.backendSecurityGroup))
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

/*    private CfnCluster createMskCluster(String id) {
        return CfnCluster.Builder.create(this, id)
                .clusterName("Kafka-Cluster")
                .kafkaVersion("3.5.1")
                .numberOfBrokerNodes(3)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.large")
                        .clientSubnets(vpc.getPrivateSubnets()
                                .stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }*/

    // ---------------------------------------------------------------------------
    // EC2-backed ECS capacity
    // ---------------------------------------------------------------------------

    /**
     * Creates an Auto Scaling Group of ECS-optimized EC2 instances and wraps it in a
     * capacity provider. The ECS cluster will place tasks on these instances.
     */
    private AsgCapacityProvider createCapacityProvider(String id) {
        AutoScalingGroup asg = AutoScalingGroup.Builder.create(this, id + "ASG")
                .vpc(this.vpc)
                .instanceType(software.amazon.awscdk.services.ec2.InstanceType.of(InstanceClass.T3, InstanceSize.MICRO))
                .machineImage(EcsOptimizedImage.amazonLinux2())
                .desiredCapacity(1)
                .minCapacity(1)
                .maxCapacity(1)
                .securityGroup(this.backendSecurityGroup)
                .build();

        return AsgCapacityProvider.Builder.create(this, id)
                .autoScalingGroup(asg)
                .enableManagedScaling(false)
                .enableManagedTerminationProtection(false)
                .build();
    }

    private Cluster createEcsCluster(String id) {
        Cluster cluster = Cluster.Builder.create(this, id)
                .vpc(this.vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name(SERVICE_DISCOVERY_NAMESPACE)
                        .build())
                .build();

        cluster.addAsgCapacityProvider(this.capacityProvider);
        cluster.addDefaultCapacityProviderStrategy(List.of(
                CapacityProviderStrategy.builder()
                        .capacityProvider(this.capacityProvider.getCapacityProviderName())
                        .weight(1)
                        .build()));

        return cluster;
    }

    // ---------------------------------------------------------------------------
    // Service factories
    // ---------------------------------------------------------------------------

    private PortMapping buildPortMapping(int port) {
        return PortMapping.builder()
                .containerPort(port)
                .hostPort(port)
                .protocol(Protocol.TCP)
                .build();
    }

    /**
     * Creates an EC2-backed ECS service with bridge networking.
     * <p>
     * Bridge mode gives each container its own set of ports mapped through the Docker
     * bridge network. Services discover each other through CloudMap DNS (e.g.
     * "auth-service.patient-management.local") and containers on the same host can
     * also reach each other directly through the Docker gateway.
     */
    private Ec2Service createEc2Service(String id,
                                        String imageName,
                                        List<Integer> ports,
                                        DatabaseInstance db,
                                        String dbName,
                                        String dbUserName,
                                        Map<String, String> additionalEnvVars) {

        Ec2TaskDefinition taskDefinition = Ec2TaskDefinition.Builder
                .create(this, id + "Task")
                .networkMode(NetworkMode.BRIDGE)
                .build();

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");
        envVars.put("JAVA_TOOL_OPTIONS", "-Djava.net.preferIPv4Stack=true");
        if (Objects.nonNull(additionalEnvVars)) {
            envVars.putAll(additionalEnvVars);
        }
        if (Objects.nonNull(db)) {
            int hostPort = dbUserName.contains("auth") ? AUTH_DATABASE_HOST_PORT : PATIENT_DATABASE_HOST_PORT;

            envVars.put("SPRING_DATASOURCE_URL",
                    "jdbc:postgresql://" + FLOCI_RDS_PROXY + ":" + hostPort + "/" + dbName);
            envVars.put("SPRING_DATASOURCE_USERNAME", dbUserName);

            String dbPassword = dbUserName.contains("auth")
                    ? AUTH_DATABASE_PASSWORD
                    : PATIENT_DATABASE_PASSWORD;
            envVars.put("SPRING_DATASOURCE_PASSWORD", dbPassword);

            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION", "true");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        List<PortMapping> portMappings = ports.stream()
                .map(this::buildPortMapping)
                .collect(Collectors.toList());

        taskDefinition.addContainer(imageName + "Container",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("cesardariogar/" + imageName))
                        .portMappings(portMappings)
                        .memoryLimitMiB(512)
                        .cpu(0.60)
                        .environment(envVars)
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder
                                        .create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()))
                        .build());

        return Ec2Service.Builder
                .create(this, id)
                .cluster(this.ecsCluster)
                .taskDefinition(taskDefinition)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name(imageName)
                        .build())
                .build();
    }

    /**
     * Creates the API Gateway EC2 service behind an Application Load Balancer.
     * This is the system entry point.
     */
    private void createApiGatewayService(String id,
                                         String imageName,
                                         List<Integer> ports) {

        Ec2TaskDefinition taskDefinition = Ec2TaskDefinition.Builder
                .create(this, id + "APIGatewayDefinition")
                .networkMode(NetworkMode.BRIDGE)
                .build();

        List<PortMapping> portMappings = ports.stream()
                .map(this::buildPortMapping)
                .collect(Collectors.toList());

        taskDefinition.addContainer("APIGatewayContainer",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("cesardariogar/" + imageName))
                        .memoryLimitMiB(512)
                        .cpu(0.60)
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005",
                                "JAVA_TOOL_OPTIONS", "-Djava.net.preferIPv4Stack=true"))
                        .portMappings(portMappings)
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder
                                        .create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()))
                        .build());

        ApplicationLoadBalancedEc2Service.Builder
                .create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .build();
    }

    // ---------------------------------------------------------------------------
    // Entrypoint
    // ---------------------------------------------------------------------------

    static void main() throws JsonProcessingException {
        App app = new App(AppProps.builder().outdir(APP_BUILD_OUTPUT_PATH).build());

        StackProps props = StackProps.builder()
                .synthesizer(new DefaultStackSynthesizer())
                .build();
        new CloudFormationFlocciEc2(app, STACK_ID, props);

        app.synth();
        System.out.println("App synthesizing in progress...");
    }
}
