package com.nageoffer.shortlink.aggregation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 短链接聚合应用
 */
@SpringBootApplication(scanBasePackages = {
        "com.nageoffer.shortlink.admin",
        "com.nageoffer.shortlink.project"
})
@EnableDiscoveryClient
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }

}
