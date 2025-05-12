package com.example.artshop.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    @Value("${logging.file.path}")
    private String logPath;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, String> logStatuses = new HashMap<>();
    private final Map<String, String> logFilePaths = new HashMap<>();

    @PostMapping("/generate")
    public DeferredResult<ResponseEntity<?>> generateLogFile() {
        String logId = UUID.randomUUID().toString();
        String fileName = "artshop-" + LocalDate.now() + "-" + logId + ".log";
        String filePath = logPath + File.separator + fileName;

        logStatuses.put(logId, "PROCESSING");
        logFilePaths.put(logId, filePath);

        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(100000L);
        deferredResult.onTimeout(() ->
                deferredResult.setErrorResult(
                        ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                                .body("Log generation timeout")));

        executor.submit(() -> {
            try {

                Path path = Paths.get(filePath);
                Files.createDirectories(path.getParent());
                Files.write(path, ("Log content for " + fileName).getBytes());
                Thread.sleep(50000);
                logStatuses.put(logId, "READY");
                deferredResult.setResult(ResponseEntity.ok(
                        Map.of("logId", logId, "status", "COMPLETED")));
            } catch (Exception e) {
                logStatuses.put(logId, "FAILED: " + e.getMessage());
                deferredResult.setErrorResult(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Log generation failed: " + e.getMessage()));
            }
        });

        return deferredResult;
    }

    @GetMapping("/status/{logId}")
    public ResponseEntity<?> getLogStatus(@PathVariable String logId) {
        String status = logStatuses.get(logId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "logId", logId,
                "status", status,
                "filePath", logFilePaths.get(logId)
        ));
    }

    @GetMapping("/download/{logId}")
    public ResponseEntity<?> downloadLogFile(@PathVariable String logId) throws IOException {
        String status = logStatuses.get(logId);

        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Log not found",
                            "logId", logId
                    ));
        }

        if (!"READY".equals(status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Log file not ready yet",
                            "logId", logId,
                            "status", status,
                            "suggestion", "Check /api/logs/status/" + logId + " for current status"
                    ));
        }

        String filePath = logFilePaths.get(logId);
        Path path = Paths.get(filePath);
        Resource resource = new UrlResource(path.toUri());

        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }

        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of(
                        "error", "Log file was generated but not found",
                        "logId", logId,
                        "status", status,
                        "filePath", filePath
                ));
    }
}