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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@Profile("browscap-cache")
public class BrowscapCacheService {

    /** UA 뒤에 붙는 광고식별자 구분자. 이 토큰부터 끝까지(madid, mappid 등)는 파싱 대상이 아니다. */
    private static final String MADID_TOKEN = ";madid=";

    private volatile UserAgentParser parser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount    = new AtomicLong(0);

    public long getSuccessCount()   { return successCount.get(); }
    public long getFailCount()      { return failCount.get(); }
    public long getCacheHitCount()  { return (long) uaCache.stats().hitCount(); }
    public long getCacheMissCount() { return (long) uaCache.stats().missCount(); }

    private final Cache<String, Capabilities> uaCache = Caffeine.newBuilder()
        .maximumSize(50_000)
        .recordStats()
        .build();

    /**
     * UA 끝에 붙은 ";madid=...;mappid=..." 구간을 잘라낸다.
     * 정규식(replaceAll/replaceFirst) 대신 indexOf + substring 사용 → 고부하 시 CPU 오버헤드 최소화.
     * madid가 없으면 입력을 그대로 반환(불필요한 객체 생성 없음).
     */
    private static String stripMadid(String userAgent) {
        if (userAgent == null) return null;
        int idx = userAgent.indexOf(MADID_TOKEN);
        return (idx >= 0) ? userAgent.substring(0, idx) : userAgent;
    }

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();

        // ✅ 파싱/캐싱 전에 madid 제거 → 캐시 키가 실제 UA로 정규화되어 캐시 히트가 정상 동작
        String normalizedUa = stripMadid(userAgent);

        try {
            Capabilities c = uaCache.get(normalizedUa, parser::parse);
            if (c == null) {
                failCount.incrementAndGet();
            } else {
                successCount.incrementAndGet();
            }
        } catch (Exception e) {
            failCount.incrementAndGet();
            log.warn("[browscap-cache] parse 실패: {}", e.getMessage());
        }
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