package com.benchmark.ua.service.yauaa;

import com.benchmark.ua.service.MemoryUtil;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Service
@Profile("yauaa-singleton")
public class YauaaSingletonService {

    private volatile UserAgentAnalyzer analyzer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount    = new AtomicLong(0);

    public long getSuccessCount() { return successCount.get(); }
    public long getFailCount()    { return failCount.get(); }

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();

        try {
            UserAgent a = analyzer.parse(userAgent);
//            if (a == null || UserAgent.UNKNOWN_VALUE.equals(a.getValue(UserAgent.AGENT_NAME))) {
            if(a == null){
                failCount.incrementAndGet();
            } else {
                successCount.incrementAndGet();
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
            log.warn("[yauaa-singleton] parse 실패: {}", e.getMessage());
        }
    }

    private synchronized void ensureInitialized() {
        if (initialized.get()) return;

        log.info("[yauaa-singleton] 초기화 시작...");
        long memBefore = MemoryUtil.usedHeapBytes();
        long start     = System.nanoTime();

        analyzer = UserAgentAnalyzer.newBuilder()
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.AGENT_VERSION_MAJOR)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.DEVICE_CLASS)
                
                .build();

        long elapsed  = (System.nanoTime() - start) / 1_000_000;
        long memDelta = (MemoryUtil.usedHeapBytes() - memBefore) / 1024 / 1024;

        System.out.printf("[yauaa-singleton] init | time[%dms] mem[%dMB]%n", elapsed, memDelta);
        initialized.set(true);
    }
}
