package com.example.artshop.service;

import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ClassificationRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassificationService {
    @Autowired
    private ClassificationRepository classificationRepository;

    public List<Classification> getAllClassifications() {
        return classificationRepository.findAll();
    }

    public Classification getClassificationById(int id) {
        return classificationRepository.findById(id);
    }

    public Classification saveClassification(Classification classification) {
        return classificationRepository.save(classification);
    }

    @Transactional
    public Classification patchClassification(int id, ClassificationPatchDTO patchDTO) {
        if (!patchDTO.hasUpdates()) {
            throw new IllegalArgumentException("No fields to update");
        }

        Classification classification = classificationRepository.findById(id);
        if (classification == null) {
            return null;
        }

        if (patchDTO.getName() != null) {
            classification.setName(patchDTO.getName());
        }
        if (patchDTO.getDescription() != null) {
            classification.setDescription(patchDTO.getDescription());
        }

        return classificationRepository.save(classification);
    }

    public void deleteClassification(int id) {
        classificationRepository.deleteById(id);
    }

    @Transactional
    public Classification updateClassification(int id, ClassificationDTO classificationDTO) {
        Classification classification = classificationRepository.findById(id);
        if (classification == null) {
            return null;
        }

        classification.setName(classificationDTO.getName());
        classification.setDescription(classificationDTO.getDescription());

        return classificationRepository.save(classification);
    }
}