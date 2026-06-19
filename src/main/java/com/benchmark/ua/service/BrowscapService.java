package com.benchmark.ua.service;

import com.blueconic.browscap.BrowsCapField;
import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import com.blueconic.browscap.UserAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@Profile("browscap")
public class BrowscapService {

    private volatile UserAgentParser parser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount    = new AtomicLong(0);

    public long getSuccessCount() { return successCount.get(); }
    public long getFailCount()    { return failCount.get(); }

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();

        try {
            Capabilities c = parser.parse(userAgent);
            if (c == null) {
                failCount.incrementAndGet();
            } else {
                successCount.incrementAndGet();
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
            log.warn("[browscap] parse 실패: {}", e.getMessage());
        }
    }

    private void ensureInitialized() {
        if (initialized.get()) return;
        lock.lock();
        try {
            if (initialized.get()) return;

            log.info("[browscap] 초기화 시작...");
            long memBefore = MemoryUtil.usedHeapBytes();
            long start = System.nanoTime();

            try {
                parser = new UserAgentService().loadParser(Arrays.asList(
                    BrowsCapField.BROWSER,
                    BrowsCapField.BROWSER_MAJOR_VERSION,
                    BrowsCapField.PLATFORM,
                    BrowsCapField.DEVICE_TYPE
                ));
            } catch (Exception e) {
                throw new RuntimeException("browscap 초기화 실패", e);
            }

            long elapsed = (System.nanoTime() - start) / 1_000_000;
            long memDelta = (MemoryUtil.usedHeapBytes() - memBefore) / 1024 / 1024;

            System.out.printf("[browscap] init | time[%dms] mem[%dMB]%n", elapsed, memDelta);
            initialized.set(true);
        } finally {
            lock.unlock();
        }
    }
}