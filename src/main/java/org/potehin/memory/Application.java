package org.potehin.memory;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.potehin.memory.util.MemoryUtil.sizeOf;

public class Application {

    public static void main(String[] args) {

        Random random = new Random();
        Map<Integer, String> data = new LinkedHashMap<>();

        for (int i = 0; i < 100; i++) {
            Integer value = random.nextInt();
            data.put(value, value.toString());
        }


        System.out.println(sizeOf(data));

        System.out.println(sizeOf(IntStream.range(1, 10)
                                          .boxed()
                                          .collect(Collectors.toList())));
    }


}
