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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
    private ClassificationPatchDTO classificationPatchDTO;

    @BeforeEach
    void setUp() {
        classification = new Classification("Painting", "Artwork created with paint");

        classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Sculpture");
        classificationDTO.setDescription("Three-dimensional artwork");

        classificationPatchDTO = new ClassificationPatchDTO();

        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
    }

    @Test
    void getClassificationsByArtTitle_WithResults() {
        when(classificationRepository.findByArtTitleContaining(anyString()))
                .thenReturn(Collections.singletonList(classification));

        List<Classification> result = classificationService.getClassificationsByArtTitle("Mona Lisa");
        assertEquals(1, result.size());
        assertEquals(classification, result.get(0));
    }

    @Test
    void getClassificationsByArtTitle_NoResults() {
        when(classificationRepository.findByArtTitleContaining(anyString()))
                .thenReturn(Collections.emptyList());

        List<Classification> result = classificationService.getClassificationsByArtTitle("Unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    void addBulkClassifications_ValidInput() {
        List<ClassificationDTO> dtos = Arrays.asList(classificationDTO, classificationDTO);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        doNothing().when(classificationCache).put(anyInt(), any(Classification.class));

        List<Classification> result = classificationService.addBulkClassifications(dtos);
        assertEquals(2, result.size());
    }

    @Test
    void addBulkClassifications_NullList() {
        assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(null));
    }

    @Test
    void addBulkClassifications_EmptyList() {
        assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(Collections.emptyList()));
    }

    @Test
    void addBulkClassifications_ExceedsMaxSize() {
        List<ClassificationDTO> largeList = Collections.nCopies(ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1, classificationDTO);
        assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(largeList));
    }

    @Test
    void addBulkClassifications_MissingName() {
        classificationDTO.setName(null);
        List<ClassificationDTO> dtos = Collections.singletonList(classificationDTO);
        assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(dtos));
    }

    @Test
    void addBulkClassifications_MissingDescription() {
        classificationDTO.setDescription(null);
        List<ClassificationDTO> dtos = Collections.singletonList(classificationDTO);
        assertThrows(ValidationException.class, () -> classificationService.addBulkClassifications(dtos));
    }

    @Test
    void getAllClassifications() {
        when(classificationRepository.findAll()).thenReturn(Collections.singletonList(classification));
        List<Classification> result = classificationService.getAllClassifications();
        assertEquals(1, result.size());
        assertEquals(classification, result.get(0));
    }

    @Test
    void getClassificationById_FromCache() {
        when(classificationCache.get(anyInt())).thenReturn(Optional.of(classification));
        Classification result = classificationService.getClassificationById(1);
        assertEquals(classification, result);
    }

    @Test
    void getClassificationById_FromRepository() {
        when(classificationCache.get(anyInt())).thenReturn(Optional.empty());
        when(classificationRepository.findById(anyInt())).thenReturn(classification);
        doNothing().when(classificationCache).put(anyInt(), any(Classification.class));

        Classification result = classificationService.getClassificationById(1);
        assertEquals(classification, result);
    }

    @Test
    void getClassificationById_NotFound() {
        when(classificationCache.get(anyInt())).thenReturn(Optional.empty());
        when(classificationRepository.findById(anyInt())).thenReturn(null);
        Classification result = classificationService.getClassificationById(1);
        assertNull(result);
    }

    @Test
    void getClassificationsByName_WithResults() {
        when(classificationRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.singletonList(classification));
        doNothing().when(classificationCache).put(anyInt(), any(Classification.class));

        List<Classification> result = classificationService.getClassificationsByName("Paint");
        assertEquals(1, result.size());
        assertEquals(classification, result.get(0));
    }

    @Test
    void getClassificationsByName_NoResults() {
        when(classificationRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());
        List<Classification> result = classificationService.getClassificationsByName("Unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    void saveClassification_ValidInput() {
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        doNothing().when(classificationCache).put(anyInt(), any(Classification.class));

        Classification result = classificationService.saveClassification(classification);
        assertEquals(classification, result);
    }

    @Test
    void saveClassification_NullInput() {
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(null));
    }

    @Test
    void saveClassification_MissingName() {
        classification.setName(null);
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(classification));
    }

    @Test
    void saveClassification_EmptyName() {
        classification.setName("");
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(classification));
    }

    @Test
    void saveClassification_NameTooLong() {
        classification.setName("a".repeat(61));
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(classification));
    }

    @Test
    void saveClassification_MissingDescription() {
        classification.setDescription(null);
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(classification));
    }

    @Test
    void saveClassification_EmptyDescription() {
        classification.setDescription("");
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(classification));
    }

    @Test
    void saveClassification_DescriptionTooLong() {
        classification.setDescription("a".repeat(121));
        assertThrows(ValidationException.class, () -> classificationService.saveClassification(classification));
    }

    @Test
    void patchClassification_NoUpdates() {
        classificationPatchDTO.setName(null);
        classificationPatchDTO.setDescription(null);
        assertThrows(IllegalArgumentException.class, () -> classificationService.patchClassification(1, classificationPatchDTO));
    }

    @Test
    void patchClassification_NotFound() {
        classificationPatchDTO.setName("Updated");
        when(classificationRepository.findById(anyInt())).thenReturn(null);
        Classification result = classificationService.patchClassification(1, classificationPatchDTO);
        assertNull(result);
    }

    @Test
    void patchClassification_UpdateName() {
        classificationPatchDTO.setName("Updated Name");
        when(classificationRepository.findById(anyInt())).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        doNothing().when(classificationCache).update(anyInt(), any(Classification.class));

        Classification result = classificationService.patchClassification(1, classificationPatchDTO);
        assertEquals("Updated Name", classification.getName());
    }

    @Test
    void patchClassification_UpdateDescription() {
        classificationPatchDTO.setDescription("Updated Description");
        when(classificationRepository.findById(anyInt())).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        doNothing().when(classificationCache).update(anyInt(), any(Classification.class));

        Classification result = classificationService.patchClassification(1, classificationPatchDTO);
        assertEquals("Updated Description", classification.getDescription());
    }

    @Test
    void deleteClassification() {
        doNothing().when(classificationRepository).deleteById(anyInt());
        doNothing().when(classificationCache).evict(anyInt());
        classificationService.deleteClassification(1);
    }

    @Test
    void updateClassification_ValidInput() {
        when(classificationRepository.findById(anyInt())).thenReturn(classification);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        doNothing().when(classificationCache).update(anyInt(), any(Classification.class));

        Classification result = classificationService.updateClassification(1, classificationDTO);
        assertEquals(classificationDTO.getName(), classification.getName());
    }

    @Test
    void updateClassification_NullInput() {
        assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, null));
    }

    @Test
    void updateClassification_MissingName() {
        classificationDTO.setName(null);
        assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, classificationDTO));
    }

    @Test
    void updateClassification_EmptyName() {
        classificationDTO.setName("");
        assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, classificationDTO));
    }

    @Test
    void updateClassification_MissingDescription() {
        classificationDTO.setDescription(null);
        assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, classificationDTO));
    }

    @Test
    void updateClassification_EmptyDescription() {
        classificationDTO.setDescription("");
        assertThrows(ValidationException.class, () -> classificationService.updateClassification(1, classificationDTO));
    }

    @Test
    void getCacheInfo() {
        when(classificationCache.getCacheInfo()).thenReturn("Cache info");
        String result = classificationService.getCacheInfo();
        assertEquals("Cache info", result);
    }
}