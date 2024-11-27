package org.example.product.product.application.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProductImageRequest {
    public enum UploadType {BASE64, SRC, FILE, NONE}

    private Integer id;
    private String base64;
    private String src;
    private MultipartFile file;
    private String fileName;
    private String alt;
    private Integer position;

    public UploadType getUploadType() {
        // ưu tiên file => base64 => src
        if (file != null) {
            return UploadType.FILE;
        }
        if (StringUtils.isNotBlank(base64)) {
            return UploadType.BASE64;
        }
        if (StringUtils.isNotBlank(src)) {
            return UploadType.SRC;
        }
        return UploadType.NONE;
    }
}
