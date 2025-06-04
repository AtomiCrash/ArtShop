package com.example.artshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void extractLogsForPeriod_shouldReturnNoEntriesWhenNoLogsExist() throws IOException {
        LocalDate date = LocalDate.of(2023, 1, 1);
        int hour = 10;

        List<String> result = logService.extractLogsForPeriod(date, hour);

        assertEquals(2, result.size());
        assertEquals(String.format("=== Logs for %s %02d:00-%02d:59 ===\n", date, hour, hour), result.get(0));
        assertEquals("No log entries found for this period", result.get(1));
    }

    @Test
    void extractLogsForPeriod_shouldFilterLogsByDateAndHour() throws IOException {
        LocalDate date = LocalDate.of(2023, 1, 1);
        int hour = 10;

        Path logFile = tempDir.resolve("test.log");
        Files.write(logFile, List.of(
                "2023-01-01T10:15:30.123 INFO - Test message 1",
                "2023-01-01T10:59:59.999 INFO - Test message 2",
                "2023-01-01T11:00:00.000 INFO - Should not appear",
                "2023-01-02T10:00:00.000 INFO - Wrong date",
                "Invalid log line",
                "2023-01-01T09:59:59.999 INFO - Wrong hour"
        ));

        List<String> result = logService.extractLogsForPeriod(date, hour);

        assertEquals(3, result.size());
        assertTrue(result.get(1).contains("Test message 1"));
        assertTrue(result.get(2).contains("Test message 2"));
    }

    @Test
    void extractLogsForPeriod_shouldHandleIOException() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.newDirectoryStream(any(Path.class), any(String.class)))
                    .thenThrow(new IOException("Test exception"));

            LocalDate date = LocalDate.of(2023, 1, 1);
            int hour = 10;

            assertThrows(IOException.class, () -> {
                logService.extractLogsForPeriod(date, hour);
            });
        }
    }

    @Test
    void extractLogsForPeriod_shouldHandleMultipleLogFiles() throws IOException {
        LocalDate date = LocalDate.of(2023, 1, 1);
        int hour = 10;

        Path logFile1 = tempDir.resolve("test1.log");
        Path logFile2 = tempDir.resolve("test2.log");

        Files.write(logFile1, List.of(
                "2023-01-01T10:15:30.123 INFO - Message from file 1"
        ));

        Files.write(logFile2, List.of(
                "2023-01-01T10:30:45.456 INFO - Message from file 2"
        ));

        List<String> result = logService.extractLogsForPeriod(date, hour);

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(line -> line.contains("file 1")));
        assertTrue(result.stream().anyMatch(line -> line.contains("file 2")));
    }

    @Test
    void extractLogsForPeriod_shouldHandleEmptyLines() throws IOException {
        LocalDate date = LocalDate.of(2023, 1, 1);
        int hour = 10;

        Path logFile = tempDir.resolve("test.log");
        Files.write(logFile, List.of(
                "",
                "2023-01-01T10:15:30.123 INFO - Valid message",
                "   ",
                ""
        ));

        List<String> result = logService.extractLogsForPeriod(date, hour);

        assertEquals(2, result.size());
        assertTrue(result.get(1).contains("Valid message"));
    }
}