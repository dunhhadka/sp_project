package org.example.product.product.application.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.model.response.ProductResponse;
import org.example.product.product.domain.product.dto.ProductDto;
import org.example.product.product.domain.product.repository.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReadService {

    private static final String PRODUCT_GET_BY_IDS_CACHE_NAME = "product_get_by_ids";

    private final ProductMapper productMapper;

    private final ProductDao productDao;
    private final VariantDao variantDao;
    private final ImageDao imageDao;
    private final ProductTagDao productTagDao;


    @Cacheable(cacheNames = PRODUCT_GET_BY_IDS_CACHE_NAME, keyGenerator = "getProductByIdsKeyGenerator")
    public List<ProductResponse> getProductByIds(int storeId, List<Integer> productIds) {
        var products = productDao.getByIds(storeId, productIds);
        return internalGetProductResponseByProducts(storeId, products);
    }

    private List<ProductResponse> internalGetProductResponseByProducts(int storeId, List<ProductDto> products) {
        var productIds = products.stream().map(ProductDto::getId).toList();
        var productListResponse = new ArrayList<ProductResponse>();
        if (CollectionUtils.isEmpty(productIds)) return productListResponse;

        List<VariantDto> productVariants = new ArrayList<>();
        List<ImageDto> productImages = new ArrayList<>();
        List<TagDto> productTags = new ArrayList<>();

        try {
            var productInfosAsync = List.of(
                    variantDao.getByProductIds(storeId, productIds),
                    imageDao.getByProductIds(storeId, productIds),
                    productTagDao.getByProductIds(storeId, productIds)
            );
            productVariants = (List<VariantDto>) productInfosAsync.get(0);
            productImages = (List<ImageDto>) productInfosAsync.get(1);
            productTags = (List<TagDto>) productInfosAsync.get(2);
        } catch (Exception e) {
            log.error("error while get products infos async");
        }

        for (var productId : productIds) {
            var product = products.stream().filter(p -> p.getId() == productId).findFirst().orElse(null);
            if (product == null) continue;

            var productResponse = productMapper.fromDtoToResponse(product);

            //variants
            var variantResponse = productVariants.stream()
                    .filter(v -> v.getProductId() == productId)
                    .sorted(Comparator.comparing(VariantDto::getCreatedOn))
                    .map(productMapper::fromDtoToResponse)
                    .toList();
            productResponse.setVariants(variantResponse);

            var tags = productTags.stream()
                    .filter(t -> t.getProductId() == productId)
                    .map(TagDto::getName)
                    .toList();
            if (tags.isEmpty()) {
                productResponse.setTags(StringUtils.EMPTY);
            } else {
                productResponse.setTags(String.join(", ", tags));
            }


        }

        return productListResponse;
    }
}
