package com.example.artshop.service;

import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ClassificationRepository;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {

    @Mock
    private ClassificationRepository classificationRepository;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private EntityCache<Classification> classificationCache;
    
    @InjectMocks
    private ClassificationService classificationService;
    
    private Classification classification;
    private ClassificationDTO classificationDTO;
    private ClassificationPatchDTO patchDTO;

    @BeforeEach
    void setUp() {
        classification = new Classification();
        classification.setId(1);
        classification.setName("Painting");
        classification.setDescription("Oil on canvas");
        
        classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Painting");
        classificationDTO.setDescription("Oil on canvas");
        
        patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated");
        
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
    }

    @Test
    void getClassificationsByArtTitle_ShouldReturnClassifications() {
        when(classificationRepository.findByArtTitleContaining("art")).thenReturn(List.of(classification));
        
        List<Classification> result = classificationService.getClassificationsByArtTitle("art");
        
        assertEquals(1, result.size());
    }

    @Test
    void getAllClassifications_ShouldReturnAll() {
        when(classificationRepository.findAll()).thenReturn(List.of(classification));
        
        List<Classification> result = classificationService.getAllClassifications();
        
        assertEquals(1, result.size());
    }

    @Test
    void getClassificationById_ShouldReturnFromCache() {
        when(classificationCache.get(1)).thenReturn(Optional.of(classification));
        
        Classification result = classificationService.getClassificationById(1);
        
        assertEquals("Painting", result.getName());
    }

    @Test
    void getClassificationById_ShouldReturnFromRepository() {
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        when(classificationRepository.findById(1)).thenReturn(classification);
        
        Classification result = classificationService.getClassificationById(1);
        
        assertEquals("Painting", result.getName());
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getClassificationsByName_ShouldReturnClassifications() {
        when(classificationRepository.findByNameContainingIgnoreCase("paint")).thenReturn(List.of(classification));
        
        List<Classification> result = classificationService.getClassificationsByName("paint");
        
        assertEquals(1, result.size());
        verify(classificationCache).put(1, classification);
    }

    @Test
    void saveClassification_ShouldSave() {
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        
        Classification result = classificationService.saveClassification(classification);
        
        assertNotNull(result);
        verify(classificationCache).put(1, classification);
    }

    @Test
    void saveClassification_ShouldThrowValidationException_WhenDataIsNull() {
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(null));
    }

    @Test
    void patchClassification_ShouldPatch() {
        when(classificationRepository.findById(1)).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        
        Classification result = classificationService.patchClassification(1, patchDTO);
        
        assertEquals("Updated", result.getName());
        verify(classificationCache).update(1, classification);
    }

    @Test
    void patchClassification_ShouldThrowException_WhenNoUpdates() {
        patchDTO.setName(null);
        patchDTO.setDescription(null);
        
        assertThrows(IllegalArgumentException.class, () -> classificationService.patchClassification(1, patchDTO));
    }

    @Test
    void deleteClassification_ShouldDelete() {
        classificationService.deleteClassification(1);
        
        verify(classificationRepository).deleteById(1);
        verify(classificationCache).evict(1);
    }

    @Test
    void updateClassification_ShouldUpdate() {
        when(classificationRepository.findById(1)).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        
        Classification result = classificationService.updateClassification(1, classificationDTO);
        
        assertNotNull(result);
        verify(classificationCache).update(1, classification);
    }
}