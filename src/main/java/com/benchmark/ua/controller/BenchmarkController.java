package com.benchmark.ua.controller;

import com.benchmark.ua.service.BrowscapCacheService;
import com.benchmark.ua.service.BrowscapService;
import com.benchmark.ua.service.UaParserService;
import com.benchmark.ua.service.yauaa.YauaaCachedService;
import com.benchmark.ua.service.yauaa.YauaaNoCacheService;
import com.benchmark.ua.service.yauaa.YauaaSingletonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    @Autowired(required = false) private UaParserService       uaParserService;
    @Autowired(required = false) private BrowscapService       browscapService;
    @Autowired(required = false) private BrowscapCacheService  browscapCacheService;
    @Autowired(required = false) private YauaaSingletonService yauaaSingletonService;
    @Autowired(required = false) private YauaaCachedService    yauaaCachedService;
    @Autowired(required = false) private YauaaNoCacheService   yauaaNoCacheService;

    private final AtomicLong requestCount = new AtomicLong(0);

    @GetMapping("/parse")
    public ResponseEntity<String> parse(
        @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        requestCount.incrementAndGet();

        if (uaParserService != null)       { uaParserService.parse(userAgent);       return ResponseEntity.ok("OK"); }
        if (browscapService != null)       { browscapService.parse(userAgent);       return ResponseEntity.ok("OK"); }
        if (browscapCacheService != null)  { browscapCacheService.parse(userAgent);  return ResponseEntity.ok("OK"); }
        if (yauaaSingletonService != null) { yauaaSingletonService.parse(userAgent); return ResponseEntity.ok("OK"); }
        if (yauaaCachedService != null)    { yauaaCachedService.parse(userAgent);    return ResponseEntity.ok("OK"); }
        if (yauaaNoCacheService != null)   { yauaaNoCacheService.parse(userAgent);   return ResponseEntity.ok("OK"); }

        return ResponseEntity.badRequest().body("활성화된 프로파일 없음");
    }

    @GetMapping("/stats")
    public ResponseEntity<String> stats() {
        long total = requestCount.get();
        if (uaParserService != null)
            return ResponseEntity.ok(String.format("ua-parser | total=%d success=%d fail=%d", total, uaParserService.getSuccessCount(), uaParserService.getFailCount()));
        if (browscapService != null)
            return ResponseEntity.ok(String.format("browscap | total=%d success=%d fail=%d", total, browscapService.getSuccessCount(), browscapService.getFailCount()));
        if (browscapCacheService != null)
            return ResponseEntity.ok(String.format("browscap-cache | total=%d success=%d fail=%d | cacheHit=%d cacheMiss=%d", total, browscapCacheService.getSuccessCount(), browscapCacheService.getFailCount(), browscapCacheService.getCacheHitCount(), browscapCacheService.getCacheMissCount()));
        if (yauaaSingletonService != null)
            return ResponseEntity.ok(String.format("yauaa-singleton | total=%d success=%d fail=%d", total, yauaaSingletonService.getSuccessCount(), yauaaSingletonService.getFailCount()));
        if (yauaaCachedService != null)
            return ResponseEntity.ok(String.format("yauaa-cache | total=%d success=%d fail=%d", total, yauaaCachedService.getSuccessCount(), yauaaCachedService.getFailCount()));
        if (yauaaNoCacheService != null)
            return ResponseEntity.ok(String.format("yauaa-no-cache | total=%d success=%d fail=%d", total, yauaaNoCacheService.getSuccessCount(), yauaaNoCacheService.getFailCount()));

        return ResponseEntity.badRequest().body("활성화된 프로파일 없음");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
