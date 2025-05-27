package com.example.artshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @InjectMocks
    private LogService logService;

    @Mock
    private Path logDir;

    @Mock
    private DirectoryStream<Path> directoryStream;

    @Mock
    private Path logFile;

    private LocalDate date;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        date = LocalDate.of(2025, 5, 22);
        logService = new LogService();
        // Use reflection to set private logPath for testing
        java.lang.reflect.Field field = LogService.class.getDeclaredField("logPath");
        field.setAccessible(true);
        field.set(logService, "/logs");
    }

    @Test
    void extractLogsForPeriod_ValidLogs_ReturnsFilteredLogs() throws IOException {
        String logLine = "2025-05-22T09:27:49.432+03:00 INFO Some log message";
        List<String> logLines = Arrays.asList(logLine, "2025-05-22T10:00:00.000+03:00 INFO Other log");

        when(logDir.toAbsolutePath()).thenReturn(logDir);
        when(Files.newDirectoryStream(eq(logDir), eq("*.log"))).thenReturn(directoryStream);
        when(directoryStream.iterator()).thenReturn(Arrays.asList(logFile).iterator());
        when(Files.readAllLines(logFile)).thenReturn(logLines);

        List<String> result = logService.extractLogsForPeriod(date, 9);

        assertEquals(2, result.size());
        assertEquals("=== Logs for 2025-05-22 09:00-09:59 ===\n", result.get(0));
        assertEquals(logLine, result.get(1));
    }

    @Test
    void extractLogsForPeriod_NoLogs_ReturnsNoEntriesMessage() throws IOException {
        when(logDir.toAbsolutePath()).thenReturn(logDir);
        when(Files.newDirectoryStream(eq(logDir), eq("*.log"))).thenReturn(directoryStream);
        when(directoryStream.iterator()).thenReturn(Collections.<Path>emptyList().iterator());

        List<String> result = logService.extractLogsForPeriod(date, 9);

        assertEquals(2, result.size());
        assertEquals("=== Logs for 2025-05-22 09:00-09:59 ===\n", result.get(0));
        assertEquals("No log entries found for this period", result.get(1));
    }

    @Test
    void extractLogsForPeriod_InvalidLogFormat_SkipsInvalidLines() throws IOException {
        List<String> logLines = Arrays.asList("Invalid log line", "2025-05-22T09:27:49.432+03:00 INFO Valid log");

        when(logDir.toAbsolutePath()).thenReturn(logDir);
        when(Files.newDirectoryStream(eq(logDir), eq("*.log"))).thenReturn(directoryStream);
        when(directoryStream.iterator()).thenReturn(Arrays.asList(logFile).iterator());
        when(Files.readAllLines(logFile)).thenReturn(logLines);

        List<String> result = logService.extractLogsForPeriod(date, 9);

        assertEquals(2, result.size());
        assertEquals("=== Logs for 2025-05-22 09:00-09:59 ===\n", result.get(0));
        assertEquals("2025-05-22T09:27:49.432+03:00 INFO Valid log", result.get(1));
    }

    @Test
    void extractLogsForPeriod_IOException_PropagatesException() throws IOException {
        when(logDir.toAbsolutePath()).thenReturn(logDir);
        when(Files.newDirectoryStream(eq(logDir), eq("*.log"))).thenThrow(new IOException("File error"));

        IOException exception = assertThrows(IOException.class, () -> logService.extractLogsForPeriod(date, 9));
        assertEquals("File error", exception.getMessage());
    }

    @Test
    void isInTimeRange_ValidTime_ReturnsTrue() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String logLine = "2025-05-22T09:27:49.432+03:00 INFO Message";
        java.lang.reflect.Method method = LogService.class.getDeclaredMethod("isInTimeRange", String.class, int.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(logService, logLine, 9);
        assertTrue(result);
    }

    @Test
    void isInTimeRange_DifferentHour_ReturnsFalse() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String logLine = "2025-05-22T10:27:49.432+03:00 INFO Message";
        java.lang.reflect.Method method = LogService.class.getDeclaredMethod("isInTimeRange", String.class, int.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(logService, logLine, 9);
        assertFalse(result);
    }

    @Test
    void isInTimeRange_InvalidFormat_ReturnsFalse() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String logLine = "Invalid log line";
        java.lang.reflect.Method method = LogService.class.getDeclaredMethod("isInTimeRange", String.class, int.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(logService, logLine, 9);
        assertFalse(result);
    }
}