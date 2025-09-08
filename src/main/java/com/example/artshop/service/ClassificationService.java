package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ClassificationRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassificationService {
    private final ClassificationRepository classificationRepository;
    private final CacheService cacheService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationService.class);

    @Autowired
    public ClassificationService(ClassificationRepository classificationRepository,
                                 CacheService cacheService) {
        this.classificationRepository = classificationRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public List<Classification> getClassificationsByArtTitle(String artTitle) {
        List<Classification> classifications = classificationRepository.findByArtTitleContaining(artTitle);
        if (classifications.isEmpty()) {
            LOGGER.warn("No classifications found for artwork title: {}", artTitle);
        }
        return classifications;
    }

    @Transactional
    public List<Classification> addBulkClassifications(List<ClassificationDTO> classificationDTOs) {
        if (classificationDTOs == null || classificationDTOs.isEmpty()) {
            throw new ValidationException("Classification list cannot be null or empty");
        }
        if (classificationDTOs.size() > ApplicationConstants.MAX_BULK_OPERATION_SIZE) {
            throw new ValidationException("Cannot add more than " +
                    ApplicationConstants.MAX_BULK_OPERATION_SIZE + " classifications at once");
        }

        return classificationDTOs.stream()
                .peek(dto -> {
                    if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                        throw new ValidationException("Classification name is required");
                    }
                    if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
                        throw new ValidationException("Classification description is required");
                    }
                })
                .map(dto -> {
                    Classification classification = new Classification();
                    classification.setName(dto.getName());
                    classification.setDescription(dto.getDescription());
                    return saveClassification(classification);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Classification> getAllClassifications() {
        List<Classification> classifications = classificationRepository.findAllWithArts();
        System.out.println("Classifications loaded: " + classifications.size());
        classifications.forEach(classification -> {
            System.out.println("Classification " + classification.getId() + " has " + (classification.getArts() != null ? classification.getArts().size() : "null") + " arts");
            if (classification.getArts() != null) {
                classification.getArts().forEach(art -> System.out.println("Art: " + art.getTitle()));
            }
        });
        classifications.forEach(c -> cacheService.getClassificationCache().put(c.getId(), c));
        return classifications;
    }

    @Transactional(readOnly = true)
    public Classification getClassificationById(int id) {
        return cacheService.getClassificationCache().get(id)
                .orElseGet(() -> {
                    Classification classification = classificationRepository.findById(id);
                    if (classification != null) {
                        cacheService.getClassificationCache().put(id, classification);
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
            classifications.forEach(c -> cacheService.getClassificationCache().put(c.getId(), c));
        }
        return classifications;
    }

    @Transactional
    public Classification saveClassification(Classification classification) {
        if (classification == null) {
            throw new ValidationException("Classification data cannot be null");
        }
        if (classification.getName() == null || classification.getName().trim().isEmpty()) {
            throw new ValidationException("Classification name is required");
        }
        if (classification.getName() != null && classification.getName().length() > 60) {
            throw new ValidationException("Classification name must be 60 characters or less");
        }

        if (classification.getDescription() == null || classification.getDescription().trim().isEmpty()) {
            throw new ValidationException("Classification description is required");
        }

        if (classification.getDescription() != null && classification.getDescription().length() > 120) {
            throw new ValidationException("Classification Description must be 120 characters or less");
        }

        Classification saved = classificationRepository.save(classification);
        cacheService.getClassificationCache().put(saved.getId(), saved);
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
        cacheService.getClassificationCache().update(id, updated);
        return updated;
    }

    @Transactional
    public void deleteClassification(int id) {
        classificationRepository.deleteById(id);
        cacheService.getClassificationCache().evict(id);
    }

    @Transactional
    public Classification updateClassification(int id, ClassificationDTO classificationDTO) {
        Classification classification;
        classification = classificationRepository.findById(id);
        if (classificationDTO == null) {
            throw new ValidationException("Classification data cannot be null");
        }
        if (classificationDTO.getName() == null || classificationDTO.getName().trim().isEmpty()) {
            throw new ValidationException("Classification name is required");
        }
        if (classificationDTO.getDescription() == null || classificationDTO.getDescription().trim().isEmpty()) {
            throw new ValidationException("Classification description is required");
        }
        
        classification.setName(classificationDTO.getName());
        classification.setDescription(classificationDTO.getDescription());
        Classification updated = classificationRepository.save(classification);
        cacheService.getClassificationCache().update(id, updated);
        return updated;
    }

    public String getCacheInfo() {
        return cacheService.getClassificationCache().getCacheInfo();
    }
}