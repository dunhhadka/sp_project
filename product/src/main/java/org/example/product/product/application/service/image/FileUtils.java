package org.example.product.product.application.service.image;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {

    private static final String BASE64_PNG_PREFIX = "iVBORw0KGgo";
    private static final String BASE64_JPD_PREFIX = "/9j/";
    private static final String BASE64_JPEG_PREFIX = "";

    public static String getContentType(String fileName) {
        if (StringUtils.isBlank(fileName)) return null;

        String extension = FilenameUtils.getExtension(fileName);
        return switch (extension.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpg";
            case "jpeg" -> "image/jpeg";
            default -> StringUtils.EMPTY;
        };
    }

    public static String getContentTypeFromBase64(String base64) {
        if (StringUtils.isBlank(base64)) return null;
        if (base64.startsWith(BASE64_PNG_PREFIX)) return "image/png";
        if (base64.startsWith(BASE64_JPD_PREFIX)) return "image/jpg";
        return StringUtils.EMPTY;
    }
}
