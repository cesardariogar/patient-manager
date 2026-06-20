package com.metalworkshop.PatientService.grpc;


import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.metalworkshop.PatientService.config.BillingProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final billing.BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    private String grpcAddress;
    private int grpcPort;

    @Autowired
    public BillingServiceGrpcClient(BillingProperties props) {
        grpcAddress = props.getGrpcServiceName();
        grpcPort = props.getGrpcPort();
        logger.info("Connecting to Billing Service GRPC at {}:{}", grpcAddress, grpcPort);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(grpcAddress, grpcPort)
                .usePlaintext()
                .build();

        this.blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        logger.info("Received response from Billing service via GRPC: {}", response.toString().replaceAll("\n", ""));

        return response;
    }
}
