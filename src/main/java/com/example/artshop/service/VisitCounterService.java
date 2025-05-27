package com.example.artshop.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class VisitCounterService {
    private final Map<String, AtomicInteger> endpointCounters = new ConcurrentHashMap<>();
    private final AtomicInteger totalVisits = new AtomicInteger(0);

    public void recordVisit(String endpoint) {
        endpointCounters.computeIfAbsent(endpoint, k -> new AtomicInteger()).incrementAndGet();
        totalVisits.incrementAndGet();
    }

    public int getEndpointVisits(String endpoint) {
        return endpointCounters.getOrDefault(endpoint, new AtomicInteger()).get();
    }

    public int getTotalVisits() {
        return totalVisits.get();
    }

    public Map<String, Integer> getAllVisits() {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        endpointCounters.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }
}