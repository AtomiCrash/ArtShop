package com.example.artshop.controller;

import com.example.artshop.service.LogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final LogService logService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, String> reportStatuses = new ConcurrentHashMap<>();
    private final Map<String, Path> reportFiles = new ConcurrentHashMap<>();

    @Value("${report.storage.path}")
    private String reportsDir;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateLogReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @Min(0) @Max(23) int hour) {
        String reportId = UUID.randomUUID().toString();
        Path reportDir = Paths.get(reportsDir).toAbsolutePath();
        Path reportFile = reportDir.resolve(String.format("report-%s-%02d-%s.log", date, hour, reportId));

        try {
            Files.createDirectories(reportDir);

            reportStatuses.put(reportId, "PROCESSING");
            reportFiles.put(reportId, reportFile);

            executor.execute(() -> {
                try {
                    Thread.sleep(20000);
                    List<String> logs = logService.extractLogsForPeriod(date, hour);

                    Path tempFile = Files.createTempFile(reportDir, "temp-", ".tmp");
                    Files.write(tempFile, logs, StandardCharsets.UTF_8);
                    Files.move(tempFile, reportFile, StandardCopyOption.ATOMIC_MOVE);
                    reportStatuses.put(reportId, "READY");
                    System.out.println("Successfully created report: " + reportFile);
                    System.out.println("Content size: " + Files.size(reportFile) + " bytes");
                } catch (InterruptedException e) {
                    // Восстанавливаем статус прерывания и выходим
                    Thread.currentThread().interrupt();
                    reportStatuses.put(reportId, "CANCELLED: Operation interrupted");
                    System.out.println("Report generation interrupted for reportId: " + reportId);
                } catch (Exception e) {
                    reportStatuses.put(reportId, "FAILED: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            return ResponseEntity.accepted().body(Map.of(
                    "reportId", reportId,
                    "status", "PROCESSING",
                    "message", "Report generation started"
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create reports directory"));
        }
    }

    @GetMapping("/status/{reportId}")
    public ResponseEntity<Object> getReportStatus(@PathVariable String reportId) {
        if (!reportStatuses.containsKey(reportId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "reportId", reportId,
                "status", reportStatuses.get(reportId),
                "filePath", reportFiles.get(reportId).toString()
        ));
    }

    @GetMapping("/download/{reportId}")
    public ResponseEntity<Object> downloadReport(@PathVariable String reportId) {
        Path reportFile = reportFiles.get(reportId);
        if (reportFile == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            if (!"READY".equals(reportStatuses.get(reportId))) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "error", "Report not ready",
                                "status", reportStatuses.get(reportId)
                        ));
            }

            if (!Files.exists(reportFile)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Report file not found on server");
            }
            
            byte[] fileContent = Files.readAllBytes(reportFile);
            if (fileContent.length == 0) {
                return ResponseEntity.internalServerError()
                        .body("Report file is empty");
            }
            
            try {
                Files.delete(reportFile);
                reportFiles.remove(reportId);
                reportStatuses.put(reportId, "DOWNLOADED");
            } catch (IOException e) {
                System.err.println("Failed to delete report file: " + e.getMessage());
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + reportFile.getFileName() + "\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(fileContent.length)
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Download error: " + e.getMessage());
        }
    }
}