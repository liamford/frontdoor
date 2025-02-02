package com.payments.frontdoor.integration;


import com.payments.frontdoor.FrontdoorApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@ExtendWith(SpringExtension.class)
@DirtiesContext
@Tag("integration")
public class IntegrationtestspringkafkaApplicationTests {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationtestspringkafkaApplicationTests.class);


    @BeforeAll
    public static void beforeAll() {
        logger.info("Integration tests started.");
    }

    @AfterAll
    public static void afterAll() {
        logger.info("Integration tests finished.");
    }


    @Test
    void contextLoads() {
        assertDoesNotThrow(() -> FrontdoorApplication.main(new String[]{}));
    }


}
