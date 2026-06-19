package com.benchmark.ua.service;

import com.blueconic.browscap.BrowsCapField;
import com.blueconic.browscap.Capabilities;
import com.blueconic.browscap.UserAgentParser;
import com.blueconic.browscap.UserAgentService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@Profile("browscap-cache")
public class BrowscapCacheService {

    private volatile UserAgentParser parser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    // Caffeine 캐시: 파싱 결과를 UA 문자열 기준으로 저장
    // 빈 HashMap 같은 자료구조를 하나 만드는 것이라 사실상 즉시 완료
    private final Cache<String, Capabilities> uaCache = Caffeine.newBuilder()
        .maximumSize(50_000) // 한 번 파싱한 결과를 최대 5만 개까지 메모리에 저장.
//        .expireAfterAccess(1, TimeUnit.DAYS) // 마지막 접근 후 1일 지나면 삭제. 즉, 같은 UA가 오면 캐시에서 꺼내서 리턴 → 빠름      .recordStats()
//        .recordStats()  // ← 통계 기록 활성화
        .build();

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();
        // 캐시에 있으면 꺼내고, 없으면 파싱 후 자동 저장
//        long start = System.nanoTime();
        Capabilities c = uaCache.get(userAgent, parser::parse);
//        long elapsed = (System.nanoTime() - start) / 1_000;

//        log.info("[browscap] {}µs | browser[{} {}] os[{}] device[{}] ua[{}]",
//                 elapsed, c.getBrowser(), c.getBrowserMajorVersion(), c.getPlatform(), c.getDeviceType(), userAgent);
    }

    private void ensureInitialized() {
        if (initialized.get()) return;
        lock.lock();
        try {
            if (initialized.get()) return;

            log.info("[browscap cached] 초기화 시작...");
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
                throw new RuntimeException("browscap cached 초기화 실패", e);
            }

            long elapsed = (System.nanoTime() - start) / 1_000_000;
            long memDelta = (MemoryUtil.usedHeapBytes() - memBefore) / 1024 / 1024;

            System.out.printf("[browscap cached] init | time[%dms] mem[%dMB]%n", elapsed, memDelta);
            initialized.set(true);
        } finally {
            lock.unlock();
        }
    }
}