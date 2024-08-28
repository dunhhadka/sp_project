package org.example.order.order.infrastructure.configuration.feign;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;

@Configuration
@RequiredArgsConstructor
public class DefaultFeignConfig {

    private final ObjectMapper mapper;

    @Bean
    @Primary
    public RequestInterceptor feignRequestInterceptorDefault() {
        return template -> {
            template.header("Store-Client", "client");
            if (!template.headers().containsKey("Content-Type")) {
                template.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            }
        };
    }

//    @Bean
//    @Primary
//    public Decoder feignDecoder() {
//        return new ResponseEntityDecoder(new SpringDecoder(() -> new Jackson2JsonDecoder()));
//    }
}
