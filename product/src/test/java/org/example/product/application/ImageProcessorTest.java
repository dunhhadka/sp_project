package org.example.product.application;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class ImageProcessorTest {

    @Test
    public void test() {
        String src = "www.example.com/search?query=hello%20world";
        var uri = UriComponentsBuilder.fromHttpUrl(URLDecoder.decode(src, StandardCharsets.UTF_8)).build();
        System.out.println(uri.getPath());

        String src1 = "https://www.example.com/search?query=helloworld";
        var uri1 = UriComponentsBuilder.fromHttpUrl(URLDecoder.decode(src1, StandardCharsets.UTF_8)).build();
        System.out.println(uri1.toUriString());
    }
}
