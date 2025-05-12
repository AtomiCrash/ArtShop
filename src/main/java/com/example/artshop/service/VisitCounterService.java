package com.example.artshop.service;

import lombok.Synchronized;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VisitCounterService {
    private final AtomicLong visitCount = new AtomicLong(0);

    @Synchronized
    public void incrementVisitCount() {
        visitCount.incrementAndGet();
    }

    public long getVisitCount() {
        return visitCount.get();
    }
}