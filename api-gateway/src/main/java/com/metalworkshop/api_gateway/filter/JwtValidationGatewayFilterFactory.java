package com.metalworkshop.api_gateway.filter;

import com.metalworkshop.api_gateway.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JwtValidationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<Object> {

    private final Logger logger = LoggerFactory.getLogger(JwtValidationGatewayFilterFactory.class);
    private final WebClient webClient;

    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
                                             Properties props) {
        logger.info("Registering auth-service-url: {} in JwtValidationGatewayFilterFactory", props.getAuthAddressUrl());
        this.webClient = webClientBuilder.baseUrl(props.getAuthAddressUrl()).build();
    }

    @Override
    public GatewayFilter apply(Object config) {

        return (exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return webClient.get()
                    .uri("/validate")
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(response -> chain.filter(exchange))
                    .onErrorResume(ex -> {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });

        };
    }
}
