package org.example.product.product.application.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadModel {
    private byte[] bytes;
    private String fileName;
    private String contentType;
}
