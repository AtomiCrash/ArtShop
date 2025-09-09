package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ArtRepository;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.repository.ClassificationRepository;
import com.example.artshop.service.cache.EntityCache;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtServiceTest {

    @Mock
    private ArtRepository artRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ClassificationRepository classificationRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private EntityCache<Art> artCache;

    @Mock
    private EntityCache<Artist> artistCache;

    @Mock
    private EntityCache<Classification> classificationCache;

    @InjectMocks
    private ArtService artService;

    private Art art;
    private ArtDTO artDTO;
    private Artist artist;
    private Classification classification;

    @BeforeEach
    void setUp() {
        artist = new Artist();
        artist.setId(1);
        artist.setFirstName("John");
        artist.setLastName("Doe");

        classification = new Classification();
        classification.setId(1);
        classification.setName("Painting");
        classification.setDescription("Oil painting");

        art = new Art();
        art.setId(1);
        art.setTitle("Mona Lisa");
        art.setYear(1503);
        art.setArtists(new HashSet<>(List.of(artist)));
        art.setClassification(classification);

        artDTO = new ArtDTO();
        artDTO.setId(1);
        artDTO.setTitle("Mona Lisa");
        artDTO.setYear(1503);

        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setId(1);
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");
        artDTO.setArtists(List.of(artistDTO));

        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setId(1);
        classificationDTO.setName("Painting");
        classificationDTO.setDescription("Oil painting");
        artDTO.setClassification(classificationDTO);
    }

    @Test
    void testAddArt_Success() {
        when(classificationRepository.findByName("Painting")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
        verify(artCache).put(1, art);
    }

    @Test
    void testAddArt_NullData() {
        assertThrows(ValidationException.class, () -> artService.addArt(null));
    }

    @Test
    void testAddArt_NoTitle() {
        artDTO.setTitle(null);
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_EmptyTitle() {
        artDTO.setTitle("");
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_FutureYear() {
        artDTO.setYear(LocalDate.now().getYear() + 1);
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_InvalidClassification() {
        artDTO.getClassification().setName(null);
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_InvalidArtist() {
        artDTO.getArtists().get(0).setFirstName(null);
        artDTO.getArtists().get(0).setLastName(null);
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_ExistingClassification() {
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(classificationRepository, never()).save(any());
    }

    @Test
    void testAddArt_NewArtist() {
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void testGetArtById_FoundInCache() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testGetArtById_NotFoundInCacheFoundInRepo() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        verify(artCache).put(1, art);
    }

    @Test
    void testGetArtById_NotFound() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtById(1));
    }

    @Test
    void testUpdateArt_Success() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        verify(artCache).update(1, art);
    }

    @Test
    void testUpdateArt_NotFound() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testUpdateArt_NullClassification() {
        artDTO.setClassification(null);
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertNull(result.getClassification());
    }

    @Test
    void testUpdateArt_NullArtists() {
        artDTO.setArtists(null);
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertNull(result.getArtists());
    }

    @Test
    void testDeleteArtById_Success() {
        Art artWithArtists = new Art();
        artWithArtists.setId(1);
        artWithArtists.setArtists(new HashSet<>(List.of(artist)));

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(artWithArtists));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        artService.deleteArtById(1);

        verify(artRepository).delete(artWithArtists);
        verify(artCache).evict(1);
    }

    @Test
    void testDeleteArtById_NotFound() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.deleteArtById(1));
    }

    @Test
    void testPatchArt_Success() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(artCache).update(1, art);
    }

    @Test
    void testPatchArt_UpdateClassification() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findById(2)).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(classificationCache).update(2, classification);
    }

    @Test
    void testPatchArt_UpdateArtists() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        Artist newArtist = new Artist();
        newArtist.setId(2);
        newArtist.setFirstName("Jane");
        newArtist.setLastName("Smith");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findAllById(any())).thenReturn(List.of(newArtist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(artistCache).update(2, newArtist);
    }

    @Test
    void testPatchArt_NoUpdates() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testPatchArt_NullPatchDTO() {
        assertThrows(ValidationException.class, () -> artService.patchArt(1, null));
    }

    @Test
    void testPatchArt_EmptyTitle() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testPatchArt_InvalidYear() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testPatchArt_NotFound() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testPatchArt_ClassificationNotFound() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findById(999)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testPatchArt_ArtistsNotFound() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testGetArtsByClassificationId() {
        when(artRepository.findByClassificationId(1)).thenReturn(List.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByClassificationId(1);

        assertEquals(1, result.size());
        verify(artCache).put(1, art);
    }

    @Test
    void testGetArtsByClassificationName() {
        when(artRepository.findByClassificationNameContainingIgnoreCase("Painting")).thenReturn(List.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByClassificationName("Painting");

        assertEquals(1, result.size());
    }

    @Test
    void testGetArtByTitle() {
        when(artRepository.findByTitle("Mona Lisa")).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtByTitle("Mona Lisa");

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testGetArtByTitle_NotFound() {
        when(artRepository.findByTitle("Unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtByTitle("Unknown"));
    }

    @Test
    void testGetArtsByArtistName() {
        when(artRepository.findByArtistsLastNameContainingIgnoreCase("Doe")).thenReturn(List.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByArtistName("Doe");

        assertEquals(1, result.size());
        verify(artCache).put(1, art);
    }

    @Test
    void testGetAllArts() {
        when(artRepository.findAllWithArtistsAndClassification()).thenReturn(List.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getAllArts();

        assertEquals(1, result.size());
        verify(artCache).put(1, art);
    }

    @Test
    void testAddBulkArts_Success() {
        List<ArtDTO> dtos = List.of(artDTO);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
    }

    @Test
    void testAddBulkArts_EmptyList() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of()));
    }

    @Test
    void testAddBulkArts_NullList() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(null));
    }

    @Test
    void testAddBulkArts_TooManyItems() {
        List<ArtDTO> dtos = Arrays.asList(new ArtDTO[ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1]);
        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_InvalidArt() {
        artDTO.setTitle(null);
        List<ArtDTO> dtos = List.of(artDTO);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testGetCacheInfo() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.getCacheInfo()).thenReturn("Cache info");

        String result = artService.getCacheInfo();

        assertEquals("Cache info", result);
    }

    @Test
    void testGetArtCache() {
        when(cacheService.getArtCache()).thenReturn(artCache);

        EntityCache<Art> result = artService.getArtCache();

        assertEquals(artCache, result);
    }

    @Test
    void testAddArt_WithNullYear_ShouldSucceed() {
        artDTO.setYear(null);

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertNull(result.getYear());
    }

    @Test
    void testAddArt_WithNullArtists_ShouldSucceed() {
        artDTO.setArtists(null);

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertNull(result.getArtists());
    }

    @Test
    void testAddArt_WithNullClassification_ShouldSucceed() {
        artDTO.setClassification(null);

        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertNull(result.getClassification());
    }

    @Test
    void testUpdateArt_WithNullYear_ShouldSucceed() {
        artDTO.setYear(null);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertNull(result.getYear());
    }

    @Test
    void testPatchArt_WithNullTitle_ShouldNotUpdateTitle() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
        assertEquals(1600, result.getYear());
    }

    @Test
    void testPatchArt_WithNullYear_ShouldNotUpdateYear() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();;

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals(1503, result.getYear());
    }

    @Test
    void testGetArtsByClassificationId_EmptyResults_ShouldReturnEmptyList() {
        when(artRepository.findByClassificationId(999)).thenReturn(Collections.emptyList());
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByClassificationId(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetArtsByClassificationName_EmptyResults_ShouldReturnEmptyList() {
        when(artRepository.findByClassificationNameContainingIgnoreCase("Nonexistent")).thenReturn(Collections.emptyList());
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByClassificationName("Nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetArtsByArtistName_EmptyResults_ShouldReturnEmptyList() {
        when(artRepository.findByArtistsLastNameContainingIgnoreCase("Nonexistent")).thenReturn(Collections.emptyList());
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByArtistName("Nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void testAddBulkArts_WithNullClassification_ShouldSucceed() {
        artDTO.setClassification(null);
        List<ArtDTO> dtos = List.of(artDTO);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertNull(result.get(0).getClassification());
    }

    // Добавить в существующий ArtServiceTest.java

    @Test
    void testAddArt_WithEmptyArtistList_ShouldSucceed() {
        artDTO.setArtists(Collections.emptyList());

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void testAddArt_WithZeroYear_ShouldSucceed() {
        artDTO.setYear(0);

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals(0, result.getYear());
    }

    @Test
    void testUpdateArt_WithEmptyArtists_ShouldClearArtists() {
        artDTO.setArtists(Collections.emptyList());

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void testUpdateArt_WithNullYear_ShouldSetNullYear() {
        artDTO.setYear(null);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertNull(result.getYear());
    }

    @Test
    void testGetArtById_WithNullClassification_ShouldHandleGracefully() {
        Art artWithoutClassification = new Art();
        artWithoutClassification.setId(2);
        artWithoutClassification.setTitle("No Classification");
        artWithoutClassification.setClassification(null);
        artWithoutClassification.setArtists(new HashSet<>());

        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(2)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(2)).thenReturn(Optional.of(artWithoutClassification));

        ArtDTO result = artService.getArtById(2);

        assertNotNull(result);
        assertEquals("No Classification", result.getTitle());
        assertNull(result.getClassification());
    }

    @Test
    void testGetArtById_WithNullArtists_ShouldHandleGracefully() {
        Art artWithoutArtists = new Art();
        artWithoutArtists.setId(3);
        artWithoutArtists.setTitle("No Artists");
        artWithoutArtists.setClassification(classification);
        artWithoutArtists.setArtists(null);

        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(3)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(3)).thenReturn(Optional.of(artWithoutArtists));

        ArtDTO result = artService.getArtById(3);

        assertNotNull(result);
        assertEquals("No Artists", result.getTitle());
        assertNull(result.getArtists());
    }

    @Test
    void testAddBulkArts_WithEmptyArtistList_ShouldSucceed() {
        artDTO.setArtists(Collections.emptyList());
        List<ArtDTO> dtos = List.of(artDTO);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getArtists().isEmpty());
    }

    @Test
    void testAddBulkArts_WithNullYear_ShouldSucceed() {
        artDTO.setYear(null);
        List<ArtDTO> dtos = List.of(artDTO);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertNull(result.get(0).getYear());
    }

    @Test
    void testConvertToDTO_WithNullValues_ShouldHandleGracefully() {
        Art nullArt = new Art();
        nullArt.setId(1);
        nullArt.setTitle("Test");
        nullArt.setYear(null);
        nullArt.setClassification(null);
        nullArt.setArtists(null);

        // Use reflection to test private method or test through public method
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(nullArt));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.empty());

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        assertEquals("Test", result.getTitle());
        assertNull(result.getYear());
        assertNull(result.getClassification());
        assertNull(result.getArtists());
    }
    // Добавить в ArtServiceTest.java

    @Test
    void testProcessArtist_WithExistingArtistById_ShouldReturnArtist() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setId(1);

        when(artistRepository.findById(1)).thenReturn(Optional.of(artist));

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void testProcessArtist_WithNewArtist_ShouldSaveAndReturnArtist() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("New");
        artistDTO.setLastName("Artist");

        when(artistRepository.findByFirstNameAndLastName("New", "Artist")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        verify(artistRepository).save(any(Artist.class));
    }


    @Test
    void testPatchArt_WithYearUpdate_ShouldSucceed() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(artCache).update(1, art);
    }

    @Test
    void testPatchArt_WithClassificationUpdate_ShouldSucceed() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        Classification newClassification = new Classification();
        newClassification.setId(2);
        newClassification.setName("Sculpture");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findById(2)).thenReturn(newClassification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(classificationCache).update(2, newClassification);
    }

    @Test
    void testPatchArt_WithArtistsUpdate_ShouldSucceed() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        Artist newArtist = new Artist();
        newArtist.setId(2);
        newArtist.setFirstName("Jane");
        newArtist.setLastName("Smith");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findAllById(any())).thenReturn(List.of(newArtist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(artistCache).update(2, newArtist);
    }

    @Test
    void testPatchArt_WithMultipleUpdates_ShouldSucceed() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(artCache).update(1, art);
    }

    @Test
    void testPatchArt_WithEmptyArtistIds_ShouldClearArtists() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void testPatchArt_WithZeroClassificationId_ShouldNotUpdateClassification() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("Painting", result.getClassification().getName());
    }

    @Test
    void testProcessClassification_WithExistingId_ShouldReturnClassification() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setId(1);

        when(classificationRepository.findById(1)).thenReturn(classification);

        Classification result = artService.processClassification(classificationDTO);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void testProcessClassification_WithExistingName_ShouldReturnClassification() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Painting");

        when(classificationRepository.findByName("Painting")).thenReturn(classification);

        Classification result = artService.processClassification(classificationDTO);

        assertNotNull(result);
        assertEquals("Painting", result.getName());
    }

    @Test
    void testProcessClassification_WithNewClassification_ShouldSaveAndReturn() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("NewType");
        classificationDTO.setDescription("New Description");

        Classification newClassification = new Classification();
        newClassification.setId(2);
        newClassification.setName("NewType");
        newClassification.setDescription("New Description");

        when(classificationRepository.findByName("NewType")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(newClassification);

        Classification result = artService.processClassification(classificationDTO);

        assertNotNull(result);
        assertEquals("NewType", result.getName());
        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void testProcessArtist_WithExistingId_ShouldReturnArtist() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setId(1);

        when(artistRepository.findById(1)).thenReturn(Optional.of(artist));

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void testProcessArtist_WithExistingName_ShouldReturnArtist() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");

        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
    }

    @Test
    void testProcessArtist_WithNewArtist_ShouldSaveAndReturn() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("New");
        artistDTO.setLastName("Artist");

        Artist newArtist = new Artist();
        newArtist.setId(2);
        newArtist.setFirstName("New");
        newArtist.setLastName("Artist");

        when(artistRepository.findByFirstNameAndLastName("New", "Artist")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals("New", result.getFirstName());
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void testProcessArtist_WithMissingLastName_ShouldThrowException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        // lastName is null

        assertThrows(IllegalArgumentException.class, () -> artService.processArtist(artistDTO));
    }

    @Test
    void testValidateArtists_WithValidArtist_ShouldNotThrow() {
        ArtistDTO validArtist = new ArtistDTO();
        validArtist.setFirstName("John");
        validArtist.setLastName("Doe");

        assertDoesNotThrow(() -> artService.validateArtists(List.of(validArtist)));
    }

    @Test
    void testValidateArtists_WithInvalidArtist_ShouldThrow() {
        ArtistDTO invalidArtist = new ArtistDTO();
        // Both firstName and lastName are null

        assertThrows(ValidationException.class, () -> artService.validateArtists(List.of(invalidArtist)));
    }

    @Test
    void testValidateClassification_WithValidClassification_ShouldNotThrow() {
        ClassificationDTO validClassification = new ClassificationDTO();
        validClassification.setName("Painting");
        validClassification.setDescription("Description");

        assertDoesNotThrow(() -> artService.validateClassification(validClassification));
    }

    @Test
    void testValidateClassification_WithInvalidClassification_ShouldThrow() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("");
        invalidClassification.setDescription("Description");

        assertThrows(ValidationException.class, () -> artService.validateClassification(invalidClassification));
    }

    @Test
    void testAddSingleArt_ShouldSaveAndCache() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);

        Art savedArt = new Art();
        savedArt.setId(1);
        savedArt.setTitle("Test Art");
        savedArt.setYear(2020);

        when(artRepository.save(any(Art.class))).thenReturn(savedArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addSingleArt(artDTO);

        assertNotNull(result);
        assertEquals("Test Art", result.getTitle());
        verify(artCache).put(1, savedArt);
    }

    @Test
    void testUpdateArtists_ShouldClearAndSetNewArtists() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>(List.of(artist)));

        Set<Integer> newArtistIds = new HashSet<>(Arrays.asList(2));

        Artist newArtist = new Artist();
        newArtist.setId(2);
        newArtist.setFirstName("Jane");
        newArtist.setLastName("Smith");

        when(artistRepository.findAllById(newArtistIds)).thenReturn(List.of(newArtist));

        artService.updateArtists(art, newArtistIds);

        assertEquals(1, art.getArtists().size());
        assertTrue(art.getArtists().contains(newArtist));
    }

    @Test
    void testUpdateArtists_WithEmptySet_ShouldClearArtists() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>(List.of(artist)));

        artService.updateArtists(art, new HashSet<>());

        assertTrue(art.getArtists().isEmpty());
    }

    @Test
    void testUpdateArtists_WithMissingArtists_ShouldThrowException() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>(List.of(artist)));

        Set<Integer> newArtistIds = new HashSet<>(Arrays.asList(2, 3));

        when(artistRepository.findAllById(newArtistIds)).thenReturn(List.of(artist)); // Only one artist found

        assertThrows(EntityNotFoundException.class, () -> artService.updateArtists(art, newArtistIds));
    }

    @Test
    void testPatchArt_WithTitleUpdate_ShouldSucceed() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        verify(artCache).update(1, art);
    }

    @Test
    void testAddArt_CallsProcessClassification_WhenClassificationProvided() {
        when(classificationRepository.findByName("Painting")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);

        verify(classificationRepository).findByName("Painting");
    }

    @Test
    void testAddArt_CallsProcessArtist_WhenArtistsProvided() {
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);

        verify(artistRepository).findByFirstNameAndLastName("John", "Doe");
    }

    @Test
    void testUpdateArt_CallsValidateMethods_ShouldSucceed() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);

        verify(artRepository).save(any(Art.class));
    }

    @Test
    void testAddArt_ValidationFails_WhenInvalidArtist() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName(null);
        invalidArtist.setLastName(null);
        artDTO.setArtists(List.of(invalidArtist));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_ValidationFails_WhenInvalidClassification() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("");
        invalidClassification.setDescription("Description");
        artDTO.setClassification(invalidClassification);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddBulkArts_CallsValidateMethods_ShouldSucceed() {
        List<ArtDTO> dtos = List.of(artDTO);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());

        verify(artRepository).save(any(Art.class));
    }

    @Test
    void testGetArtById_WithArtistsButNullClassification_ShouldHandleGracefully() {
        Art artWithNullClassification = new Art();
        artWithNullClassification.setId(4);
        artWithNullClassification.setTitle("Test Art");
        artWithNullClassification.setClassification(null);
        artWithNullClassification.setArtists(new HashSet<>(List.of(artist)));

        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(4)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(4)).thenReturn(Optional.of(artWithNullClassification));

        ArtDTO result = artService.getArtById(4);

        assertNotNull(result);
        assertEquals("Test Art", result.getTitle());
        assertNull(result.getClassification());
        assertNotNull(result.getArtists());
    }

    @Test
    void testGetArtById_WithClassificationButNullArtists_ShouldHandleGracefully() {
        Art artWithNullArtists = new Art();
        artWithNullArtists.setId(5);
        artWithNullArtists.setTitle("Test Art 2");
        artWithNullArtists.setClassification(classification);
        artWithNullArtists.setArtists(null);

        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(5)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(5)).thenReturn(Optional.of(artWithNullArtists));

        ArtDTO result = artService.getArtById(5);

        assertNotNull(result);
        assertEquals("Test Art 2", result.getTitle());
        assertNotNull(result.getClassification());
        assertNull(result.getArtists());
    }

    @Test
    void testConvertToDTO_ComprehensiveTest() {
        Art testArt = new Art();
        testArt.setId(6);
        testArt.setTitle("Comprehensive Test");
        testArt.setYear(2000);
        testArt.setClassification(null);
        testArt.setArtists(null);

        when(artRepository.findWithArtistsAndClassificationById(6)).thenReturn(Optional.of(testArt));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(6)).thenReturn(Optional.empty());

        ArtDTO result = artService.getArtById(6);

        assertNotNull(result);
        assertEquals("Comprehensive Test", result.getTitle());
        assertEquals(2000, result.getYear());
        assertNull(result.getClassification());
        assertNull(result.getArtists());
    }

    // Добавить в ArtServiceTest.java

    @Test
    void testAddArt_WithEmptyArtistName_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("");
        invalidArtist.setLastName("");
        artDTO.setArtists(List.of(invalidArtist));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithWhitespaceOnlyNames_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("   ");
        invalidArtist.setLastName("   ");
        artDTO.setArtists(List.of(invalidArtist));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithLongFirstName_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("A".repeat(61));
        invalidArtist.setLastName("Doe");
        artDTO.setArtists(List.of(invalidArtist));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithLongMiddleName_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("John");
        invalidArtist.setMiddleName("A".repeat(61));
        invalidArtist.setLastName("Doe");
        artDTO.setArtists(List.of(invalidArtist));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithLongLastName_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("John");
        invalidArtist.setLastName("A".repeat(61));
        artDTO.setArtists(List.of(invalidArtist));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithEmptyClassificationName_ShouldThrowValidationException() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("");
        invalidClassification.setDescription("Valid description");
        artDTO.setClassification(invalidClassification);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithEmptyClassificationDescription_ShouldThrowValidationException() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("Valid name");
        invalidClassification.setDescription("");
        artDTO.setClassification(invalidClassification);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testAddArt_WithWhitespaceClassification_ShouldThrowValidationException() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("   ");
        invalidClassification.setDescription("   ");
        artDTO.setClassification(invalidClassification);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_WithInvalidArtist_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName(null);
        invalidArtist.setLastName(null);
        artDTO.setArtists(List.of(invalidArtist));

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testUpdateArt_WithInvalidClassification_ShouldThrowValidationException() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("");
        invalidClassification.setDescription("Description");
        artDTO.setClassification(invalidClassification);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddBulkArts_WithInvalidArtistInList_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName(null);
        invalidArtist.setLastName(null);
        artDTO.setArtists(List.of(invalidArtist));

        List<ArtDTO> dtos = List.of(artDTO);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_WithInvalidClassificationInList_ShouldThrowValidationException() {
        ClassificationDTO invalidClassification = new ClassificationDTO();
        invalidClassification.setName("");
        invalidClassification.setDescription("Description");
        artDTO.setClassification(invalidClassification);

        List<ArtDTO> dtos = List.of(artDTO);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testProcessArtist_WithNullLastName_ShouldThrowException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName(null);

        assertThrows(IllegalArgumentException.class, () -> artService.processArtist(artistDTO));
    }

    @Test
    void testProcessArtist_WithEmptyLastName_ShouldThrowException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName("");

        assertThrows(IllegalArgumentException.class, () -> artService.processArtist(artistDTO));
    }

    @Test
    void testProcessClassification_WithNullName_ShouldReturnNull() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName(null);

        Classification result = artService.processClassification(classificationDTO);

        assertNull(result);
    }

    @Test
    void testValidateArtists_WithEmptyList_ShouldNotThrow() {
        assertDoesNotThrow(() -> artService.validateArtists(Collections.emptyList()));
    }

    @Test
    void testValidateArtists_WithNullList_ShouldNotThrow() {
        assertDoesNotThrow(() -> artService.validateArtists(null));
    }

    @Test
    void testUpdateArtists_WithNullArtistsSet_ShouldHandleGracefully() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(null);

        Set<Integer> newArtistIds = new HashSet<>(Arrays.asList(2));

        Artist newArtist = new Artist();
        newArtist.setId(2);
        newArtist.setFirstName("Jane");
        newArtist.setLastName("Smith");

        when(artistRepository.findAllById(newArtistIds)).thenReturn(List.of(newArtist));

        assertDoesNotThrow(() -> artService.updateArtists(art, newArtistIds));
        assertNotNull(art.getArtists());
        assertEquals(1, art.getArtists().size());
    }

    @Test
    void testAddSingleArt_WithNullClassification_ShouldHandleGracefully() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);
        artDTO.setClassification(null);

        Art savedArt = new Art();
        savedArt.setId(1);
        savedArt.setTitle("Test Art");
        savedArt.setYear(2020);
        savedArt.setClassification(null);

        when(artRepository.save(any(Art.class))).thenReturn(savedArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addSingleArt(artDTO);

        assertNotNull(result);
        assertNull(result.getClassification());
    }

    @Test
    void testAddSingleArt_WithNullArtists_ShouldHandleGracefully() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);
        artDTO.setArtists(null);

        Art savedArt = new Art();
        savedArt.setId(1);
        savedArt.setTitle("Test Art");
        savedArt.setYear(2020);
        savedArt.setArtists(null);

        when(artRepository.save(any(Art.class))).thenReturn(savedArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addSingleArt(artDTO);

        assertNotNull(result);
        assertNull(result.getArtists());
    }

    @Test
    void testGetArtByTitle_WithEmptyString_ShouldThrowNotFoundException() {
        when(artRepository.findByTitle("")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtByTitle(""));
    }

    @Test
    void testGetArtByTitle_WithWhitespace_ShouldThrowNotFoundException() {
        when(artRepository.findByTitle("   ")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtByTitle("   "));
    }

    @Test
    void testGetArtsByClassificationName_WithEmptyString_ShouldReturnEmptyList() {
        when(artRepository.findByClassificationNameContainingIgnoreCase("")).thenReturn(Collections.emptyList());
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByClassificationName("");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetArtsByArtistName_WithEmptyString_ShouldReturnEmptyList() {
        when(artRepository.findByArtistsLastNameContainingIgnoreCase("")).thenReturn(Collections.emptyList());
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByArtistName("");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetArtsByClassificationId_WithNullId_ShouldReturnEmptyList() {
        when(artRepository.findByClassificationId(null)).thenReturn(Collections.emptyList());
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getArtsByClassificationId(null);

        assertTrue(result.isEmpty());
    }

    // Тесты для edge cases с годами
    @Test
    void testAddArt_WithYearZero_ShouldSucceed() {
        artDTO.setYear(0);

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals(0, result.getYear());
    }

    @Test
    void testAddArt_WithYear999_ShouldSucceed() {
        artDTO.setYear(999);

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals(999, result.getYear());
    }

    @Test
    void testAddArt_WithCurrentYear_ShouldSucceed() {
        int currentYear = LocalDate.now().getYear();
        artDTO.setYear(currentYear);

        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals(currentYear, result.getYear());
    }

    @Test
    void testProcessClassification_WithNullDTO_ShouldReturnNull() {
        Classification result = artService.processClassification(null);
        assertNull(result);
    }

    @Test
    void testProcessArtist_WithNullDTO_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> artService.processArtist(null));
    }

    @Test
    void testGetArtById_WithCachedArt_ShouldReturnFromCache() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        verify(artRepository, never()).findWithArtistsAndClassificationById(anyInt());
    }

    @Test
    void testGetArtById_WithNullCachedArt_ShouldFetchFromRepository() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        verify(artRepository).findWithArtistsAndClassificationById(1);
    }
}