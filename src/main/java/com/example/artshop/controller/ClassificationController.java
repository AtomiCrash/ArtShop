package com.example.artshop.controller;

import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.model.Classification;
import com.example.artshop.service.ClassificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classification")
@Tag(name = "Classification Management", description = "Operations related to artwork classifications")
public class ClassificationController {

    private ClassificationService classificationService;

    @Operation(summary = "Get all classifications", description = "Returns list of all classifications")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClassificationDTO.class))))
    @GetMapping("/all")
    public List<ClassificationDTO> getAllClassifications() {
        return classificationService.getAllClassifications();
    }

    @Operation(summary = "Get classification by ID", description = "Returns single classification by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classification found",
                    content = @Content(schema = @Schema(implementation = ClassificationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClassificationDTO> getClassificationById(
            @Parameter(description = "ID of classification to be retrieved", required = true)
            @PathVariable int id) {
        ClassificationDTO classification = classificationService.getClassificationById(id);
        if (classification != null) {
            return ResponseEntity.ok(classification);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Add multiple classifications",
            description = "Creates multiple classifications in one request (max 10 items)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Classifications created successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Classification.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input (empty list or more than 10 items)")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<Classification>> addBulkClassifications(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "List of ClassificationDTO objects (max 10 items)",
                    required = true,
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = ClassificationDTO.class))))
            @RequestBody List<ClassificationDTO> classificationDTOs) {
        List<Classification> createdClassifications = classificationService.addBulkClassifications(classificationDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClassifications);
    }

    @Operation(summary = "Create classification", description = "Creates a new classification")
    @ApiResponse(responseCode = "200", description = "Classification created successfully",
            content = @Content(schema = @Schema(implementation = Classification.class)))
    @PostMapping("/add")
    public Classification createClassification(
            @RequestBody ClassificationDTO classificationDTO) {
        Classification classification = new Classification();
        classification.setName(classificationDTO.getName());
        classification.setDescription(classificationDTO.getDescription());
        return classificationService.saveClassification(classification);
    }

    @Operation(summary = "Partially update classification",
            description = "Updates specific fields of a classification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classification updated successfully",
                    content = @Content(schema = @Schema(implementation = Classification.class))),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Classification> patchClassification(
            @Parameter(description = "ID of classification to be updated", required = true)
            @PathVariable int id,
            @RequestBody ClassificationPatchDTO patchDTO) {
        Classification updated = classificationService.patchClassification(id, patchDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete classification", description = "Deletes classification by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Classification deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @DeleteMapping("/{id}")
    public void deleteClassification(
            @Parameter(description = "ID of classification to be deleted", required = true)
            @PathVariable int id) {
        classificationService.deleteClassification(id);
    }

    @Operation(summary = "Update classification", description = "Updates existing classification by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classification updated successfully",
                    content = @Content(schema = @Schema(implementation = Classification.class))),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Classification> updateClassification(
            @Parameter(description = "ID of classification to be updated", required = true)
            @PathVariable int id,
            @RequestBody ClassificationDTO classificationDTO) {
        Classification updated = classificationService.updateClassification(id, classificationDTO);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get cache info", description = "Returns classification cache statistics")
    @ApiResponse(responseCode = "200", description = "Cache info retrieved",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping("/cache-info")
    public ResponseEntity<String> getClassificationCacheInfo() {
        return ResponseEntity.ok(classificationService.getCacheInfo());
    }
}