package org.potehin.memory.util;


import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.potehin.memory.util.MemoryUtil.sizeOf;


public class MemoryUtilTest {
    @Test
    public void sizeOfTest() {


        Map<Integer, String> data = IntStream.range(1, 1000).boxed()
                .collect(Collectors.toMap(Function.identity(), Objects::toString));

        Assert.assertTrue(sizeOf(data) > 0);

        Map<Integer, String> cycleRefData = new LinkedHashMap<>(data);

        Assert.assertTrue(sizeOf(cycleRefData) > 0);
    }
}
