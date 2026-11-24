package com.parcelplus.shipment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI shipmentServiceApi() {
        return new OpenAPI().info(new Info().title("shipment-service").version("0.1.0"));
    }
}
