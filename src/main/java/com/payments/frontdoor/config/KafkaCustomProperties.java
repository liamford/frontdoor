package com.payments.frontdoor.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payments.kafka")
public class KafkaCustomProperties {
    private String executionTopic;

}
