package com.metalworkshop.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Properties {

    @Value("${auth.service.url}")
    private String authAddressUrl;

    public String getAuthAddressUrl() {
        return authAddressUrl;
    }
}
