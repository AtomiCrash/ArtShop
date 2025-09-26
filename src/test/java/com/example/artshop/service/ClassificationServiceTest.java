package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.dto.ClassificationPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ClassificationRepository;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private Classification classificationWithArts;

    @BeforeEach
    void setUp() {
        classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Painting");
        classificationDTO.setDescription("Oil painting");

        classification = new Classification();
        classification.setId(1);
        classification.setName("Painting");
        classification.setDescription("Oil painting");

        classificationWithArts = new Classification();
        classificationWithArts.setId(2);
        classificationWithArts.setName("Sculpture");
        classificationWithArts.setDescription("Bronze sculpture");

        Art art = new Art();
        art.setTitle("Mona Lisa");
        classificationWithArts.setArts(Set.of(art));
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
        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }

    @Test
    void getClassificationById_NotInCache_FetchesFromRepository() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));

        ClassificationDTO result = classificationService.getClassificationById(1);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
        verify(classificationRepository).findWithArtsById(1);
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getClassificationById_NotFound_ThrowsNotFoundException() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationCache.get(1)).thenReturn(Optional.empty());
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> classificationService.getClassificationById(1));
        assertEquals("Classification not found with id: 1", exception.getMessage());
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
        verify(cacheService, never()).getClassificationCache();
    }

    @Test
    void patchClassification_ValidPatch_UpdatesAndCaches() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated Painting");
        patchDTO.setDescription("Updated description");

        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        verify(classificationCache).update(1, classification);
    }

    @Test
    void patchClassification_NoUpdates_ThrowsValidationException() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.patchClassification(1, patchDTO));
        assertEquals("No fields to update", exception.getMessage());

        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }

    @Test
    void patchClassification_NotFound_ThrowsNotFoundException() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated");

        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> classificationService.patchClassification(1, patchDTO));
        assertEquals("Classification not found with id: 1", exception.getMessage());
    }

    @Test
    void updateClassification_ValidDTO_UpdatesClassification() {
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));
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
        // Не мокаем findWithArtsById, так как валидация должна произойти раньше
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, null));
        assertEquals("Classification data cannot be null", exception.getMessage());

        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }

    @Test
    void updateClassification_EmptyName_ThrowsValidationException() {
        classificationDTO.setName("");

        // Не мокаем findWithArtsById, так как валидация должна произойти раньше
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, classificationDTO));
        assertEquals("Classification name is required", exception.getMessage());

        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }

    @Test
    void updateClassification_EmptyDescription_ThrowsValidationException() {
        classificationDTO.setDescription("");

        // Не мокаем findWithArtsById, так как валидация должна произойти раньше
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.updateClassification(1, classificationDTO));
        assertEquals("Classification description is required", exception.getMessage());

        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }

    @Test
    void updateClassification_NotFound_ThrowsNotFoundException() {
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> classificationService.updateClassification(1, classificationDTO));
        assertEquals("Classification not found with id: 1", exception.getMessage());
    }

    @Test
    void deleteClassification_DeletesAndEvictsCache() {
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        classificationService.deleteClassification(1);

        verify(classificationRepository).delete(classification);
        verify(classificationCache).evict(1);
    }

    @Test
    void deleteClassification_NotFound_ThrowsNotFoundException() {
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> classificationService.deleteClassification(1));
        assertEquals("Classification not found with id: 1", exception.getMessage());

        verify(classificationRepository, never()).delete(any());
        verify(cacheService, never()).getClassificationCache();
    }

    @Test
    void getClassificationsByName_ValidName_ReturnsClassifications() {
        when(classificationRepository.findByNameContainingIgnoreCase("Painting")).thenReturn(List.of(classification));
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        List<ClassificationDTO> result = classificationService.getClassificationsByName("Painting");

        assertEquals(1, result.size());
        assertEquals("Painting", result.get(0).getName());
        verify(classificationCache).put(1, classification);
    }

    @Test
    void getClassificationsByName_EmptyResults_ShouldReturnEmptyList() {
        when(classificationRepository.findByNameContainingIgnoreCase("Nonexistent")).thenReturn(Collections.emptyList());

        List<ClassificationDTO> result = classificationService.getClassificationsByName("Nonexistent");

        assertTrue(result.isEmpty());
        verify(classificationRepository, never()).findWithArtsById(anyInt());
        verify(cacheService, never()).getClassificationCache();
    }

    @Test
    void getClassificationsByArtTitle_ValidTitle_ReturnsClassifications() {
        when(classificationRepository.findByArtTitleContaining("Mona Lisa")).thenReturn(List.of(classificationWithArts));
        when(classificationRepository.findWithArtsById(2)).thenReturn(Optional.of(classificationWithArts));

        List<ClassificationDTO> result = classificationService.getClassificationsByArtTitle("Mona Lisa");

        assertEquals(1, result.size());
        assertEquals("Sculpture", result.get(0).getName());
        assertEquals(1, result.get(0).getArtworkCount());
    }

    @Test
    void getClassificationsByArtTitle_EmptyResults_ShouldReturnEmptyList() {
        when(classificationRepository.findByArtTitleContaining("Nonexistent")).thenReturn(Collections.emptyList());

        List<ClassificationDTO> result = classificationService.getClassificationsByArtTitle("Nonexistent");

        assertTrue(result.isEmpty());
        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }

    @Test
    void saveClassification_ValidClassification_SavesAndCaches() {
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
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
    void saveClassification_WithNullName_ShouldThrowValidationException() {
        Classification invalidClassification = new Classification();
        invalidClassification.setName(null);
        invalidClassification.setDescription("Description");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(invalidClassification));
        assertEquals("Classification name is required", exception.getMessage());
    }

    @Test
    void saveClassification_WithEmptyName_ShouldThrowValidationException() {
        Classification invalidClassification = new Classification();
        invalidClassification.setName("");
        invalidClassification.setDescription("Description");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(invalidClassification));
        assertEquals("Classification name is required", exception.getMessage());
    }

    @Test
    void saveClassification_WithNullDescription_ShouldThrowValidationException() {
        Classification invalidClassification = new Classification();
        invalidClassification.setName("Test");
        invalidClassification.setDescription(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(invalidClassification));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void saveClassification_WithEmptyDescription_ShouldThrowValidationException() {
        Classification invalidClassification = new Classification();
        invalidClassification.setName("Test");
        invalidClassification.setDescription("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.saveClassification(invalidClassification));
        assertEquals("Classification description is required", exception.getMessage());
    }

    @Test
    void getCacheInfo_DelegatesToCache() {
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(classificationCache.getCacheInfo()).thenReturn("Cache info");

        String result = classificationService.getCacheInfo();

        assertEquals("Cache info", result);
        verify(classificationCache).getCacheInfo();
    }

    @Test
    void testPatchClassification_UpdateOnlyName_ShouldSucceed() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setName("Updated Name");

        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        verify(classificationCache).update(1, classification);
    }

    @Test
    void testPatchClassification_UpdateOnlyDescription_ShouldSucceed() {
        ClassificationPatchDTO patchDTO = new ClassificationPatchDTO();
        patchDTO.setDescription("Updated Description");

        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification));
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Classification result = classificationService.patchClassification(1, patchDTO);

        assertNotNull(result);
        assertEquals("Updated Description", result.getDescription());
        verify(classificationCache).update(1, classification);
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

    @Test
    void testConvertToDTO_WithArts() {
        // Создаем classification с arts для теста конвертации
        Classification classificationWithArts = new Classification();
        classificationWithArts.setId(3);
        classificationWithArts.setName("Test Category");
        classificationWithArts.setDescription("Test Description");

        Art art1 = new Art();
        art1.setTitle("Artwork 1");
        Art art2 = new Art();
        art2.setTitle("Artwork 2");
        classificationWithArts.setArts(Set.of(art1, art2));

        // Тестируем через метод, который использует convertToDTO
        when(classificationRepository.findWithArtsById(3)).thenReturn(Optional.of(classificationWithArts));
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        ClassificationDTO result = classificationService.getClassificationById(3);

        assertNotNull(result);
        assertEquals(3, result.getId());
        assertEquals("Test Category", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals(2, result.getArtworkCount());
        assertNotNull(result.getArtworkTitles());
        assertEquals(2, result.getArtworkTitles().size());
    }

    @Test
    void testConvertToDTO_WithoutArts() {
        Classification classificationWithoutArts = new Classification();
        classificationWithoutArts.setId(4);
        classificationWithoutArts.setName("Empty Category");
        classificationWithoutArts.setDescription("No artworks");
        classificationWithoutArts.setArts(Collections.emptySet());

        when(classificationRepository.findWithArtsById(4)).thenReturn(Optional.of(classificationWithoutArts));
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        ClassificationDTO result = classificationService.getClassificationById(4);

        assertNotNull(result);
        assertEquals(4, result.getId());
        assertEquals("Empty Category", result.getName());
        assertEquals("No artworks", result.getDescription());
        assertEquals(0, result.getArtworkCount());
        assertNull(result.getArtworkTitles()); // или пустой список, в зависимости от реализации
    }

    @Test
    void testGetClassificationsByArtTitle_WithMultipleClassifications() {
        Classification classification1 = new Classification();
        classification1.setId(1);
        classification1.setName("Painting");

        Classification classification2 = new Classification();
        classification2.setId(2);
        classification2.setName("Sculpture");

        when(classificationRepository.findByArtTitleContaining("Art")).thenReturn(List.of(classification1, classification2));
        when(classificationRepository.findWithArtsById(1)).thenReturn(Optional.of(classification1));
        when(classificationRepository.findWithArtsById(2)).thenReturn(Optional.of(classification2));

        List<ClassificationDTO> result = classificationService.getClassificationsByArtTitle("Art");

        assertEquals(2, result.size());
        assertEquals("Painting", result.get(0).getName());
        assertEquals("Sculpture", result.get(1).getName());
    }

    @Test
    void testPatchClassification_NullPatchDTO_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> classificationService.patchClassification(1, null));
        assertEquals("No fields to update", exception.getMessage());

        verify(classificationRepository, never()).findWithArtsById(anyInt());
    }
}