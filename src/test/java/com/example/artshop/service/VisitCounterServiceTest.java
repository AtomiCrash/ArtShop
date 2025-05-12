package com.example.artshop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VisitCounterServiceTest {

    @InjectMocks
    private VisitCounterService visitCounterService;

    @Test
    void incrementVisitCount_ShouldIncrementCount() {
        long initialCount = visitCounterService.getVisitCount();
        visitCounterService.incrementVisitCount();
        assertEquals(initialCount + 1, visitCounterService.getVisitCount());
    }

    @Test
    void getVisitCount_ShouldReturnCurrentCount() {
        long count = visitCounterService.getVisitCount();
        assertTrue(count >= 0);
    }
}