package com.example.artshop.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    @Value("${logging.file.path}")
    private String logPath;

    public List<String> extractLogsForPeriod(LocalDate date, int hour) throws IOException {
        Path logDir = Paths.get(logPath).toAbsolutePath();
        List<String> result = new ArrayList<>();

        result.add(String.format("=== Logs for %s %02d:00-%02d:59 ===\n", date, hour, hour));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "*.log")) {
            for (Path file : stream) {
                List<String> fileLines = Files.readAllLines(file);
                for (String line : fileLines) {
                    if (isInTimeRange(line, hour) && line.contains(date.toString())) {
                        result.add(line);
                    }
                }
            }
        }

        if (result.size() == 1) {
            result.add("No log entries found for this period");
        }

        return result;
    }

    private boolean isInTimeRange(String logLine, int targetHour) {
        try {
            String[] parts = logLine.split("T");
            if (parts.length < 2) return false;

            String timePart = parts[1].split("\\.")[0];
            int logHour = Integer.parseInt(timePart.substring(0, 2));
            return logHour == targetHour;
        } catch (Exception e) {
            return false;
        }
    }
}