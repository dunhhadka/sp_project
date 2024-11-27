package org.example.product.product.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.SapoClient;
import org.example.product.product.application.exception.ConstrainViolationException;
import org.example.product.product.application.model.*;
import org.example.product.product.domain.inventory.InventoryItem;
import org.example.product.product.domain.inventory.InventoryLevel;
import org.example.product.product.domain.inventory.repository.InventoryItemRepository;
import org.example.product.product.domain.inventory.repository.InventoryLevelRepository;
import org.example.product.product.domain.product.model.*;
import org.example.product.product.domain.product.repository.ProductIdGenerator;
import org.example.product.product.domain.product.repository.ProductRepository;
import org.example.product.product.infrastructure.data.dao.StoreDao;
import org.example.product.product.infrastructure.data.dto.StoreDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductWriteService {

    public static final String MANAGER = "bizweb";

    private final SapoClient sapoClient;

    private final ProductIdGenerator idGenerator;

    private final StoreDao storeDao;

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final ProductRepository productRepository;

    private final ImageProcessorService imageProcessorService;

    @Transactional
    public ProductId createProduct(ProductCreateRequest request, Integer storeId) throws ExecutionException, InterruptedException, IOException {
        var store = getStoreById(storeId);
        var productCount = 100; // count product in store
        if (productCount >= store.getMaxProduct()) {
            throw new ConstrainViolationException("maximum_product", "total product must be less than or equal to " + store.getMaxProduct());
        }

        var storedImages = imageProcessorService.storeImages(storeId, request.getImages());
        var images = buildNewImages(storedImages, request.getImages());

        var productId = new ProductId(storeId, idGenerator.generateProductId());

        List<Variant> variants = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        List<InventoryLevel> inventoryLevels = new ArrayList<>();
        if (CollectionUtils.isEmpty(request.getVariants())) {
            var variant = defaultVariant(
                    idGenerator.generateVariantId(),
                    idGenerator.generateInventoryItemId(),
                    request.getDefaultVariantUnit()
            );
            variants.add(variant);
            inventoryItems.add(defaultInventoryItem(variant, productId));
            inventoryLevels.add(defaultInventoryLevel(variant, productId));
        } else {
            var variantCount = request.getVariants().size();
            var variantIds = idGenerator.generateVariantIds(variantCount);
            var inventoryItemIds = idGenerator.generateInventoryItemIds(variantCount);
            variants = buildNewVariants(variantIds, inventoryItemIds, request, images);
            inventoryItems = buildNewInventoryItems(variants, request, productId);
            inventoryLevels = buildNewInventoryLevels(variants, request, productId);
        }

        inventoryItemRepository.saveAll(inventoryItems);

        if (CollectionUtils.isNotEmpty(inventoryLevels)) {
            inventoryLevelRepository.saveAll(inventoryLevels);
        }

        if (CollectionUtils.isNotEmpty(inventoryLevels)) {
            setInventoryQuantity(inventoryLevels, variants);
        }

        var product = new Product(
                productId,
                request.getName(),
                request.getAlias(),
                buildProductGeneralInfo(request),
                convertTagToStringList(request.getTags()),
                variants,
                images,
                idGenerator,
                request.getStatus(),
                request.getPublishedOn()
        );

        productRepository.store(product);

        return product.getId();
    }

    private List<String> convertTagToStringList(String tags) {
        if (StringUtils.isBlank(tags)) {
            return List.of();
        }

        return Arrays.stream(tags.split(","))
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .distinct()
                .toList();
    }

    private ProductGeneralInfo buildProductGeneralInfo(ProductCreateRequest request) {
        return ProductGeneralInfo.builder()
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .templateLayout(request.getTemplateLayout())
                .summary(request.getSummary())
                .vendor(request.getVendor())
                .productType(request.getProductType())
                .build();
    }

    private void setInventoryQuantity(List<InventoryLevel> inventoryLevels, List<Variant> variants) {
        for (var variant : variants) {
            int inventoryLevelQuantity = inventoryLevels.stream()
                    .filter(i -> i.getVariantId() == variant.getId())
                    .filter(i -> i.getOnHand() != null && i.getOnHand().compareTo(BigDecimal.ZERO) >= 0)
                    .mapToInt(i -> i.getOnHand().intValue())
                    .sum();
            if (variant.getInventoryQuantity() != inventoryLevelQuantity) {
                variant.setInventoryQuantity(inventoryLevelQuantity);
            }
        }
    }

    private List<InventoryLevel> buildNewInventoryLevels(List<Variant> variants, ProductCreateRequest request, ProductId productId) {
        if (CollectionUtils.isEmpty(request.getVariants())) {
            return List.of();
        }

        var variantRequests = request.getVariants();

        // validate locations
        List<Integer> locationIds = variantRequests.stream()
                .filter(v -> CollectionUtils.isNotEmpty(v.getInventoryQuantities()))
                .flatMap(v -> v.getInventoryQuantities().stream())
                .map(InventoryQuantityRequest::getLocationId)
                .filter(locationId -> locationId > 0)
                .toList();
        this.validateLocationIds(locationIds, productId.getStoreId());

        var inventoryLevelCount = variantRequests.stream()
                .mapToInt(v -> {
                    var inventoryQuantities = v.getInventoryQuantities();
                    if (CollectionUtils.isEmpty(inventoryQuantities)) return 1;
                    return inventoryQuantities.size();
                })
                .reduce(0, Integer::sum);
        var inventoryLevelIds = idGenerator.generateInventoryLevelIds(inventoryLevelCount);
        List<InventoryLevel> inventoryLevels = new ArrayList<>();
        var defaultLocationId = this.getDefaultLocationId(productId.getStoreId());
        for (int i = 0; i < variants.size(); i++) {
            var variant = variants.get(i);
            var variantRequest = variantRequests.get(i);
            boolean tracked = MANAGER.equals(variantRequest.getInventoryManagement());
            if (CollectionUtils.isEmpty(variantRequest.getInventoryQuantities())) {
                int inventoryQuantity = tracked ? variantRequest.getInventoryQuantity() : 0;
                BigDecimal quantity = BigDecimal.valueOf(inventoryQuantity);
                inventoryLevels.add(new InventoryLevel(
                        inventoryLevelIds.removeFirst(),
                        productId,
                        variant.getId(),
                        variant.getInventoryItemId(),
                        defaultLocationId,
                        quantity,
                        quantity,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ));
            } else {
                inventoryLevels.addAll(
                        buildInventoryLevelsForVariant(
                                variantRequest.getInventoryQuantities(),
                                inventoryLevelIds,
                                productId,
                                variant.getId(),
                                variant.getInventoryItemId())
                );
            }
        }

        return inventoryLevels;
    }

    private List<InventoryLevel> buildInventoryLevelsForVariant(
            List<InventoryQuantityRequest> inventoryQuantities,
            Deque<Integer> inventoryLevelIds, ProductId productId,
            int variantId, int inventoryItemId
    ) {
        this.validateOnHand(inventoryQuantities);

        List<InventoryLevel> inventoryLevels = new ArrayList<>();
        for (var inventoryQuantity : inventoryQuantities) {
            BigDecimal quantity = inventoryQuantity.getQuantity();
            BigDecimal defaultQuantity = BigDecimal.ZERO;
            inventoryLevels.add(new InventoryLevel(
                    inventoryLevelIds.removeFirst(),
                    productId,
                    variantId,
                    inventoryItemId,
                    inventoryQuantity.getLocationId(),
                    quantity,
                    quantity,
                    defaultQuantity,
                    defaultQuantity
            ));
        }
        return inventoryLevels;
    }

    private void validateOnHand(List<InventoryQuantityRequest> inventoryQuantities) {
        for (var inventoryQuantity : inventoryQuantities) {
            if (inventoryQuantity.getQuantity() == null || inventoryQuantity.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new ConstrainViolationException("inventory_quantity", "must be greater than or equal to 0");
            }
        }
    }

    private void validateLocationIds(List<Integer> locationIds, int storeId) {
        if (CollectionUtils.isEmpty(locationIds)) {
            return;
        }

        var locations = sapoClient.locations(locationIds, storeId);
        for (var locationId : locationIds) {
            var location = locations.stream()
                    .filter(l -> Objects.equals(l.getId(), locationId))
                    .findFirst().orElse(null);
            if (location == null) {
                throw new ConstrainViolationException("location_id", "location not found");
            }
            if (!location.isInventoryManagement()) {
                throw new ConstrainViolationException("inventory_management", "location doesn't manage inventory");
            }
        }
    }

    private List<InventoryItem> buildNewInventoryItems(List<Variant> variants, ProductCreateRequest productRequest, ProductId productId) {
        if (CollectionUtils.isEmpty(variants)) {
            return List.of();
        }

        var variantRequests = productRequest.getVariants();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        var variantIds = variants.stream().map(Variant::getId).collect(Collectors.toCollection(LinkedList::new));
        var inventoryItemIds = variants.stream().map(Variant::getInventoryItemId).collect(Collectors.toCollection(LinkedList::new));
        for (var variantRequest : variantRequests) {
            var variantIdentifyInfo = this.buildVariantIdentityInfo(variantRequest);
            boolean tracked = MANAGER.equals(variantRequest.getInventoryManagement());
            boolean requireShipping = variantRequest.isRequireShipping();

            var inventoryItem = new InventoryItem(
                    inventoryItemIds.removeFirst(),
                    productId,
                    variantIds.removeFirst(),
                    variantIdentifyInfo.getSku(),
                    variantIdentifyInfo.getBarcode(),
                    tracked,
                    requireShipping,
                    BigDecimal.ZERO
            );
            inventoryItems.add(inventoryItem);
        }
        return inventoryItems;
    }

    private List<Variant> buildNewVariants(Deque<Integer> generatedVariantIds, Deque<Integer> generatedInventoryItemIds, ProductCreateRequest productRequest, List<Image> images) {
        if (CollectionUtils.isEmpty(productRequest.getVariants())) {
            return List.of();
        }

        var variantRequests = productRequest.getVariants();
        var variantIds = new ArrayDeque<>(generatedVariantIds);
        var inventoryItemIds = new ArrayDeque<>(generatedInventoryItemIds);
        List<Variant> variants = new ArrayList<>();
        for (var request : variantRequests) {
            List<Integer> imagePositions = request.getImagePosition();
            List<Image> imageVariants = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(imagePositions)) {
                imageVariants = images.stream()
                        .filter(i -> imagePositions.contains(i.getPosition()))
                        .toList();
            }

            variants.add(buildNewVariant(request, variantIds.removeFirst(), inventoryItemIds.removeFirst(), imageVariants));
        }
        return variants;
    }

    private Variant buildNewVariant(ProductVariantRequest request, Integer variantId, Integer inventoryItemId, List<Image> imageVariants) {
        return new Variant(
                variantId,
                inventoryItemId,
                buildVariantIdentityInfo(request),
                buildVariantPricingInfo(request),
                buildVariantOptionInfo(request),
                buildVariantInventoryManagementInfo(request),
                buildVariantPhysicalInfo(request),
                request.getInventoryQuantity(),
                CollectionUtils.isNotEmpty(imageVariants)
                        ? imageVariants.stream().map(Image::getId).toList()
                        : List.of()
        );
    }

    private VariantPhysicalInfo buildVariantPhysicalInfo(ProductVariantRequest request) {
        return VariantPhysicalInfo.builder()
                .requireShipping(request.isRequireShipping())
                .weightUnit(request.getWeightUnit())
                .unit(request.getUnit())
                .weight(request.getWeight())
                .build();
    }

    private VariantInventoryManagementInfo buildVariantInventoryManagementInfo(ProductVariantRequest request) {
        return VariantInventoryManagementInfo.builder()
                .inventoryManagement(Optional.ofNullable(request.getInventoryManagement()).orElse("bizweb"))
                .inventoryPolicy(Optional.ofNullable(request.getInventoryPolicy()).orElse("deny"))
                .build();
    }

    private VariantOptionInfo buildVariantOptionInfo(ProductVariantRequest request) {
        return VariantOptionInfo.builder()
                .option1(Optional.ofNullable(request.getOption1()).orElse(VariantOptionInfo.DEFAULT_OPTION_VALUE))
                .option2(request.getOption2())
                .option3(request.getOption3())
                .build();
    }

    private VariantPricingInfo buildVariantPricingInfo(ProductVariantRequest request) {
        return VariantPricingInfo.builder()
                .price(Optional.ofNullable(request.getPrice()).orElse(BigDecimal.ZERO))
                .compareAtPrice(Optional.ofNullable(request.getCompareAtPrice()).orElse(BigDecimal.ZERO))
                .taxable(request.isTaxable())
                .build();
    }

    private VariantIdentityInfo buildVariantIdentityInfo(ProductVariantRequest request) {
        return VariantIdentityInfo.builder()
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .build();
    }

    private InventoryLevel defaultInventoryLevel(Variant variant, ProductId productId) {
        return InventoryLevel.builder()
                .id(idGenerator.generateInventoryLevelId())
                .storeId(productId.getStoreId())
                .inventoryItemId(variant.getInventoryItemId())
                .productId(productId.getId())
                .variantId(variant.getId())
                .locationId(getDefaultLocationId(productId.getStoreId()))
                .onHand(BigDecimal.ZERO)
                .available(BigDecimal.ZERO)
                .committed(BigDecimal.ZERO)
                .incoming(BigDecimal.ZERO)
                .build();
    }

    private int getDefaultLocationId(int storeId) {
        return 0;
    }

    private InventoryItem defaultInventoryItem(Variant variant, ProductId productId) {
        return InventoryItem.builder()
                .id(variant.getInventoryItemId())
                .storeId(productId.getStoreId())
                .productId(productId.getId())
                .variantId(variant.getId())
                .sku(variant.getIdentityInfo().getSku())
                .barcode(variant.getIdentityInfo().getBarcode())
                .tracked(false)
                .requireShipping(true)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();
    }

    private Variant defaultVariant(int variantId, int inventoryItemId, String unit) {
        return new Variant(
                variantId,
                inventoryItemId,
                new VariantIdentityInfo(),
                new VariantPricingInfo(),
                new VariantOptionInfo(),
                new VariantInventoryManagementInfo(),
                VariantPhysicalInfo.builder()
                        .unit(unit)
                        .build(),
                0,
                null
        );
    }

    private List<Image> buildNewImages(List<StoredImageResult> storedImages, List<ProductImageRequest> imageRequests) {
        if (CollectionUtils.isEmpty(storedImages)) {
            return List.of();
        }

        List<Image> images = new ArrayList<>();
        int imageCount = (int) storedImages.stream().filter(Objects::nonNull).count();
        var imageIds = idGenerator.generateImageIds(imageCount);
        List<Integer> positionExisted = new ArrayList<>();
        for (int i = 0; i < imageRequests.size(); i++) {
            var storedImage = storedImages.get(i);
            var imageRequest = imageRequests.get(i);
            if (storedImage == null) {
                continue;
            }

            int position = imageRequest.getPosition(); // lấy position đầu tiên
            if (positionExisted.contains(position) || position == Integer.MAX_VALUE) continue;
            positionExisted.add(position);

            String alt = imageRequest.getAlt();

            var image = new Image(
                    imageIds.removeFirst(),
                    position,
                    alt,
                    storedImage.getSrc(),
                    storedImage.getFileName(),
                    ImagePhysicalInfo.builder()
                            .size(storedImage.getSize())
                            .width(storedImage.getWidth())
                            .height(storedImage.getHeight())
                            .build());
            images.add(image);
        }
        return images;
    }

    private StoreDto getStoreById(Integer storeId) {
        var store = storeDao.findById(storeId);
        if (store != null) return store;
        throw new ConstrainViolationException("not_found", "store not found by id = " + storeId);
    }
}
