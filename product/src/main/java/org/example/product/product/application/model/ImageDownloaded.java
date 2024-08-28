package org.example.product.product.application.model;

import com.google.common.net.MediaType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImageDownloaded {
    private byte[] bytes;
    private String contentType;

    private static final List<MediaType> supportedContentTypes = List.of(
            MediaType.parse("image/png"),
            MediaType.parse("image/jpg"),
            MediaType.parse("image/jpeg")
    );

    public static boolean isSupportedContentType(String contentType) {
        try {
            var type = MediaType.parse(contentType);
            return supportedContentTypes.stream()
                    .anyMatch(type::is);
        } catch (Exception e) {
            return false;
        }
    }
}
