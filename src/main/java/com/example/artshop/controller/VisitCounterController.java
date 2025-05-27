package com.example.artshop.controller;

import com.example.artshop.service.VisitCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visits")
public class VisitCounterController {
    @Autowired
    private VisitCounterService visitCounterService;

    @Operation(summary = "Get total visits",
            description = "Returns total number of GET requests to all endpoints")
    @ApiResponse(responseCode = "200", description = "Total visits count",
            content = @Content(schema = @Schema(implementation = Integer.class)))
    @GetMapping("/total")
    public ResponseEntity<Integer> getTotalVisits() {
        return ResponseEntity.ok(visitCounterService.getTotalVisits());
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Integer>> getAllVisits() {
        return ResponseEntity.ok(visitCounterService.getAllVisits());
    }

    @GetMapping("/endpoint")
    public ResponseEntity<Integer> getEndpointVisits(@RequestParam String endpoint) {
        return ResponseEntity.ok(visitCounterService.getEndpointVisits(endpoint));
    }
}