package org.example.product.product.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.product.product.infrastructure.data.dto.ImageDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ImageDaoImpl implements ImageDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ImageDto> getByFileName(int storeId, String fileName) {
        return jdbcTemplate.query(
                "SELECT * FROM images WHERE store_id = :storeId AND file_name = :fileName",
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("fileName", fileName),
                BeanPropertyRowMapper.newInstance(ImageDto.class)
        );
    }
}
