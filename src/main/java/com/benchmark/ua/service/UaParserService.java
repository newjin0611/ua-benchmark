package com.benchmark.ua.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@Profile("ua-parser")
public class UaParserService {

    private volatile Parser parser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();

    public void parse(String userAgent) {
        if (!initialized.get()) ensureInitialized();

//        long start = System.nanoTime();
        Client c = parser.parse(userAgent);
//        long elapsed = (System.nanoTime() - start) / 1_000;

//        System.out.printf("[ua-parser] %dµs | browser[%s %s] os[%s] device[%s] ua[%s]%n",
//                elapsed, c.userAgent.family, c.userAgent.major, c.os.family, c.device.family, userAgent);
    }

    private void ensureInitialized() {
        if (initialized.get()) return;
        lock.lock();
        try {
            if (initialized.get()) return;

            log.info("[ua-parser] 초기화 시작...");
            long memBefore = MemoryUtil.usedHeapBytes();
            long start     = System.nanoTime();

            try {
                parser = new Parser();
            } catch (Exception e) {
                throw new RuntimeException("ua-parser 초기화 실패", e);
            }

            long elapsed  = (System.nanoTime() - start) / 1_000_000;
            long memDelta = (MemoryUtil.usedHeapBytes() - memBefore) / 1024 / 1024;

            System.out.printf("[ua-parser] init | time[%dms] mem[%dMB]%n", elapsed, memDelta);
            initialized.set(true);
        } finally {
            lock.unlock();
        }
    }
}