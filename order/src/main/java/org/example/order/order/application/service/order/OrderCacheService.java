package org.example.order.order.application.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.model.order.context.ProductImage;
import org.example.order.order.application.model.order.context.ProductImageCache;
import org.example.order.order.application.model.product.log.ProductLog;
import org.example.order.order.application.model.product.model.ProductPayloadKafkaConnectLog;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCacheService {

    private static final String PREFIX_IMAGE_ORDER_CACHE = "image:";
    private static final String UPDATE_VERB = "update";
    private static final String DELETE_VERB = "delete";

    @Qualifier("redis-template-order-cache")
    private final RedisTemplate<String, String> redisTemplate;

    private final ProductDao productDao;

    public List<ProductImageCache> getImageForLineItem(int storeId, List<Integer> productIds) {
        if (CollectionUtils.isEmpty(productIds)) return List.of();

        List<ProductImageCache> result = new ArrayList<>();
        var keys = productIds.stream().map(id -> PREFIX_IMAGE_ORDER_CACHE + id).toList();
        List<Integer> productIdsNotCached = new ArrayList<>();

        List<Object> resultSearchByKeys = redisTemplate.executePipelined((RedisCallback<Map<Integer, String>>) connection -> {
            for (String key : keys) {
                connection.hashCommands().hGetAll(key.getBytes());
            }
            return null;
        });

        for (int i = 0; i < keys.size(); i++) {
            var searchResult = (Map<String, String>) resultSearchByKeys.get(i);
            if (searchResult.isEmpty()) {
                productIdsNotCached.add(productIds.get(i));
            } else {
                for (var entry : searchResult.entrySet()) {
                    var field = entry.getKey();
                    var value = entry.getValue();
                    result.add(ProductImageCache.builder()
                            .productId(productIds.get(i))
                            .variantId(Integer.parseInt(field.replaceFirst(PREFIX_IMAGE_ORDER_CACHE, "")))
                            .imageUrl(value)
                            .build());
                }
            }
        }

        if (!productIdsNotCached.isEmpty()) {
            var productNotCached = productDao.getByIds(productIdsNotCached);
            List<ProductImageCache> dataToCache = new ArrayList<>();
            for (var product : productNotCached) {
                var defaultImage = CollectionUtils.isEmpty(product.getImages()) ? null : product.getImages().get(0);
                for (var variant : product.getVariants()) {
                    ProductImage imageFound;
                    if (variant.getImageId() == null) {
                        imageFound = defaultImage;
                    } else {
                        imageFound = product.getImages().stream()
                                .filter(i -> i.getId() == variant.getImageId())
                                .findFirst().orElse(null);
                    }
                    var sourceImage = imageFound == null ? StringUtils.EMPTY : imageFound.getSrc();
                    var productImageCache = ProductImageCache.builder()
                            .productId(product.getId())
                            .variantId(variant.getId())
                            .imageUrl(sourceImage)
                            .build();
                    dataToCache.add(productImageCache);
                    result.add(productImageCache);
                }
            }

            if (!dataToCache.isEmpty()) {
                redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                    for (var data : dataToCache) {
                        connection.hashCommands().hMSet((PREFIX_IMAGE_ORDER_CACHE + data.getProductId()).getBytes(),
                                Map.of(String.valueOf(data.getVariantId()).getBytes(),
                                        data.getImageUrl().getBytes())
                        );
                    }
                    return null;
                });
            }
        }

        return result;
    }

    public void handleProductChanged(ProductLog productLog) throws JsonProcessingException {
        if (StringUtils.equals(productLog.getVerb(), "add")
                || StringUtils.isBlank(productLog.getData())) return;

        redisTemplate.delete(PREFIX_IMAGE_ORDER_CACHE + productLog.getProductId());

        List<ProductImageCache> validCacheUpdates = new ArrayList<>();
        List<ProductImageCache> validCacheDeletes = new ArrayList<>();
        var product = JsonUtils.unmarshal(productLog.getData(), ProductPayloadKafkaConnectLog.class);
        Map<Integer, String> images = new HashMap<>();
        if (UPDATE_VERB.equals(productLog.getVerb())) {
            product.getEvents().forEach(event -> {
                switch (event.getEventName()) {
                    case "ProductVariantEdited" -> {
                        if (!CollectionUtils.isEmpty(event.getChanges())) {
                            event.getChanges().forEach(change -> {
                                if ("image_id".equals(change.getPropertyName())) {
                                    validCacheUpdates.add(
                                            ProductImageCache.builder()
                                                    .productId(product.getId())
                                                    .variantId(event.getId())
                                                    .imageUrl(change.getCurrentValue() != null ? String.valueOf(change.getCurrentValue()) : StringUtils.EMPTY)
                                                    .build()
                                    );
                                }
                            });
                        }
                    }
                    case "ProductVariantRemoved" -> validCacheDeletes.add(
                            ProductImageCache.builder()
                                    .productId(product.getId())
                                    .variantId(event.getId())
                                    .build()
                    );
                    case "ProductImageAdded" -> images.put(event.getId(), event.getSrc());
                }
            });
        } else if (DELETE_VERB.equals(productLog.getVerb())) {
            validCacheDeletes.add(
                    ProductImageCache.builder()
                            .productId(product.getId())
                            .build()
            );
        }

        validCacheUpdates.forEach(item -> {
            if (!StringUtils.EMPTY.equals(item.getImageUrl())) {
                int imageId = Integer.parseInt(item.getImageUrl());
                String sourceImage = images.get(imageId);
                item.setImageUrl(sourceImage);
            }
        });

        redisTemplate.executePipelined((RedisCallback<Map<Integer, String>>) connection -> {
            // case cập nhật ảnh sản phẩm
            Map<byte[], byte[]> mapValue = validCacheUpdates.stream()
                    .collect(Collectors.toMap(
                            item -> String.valueOf(item.getProductId()).getBytes(),
                            item -> StringUtils.firstNonEmpty(item.getImageUrl(), StringUtils.EMPTY).getBytes()
                    ));
            if (!mapValue.isEmpty()) {
                connection.hashCommands().hMSet((PREFIX_IMAGE_ORDER_CACHE + product.getId()).getBytes(), mapValue);
            }

            // case xóa sản phẩm
            var productsDeleted = validCacheDeletes.stream()
                    .filter(i -> i.getVariantId() == 0)
                    .map(item -> (PREFIX_IMAGE_ORDER_CACHE + item.getProductId()).getBytes())
                    .toArray(byte[][]::new);
            if (productsDeleted.length > 0) {
                connection.keyCommands().del(productsDeleted);
            }

            // case xóa variant
            var variantsUpdated = validCacheDeletes.stream()
                    .filter(i -> i.getVariantId() != 0)
                    .map(item -> String.valueOf(item.getVariantId()).getBytes())
                    .toArray(byte[][]::new);
            if (variantsUpdated.length > 0) {
                connection.hashCommands().hDel((PREFIX_IMAGE_ORDER_CACHE + product.getId()).getBytes(), variantsUpdated);
            }

            return null;
        });
    }
}
