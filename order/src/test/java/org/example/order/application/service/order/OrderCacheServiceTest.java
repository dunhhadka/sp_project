package org.example.order.application.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.model.order.context.Product;
import org.example.order.order.application.model.order.context.ProductImage;
import org.example.order.order.application.model.order.context.ProductImageCache;
import org.example.order.order.application.model.order.context.ProductResponse;
import org.example.order.order.application.service.order.OrderCacheService;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.infrastructure.configuration.RedisOrderCacheConfig;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import(RedisOrderCacheConfig.class)
public class OrderCacheServiceTest {

    @Autowired
    @Qualifier("redis-template-order-cache")
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ProductDao productDao;

    private OrderCacheService orderCacheService;

    @BeforeEach
    public void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        orderCacheService = new OrderCacheService(redisTemplate, productDao);
    }


    @ParameterizedTest
    @ValueSource(strings = {"""
            {
                      "products":[
                          {
                              "id": 20,
                              "variants":[
                                  {
                                      "id": 1
                                  },
                                  {
                                      "id" : 2,
                                      "image_id": 1
                                  }
                              ],
                              "images": [
                                  {
                                      "id": 1,
                                      "src": "http://image_1",
                                      "variant_ids":[
                                          2
                                      ]
                                  },
                                  {
                                      "id": 2,
                                      "src": "http://image_2",
                                      "variant_ids":[]
                                  }
                              ]
                          },
                          {
                              "id": 21,
                              "variants":[
                                  {
                                      "id": 3,
                                      "image_id": 4
                                  },
                                  {
                                      "id" : 4,
                                      "image_id": 3
                                  }
                              ],
                              "images": [
                                  {
                                      "id": 3,
                                      "src": "http://image_3",
                                      "variant_ids":[
                                          4
                                      ]
                                  },
                                  {
                                      "id": 4,
                                      "src": "http://image_4",
                                      "variant_ids":[
                                          3
                                      ]
                                  }
                              ]
                          }
                      ]
                  }
            """, """
            {
                      "products":[
                          {
                              "id": 22,
                              "variants":[
                                  {
                                      "id": 5,
                                      "image_id": 4
                                  },
                                  {
                                      "id" : 6,
                                      "image_id": 4
                                  }
                              ],
                              "images": [
                                  {
                                      "id": 4,
                                      "src": "http://image_4",
                                      "variant_ids":[
                                          5, 6
                                      ]
                                  }
                              ]
                          },
                          {
                              "id": 23,
                              "variants":[
                                  {
                                      "id": 7,
                                      "image_id": null
                                  },
                                  {
                                      "id" : 8,
                                      "image_id": 5
                                  }
                              ],
                              "images": [
                                  {
                                      "id": 5,
                                      "src": "http://image_5",
                                      "variant_ids":[
                                          8
                                      ]
                                  }
                              ]
                          }
                      ]
                  }
            """})
    public void test_get_image_from_product_ids_in_cache(String productResponseString) throws JsonProcessingException {
        var productResponse = JsonUtils.unmarshal(productResponseString, ProductResponse.class);
        var productIds = productResponse.products().stream().map(Product::getId).toList();

        when(productDao.getByIds(anyList())).thenReturn(productResponse.products());

        var result = orderCacheService.getImageForLineItem(1, productIds);

        var expectedResult = new ArrayList<ProductImageCache>();
        for (var product : productResponse.products()) {
            var defaultImage = CollectionUtils.isEmpty(product.getImages()) ? null : product.getImages().get(0);
            for (var variant : product.getVariants()) {
                ProductImage imageFound;
                if (variant.getImageId() != null) {
                    imageFound = product.getImages().stream()
                            .filter(i -> i.getId() == variant.getImageId())
                            .findFirst().orElse(null);
                } else {
                    imageFound = defaultImage;
                }
                String src = imageFound == null ? StringUtils.EMPTY : imageFound.getSrc();
                var productImage = ProductImageCache.builder()
                        .productId(product.getId())
                        .variantId(variant.getId())
                        .imageUrl(src)
                        .build();
                expectedResult.add(productImage);
            }
        }

        assertThat(result).isEqualTo(expectedResult);

        verify(productDao, times(1)).getByIds(anyList());

        reset(productDao);

        var results = orderCacheService.getImageForLineItem(1, productIds);
        assertThat(results).isEqualTo(expectedResult);

        verify(productDao, never()).getByIds(anyList());
    }
}
