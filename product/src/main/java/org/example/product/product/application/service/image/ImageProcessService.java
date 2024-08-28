package org.example.product.product.application.service.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.product.product.application.model.FileUploadModel;
import org.example.product.product.application.model.ImageDownloaded;
import org.example.product.product.application.model.ResourceType;
import org.example.product.product.application.model.StoredImageResult;
import org.example.product.product.application.model.request.ProductImageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessService {

    private final Cloudinary cloudinary;

    public Pair<ResourceType, List<StoredImageResult>> process(int storeId, List<ProductImageRequest> images) throws ExecutionException, InterruptedException, IOException {
        return this.process(storeId, images, true, ResourceType.product);
    }

    private Pair<ResourceType, List<StoredImageResult>> process(int storeId, List<ProductImageRequest> images, boolean override, ResourceType type) throws ExecutionException, InterruptedException, IOException {
        // validate storeId;
        if (images == null || images.isEmpty()) return Pair.of(type, List.of());

        var fileUploads = prepareFileUploadModel(images, override);
        if (fileUploads.isEmpty()) return Pair.of(type, List.of());
        return this.multiUpload(storeId, fileUploads, type);
    }

    private Pair<ResourceType, List<StoredImageResult>> multiUpload(int storeId, List<FileUploadModel> fileUploads, ResourceType type) throws IOException {
        String folder = storeId + "/" + type.toString();
        List<CompletableFuture<StoredImageResult>> futures = fileUploads.stream()
                .filter(model -> model != null)
                .map(model -> {
                    var params = ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", model.getFileName(),
                            "resource_type", model.getContentType().split("/")[0]
                    );

                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            Map uploadResult = cloudinary.uploader()
                                    .upload(model.getBytes(), params);
                            StoredImageResult result = new StoredImageResult();
                            result.setSrc(Optional.ofNullable(uploadResult.get("url")).map(url -> (String) url).orElse(null));
                            result.setFileName(Optional.ofNullable(uploadResult.get("public_id")).map(p -> (String) p).orElse(null));
                            result.setSize(Optional.ofNullable(uploadResult.get("bytes")).map(size -> (int) size).orElse(null));
                            result.setWidth(Optional.ofNullable(uploadResult.get("width")).map(size -> (int) size).orElse(null));
                            result.setHeight(Optional.ofNullable(uploadResult.get("height")).map(size -> (int) size).orElse(null));
                            return result;
                        } catch (Exception e) {
                            log.error(e.getMessage());
                            return null;
                        }
                    });
                }).toList();
        List<StoredImageResult> results;
        results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        return Pair.of(type, results);
    }

    private List<FileUploadModel> prepareFileUploadModel(List<ProductImageRequest> images, boolean override) throws ExecutionException, InterruptedException, IOException {
        List<FileUploadModel> fileUploadModels = new ArrayList<>();

        var mapFuture = this.downloadAllSrc(images);
        List<String> existedFileNames = new ArrayList<>();
        for (var image : images) {
            var uploadType = image.getUploadType();
            if (uploadType == ProductImageRequest.UploadType.NONE) {
                fileUploadModels.add(null);
                continue;
            }

            var fileName = detectFileName(image);

            var imageDownloaded = mapFuture.get(image.getSrc());
            var contentType = detectContentType(uploadType, image, imageDownloaded);

            var binaryContent = detectBinaryContent(uploadType, image, imageDownloaded);

            if (!override) {
                fileName = enrichFileNameIfDuplicate(1, fileName, existedFileNames);
            }

            if (binaryContent != null && binaryContent.length <= 1024 * 1024 * 2) {
                var fileUpload = new FileUploadModel();
                fileUpload.setContentType(contentType);
                fileUpload.setFileName(fileName);
                fileUpload.setBytes(binaryContent);
                fileUploadModels.add(fileUpload);
                existedFileNames.add(fileName);
            } else {
                fileUploadModels.add(null);
            }
        }
        return fileUploadModels;
    }

    private String detectFileName(ProductImageRequest image) {
        String fileName = image.getFile() != null ? image.getFile().getOriginalFilename() : image.getFileName();
        if (StringUtils.isBlank(fileName) && StringUtils.isNotBlank(image.getSrc())) {
            try {
                URL path = new URL(image.getSrc());
                fileName = FilenameUtils.getName(path.getPath());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        if (StringUtils.isBlank(fileName)) {
            fileName = UUID.randomUUID().toString();
        }
        fileName = fileName.toLowerCase();
        return fileName;
    }

    private String detectContentType(ProductImageRequest.UploadType uploadType, ProductImageRequest image, ImageDownloaded imageDownloaded) {
        if (StringUtils.isNotBlank(image.getFileName())) {
            String contentType = FileUtils.getContentType(image.getFileName());
            if (StringUtils.isNotBlank(contentType)) return contentType;
        }

        if (uploadType == ProductImageRequest.UploadType.BASE64) {
            return FileUtils.getContentTypeFromBase64(image.getBase64());
        }

        if (uploadType == ProductImageRequest.UploadType.FILE) {
            return image.getFile().getContentType();
        }

        if (uploadType == ProductImageRequest.UploadType.URL && imageDownloaded != null) {
            return imageDownloaded.getContentType();
        }

        return null;
    }

    private byte[] detectBinaryContent(ProductImageRequest.UploadType uploadType, ProductImageRequest image, ImageDownloaded imageDownloaded) throws IOException {
        if (uploadType == ProductImageRequest.UploadType.BASE64) {
            return Base64.decodeBase64(image.getBase64());
        }

        if (uploadType == ProductImageRequest.UploadType.FILE) {
            return image.getFile().getBytes();
        }

        if (uploadType == ProductImageRequest.UploadType.URL && imageDownloaded != null) {
            return imageDownloaded.getBytes();
        }

        return null;
    }

    private String enrichFileNameIfDuplicate(int storeId, String fileName, List<String> existedFileNames) {
        String ext = FilenameUtils.getExtension(fileName);
        int i = 0;
        while (i < 4) {
            i++;
            var fileNameTemp = fileName;
            var existedImage = getImageInCloud(storeId, fileName);
            var fileNameExists = existedFileNames.stream().anyMatch(m -> StringUtils.equals(m, fileNameTemp));
            if (existedImage == null || !fileNameExists) {
                break;
            }
            if (i == 4) {
                throw new IllegalArgumentException("please change file name");
            }

            String randomUUID = UUID.randomUUID().toString();
            int lengthUUID = randomUUID.length();
            String baseName = FilenameUtils.getBaseName(fileName);
        }
        return fileName;
    }

    private String getImageInCloud(int storeId, String fileName) {
        return null;
    }

    private LinkedHashMap<String, ImageDownloaded> downloadAllSrc(List<ProductImageRequest> images) throws ExecutionException, InterruptedException {
        var mapFuture = new LinkedHashMap<String, CompletableFuture<ImageDownloaded>>(images.size());
        var downloadSrcs = images.stream()
                .filter(i -> (i.getFile() == null)
                        && StringUtils.isBlank(i.getBase64())
                        && StringUtils.isNotBlank(i.getSrc()))
                .map(ProductImageRequest::getSrc).distinct()
                .toList();
        for (var src : downloadSrcs) {
            try {
                var urlBuilder = UriComponentsBuilder.fromHttpUrl(URLDecoder.decode(src, StandardCharsets.UTF_8));
                var encodeSrc = urlBuilder.build(false).encode().toUriString();
                mapFuture.put(src, ImageDownloadUtils.getImageFromUrlAsync(encodeSrc));
            } catch (Exception e) {
                mapFuture.put(src, ImageDownloadUtils.getImageFromUrlAsync(src));
            }
        }

        AsyncUtils.allOf(mapFuture.values()).get();

        var result = new LinkedHashMap<String, ImageDownloaded>(images.size());
        for (var entry : mapFuture.entrySet()) {
            ImageDownloaded image = null;
            if (entry.getValue().isDone() && !entry.getValue().isCompletedExceptionally()) {
                image = entry.getValue().get();
            }
            result.put(entry.getKey(), image);
        }
        return result;
    }

}
