package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ClassificationRepository;
import java.util.List;
import java.util.Optional;
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
    public List<ClassificationDTO> getClassificationsByArtTitle(String artTitle) {
        List<Classification> classifications = classificationRepository.findByArtTitleContaining(artTitle);
        List<Classification> fullClassifications = classifications.stream()
                .map(cls -> classificationRepository.findWithArtsById(cls.getId()).orElse(cls))
                .collect(Collectors.toList());

        if (fullClassifications.isEmpty()) {
            LOGGER.warn("No classifications found for artwork title: {}", artTitle);
        }
        return fullClassifications.stream().map(this::convertToDTO).collect(Collectors.toList());
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
    public List<ClassificationDTO> getAllClassifications() {
        List<Classification> classifications = classificationRepository.findAllWithArts();
        classifications.forEach(c -> cacheService.getClassificationCache().put(c.getId(), c));
        return classifications.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassificationDTO getClassificationById(int id) {
        return cacheService.getClassificationCache().get(id)
                .map(this::convertToDTO)
                .orElseGet(() -> {
                    Optional<Classification> classification = classificationRepository.findWithArtsById(id);
                    if (classification.isPresent()) {
                        Classification cls = classification.get();
                        cacheService.getClassificationCache().put(cls.getId(), cls);
                        return convertToDTO(cls);
                    }
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public List<ClassificationDTO> getClassificationsByName(String name) {
        List<Classification> classifications = classificationRepository.findByNameContainingIgnoreCase(name);
        List<Classification> fullClassifications = classifications.stream()
                .map(cls -> classificationRepository.findWithArtsById(cls.getId()).orElse(cls))
                .collect(Collectors.toList());

        if (fullClassifications.isEmpty()) {
            LOGGER.warn("No classifications found with name containing: {}", name);
        } else {
            fullClassifications.forEach(c -> cacheService.getClassificationCache().put(c.getId(), c));
        }
        return fullClassifications.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public Classification saveClassification(Classification classification) {
        if (classification == null) {
            throw new ValidationException("Classification data cannot be null");
        }
        if (classification.getName() == null || classification.getName().trim().isEmpty()) {
            throw new ValidationException("Classification name is required");
        }
        if (classification.getDescription() == null || classification.getDescription().trim().isEmpty()) {
            throw new ValidationException("Classification description is required");
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

    private ClassificationDTO convertToDTO(Classification classification) {
        ClassificationDTO dto = new ClassificationDTO();
        dto.setId(classification.getId());
        dto.setName(classification.getName());
        dto.setDescription(classification.getDescription());

        if (classification.getArts() != null && !classification.getArts().isEmpty()) {
            List<String> titles = classification.getArts().stream()
                    .map(Art::getTitle)
                    .collect(Collectors.toList());
            dto.setArtworkTitles(titles);
            dto.setArtworkCount(titles.size());
        } else {
            dto.setArtworkCount(0);
        }

        return dto;
    }
}