package org.potehin.memory.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MemoryUtil {

    private static final MemoryArchOptions OPTIONS = initOptions();

    private static final long HEAP_SIZE_32GB = 32L * 1024L * 1024L * 1024L;

    private static final long OBJECT_PADDING = 8L;

    private static final Map<Class<?>, Integer> sizeOfPrimitive = new HashMap<>();

    static {
        sizeOfPrimitive.put(Boolean.TYPE, 1);
        sizeOfPrimitive.put(Byte.TYPE, 1);
        sizeOfPrimitive.put(Short.TYPE, 2);
        sizeOfPrimitive.put(Character.TYPE, 2);
        sizeOfPrimitive.put(Integer.TYPE, 4);
        sizeOfPrimitive.put(Float.TYPE, 4);
        sizeOfPrimitive.put(Long.TYPE, 8);
        sizeOfPrimitive.put(Double.TYPE, 8);
    }

    private MemoryUtil() {
        throw new IllegalStateException("Util class instanced");
    }

    public static long sizeOf(Object object) {
        try {
            return sizeOf(object, false);
        } catch (StackOverflowError e) {
            return sizeOf(object, true);
        }
    }

    public static long sizeOf(Object object, boolean checkCycleRef) {
        return sizeOfInner(object, checkCycleRef ? new HashSet<>() : null);
    }

    public static long sizeOfInner(Object object, Set<Integer> processedRefs) {
        long size = 0L;
        if (object != null) {
            if (processedRefs != null) {
                if (processedRefs.contains(object.hashCode())) {
                    return size;
                } else {
                    processedRefs.add(object.hashCode());
                }
            }
            Class<?> clazz = object.getClass();

            if (clazz.isArray()) {
                size += OPTIONS.arrayHeaderSize;
                if (clazz.getComponentType().isPrimitive()) {
                    size += sizeOfPrimitiveType(clazz.getComponentType()) * Array.getLength(object);
                } else {
                    for (int i = 0; i < Array.getLength(object); i++) {
                        Object arrayItem = Array.get(object, i);
                        if (arrayItem != null) {
                            size += sizeOfInner(arrayItem, processedRefs);
                        }
                    }
                }
            } else {
                size += OPTIONS.objectHeaderSize;
            }

            //Object hashCode field is included here
            for (Field field : object.getClass().getDeclaredFields()) {
                if (!(Modifier.isStatic(field.getModifiers()))) {
                    if (field.getType().isPrimitive()) {
                        size += sizeOfPrimitiveType(field.getType());
                    } else {
                        field.setAccessible(true);
                        try {
                            size += OPTIONS.referenceSize;
                            size += sizeOfInner(field.get(object), processedRefs);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return roundToObjectSize(size);
    }

    private static int sizeOfPrimitiveType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return sizeOfPrimitive.get(clazz);
        } else {
            throw new IllegalArgumentException(clazz.getSimpleName() + " is not primitive time");
        }
    }

    private static long roundToObjectSize(long value) {
        return (value + OBJECT_PADDING - 1) / OBJECT_PADDING * OBJECT_PADDING;
    }

    private static MemoryArchOptions initOptions() {
        switch (vmArch()) {
            case "32":
                // 32-bit mode
                return new MemoryArchOptions(8, 12, 4);
            case "64":
                if (maxMemory() < HEAP_SIZE_32GB) {
                    //32-bit compressed oop mode
                    //https://docs.oracle.com/javase/7/docs/technotes/guides/vm/performance-enhancements-7.html#compressedOop%23compressedOop
                    // In Java SE 7, use of compressed oops is the default for 64-bit JVM processes
                    // when -Xmx isn't specified and for values of -Xmx less than 32 gigabytes
                    return new MemoryArchOptions(12, 16, 4);
                } else {
                    // 64-bit mode
                    return new MemoryArchOptions(16, 24, 8);
                }
            default:
                throw new IllegalStateException("Unexpected vmArch value " + vmArch());
        }
    }

    private static String vmArch() {
        return System.getProperty("sun.arch.data.model");
    }


    private static long maxMemory() {
        long memory = 0L;

        MemoryPoolMXBean mp;
        for (Iterator i$ = ManagementFactory.getMemoryPoolMXBeans().iterator(); i$.hasNext(); memory += mp.getUsage().getMax()) {
            mp = (MemoryPoolMXBean) i$.next();
        }

        return memory;
    }

    private static class MemoryArchOptions {
        private final int objectHeaderSize; // it's not include hashCode field
        private final int arrayHeaderSize; // include additional array length field which is not seen in getDeclaredFields()
        private final int referenceSize;


        public MemoryArchOptions(int objectHeaderSize, int arrayHeaderSize, int referenceSize) {
            this.objectHeaderSize = objectHeaderSize;
            this.arrayHeaderSize = arrayHeaderSize;
            this.referenceSize = referenceSize;
        }
    }
}
