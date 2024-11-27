package org.example.product.product.application.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import java.util.List;

@Slf4j
@Getter
@Builder
public class ImageDownloaded {
    private byte[] bytes;
    private String contentType;

    private static final List<MediaType> supportedTypes = List.of(
            MediaType.parseMediaType("image/gif"),
            MediaType.parseMediaType("image/jpeg"),
            MediaType.parseMediaType("image/jpg"),
            MediaType.parseMediaType("image/png"),
            MediaType.parseMediaType("image/webp")
    );

    public static boolean isSupportedContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) return false;
        try {
            var mediaType = MediaType.parseMediaType(contentType);
            return supportedTypes.stream().anyMatch(mediaType::includes);
        } catch (Exception ignored) {
            log.warn("can't pare to media type from {}", contentType);
        }
        return false;
    }
}
