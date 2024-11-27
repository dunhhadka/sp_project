package org.example.product.product.infrastructure.data.dao;

import org.example.product.product.infrastructure.data.dto.ImageDto;

import java.util.List;

public interface ImageDao {
    List<ImageDto> getByFileName(int storeId, String fileName);
}
