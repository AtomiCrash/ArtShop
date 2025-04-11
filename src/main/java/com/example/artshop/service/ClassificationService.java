package com.example.artshop.service;

import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ClassificationRepository;
import com.example.artshop.service.cache.EntityCache;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassificationService {
    private final ClassificationRepository classificationRepository;
    private final EntityCache<Classification> classificationCache;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationService.class);

    @Autowired
    public ClassificationService(ClassificationRepository classificationRepository) {
        this.classificationRepository = classificationRepository;
        this.classificationCache = new EntityCache<>("Classification");
    }

    @Transactional(readOnly = true)
    public List<Classification> getClassificationsByArtTitle(String artTitle) {
        List<Classification> classifications = classificationRepository.findByArtTitleContaining(artTitle);
        if (classifications.isEmpty()) {
            LOGGER.warn("No classifications found for artwork title: {}", artTitle);
        }
        return classifications;
    }

    @Transactional(readOnly = true)
    public List<Classification> getAllClassifications() {
        return classificationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Classification getClassificationById(int id) {
        return classificationCache.get(id)
                .orElseGet(() -> {
                    Classification classification = classificationRepository.findById(id);
                    if (classification != null) {
                        classificationCache.put(id, classification);
                    }
                    return classification;
                });
    }

    @Transactional(readOnly = true)
    public List<Classification> getClassificationsByName(String name) {
        List<Classification> classifications = classificationRepository.findByNameContainingIgnoreCase(name);
        if (classifications.isEmpty()) {
            LOGGER.warn("No classifications found with name containing: {}", name);
        } else {
            classifications.forEach(c -> classificationCache.put(c.getId(), c));
        }
        return classifications;
    }

    @Transactional
    public Classification saveClassification(Classification classification) {
        Classification saved = classificationRepository.save(classification);
        classificationCache.put(saved.getId(), saved);
        return saved;
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

        Classification updated = classificationRepository.save(classification);
        classificationCache.update(id, updated);
        return updated;
    }

    @Transactional
    public void deleteClassification(int id) {
        classificationRepository.deleteById(id);
        classificationCache.evict(id);
    }

    @Transactional
    public Classification updateClassification(int id, ClassificationDTO classificationDTO) {
        Classification classification = classificationRepository.findById(id);
        if (classification == null) {
            return null;
        }

        classification.setName(classificationDTO.getName());
        classification.setDescription(classificationDTO.getDescription());

        Classification updated = classificationRepository.save(classification);
        classificationCache.update(id, updated);
        return updated;
    }

    public String getCacheInfo() {
        return classificationCache.getCacheInfo();
    }
}