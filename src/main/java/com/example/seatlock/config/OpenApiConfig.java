package com.example.seatlock.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI seatLockOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("SeatLock - High-Concurrency Ticket Reservation API")
                        .description("High-throughput seat reservation engine featuring Redis Distributed Locking, 10-minute TTL temporary holds, database optimistic locking, and idempotent payment confirmation.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SeatLock Engineering")
                                .email("developer@seatlock.com"))
                        .license(new License().name("Apache 2.0")));
    }
}
