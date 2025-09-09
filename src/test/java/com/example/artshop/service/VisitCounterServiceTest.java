package com.example.artshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VisitCounterServiceTest {

    private VisitCounterService visitCounterService;

    @BeforeEach
    void setUp() {
        visitCounterService = new VisitCounterService();
    }

    @Test
    void testRecordVisit_SingleEndpoint() {
        visitCounterService.recordVisit("/api/art/all");
        visitCounterService.recordVisit("/api/art/all");
        visitCounterService.recordVisit("/api/art/all");

        assertEquals(3, visitCounterService.getEndpointVisits("/api/art/all"));
        assertEquals(3, visitCounterService.getTotalVisits());
    }

    @Test
    void testRecordVisit_MultipleEndpoints() {
        visitCounterService.recordVisit("/api/art/all");
        visitCounterService.recordVisit("/api/artist/all");
        visitCounterService.recordVisit("/api/art/all");
        visitCounterService.recordVisit("/api/artist/1");

        assertEquals(2, visitCounterService.getEndpointVisits("/api/art/all"));
        assertEquals(1, visitCounterService.getEndpointVisits("/api/artist/all"));
        assertEquals(1, visitCounterService.getEndpointVisits("/api/artist/1"));
        assertEquals(4, visitCounterService.getTotalVisits());
    }

    @Test
    void testGetEndpointVisits_NonExistentEndpoint() {
        assertEquals(0, visitCounterService.getEndpointVisits("/non/existent"));
    }

    @Test
    void testGetAllVisits() {
        visitCounterService.recordVisit("/api/art/all");
        visitCounterService.recordVisit("/api/artist/all");
        visitCounterService.recordVisit("/api/art/all");

        Map<String, Integer> allVisits = visitCounterService.getAllVisits();

        assertEquals(2, allVisits.size());
        assertEquals(2, allVisits.get("/api/art/all"));
        assertEquals(1, allVisits.get("/api/artist/all"));
        assertEquals(3, visitCounterService.getTotalVisits());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int visitsPerThread = 100;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < visitsPerThread; j++) {
                    visitCounterService.recordVisit("/api/test");
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadCount * visitsPerThread, visitCounterService.getEndpointVisits("/api/test"));
        assertEquals(threadCount * visitsPerThread, visitCounterService.getTotalVisits());
    }

    @Test
    void testMultipleEndpointsConcurrently() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                visitCounterService.recordVisit("/api/art");
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                visitCounterService.recordVisit("/api/artist");
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertEquals(1000, visitCounterService.getEndpointVisits("/api/art"));
        assertEquals(1000, visitCounterService.getEndpointVisits("/api/artist"));
        assertEquals(2000, visitCounterService.getTotalVisits());
    }
}