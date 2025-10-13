package com.metalworkshop.patient_billing.grpc;


import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(BillingRequest request,
                                     StreamObserver<BillingResponse> responseObserver) {
        logger.info("createBillingAccount request received: ", request.toString());

        // Business logic - e.g. save to database, etc.
        BillingResponse response = billing.BillingResponse.newBuilder()
                .setAccountId(request.getPatientId())
                .setStatus("ACTIVE")
                .build();

        // We are able to pass as many responses as we need
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
