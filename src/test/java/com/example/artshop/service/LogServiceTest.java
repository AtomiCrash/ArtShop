package com.example.artshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogServiceTest {

    private LogService logService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logService = new LogService();
        ReflectionTestUtils.setField(logService, "logPath", tempDir.toString());
    }

    @Test
    void testExtractLogsForPeriod_WithMatchingLogs() throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = "2025-01-15T10:15:30.123+03:00 INFO  - Test message 1\n" +
                "2025-01-15T10:45:12.456+03:00 DEBUG - Test message 2\n" +
                "2025-01-15T11:30:45.789+03:00 ERROR - Test message 3";
        Files.write(logFile, logContent.getBytes());

        LocalDate date = LocalDate.of(2025, 1, 15);
        List<String> result = logService.extractLogsForPeriod(date, 10);

        assertEquals(3, result.size());
        assertTrue(result.get(0).contains("Logs for 2025-01-15 10:00-10:59"));
        assertTrue(result.get(1).contains("10:15:30"));
        assertTrue(result.get(2).contains("10:45:12"));
    }

    @Test
    void testExtractLogsForPeriod_NoMatchingLogs() throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = "2025-01-15T09:59:59.123+03:00 INFO - Test message\n" +
                "2025-01-15T11:00:00.456+03:00 DEBUG - Test message";
        Files.write(logFile, logContent.getBytes());

        LocalDate date = LocalDate.of(2025, 1, 15);
        List<String> result = logService.extractLogsForPeriod(date, 10);

        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("Logs for 2025-01-15 10:00-10:59"));
        assertTrue(result.get(1).contains("No log entries found for this period"));
    }

    @Test
    void testExtractLogsForPeriod_MultipleLogFiles() throws IOException {
        Path logFile1 = tempDir.resolve("app.log");
        Path logFile2 = tempDir.resolve("debug.log");

        String content1 = "2025-01-15T10:10:00.123+03:00 INFO - Message from app.log";
        String content2 = "2025-01-15T10:20:00.456+03:00 DEBUG - Message from debug.log";

        Files.write(logFile1, content1.getBytes());
        Files.write(logFile2, content2.getBytes());

        LocalDate date = LocalDate.of(2025, 1, 15);
        List<String> result = logService.extractLogsForPeriod(date, 10);

        assertEquals(3, result.size());
        assertTrue(result.get(1).contains("app.log") || result.get(2).contains("app.log"));
        assertTrue(result.get(1).contains("debug.log") || result.get(2).contains("debug.log"));
    }

    @Test
    void testExtractLogsForPeriod_InvalidLogFormat() throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = "Invalid log format without timestamp\n" +
                "2025-01-15T10:15:30.123+03:00 INFO - Valid message";
        Files.write(logFile, logContent.getBytes());

        LocalDate date = LocalDate.of(2025, 1, 15);
        List<String> result = logService.extractLogsForPeriod(date, 10);

        assertEquals(2, result.size());
        assertTrue(result.get(1).contains("10:15:30"));
    }

    @Test
    void testIsInTimeRange_ValidTime() {
        LogService service = new LogService();
        String validLogLine = "2025-01-15T10:15:30.123+03:00 INFO - Test message";

        boolean result = ReflectionTestUtils.invokeMethod(service, "isInTimeRange", validLogLine, 10);
        assertTrue(result);
    }

    @Test
    void testIsInTimeRange_InvalidTime() {
        LogService service = new LogService();
        String validLogLine = "2025-01-15T10:15:30.123+03:00 INFO - Test message";

        boolean result = ReflectionTestUtils.invokeMethod(service, "isInTimeRange", validLogLine, 11);
        assertFalse(result);
    }

    @Test
    void testIsInTimeRange_InvalidFormat() {
        LogService service = new LogService();
        String invalidLogLine = "Invalid log line without timestamp";

        boolean result = ReflectionTestUtils.invokeMethod(service, "isInTimeRange", invalidLogLine, 10);
        assertFalse(result);
    }

    @Test
    void testExtractLogsForPeriod_EmptyDirectory() throws IOException {
        LocalDate date = LocalDate.of(2025, 1, 15);
        List<String> result = logService.extractLogsForPeriod(date, 10);

        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("Logs for 2025-01-15 10:00-10:59"));
        assertTrue(result.get(1).contains("No log entries found for this period"));
    }
}