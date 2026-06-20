package com.metalworkshop.PatientService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "billing.service")
@Component
public class BillingProperties {

    @Value("${billing.service.name}")
    private String grpcServiceName;

    @Value("${billing.service.grpc.port}")
    private int grpcPort;

    public String getGrpcServiceName() {
        return grpcServiceName;
    }

    public int getGrpcPort() {
        return grpcPort;
    }
}
