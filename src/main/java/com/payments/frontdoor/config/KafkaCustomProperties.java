package com.payments.frontdoor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payments.kafka.topics")
public class KafkaCustomProperties {
    private String initiated;
    private String managed;
    private String authorized;
    private String executed;
    private String cleared;
    private String notified;
    private String reconciled;
    private String posted;
    private String refund;
    private String reported;
    private String archived;
}