package org.example.product.product.application.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.example.product.product.application.model.ImageDownloaded;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageDownloadUtils {

    public static CompletableFuture<ImageDownloaded> getImageFromUrlAsync(String url) {
        return CompletableFuture.supplyAsync(() -> getImageFromUrl(url));
    }

    private static ImageDownloaded getImageFromUrl(String url) {
        if (StringUtils.isBlank(url)) return null;
        if (url.startsWith("//")) url = "http:" + url;
        if (!url.startsWith("http")) url = "http://" + url;

        StopWatch watch = new StopWatch();
        try {
            watch.start();
            URL path = new URL(url);
            URLConnection connection = getConnection(path);
            connection.setConnectTimeout(1000); // cài đặt thời gian connect đến resource, nếu quá => throw
            connection.setReadTimeout(10000); // đảm bảo ứng dụng không bị treo hay chờ đợi quá lâu
            try (InputStream is = connection.getInputStream()) {
                int maxDownloaded = 1024 * 1024 * 2; //2MB
                byte[] buffer = new byte[maxDownloaded];
                int actualLength = IOUtils.read(is, buffer, 0, maxDownloaded); // số byte thực tế đọc dược
                watch.start();
                if (is.read() != -1) {
                    log.warn("file too big: {}", url);
                } else {
                    String contentType = connection.getContentType();
                    if (ImageDownloaded.isSupportedContentType(contentType)) {
                        return ImageDownloaded.builder()
                                .bytes(Arrays.copyOfRange(buffer, 0, actualLength))
                                .contentType(contentType)
                                .build();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (watch.isStarted()) watch.stop();
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    private static URLConnection getConnection(URL path) throws IOException {
        return path.openConnection();
    }
}
