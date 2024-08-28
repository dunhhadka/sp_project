package org.example.product.product.application.service.product;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.model.StoredImageResult;
import org.example.product.product.application.model.request.ProductImageRequest;
import org.example.product.product.application.model.request.ProductRequest;
import org.example.product.product.application.model.request.ProductVariantRequest;
import org.example.product.product.application.service.image.ImageProcessService;
import org.example.product.product.domain.product.model.*;
import org.example.product.product.domain.product.repository.InventoryItemRepository;
import org.example.product.product.domain.product.repository.InventoryLevelRepository;
import org.example.product.product.domain.product.repository.ProductRepository;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductWriteService {

    private final String MANAGER = "thanhhoa";

    private final ProductIdGenerator idGenerator;

    private final ImageProcessService imageProcessService;

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLevelRepository inventoryLevelRepository;

    @Transactional
    public void create(ProductRequest request) throws ExecutionException, InterruptedException, IOException {
        int storeId = 1;
        // TODO: validate storeId

        // images
        var storeImages = imageProcessService.process(storeId, request.getImages());
        var images = this.buildNewImages(request.getImages(), storeImages.getValue());

        int productId = idGenerator.generateProductId();

        // variants
        List<Variant> variants = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        List<InventoryLevel> inventoryLevels = new ArrayList<>();
        if (CollectionUtils.isEmpty(request.getVariants())) {
            var variant = buildDefaultVariant(
                    idGenerator.generateVariantId(),
                    idGenerator.generateInventoryItemId(),
                    request.getDefaultVariantUnit()
            );
            variants.add(variant);
            inventoryItems.add(buildDefaultInventoryItem(variant, storeId, productId));
            inventoryLevels.add(buildInventoryLevel(variant, storeId, productId));
        } else {
            var generatedVariantIds = idGenerator.generateVariantIds(request.getVariants().size());
            var generatedInventoryItemIds = idGenerator.generateInventoryItemIds(request.getVariants().size());
            inventoryItems = buildInventoryItems(request.getVariants(), storeId, productId, generatedInventoryItemIds, generatedVariantIds);
            variants = buildNewVariants(request.getVariants(), storeImages.getRight(), images, inventoryItems);
            inventoryLevels = buildInventoryLevels(request.getVariants(), storeId, productId, inventoryItems);
        }

        inventoryItemRepository.saveAll(inventoryItems);

        if (CollectionUtils.isNotEmpty(inventoryLevels)) {
            inventoryLevelRepository.saveAll(inventoryLevels);
        }

        var product = new Product(
                idGenerator,
                new ProductId(storeId, productId),
                request.getName(),
                buildProductGeneralInfo(ProductGeneralInfo.builder(), request),
                convertTagStringToList(request.getTags()),
                variants,
                images
        );
    }

    private ProductGeneralInfo buildProductGeneralInfo(ProductGeneralInfo.ProductGeneralInfoBuilder builder, ProductRequest request) {
        return builder
                .title(request.getTitle())
                .productType(request.getProductType())
                .summary(request.getSummary())
                .description(request.getDescription())
                .build();
    }

    private List<String> convertTagStringToList(String tags) {
        if (StringUtils.isBlank(tags)) return List.of();
        return Arrays.stream(StringUtils.split(tags.trim(), ","))
                .filter(StringUtils::isNotBlank)
                .distinct().toList();
    }

    private List<Image> buildNewImages(List<ProductImageRequest> imageRequests, List<StoredImageResult> storedImageResults) {
        if (CollectionUtils.isEmpty(storedImageResults)) return List.of();

        var imageCount = storedImageResults.stream().filter(Objects::nonNull).count();
        var generatedIds = idGenerator.generateImageIds(imageCount);
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < storedImageResults.size(); i++) {
            var imageReq = storedImageResults.get(i);
            if (imageReq == null) continue;
            var imageCreateReq = imageRequests.get(i);
            var image = new Image(
                    generatedIds.removeFirst(),
                    imageReq.getSrc(),
                    imageReq.getFileName(),
                    imageCreateReq.getAlt(),
                    imageCreateReq.getPosition(),
                    ImagePhysicalInfo.builder()
                            .size(imageReq.getSize())
                            .width(imageReq.getWidth())
                            .height(imageReq.getHeight())
                            .build()
            );
            images.add(image);
        }
        return images;
    }

    private Variant buildDefaultVariant(Integer variantId, Integer inventoryItemId, String unit) {
        return new Variant(
                variantId,
                inventoryItemId,
                new VariantIdentityInfo(),
                new VariantPricingInfo(),
                new VariantOptionInfo(),
                new VariantInventoryInfo(),
                VariantPhysicalInfo.builder().unit(unit).build(),
                null
        );
    }

    private InventoryItem buildDefaultInventoryItem(Variant variant, int storeId, int productId) {
        var inventoryItem = new InventoryItem();
        inventoryItem.setId(variant.getInventoryItemId());
        inventoryItem.setStoreId(storeId);
        inventoryItem.setProductId(productId);
        inventoryItem.setSku(variant.getIdentityInfo().getSku());
        inventoryItem.setRequireShipping(variant.getInventoryInfo().getRequireShipping());
        inventoryItem.setTracked(false);
        inventoryItem.setCreatedAt(Instant.now());
        inventoryItem.setModifiedAt(Instant.now());
        return inventoryItem;
    }

    private InventoryLevel buildInventoryLevel(Variant variant, int storeId, int productId) {
        var defaultLocationId = 1;
        return new InventoryLevel(
                storeId, productId, variant.getId(), variant.getInventoryItemId(), defaultLocationId,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    private List<InventoryItem> buildInventoryItems(
            List<ProductVariantRequest> variantRequests,
            int storeId, int productId,
            Deque<Integer> inventoryItemIds, Deque<Integer> variantIds
    ) {
        if (CollectionUtils.isEmpty(variantRequests)) return Collections.emptyList();

        var inventoryItems = new ArrayList<InventoryItem>();
        for (var variantReq : variantRequests) {
            var variantIdentityInfo = buildVariantIdentityInfo(VariantIdentityInfo.builder(), variantReq);
            var tracked = StringUtils.equals(variantReq.getInventoryManagement(), MANAGER);
            var requireShipping = variantReq.getRequireShipping();
            var inventoryItem = new InventoryItem(
                    inventoryItemIds.removeFirst(),
                    storeId,
                    productId,
                    variantIds.removeFirst(),
                    variantIdentityInfo.getSku(),
                    variantIdentityInfo.getBarcode(),
                    tracked,
                    requireShipping
            );
            inventoryItems.add(inventoryItem);
        }

        return inventoryItems;
    }

    private VariantIdentityInfo buildVariantIdentityInfo(
            VariantIdentityInfo.VariantIdentityInfoBuilder builder,
            ProductVariantRequest variantReq
    ) {
        builder.sku(variantReq.getSku())
                .barcode(variantReq.getBarcode());
        return builder.build();
    }

    private List<Variant> buildNewVariants(List<ProductVariantRequest> variantRequests, List<StoredImageResult> storeImages, List<Image> images, List<InventoryItem> inventoryItems) {
        if (CollectionUtils.isEmpty(variantRequests)) return List.of();
        var inventoryItemIds = inventoryItems.stream().map(InventoryItem::getId).collect(Collectors.toCollection(LinkedList::new));
        var variantIds = inventoryItems.stream().map(InventoryItem::getVariantId).collect(Collectors.toCollection(LinkedList::new));
        var variants = new ArrayList<Variant>();
        for (var variantReq : variantRequests) {
            Image image = null;
            var imagePosition = variantReq.getImagePosition();
            if (imagePosition != null && imagePosition < storeImages.size()) {
                var storedImage = storeImages.get(imagePosition);
                if (storedImage != null) {
                    image = images.stream()
                            .filter(i -> StringUtils.equals(i.getSrc(), storedImage.getSrc()))
                            .findFirst().orElse(null);
                }
            }
            variants.add(buildNewVariant(variantIds.removeFirst(), inventoryItemIds.removeFirst(), variantReq, image));
        }
        return variants;
    }

    private Variant buildNewVariant(
            Integer variantId, Integer inventoryItemId,
            ProductVariantRequest variantReq, Image image
    ) {
        return new Variant(
                variantId, inventoryItemId,
                buildVariantIdentityInfo(VariantIdentityInfo.builder(), variantReq),
                buildVariantPricingInfo(VariantPricingInfo.builder(), variantReq),
                buildVariantOptionInfo(VariantOptionInfo.builder(), variantReq),
                buildVariantIventoryInfo(VariantInventoryInfo.builder(), variantReq),
                buildVariantPhysicalInfo(VariantPhysicalInfo.builder(), variantReq),
                image != null ? image.getId() : null
        );
    }

    private VariantPricingInfo buildVariantPricingInfo(VariantPricingInfo.VariantPricingInfoBuilder builder, ProductVariantRequest variantReq) {
        return builder
                .price(Optional.ofNullable(variantReq.getPrice()).orElse(BigDecimal.ZERO))
                .compareAtPrice(variantReq.getCompareAtPrice())
                .taxable(variantReq.getTaxable())
                .build();
    }

    private VariantOptionInfo buildVariantOptionInfo(VariantOptionInfo.VariantOptionInfoBuilder builder, ProductVariantRequest variantReq) {
        if (StringUtils.isNotBlank(variantReq.getOption1())) {
            builder.option1(variantReq.getOption1());
        } else {
            builder.option1(VariantOptionInfo.DEFAULT_OPTION_VARIANT);
        }
        return builder
                .option2(variantReq.getOption2())
                .option3(variantReq.getOption3())
                .build();
    }

    private VariantInventoryInfo buildVariantIventoryInfo(VariantInventoryInfo.VariantInventoryInfoBuilder builder, ProductVariantRequest variantReq) {
        return builder
                .inventoryManagement(variantReq.getInventoryManagement())
                .inventoryQuantity(variantReq.getInventoryQuantity())
                .oldInventoryQuantity(variantReq.getOldInventoryQuantity())
                .quantityAdjustable(variantReq.getQuantityAdjustable())
                .requireShipping(variantReq.getRequireShipping())
                .build();
    }

    private VariantPhysicalInfo buildVariantPhysicalInfo(VariantPhysicalInfo.VariantPhysicalInfoBuilder builder, ProductVariantRequest variantReq) {
        return builder
                .weight(variantReq.getWeight())
                .unit(variantReq.getUnit())
                .weightUnit(variantReq.getWeightUnit())
                .build();
    }

    private List<InventoryLevel> buildInventoryLevels(List<ProductVariantRequest> variantRequests, int storeId, int productId, List<InventoryItem> inventoryItems) {
        if (CollectionUtils.isEmpty(variantRequests)) return Collections.emptyList();

        List<Integer> locationIds = variantRequests.stream()
                .flatMap(v -> v.getInventoryQuantities().stream())
                .map(i -> Math.toIntExact(i.getLocationId()))
                .distinct().toList();

        var inventoryItemIds = inventoryItems.stream().map(InventoryItem::getId).collect(Collectors.toCollection(LinkedList::new));
        var variantIds = inventoryItems.stream().map(InventoryItem::getVariantId).collect(Collectors.toCollection(LinkedList::new));
        var inventoryLevels = new ArrayList<InventoryLevel>();
        int locationDefault = 1;
        for (var variantReq : variantRequests) {
            var inventoryItemId = inventoryItemIds.removeFirst();
            var variantId = variantIds.removeFirst();
            var inventoryItem = inventoryItems.stream()
                    .filter(i -> i.getId() == inventoryItemId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("error"));
            var tracked = StringUtils.equals(variantReq.getInventoryManagement(), MANAGER);
            if (CollectionUtils.isEmpty(variantReq.getInventoryQuantities())) {
                var inventoryQuantity = tracked ? Optional.ofNullable(variantReq.getInventoryQuantity()).orElse(0) : 0;
                var inventoryLevel = new InventoryLevel(storeId, productId, variantId, inventoryItemId, locationDefault,
                        BigDecimal.valueOf(inventoryQuantity), BigDecimal.valueOf(inventoryQuantity), BigDecimal.ZERO, BigDecimal.ZERO);
                inventoryLevels.add(inventoryLevel);
            } else {
                buildInventoryLevelForVariant(inventoryLevels, variantReq, storeId, productId, variantId, inventoryItemId, tracked);
            }
        }
        return inventoryLevels;
    }

    private void buildInventoryLevelForVariant(
            List<InventoryLevel> inventoryLevels,
            ProductVariantRequest variantReq,
            int storeId, int productId, int variantId, int inventoryItemId,
            boolean tracked
    ) {
        validateOnHand(variantReq);
        for (var inventoryQuantityReq : variantReq.getInventoryQuantities()) {
            var locationId = inventoryQuantityReq.getLocationId();
            BigDecimal quantity = tracked ? inventoryQuantityReq.getOnHand() : BigDecimal.ZERO;
            BigDecimal defaultQuantity = BigDecimal.ZERO;
            var inventoryLevel = new InventoryLevel(storeId, productId, variantId, inventoryItemId, (int) locationId,
                    quantity, quantity, defaultQuantity, defaultQuantity
            );
            inventoryLevels.add(inventoryLevel);
        }
    }

    private void validateOnHand(ProductVariantRequest variantReq) {
        for (var inventoryQuantity : variantReq.getInventoryQuantities()) {
            if (inventoryQuantity.getOnHand().signum() < 0) {
                throw new IllegalArgumentException("invalid onHand");
            }
        }
    }

    @Transactional
    public void update(ProductId productId, ProductRequest request) throws IOException, ExecutionException, InterruptedException {
        var product = productRepository.findById(productId);
        if (product == null) throw new IllegalArgumentException("product not found");

        int storeId = 1;
        this.processImageForUpdate(storeId, product, request);

        var addAndUpdateVariants = buildAddAndUpdateVariants(product, request);

        List<InventoryItem> inventoryItemInserts = new ArrayList<>();
        List<InventoryItem> inventoryItemUpdates = new ArrayList<>();
        List<InventoryLevel> inventoryLevelInserts = new ArrayList<>();
        List<InventoryLevel> inventoryLevelUpdates = new ArrayList<>();
        var variantAddAndUpdateRequests = buildVariantAddAndUpdateRequests(product, request);
        var addVariantRequests = variantAddAndUpdateRequests.getFirst();
        var updateVariantRequests = variantAddAndUpdateRequests.getSecond();
        if (CollectionUtils.isNotEmpty(addVariantRequests)) {
            var newVariants = addAndUpdateVariants.getFirst();
            var inventoryItemIds = newVariants.stream().map(Variant::getInventoryItemId).collect(Collectors.toCollection(LinkedList::new));
            var variantIds = newVariants.stream().map(Variant::getId).collect(Collectors.toCollection(LinkedList::new));
            inventoryItemInserts.addAll(buildInventoryItems(addVariantRequests, storeId, productId.getId(), inventoryItemIds, variantIds));
            inventoryLevelInserts.addAll(buildInventoryLevels(addVariantRequests, storeId, productId.getId(), inventoryItemInserts));
        }
        if (CollectionUtils.isNotEmpty(updateVariantRequests)) {
            var variantIds = new ArrayList<>(addAndUpdateVariants.getSecond().keySet());
            inventoryItemUpdates.addAll(buildInventoryItemUpdates(updateVariantRequests, storeId, variantIds));
            var locationIdDefault = 1;
            var inventoryItemUpdateIds = inventoryItemUpdates.stream().map(InventoryItem::getId).toList();
            var inventoryLevelOlds = inventoryLevelRepository.findByStoreIdAndInventoryItemIdIn(storeId, inventoryItemUpdateIds);
            for (var variantId : variantIds) {
                var variantUpdateInfos = addAndUpdateVariants.getSecond().get(variantId);
                List<InventoryLevel> inventoryLevelOldVariants = inventoryLevelOlds.stream().filter(i -> i.getVariantId() == variantId).toList();
                var inventoryLevelDefault = inventoryLevelOldVariants.stream().filter(i -> i.getLocationId() == locationIdDefault).findFirst().orElse(null);
            }
        }
    }

    private List<InventoryItem> buildInventoryItemUpdates(
            List<ProductVariantRequest> variantRequests,
            int storeId, List<Integer> variantIds
    ) {
        if (CollectionUtils.isEmpty(variantRequests)) return List.of();

        var inventoryItems = new ArrayList<InventoryItem>();
        var currentInventoryItems = inventoryItemRepository.getByStoreIdAndVariantIdIn(storeId, variantIds);
        for (int i = 0; i < variantRequests.size(); i++) {
            var request = variantRequests.get(i);
            var variantId = request.getId();
            var inventoryItem = currentInventoryItems.stream()
                    .filter(ii -> ii.getVariantId() == variantId)
                    .findFirst().orElse(null);
            if (inventoryItem == null) continue;

            if (!StringUtils.equals(inventoryItem.getSku(), request.getSku())) {
                inventoryItem.setSku(request.getSku());
                inventoryItem.setUpdate(true);
            }
        }
        return inventoryItems;
    }

    private Pair<List<Variant>, LinkedHashMap<Integer, VariantUpdateInfo>> buildAddAndUpdateVariants(Product product, ProductRequest request) {
        var variantAddAndUpdateRequests = buildVariantAddAndUpdateRequests(product, request);

        var newVariants = this.buildNewVariantsForUpdate(variantAddAndUpdateRequests.getFirst(), product.getImages());

        var currentVariants = product.getVariants();
        var variantUpdateInfos = this.buildVariantUpdateInfos(variantAddAndUpdateRequests.getSecond(), currentVariants, product.getImages());

        return Pair.of(newVariants, variantUpdateInfos);
    }

    private Pair<List<ProductVariantRequest>, List<ProductVariantRequest>> buildVariantAddAndUpdateRequests(
            Product product, ProductRequest request
    ) {
        if (CollectionUtils.isEmpty(request.getVariants())) return Pair.of(List.of(), List.of());

        var variantRequests = request.getVariants();
        var currentVariants = product.getVariants();
        var currentVariantIds = currentVariants.stream().map(Variant::getId).toList();
        var addVariantRequests = new ArrayList<ProductVariantRequest>();
        var updateVariantRequests = new ArrayList<ProductVariantRequest>();
        for (var variantReq : variantRequests) {
            if (variantReq.getId() != null && currentVariantIds.contains(variantReq.getId())) {
                addVariantRequests.add(variantReq);
            } else {
                updateVariantRequests.add(variantReq);
            }
        }

        return Pair.of(addVariantRequests, updateVariantRequests);
    }

    private List<Variant> buildNewVariantsForUpdate(List<ProductVariantRequest> variantRequests, List<Image> images) {
        if (CollectionUtils.isEmpty(variantRequests)) return List.of();

        var inventoryItemIds = idGenerator.generateInventoryItemIds(variantRequests.size());
        var variantIds = idGenerator.generateVariantIds(variantRequests.size());
        List<Variant> variants = new ArrayList<>();
        for (var variantReq : variantRequests) {
            Image image = null;
            if (variantReq.getImageId() != null) {
                image = images.stream()
                        .filter(i -> i.getId() == variantReq.getImageId())
                        .findFirst().orElse(null);
            }
            variants.add(buildNewVariant(variantIds.removeFirst(), inventoryItemIds.removeFirst(), variantReq, image));
        }
        return variants;
    }

    private LinkedHashMap<Integer, VariantUpdateInfo> buildVariantUpdateInfos(
            List<ProductVariantRequest> variantRequests,
            List<Variant> variants, List<Image> images
    ) {
        if (CollectionUtils.isEmpty(variantRequests)) return new LinkedHashMap<>();

        var variantUpdateInfos = new LinkedHashMap<Integer, VariantUpdateInfo>();
        for (var variantReq : variantRequests) {
            var variantId = variantReq.getId();
            if (variantId == null) continue;
            var variant = variants.stream()
                    .filter(v -> v.getId() == variantId)
                    .findFirst().orElse(null);
            if (variant == null) continue;

            Image image = null;
            if (variantReq.getImageId() != null) {
                image = images.stream()
                        .filter(i -> i.getId() == variantReq.getImageId())
                        .findFirst().orElse(null);
            }

            variantUpdateInfos.put(variantId, buildVariantUpdateInfo(variantReq, variant, image));
        }

        return variantUpdateInfos;
    }

    private VariantUpdateInfo buildVariantUpdateInfo(
            ProductVariantRequest variantReq, Variant variant, Image image
    ) {
        var updateInfoBuilder = VariantUpdateInfo.builder();
        updateInfoBuilder
                .identityInfo(buildVariantIdentityInfo(variant.getIdentityInfo().toBuilder(), variantReq))
                .pricingInfo(buildVariantPricingInfo(variant.getPricingInfo().toBuilder(), variantReq))
                .optionInfo(buildVariantOptionInfo(variant.getOptionInfo().toBuilder(), variantReq))
                .inventoryInfo(buildVariantIventoryInfo(variant.getInventoryInfo().toBuilder(), variantReq))
                .physicalInfo(buildVariantPhysicalInfo(variant.getPhysicalInfo().toBuilder(), variantReq))
                .imageId(image == null ? null : image.getId());
        return updateInfoBuilder.build();
    }

    private void processImageForUpdate(int storeId, Product product, ProductRequest request) throws IOException, ExecutionException, InterruptedException {
        var imageRequests = CollectionUtils.isEmpty(request.getImages()) ? new ArrayList<ProductImageRequest>() : request.getImages();

        var images = product.getImages();
        var imageIds = images.stream().map(Image::getId).toList();
        List<ProductImageRequest> addImageRequests = new ArrayList<>();
        List<ProductImageRequest> updateImageRequests = new ArrayList<>();
        for (var imageReq : imageRequests) {
            if (imageReq.getId() != null && imageIds.contains(imageReq.getId())) {
                updateImageRequests.add(imageReq);
            } else {
                addImageRequests.add(imageReq);
            }
        }

        var newStoredImage = this.imageProcessService.process(storeId, addImageRequests);
        var newImages = buildNewImages(addImageRequests, newStoredImage.getRight());

        var updateImageInfos = buildUpdateImageInfos(product, images, updateImageRequests);

        product.setImages(newImages, updateImageInfos);

        if (CollectionUtils.isNotEmpty(request.getVariants())) {
            for (var variantReq : request.getVariants()) {
                Integer imagePosition = variantReq.getImagePosition();
                if (imagePosition == null) continue;
                if (imagePosition >= 0 && imagePosition < product.getImages().size()) {
                    variantReq.setImageId(product.getImages().get(imagePosition).getId());
                }
            }
            request.getVariants().stream()
                    .filter(v -> v.getImageId() != null && v.getId() != null)
                    .collect(Collectors.groupingBy(
                            ProductVariantRequest::getImageId,
                            Collectors.mapping(ProductVariantRequest::getId, Collectors.toList())))
                    .forEach(product::setImageToVariants);
        }
    }

    private LinkedHashMap<Integer, ImageUpdatableInfo> buildUpdateImageInfos(
            Product product, List<Image> images,
            List<ProductImageRequest> updateImageRequests
    ) {
        if (CollectionUtils.isEmpty(updateImageRequests)) return null;

        var updateImageInfos = new LinkedHashMap<Integer, ImageUpdatableInfo>();
        for (var imageReq : updateImageRequests) {
            var imageId = imageReq.getId();
            var image = images.stream()
                    .filter(i -> ObjectUtils.nullSafeEquals(imageId, i.getId()))
                    .findFirst().orElse(null);
            if (image == null) {
                log.error("image not found by id = " + imageId);
                continue;
            }
            updateImageInfos.put(imageId, buildUpdateImageInfo(image, imageReq));

            if (CollectionUtils.isNotEmpty(imageReq.getVariantIds())) {
                product.setImageIdToVariants(image.getId(), imageReq.getVariantIds());
            }
        }

        return updateImageInfos;
    }

    private ImageUpdatableInfo buildUpdateImageInfo(Image image, ProductImageRequest imageRequest) {
        return ImageUpdatableInfo.builder()
                .alt(imageRequest.getAlt())
                .position(imageRequest.getPosition())
                .build();
    }

    @Transactional
    public void createVariant(ProductId productId, ProductVariantRequest request) {
        var product = productRepository.findById(productId);
        if (product == null) throw new IllegalArgumentException("not found");

        Image image = null;
        if (request.getImagePosition() != null) {
            var position = request.getImagePosition();
            if (position >= 0 && position < product.getImages().size()) {
                image = product.getImages().get(position);
            }
        }

        var variant = buildNewVariant(idGenerator.generateVariantId(), idGenerator.generateInventoryItemId(), request, image);
    }
}
