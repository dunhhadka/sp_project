package org.example.order.application.utils;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ChunkSizeTest {

    @Test
    void testChunkSize() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<List<Integer>> numberChunk = Lists.partition(numbers, 4);
    }
}
