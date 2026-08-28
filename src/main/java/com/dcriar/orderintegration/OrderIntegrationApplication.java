package com.dcriar.orderintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderIntegrationApplication.class, args);
    }

}
