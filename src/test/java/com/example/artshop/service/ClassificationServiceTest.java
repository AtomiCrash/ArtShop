package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    private ClassificationDTO classificationDTO;
    private Classification classification;

    @BeforeEach
    void setUp() {
        classificationDTO = new ClassificationDTO();
        classificationDTO.setId(1);
        classificationDTO.setName("Painting");
        classificationDTO.setDescription("Oil painting");

        classification = new Classification();
        //classification.setId(1);
        classification.setName("Painting");
        classification.setDescription("Oil painting");

        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
    }

    @Test
    void addBulkClassifications_ValidDTOs_SavesAndCachesClassifications() {
        List<ClassificationDTO> dtos = new ArrayList<>(Collections.singletonList(classificationDTO));
        List<Classification> result = classificationService.addBulkClassifications(dtos);

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).put(eq(1), any(Classification.class));
    }

    @Test
    void addBulkClassifications_EmptyList_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(Collections.emptyList()));
        assertEquals("Classification list cannot be null or empty", exception.getMessage());
    }

    @Test
    void addBulkClassifications_TooManyItems_ThrowsValidationException() {
        List<ClassificationDTO> dtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1; i++) {
            dtos.add(new ClassificationDTO());
        }
        ValidationException exception = assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(dtos));
        assertEquals("Cannot add more than " + ApplicationConstants.MAX_BULK_OPERATION_SIZE + " classifications at once", exception.getMessage());
    }

    @Test
    void getClassificationById_FromCache_ReturnsClassification() {
        when(classificationCache.get(1)).thenReturn(Optional.of(classification));

        Classification result = classificationService.getClassificationById(1);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository, never()).findById(anyInt());
        verify(classificationCache).get(1);
    }

    @Test
    void getClassificationById_NotInCache_FetchesFromRepository() {
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        //when(classificationRepository.findById(1)).thenReturn(Optional.of(classification));

        Classification result = classificationService.getClassificationById(1);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).findById(1);
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getClassificationById_NullFromRepository_ReturnsNull() {
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        //when(classificationRepository.findById(1)).thenReturn(Optional.empty());

        Classification result = classificationService.getClassificationById(1);

        assertNull(result);
        verify(classificationCache).get(1);
        verify(classificationRepository).findById(1);
        verify(classificationCache, never()).put(anyInt(), any());
    }

    @Test
    void patchClassification_ValidPatch_UpdatesClassification() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated Painting");
        patchDTO.setDescription("Updated description");

        //when(classificationRepository.findById(1)).thenReturn(Optional.of(classification));
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        assertEquals("Updated Painting", result.getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).update(eq(1), any(Classification.class));
    }

    @Test
    void patchClassification_NoUpdates_ThrowsIllegalArgumentException() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> classificationService.patchClassification(1, patchDTO));
        assertEquals("No fields to update", exception.getMessage());
    }

    @Test
    void patchClassification_NullClassification_ReturnsNull() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated Painting");

        //when(classificationRepository.findById(1)).thenReturn(Optional.empty());

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNull(result);
        verify(classificationRepository).findById(1);
        verify(classificationRepository, never()).save(any());
        verify(classificationCache, never()).update(anyInt(), any());
    }

    @Test
    void deleteClassification_ExistingId_RemovesAndEvictsFromCache() {
        classificationService.deleteClassification(1);

        verify(classificationRepository).deleteById(1);
        verify(classificationCache).evict(1);
    }

    @Test
    void updateClassification_ValidDTO_UpdatesClassification() {
        //when(classificationRepository.findById(1)).thenReturn(Optional.of(classification));
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);

        Classification result = classificationService.updateClassification(1, classificationDTO);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).update(eq(1), any(Classification.class));
    }

    @Test
    void updateClassification_NullDTO_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, null));
        assertEquals("Classification data cannot be null", exception.getMessage());
    }

    @Test
    void updateClassification_EmptyName_ThrowsValidationException() {
        classificationDTO.setName("");
        ValidationException exception = assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, classificationDTO));
        assertEquals("Classification name is required", exception.getMessage());
    }

    @Test
    void getClassificationsByName_ValidName_ReturnsClassifications() {
        when(classificationRepository.findByNameContainingIgnoreCase("Painting")).thenReturn(Collections.singletonList(classification));

        List<Classification> result = classificationService.getClassificationsByName("Painting");

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationCache).put(eq(1), eq(classification));
    }

    @Test
    void getClassificationsByArtTitle_ValidTitle_ReturnsClassifications() {
        when(classificationRepository.findByArtTitleContaining("Mona Lisa")).thenReturn(Collections.singletonList(classification));

        List<Classification> result = classificationService.getClassificationsByArtTitle("Mona Lisa");

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationRepository).findByArtTitleContaining("Mona Lisa");
    }

    @Test
    void getAllClassifications_ReturnsAllClassifications() {
        when(classificationRepository.findAll()).thenReturn(Collections.singletonList(classification));

        List<Classification> result = classificationService.getAllClassifications();

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationRepository).findAll();
    }

    @Test
    void saveClassification_ValidClassification_SavesAndCaches() {
        Classification result = classificationService.saveClassification(classification);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).put(eq(1), any(Classification.class));
    }

    @Test
    void saveClassification_Null_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> classificationService.saveClassification(null));
        assertEquals("Classification data cannot be null", exception.getMessage());
    }

    @Test
    void getCacheInfo_DelegatesToCache() {
        when(classificationCache.getCacheInfo()).thenReturn("Cache info");

        String result = classificationService.getCacheInfo();

        assertEquals("Cache info", result);
        verify(classificationCache).getCacheInfo();
    }
}