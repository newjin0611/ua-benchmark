package com.benchmark.ua.service.yauaa;

import com.benchmark.ua.service.MemoryUtil;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Service
@Profile("yauaa-singleton")
public class YauaaSingletonService {
    

    private volatile UserAgentAnalyzer analyzer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();

//        long start = System.nanoTime();
        UserAgent a = analyzer.parse(userAgent);
//        long elapsed = (System.nanoTime() - start) / 1_000;

//        System.out.printf("[yauaa-singleton] %dµs | browser[%s %s] os[%s] device[%s] ua[%s]%n",
//                elapsed,
//                a.getValue(UserAgent.AGENT_NAME),
//                a.getValue(UserAgent.AGENT_VERSION_MAJOR),
//                a.getValue(UserAgent.OPERATING_SYSTEM_NAME),
//                a.getValue(UserAgent.DEVICE_CLASS),
//                userAgent);
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
