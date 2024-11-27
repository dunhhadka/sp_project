package org.example.order.application.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest {

    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDateFormat(new ISO8601DateFormat());

        // Register the JavaTimeModule to handle Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private String name;
        private String email;
        private String phone;
    }

    @Test
    public void test() throws JsonProcessingException {
        var user = User.builder()
                .name("Hà Văn Dũng")
                .email("hadung@gmail.com")
                .phone("12345678910")
                .build();
        String json = mapper.writeValueAsString(user);
        Assertions.assertNotNull(json);

        String jsonSerialized = "{\n" +
                "    \"name\": \"Hà Văn Dũng\",\n" +
                "    \"email\": \"hadung@gmail.com\"\n" +
                "}";
        User userDeserialized = mapper.readValue(jsonSerialized, User.class);
        Assertions.assertNotNull(userDeserialized);
        Assertions.assertEquals(user.email, userDeserialized.email);
        Assertions.assertEquals(user.name, userDeserialized.name);
    }
}
