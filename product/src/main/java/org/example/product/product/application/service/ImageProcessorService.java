package org.example.product.product.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.exception.ConstrainViolationException;
import org.example.product.product.application.model.FileUploadModel;
import org.example.product.product.application.model.ImageDownloaded;
import org.example.product.product.application.model.ProductImageRequest;
import org.example.product.product.application.model.StoredImageResult;
import org.example.product.product.application.model.enums.ResourceType;
import org.example.product.product.application.utils.FileUtils;
import org.example.product.product.application.utils.ImageDownloadUtils;
import org.example.product.product.infrastructure.data.dao.ImageDao;
import org.example.product.product.infrastructure.data.dto.ImageDto;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ImageProcessorService {

    public static final int MAX_S3_SIZE = 2097152;

    private final ImageDao imageDao;

    public List<StoredImageResult> storeImages(Integer storeId, List<ProductImageRequest> images) throws ExecutionException, InterruptedException, IOException {
        return this.storeImages(storeId, images, ResourceType.product);
    }

    private List<StoredImageResult> storeImages(Integer storeId, List<ProductImageRequest> images, ResourceType type) throws ExecutionException, InterruptedException, IOException {
        if (CollectionUtils.isEmpty(images)) {
            return Collections.emptyList();
        }

        var fileUploads = prepareFileUploads(storeId, images, false);
        var keys = multiUploads(storeId, fileUploads);

        List<StoredImageResult> results = new ArrayList<>();
        for (int i = 0; i < fileUploads.size(); i++) {
            var upload = fileUploads.get(i);
            String key = keys.get(i);
            if (upload == null || key == null) {
                results.add(null);
                continue;
            }
            results.add(getImageResult(upload, key, images.get(i)));
        }
        return results;
    }

    private StoredImageResult getImageResult(FileUploadModel upload, String key, ProductImageRequest request) throws IOException {
        var storedImage = StoredImageResult.builder()
                .fileName(upload.getFileName())
                .contentType(upload.getContentType())
                .size(upload.getBytes().length)
                .position(request.getPosition())
                .src(key);
        var imageInfo = ImageIO.read(new ByteArrayInputStream(upload.getBytes()));
        if (imageInfo != null) {
            storedImage
                    .width(imageInfo.getWidth())
                    .height(imageInfo.getHeight());
        }
        return storedImage.build();
    }

    private LinkedHashMap<Integer, String> multiUploads(Integer storeId, List<FileUploadModel> fileUploads) {
        var keys = new LinkedHashMap<Integer, String>();
        for (int i = 0; i < fileUploads.size(); i++) {
            var upload = fileUploads.get(i);
            if (upload == null) continue;
            String key = "key"; // generate key
            String result = null; // upload
            keys.put(i, key);
        }

        return keys;
    }

    /**
     * return: List file model tương ứng với request => nếu request không hợp lệ => file model = null
     */
    private List<FileUploadModel> prepareFileUploads(Integer storeId, List<ProductImageRequest> imagesRequests, boolean allowDuplicate) throws ExecutionException, InterruptedException, IOException {
        List<FileUploadModel> results = new ArrayList<>();

        var resourceMap = downloadAllResource(imagesRequests);
        List<String> fileNameExisted = new ArrayList<>();
        for (var imageRequest : imagesRequests) {
            if (imageRequest == null || imageRequest.getUploadType() == ProductImageRequest.UploadType.NONE) {
                results.add(null);
                continue;
            }

            String fileName = detectFileName(imageRequest, resourceMap);
            String contentType = detectContentType(imageRequest, fileName, resourceMap);
            byte[] binaryFile = detectBinaryFile(imageRequest, resourceMap);

            if (!allowDuplicate) {
                fileName = enrichFileNameWithNotDuplicate(storeId, fileName, fileNameExisted);
            }

            if (binaryFile != null && binaryFile.length <= MAX_S3_SIZE) {
                results.add(FileUploadModel.builder()
                        .fileName(fileName)
                        .contentType(contentType)
                        .bytes(binaryFile)
                        .build());
                fileNameExisted.add(fileName);
            } else {
                results.add(null);
            }
        }
        return results;
    }

    private String enrichFileNameWithNotDuplicate(Integer storeId, String fileName, List<String> fileNameExisted) {
        String ext = FilenameUtils.getExtension(fileName);
        int i = 0;
        while (i <= 4) {
            i++;
            List<ImageDto> images = imageDao.getByFileName(storeId, fileName);
            boolean isExisted = fileNameExisted.contains(fileName);
            if (CollectionUtils.isNotEmpty(images) || isExisted) {
                continue;
            }

            if (i == 4) {
                throw new ConstrainViolationException("file_name", "exited. please change file name");
            }

            String uuidFileName = UUID.randomUUID().toString();
            String baseFileName = uuidFileName.replaceAll("-", "");
            fileName = baseFileName + "." + ext;
        }
        return fileName;
    }

    private byte[] detectBinaryFile(ProductImageRequest imageRequest, LinkedHashMap<String, ImageDownloaded> resourceMap) throws IOException {
        return switch (imageRequest.getUploadType()) {
            case BASE64 -> Base64Coder.decode(imageRequest.getBase64());
            case FILE -> imageRequest.getFile().getBytes();
            case SRC -> {
                var imageDownloaded = resourceMap.get(imageRequest.getSrc());
                if (imageDownloaded != null) {
                    yield imageDownloaded.getBytes();
                }
                yield null;
            }
            case NONE -> null;
        };
    }

    private String detectContentType(ProductImageRequest imageRequest, String fileName, LinkedHashMap<String, ImageDownloaded> resourceMap) {
        var uploadType = imageRequest.getUploadType();
        String contentType = null;

        if (uploadType == ProductImageRequest.UploadType.BASE64) {
            if (StringUtils.isNotBlank(fileName)) {
                contentType = FileUtils.getContentType(fileName);
            }
            if (StringUtils.isBlank(contentType)) {
                return FileUtils.getContentTypeFromBase64(imageRequest.getBase64());
            }
        }

        if (StringUtils.isBlank(contentType) && imageRequest.getFile() != null) {
            contentType = imageRequest.getFile().getContentType();
        }

        if (StringUtils.isBlank(contentType) && uploadType == ProductImageRequest.UploadType.SRC) {
            var imageDownloaded = resourceMap.get(imageRequest.getSrc());
            if (imageDownloaded != null) {
                contentType = imageDownloaded.getContentType();
            }
        }

        if (StringUtils.isNotBlank(contentType)) return contentType;

        return null;
    }

    private String detectFileName(ProductImageRequest imageRequest, LinkedHashMap<String, ImageDownloaded> resourceMap) {
        String fileName = imageRequest.getFile() != null ? imageRequest.getFile().getOriginalFilename() : imageRequest.getFileName();

        if (StringUtils.isBlank(fileName) && imageRequest.getUploadType() == ProductImageRequest.UploadType.SRC) {
            String src = imageRequest.getSrc();
            if (resourceMap.containsKey(src)) {
                fileName = FilenameUtils.getName(src);
            }
        }

        if (StringUtils.isBlank(fileName)) {
            fileName = UUID.randomUUID().toString().replaceAll("-", "");
        }

        fileName = fileName.toLowerCase();
        return fileName;
    }

    private LinkedHashMap<String, ImageDownloaded> downloadAllResource(List<ProductImageRequest> images) throws ExecutionException, InterruptedException {
        // sử dụng completableFuture để tối ưu gọi resource
        var mapFuture = new LinkedHashMap<String, CompletableFuture<ImageDownloaded>>();

        var downloadSrcs = images.stream()
                .filter(Objects::nonNull)
                .filter(req -> req.getUploadType() == ProductImageRequest.UploadType.SRC)
                .map(ProductImageRequest::getSrc)
                .distinct().toList();
        for (var src : downloadSrcs) {
            try {
                var uri = UriComponentsBuilder.fromHttpUrl(URLDecoder.decode(src, StandardCharsets.UTF_8)).build(false);
                var encodeSrc = uri.toUriString();
                mapFuture.put(src, ImageDownloadUtils.getImageFromUrlAsync(encodeSrc));
            } catch (IllegalArgumentException exception) {
                mapFuture.put(src, ImageDownloadUtils.getImageFromUrlAsync(src));
            }
        }

        allOf(mapFuture.values());

        var mapResult = new LinkedHashMap<String, ImageDownloaded>();
        for (var entry : mapFuture.entrySet()) {
            var src = entry.getKey();
            var result = entry.getValue();
            if (result.isDone() && !result.isCompletedExceptionally()) {
                mapResult.put(src, result.get());
            }
        }
        return mapResult;
    }

    public static <T> CompletableFuture<List<T>> allOf(Collection<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        return allFuturesResult.thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }
}
