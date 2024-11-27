package org.example.order.async;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TestFirstAsync {
    public Future<String> calculateAsync() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            try {
                Thread.sleep(1000);
                completableFuture.complete("Hello");
                return null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return completableFuture;
    }

    @Test
    void test() throws ExecutionException, InterruptedException {
        Future<String> completableFuture = calculateAsync();

        String result = completableFuture.get();
        Assertions.assertEquals("Hello", result);
    }

    @Test
    void testAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "TEST");
        CompletableFuture<Void> completableFutureVoid = CompletableFuture.runAsync(() -> {
            System.out.println("run complete feature");
        });

        String result1 = completableFuture.get();
        completableFutureVoid.get();

        Assertions.assertEquals(result1, "TEST");
    }
}
