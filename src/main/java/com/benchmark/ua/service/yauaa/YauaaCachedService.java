package com.benchmark.ua.service.yauaa;

import com.benchmark.ua.service.MemoryUtil;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@Profile("yauaa-cache")
public class YauaaCachedService {

    @Value("${ua.yauaa.cache-size:10000}")
    private int cacheSize;

    private volatile UserAgentAnalyzer analyzer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();

//        long start = System.nanoTime();
        UserAgent a = analyzer.parse(userAgent);
//        long elapsed = (System.nanoTime() - start) / 1_000;

//        System.out.printf("[yauaa-cache] %dµs | browser[%s %s] os[%s] device[%s] ua[%s]%n",
//                elapsed,
//                a.getValue(UserAgent.AGENT_NAME),
//                a.getValue(UserAgent.AGENT_VERSION_MAJOR),
//                a.getValue(UserAgent.OPERATING_SYSTEM_NAME),
//                a.getValue(UserAgent.DEVICE_CLASS),
//                userAgent);
    }

    private void ensureInitialized() {
        if (initialized.get()) return;
        lock.lock();
        try {
            if (initialized.get()) return;

            log.info("[yauaa-cache] 초기화 시작...");
            long memBefore = MemoryUtil.usedHeapBytes();
            long start = System.nanoTime();

            analyzer = UserAgentAnalyzer.newBuilder()
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.AGENT_VERSION_MAJOR)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.DEVICE_CLASS)
                .withCache(cacheSize)
                .build();

            long elapsed = (System.nanoTime() - start) / 1_000_000;
            long memDelta = (MemoryUtil.usedHeapBytes() - memBefore) / 1024 / 1024;

            System.out.printf("[yauaa-cache] init | time[%dms] mem[%dMB]%n", elapsed, memDelta);
            initialized.set(true);
        } finally {
            lock.unlock();
        }
    }
}