package com.example.artshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VisitCounterServiceTest {

    private VisitCounterService visitCounterService;

    @BeforeEach
    void setUp() {
        visitCounterService = new VisitCounterService();
    }

    @Test
    void recordVisit_NewEndpoint_IncrementsCounter() {
        visitCounterService.recordVisit("/api/art");

        assertEquals(1, visitCounterService.getEndpointVisits("/api/art"));
        assertEquals(1, visitCounterService.getTotalVisits());
    }

    @Test
    void recordVisit_ExistingEndpoint_IncrementsCounter() {
        visitCounterService.recordVisit("/api/art");
        visitCounterService.recordVisit("/api/art");

        assertEquals(2, visitCounterService.getEndpointVisits("/api/art"));
        assertEquals(2, visitCounterService.getTotalVisits());
    }

    @Test
    void getEndpointVisits_UnknownEndpoint_ReturnsZero() {
        assertEquals(0, visitCounterService.getEndpointVisits("/api/unknown"));
    }

    @Test
    void getAllVisits_MultipleEndpoints_ReturnsCorrectMap() {
        visitCounterService.recordVisit("/api/art");
        visitCounterService.recordVisit("/api/artist");
        visitCounterService.recordVisit("/api/art");

        Map<String, Integer> result = visitCounterService.getAllVisits();

        assertEquals(2, result.size());
        assertEquals(2, result.get("/api/art"));
        assertEquals(1, result.get("/api/artist"));
    }

    @Test
    void getTotalVisits_MultipleVisits_ReturnsSum() {
        visitCounterService.recordVisit("/api/art");
        visitCounterService.recordVisit("/api/artist");
        visitCounterService.recordVisit("/api/art");

        assertEquals(3, visitCounterService.getTotalVisits());
    }
}