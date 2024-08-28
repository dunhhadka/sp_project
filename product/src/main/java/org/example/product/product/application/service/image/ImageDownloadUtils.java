package org.example.product.product.application.service.image;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.example.product.product.application.model.ImageDownloaded;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class ImageDownloadUtils {

    public static CompletableFuture<ImageDownloaded> getImageFromUrlAsync(String src) {
        return CompletableFuture.supplyAsync(() -> getImageFromUrl(src));
    }

    private static ImageDownloaded getImageFromUrl(String url) {
        if (StringUtils.isBlank(url)) return null;
        if (StringUtils.startsWith(url, "//")) url = "http:" + url;
        if (!StringUtils.startsWith(url, "http")) url = "http://" + url;

        ImageDownloaded image = null;
        StopWatch watch = new StopWatch();
        try {
            URL path = new URL(url);
            URLConnection connection = path.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(5000);
            InputStream is = null;
            try {
                watch.start();
                is = connection.getInputStream();
                int maxDownload = 1024 * 1024 * 2;// max 2MB
                byte[] buffer = new byte[maxDownload];
                var actualLength = IOUtils.read(is, buffer, 0, maxDownload);
                if (is.read() != -1) {
                    log.error("File too big {}", actualLength);
                } else {
                    String contentType = connection.getContentType();
                    if (ImageDownloaded.isSupportedContentType(contentType)) {
                        image = new ImageDownloaded();
                        image.setContentType(contentType);
                        image.setBytes(Arrays.copyOfRange(buffer, 0, maxDownload));
                    } else {
                        log.error("ContentType is not supported for {}", contentType);
                    }
                }
            } catch (Exception e) {
                log.error("Error while downloading file");
            } finally {
                if (watch.isStarted()) watch.stop();
                if (watch.getTime(TimeUnit.MILLISECONDS) > 7000) {
                    log.warn("get data from url {} slow: {}", url, watch.getTime(TimeUnit.MILLISECONDS));
                }
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }

}
