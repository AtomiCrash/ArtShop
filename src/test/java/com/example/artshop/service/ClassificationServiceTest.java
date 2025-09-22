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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        classificationDTO.setName("Painting");
        classificationDTO.setDescription("Oil painting");

        classification = new Classification();
        classification.setId(1);
        classification.setName("Painting");
        classification.setDescription("Oil painting");
    }

    @Test
    void addBulkClassifications_ValidDTOs_SavesAndCachesClassifications() {
        List<ClassificationDTO> dtos = List.of(classificationDTO);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        List<Classification> result = classificationService.addBulkClassifications(dtos);

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).put(1, classification);
    }

    @Test
    void addBulkClassifications_EmptyList_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(Collections.emptyList()));
        assertEquals("Classification list cannot be null or empty", exception.getMessage());
    }

    @Test
    void addBulkClassifications_NullList_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(null));
        assertEquals("Classification list cannot be null or empty", exception.getMessage());
    }

    @Test
    void addBulkClassifications_TooManyItems_ThrowsValidationException() {
        List<ClassificationDTO> dtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1; i++) {
            dtos.add(new ClassificationDTO());
        }
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(dtos));
        assertEquals("Cannot add more than " + ApplicationConstants.MAX_BULK_OPERATION_SIZE + " classifications at once", exception.getMessage());
    }

    @Test
    void addBulkClassifications_InvalidName_ThrowsValidationException() {
        classificationDTO.setName("");
        List<ClassificationDTO> dtos = List.of(classificationDTO);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(dtos));
        assertEquals("Classification name is required", exception.getMessage());
    }

    @Test
    void addBulkClassifications_InvalidDescription_ThrowsValidationException() {
        classificationDTO.setDescription("");
        List<ClassificationDTO> dtos = List.of(classificationDTO);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(dtos));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void getClassificationById_FromCache_ReturnsClassification() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationCache.get(1)).thenReturn(Optional.of(classification));

        ClassificationDTO result = classificationService.getClassificationById(1);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository, never()).findById(anyInt());
    }

    @Test
    void getClassificationById_NotInCache_FetchesFromRepository() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        when(classificationRepository.findById(1)).thenReturn(classification);

        ClassificationDTO result = classificationService.getClassificationById(1);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).findById(1);
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getClassificationById_NullFromRepository_ReturnsNull() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        when(classificationRepository.findById(1)).thenReturn(null);

        ClassificationDTO result = classificationService.getClassificationById(1);

        assertNull(result);
        verify(classificationCache, never()).put(anyInt(), any());
    }

    @Test
    void getAllClassifications_ReturnsAllClassifications() {
        when(classificationRepository.findAllWithArts()).thenReturn(List.of(classification));
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        List<ClassificationDTO> result = classificationService.getAllClassifications();

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getAllClassifications_Empty_ReturnsEmptyList() {
        when(classificationRepository.findAllWithArts()).thenReturn(Collections.emptyList());

        List<ClassificationDTO> result = classificationService.getAllClassifications();

        assertTrue(result.isEmpty());
    }

    @Test
    void patchClassification_ValidPatch_UpdatesAndCaches() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated Painting");
        patchDTO.setDescription("Updated description");

        when(classificationRepository.findById(1)).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        verify(classificationCache).update(1, classification);
    }

    @Test
    void patchClassification_NoUpdates_ThrowsIllegalArgumentException() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> classificationService.patchClassification(1, patchDTO));
        assertEquals("No fields to update", exception.getMessage());
    }

    @Test
    void patchClassification_NotFound_ReturnsNull() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated");

        when(classificationRepository.findById(1)).thenReturn(null);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNull(result);
        verify(classificationCache, never()).update(anyInt(), any());
    }

    @Test
    void updateClassification_ValidDTO_UpdatesClassification() {
        when(classificationRepository.findById(1)).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.updateClassification(1, classificationDTO);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).update(1, classification);
    }

    @Test
    void updateClassification_NullDTO_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, null));
        assertEquals("Classification data cannot be null", exception.getMessage());
    }

    @Test
    void updateClassification_EmptyName_ThrowsValidationException() {
        classificationDTO.setName("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, classificationDTO));
        assertEquals("Classification name is required", exception.getMessage());
    }

    @Test
    void deleteClassification_DeletesAndEvictsCache() {
        classificationService.deleteClassification(1);

        verify(classificationRepository).deleteById(1);
        verify(classificationCache).evict(1);
    }

    @Test
    void getClassificationsByName_ValidName_ReturnsClassifications() {
        when(classificationRepository.findByNameContainingIgnoreCase("Painting")).thenReturn(List.of(classification));
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        List<ClassificationDTO> result = classificationService.getClassificationsByName("Painting");

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getClassificationsByArtTitle_ValidTitle_ReturnsClassifications() {
        when(classificationRepository.findByArtTitleContaining("Mona Lisa")).thenReturn(List.of(classification));

        List<ClassificationDTO> result = classificationService.getClassificationsByArtTitle("Mona Lisa");

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
    }

    @Test
    void saveClassification_ValidClassification_SavesAndCaches() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.saveClassification(classification);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).save(any(Classification.class));
        verify(classificationCache).put(1, classification);
    }

    @Test
    void saveClassification_Null_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(null));
        assertEquals("Classification data cannot be null", exception.getMessage());
    }

    @Test
    void getCacheInfo_DelegatesToCache() {
        when(classificationCache.getCacheInfo()).thenReturn("Cache info");

        String result = classificationService.getCacheInfo();

        assertEquals("Cache info", result);
        verify(classificationCache).getCacheInfo();
    }

    @Test
    void testSaveClassification_WithNullDescription_ShouldThrowValidationException() {
        Classification invalidClassification = new Classification();
        invalidClassification.setName("Test");
        invalidClassification.setDescription(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(invalidClassification));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void testSaveClassification_WithEmptyDescription_ShouldThrowValidationException() {
        Classification invalidClassification = new Classification();
        invalidClassification.setName("Test");
        invalidClassification.setDescription("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(invalidClassification));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void testUpdateClassification_WithNullDescription_ShouldThrowValidationException() {
        ClassificationDTO invalidDTO = new ClassificationDTO();
        invalidDTO.setName("Test");
        invalidDTO.setDescription(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, invalidDTO));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void testUpdateClassification_WithEmptyDescription_ShouldThrowValidationException() {
        ClassificationDTO invalidDTO = new ClassificationDTO();
        invalidDTO.setName("Test");
        invalidDTO.setDescription("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, invalidDTO));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void testPatchClassification_UpdateOnlyName_ShouldSucceed() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated Name");
        patchDTO.setDescription(null);

        when(classificationRepository.findById(1)).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        verify(classificationCache).update(1, classification);
    }

    @Test
    void testPatchClassification_UpdateOnlyDescription_ShouldSucceed() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName(null);
        patchDTO.setDescription("Updated Description");

        when(classificationRepository.findById(1)).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        verify(classificationCache).update(1, classification);
    }

    @Test
    void testGetClassificationsByName_EmptyResults_ShouldReturnEmptyList() {
        when(classificationRepository.findByNameContainingIgnoreCase("Nonexistent")).thenReturn(Collections.emptyList());
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        List<ClassificationDTO> result = classificationService.getClassificationsByName("Nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetClassificationsByArtTitle_EmptyResults_ShouldReturnEmptyList() {
        when(classificationRepository.findByArtTitleContaining("Nonexistent")).thenReturn(Collections.emptyList());

        List<ClassificationDTO> result = classificationService.getClassificationsByArtTitle("Nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void testAddBulkClassifications_WithNullName_ShouldThrowValidationException() {
        ClassificationDTO invalidDTO = new ClassificationDTO();
        invalidDTO.setName(null);
        invalidDTO.setDescription("Description");

        List<ClassificationDTO> dtos = List.of(invalidDTO);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(dtos));
        assertEquals("Classification name is required", exception.getMessage());
    }

    @Test
    void testAddBulkClassifications_WithNullDescription_ShouldThrowValidationException() {
        ClassificationDTO invalidDTO = new ClassificationDTO();
        invalidDTO.setName("Test");
        invalidDTO.setDescription(null);

        List<ClassificationDTO> dtos = List.of(invalidDTO);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.addBulkClassifications(dtos));
        assertEquals("Classification description is required", exception.getMessage());
    }
}