package org.example.order.order.infrastructure.configuration.feign;

import feign.Logger;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LoggingFeignConfig {
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
//    @Profile({"dev", "local"})
    public Logger.Level feignLoggerLevel() {
        log.info("configuration is started");
        return Logger.Level.FULL;
    }
}
