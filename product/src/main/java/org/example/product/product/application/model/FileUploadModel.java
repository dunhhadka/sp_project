package org.example.product.product.application.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadModel {
    private byte[] bytes;
    private String fileName;
    private String contentType;
}
