package com.benchmark.ua.service;

/**
 * 메모리 측정 유틸리티
 * GC를 유도한 뒤 힙 사용량을 측정해 초기화 전후 차이를 계산합니다.
 */
public class MemoryUtil {

    private MemoryUtil() {}

    /** 현재 힙 사용량(bytes) 반환. GC 유도 후 측정하므로 비교적 정확합니다. */
    public static long usedHeapBytes() {
        Runtime rt = Runtime.getRuntime();
        // GC 2회 유도하여 측정 노이즈 최소화
        System.gc();
        System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }
}
