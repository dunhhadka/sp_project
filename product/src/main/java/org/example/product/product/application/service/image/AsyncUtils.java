package org.example.product.product.application.service.image;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AsyncUtils {

    public static <T> CompletableFuture<List<T>> allOf(Collection<CompletableFuture<T>> futures) {
        CompletableFuture<Void> completableFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return completableFutures.thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }
}
