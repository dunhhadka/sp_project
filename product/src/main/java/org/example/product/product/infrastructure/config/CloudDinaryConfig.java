package org.example.product.product.infrastructure.config;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
@RequiredArgsConstructor
public class CloudDinaryConfig {

    private final CloudDinaryProperties properties;

    @Bean
    public Cloudinary cloudinary() {
        var map = new HashMap<String, Object>();
        map.put("cloud_name", properties.getCloudName());
        map.put("api_key", properties.getApiKey());
        map.put("api_secret", properties.getApiSecret());
        return new Cloudinary(map);
    }
}
