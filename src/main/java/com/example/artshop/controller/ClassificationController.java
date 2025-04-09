package com.example.artshop.controller;

import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.model.Classification;
import com.example.artshop.service.ClassificationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ClassificationController {
    @Autowired
    private ClassificationService classificationService;

    @GetMapping("/all")
    public List<Classification> getAllClassifications() {
        return classificationService.getAllClassifications();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Classification> getClassificationById(@PathVariable int id) {
        Classification classification = classificationService.getClassificationById(id);
        if (classification != null) {
            return ResponseEntity.ok(classification);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-art")
    public ResponseEntity<List<Classification>> getClassificationsByArtTitle(@RequestParam String artTitle) {
        List<Classification> classifications = classificationService.getClassificationsByArtTitle(artTitle);
        return ResponseEntity.ok(classifications);
    }

    @PostMapping("/add")
    public Classification createClassification(@RequestBody ClassificationDTO classificationDTO) {
        Classification classification = new Classification();
        classification.setName(classificationDTO.getName());
        classification.setDescription(classificationDTO.getDescription());
        return classificationService.saveClassification(classification);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Classification> patchClassification(
            @PathVariable int id,
            @RequestBody ClassificationPatchDTO patchDTO) {
        Classification updated = classificationService.patchClassification(id, patchDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public void deleteClassification(@PathVariable int id) {
        classificationService.deleteClassification(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Classification> updateClassification(
            @PathVariable int id,
            @RequestBody ClassificationDTO classificationDTO) {
        Classification updated = classificationService.updateClassification(id, classificationDTO);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cache-info")
    public ResponseEntity<String> getClassificationCacheInfo() {
        return ResponseEntity.ok(classificationService.getCacheInfo());
    }
}