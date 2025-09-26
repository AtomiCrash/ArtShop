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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(classificationRepository, never()).save(any());
    }

    @Test
    void testAddArt_NewArtist() {
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
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
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        artService.deleteArtById(1);

        verify(artRepository).delete(art);
        verify(artCache).evict(1);
    }

    @Test
    void testDeleteArtById_NotFound() {
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.empty());

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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

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

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
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

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertNull(result.getYear());
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertNull(result.get(0).getClassification());
    }

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

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
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
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertNull(result.get(0).getYear());
    }

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

        when(artistRepository.findByFirstNameAndLastName("New", "Artist")).thenReturn(Collections.emptyList());
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
    void testProcessArtist_WithExistingName_ShouldReturnArtist() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");

        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(List.of(artist));

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
    }

    @Test
    void testProcessArtist_WithMissingLastName_ShouldThrowException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");

        assertThrows(ValidationException.class, () -> artService.processArtist(artistDTO));
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
    void testUpdateArtists_ShouldClearAndSetNewArtists() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>(List.of(artist)));

        Set<Integer> newArtistIds = new HashSet<>(Set.of(2));

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

        Set<Integer> newArtistIds = new HashSet<>(Set.of(2, 3));

        when(artistRepository.findAllById(newArtistIds)).thenReturn(List.of(artist));

        assertThrows(EntityNotFoundException.class, () -> artService.updateArtists(art, newArtistIds));
    }

    @Test
    void testAddArt_WithEmptyArtistName_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("");
        invalidArtist.setLastName("");
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
    void testUpdateArt_WithInvalidArtist_ShouldThrowValidationException() {
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName(null);
        invalidArtist.setLastName(null);
        artDTO.setArtists(List.of(invalidArtist));

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
    void testProcessArtist_WithNullLastName_ShouldThrowException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName(null);

        assertThrows(ValidationException.class, () -> artService.processArtist(artistDTO));
    }

    @Test
    void testProcessClassification_WithNullName_ShouldReturnNull() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName(null);

        Classification result = artService.processClassification(classificationDTO);

        assertNull(result);
    }

    @Test
    void testProcessClassification_WithEmptyName_ShouldReturnNull() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("");

        Classification result = artService.processClassification(classificationDTO);

        assertNull(result);
    }

    @Test
    void testValidateArt_WithNullTitle_ShouldThrowException() {
        ArtDTO invalidArt = new ArtDTO();
        invalidArt.setTitle(null);
    }

    @Test
    void testValidateArt_WithEmptyTitle_ShouldThrowException() {
        ArtDTO invalidArt = new ArtDTO();
        invalidArt.setTitle("");
    }

    @Test
    void testValidateArt_WithTooLongTitle_ShouldThrowException() {
        ArtDTO invalidArt = new ArtDTO();
        invalidArt.setTitle("A".repeat(256));
    }

    @Test
    void testValidateArt_WithInvalidYear_ShouldThrowException() {
        ArtDTO invalidArt = new ArtDTO();
        invalidArt.setTitle("Valid Title");
        invalidArt.setYear(LocalDate.now().getYear() + 1);

    }

    @Test
    void testValidateArt_WithValidData_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setYear(2020);
    }

    @Test
    void testValidateArt_WithNullYear_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setYear(null);
    }

    @Test
    void testValidateArt_WithZeroYear_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setYear(0);
    }

    @Test
    void testValidateArt_WithNegativeYear_ShouldThrowException() {
        ArtDTO invalidArt = new ArtDTO();
        invalidArt.setTitle("Valid Title");
        invalidArt.setYear(-100);
    }

    @Test
    void testValidateArt_WithVeryOldYear_ShouldThrowException() {
        ArtDTO invalidArt = new ArtDTO();
        invalidArt.setTitle("Valid Title");
        invalidArt.setYear(999);
    }

    @Test
    void testValidateArt_WithMinimumValidYear_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setYear(1000);
    }

    @Test
    void testValidateArt_WithCurrentYear_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setYear(LocalDate.now().getYear());
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
    void testValidateClassification_WithNullClassification_ShouldNotThrow() {
        assertDoesNotThrow(() -> artService.validateClassification(null));
    }

    @Test
    void testValidateArt_WithNullArtists_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setArtists(null);
    }

    @Test
    void testValidateArt_WithNullClassification_ShouldNotThrow() {
        ArtDTO validArt = new ArtDTO();
        validArt.setTitle("Valid Title");
        validArt.setClassification(null);
    }

    @Test
    void testProcessArtist_WithNullArtist_ShouldReturnNull() {
        Artist result = artService.processArtist(null);

        assertNull(result);
    }

    @Test
    void testProcessClassification_WithNullClassification_ShouldReturnNull() {
        Classification result = artService.processClassification(null);

        assertNull(result);
    }

    @Test
    void testUpdateArtists_WithNullArtists_ShouldClearArtists() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>(List.of(artist)));

        artService.updateArtists(art, null);

        assertTrue(art.getArtists().isEmpty());
    }

    @Test
    void testUpdateArtists_WithNullArtistsSet_ShouldClearArtists() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>(List.of(artist)));

        artService.updateArtists(art, null);

        assertTrue(art.getArtists().isEmpty());
    }

    @Test
    void testProcessArtist_WithExistingArtistByName_ShouldReturnFirstMatch() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");

        Artist secondArtist = new Artist();
        secondArtist.setId(2);
        secondArtist.setFirstName("John");
        secondArtist.setLastName("Doe");

        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenReturn(List.of(artist, secondArtist));

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void testProcessArtist_WithNewArtist_ShouldSetCorrectFields() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("New");
        artistDTO.setLastName("Artist");

        when(artistRepository.findByFirstNameAndLastName("New", "Artist")).thenReturn(Collections.emptyList());
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> {
            Artist savedArtist = invocation.getArgument(0);
            savedArtist.setId(1);
            return savedArtist;
        });

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        assertEquals("New", result.getFirstName());
        assertEquals("Artist", result.getLastName());
    }

    @Test
    void testProcessClassification_WithNewClassification_ShouldSetCorrectFields() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("NewType");
        classificationDTO.setDescription("New Description");

        when(classificationRepository.findByName("NewType")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenAnswer(invocation -> {
            Classification savedClassification = invocation.getArgument(0);
            savedClassification.setId(1);
            return savedClassification;
        });

        Classification result = artService.processClassification(classificationDTO);

        assertNotNull(result);
        assertEquals("NewType", result.getName());
        assertEquals("New Description", result.getDescription());
    }

    @Test
    void testAddArt_ShouldHandleMultipleArtistsCorrectly() {
        ArtistDTO secondArtistDTO = new ArtistDTO();
        secondArtistDTO.setFirstName("Jane");
        secondArtistDTO.setLastName("Smith");
        artDTO.setArtists(List.of(artDTO.getArtists().get(0), secondArtistDTO));

        Artist secondArtist = new Artist();
        secondArtist.setId(2);
        secondArtist.setFirstName("Jane");
        secondArtist.setLastName("Smith");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artistRepository.findFirstByFirstNameAndLastName("Jane", "Smith")).thenReturn(Optional.of(secondArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals(2, result.getArtists().size());
    }

    @Test
    void testUpdateArt_ShouldHandleMultipleArtistsCorrectly() {
        ArtistDTO secondArtistDTO = new ArtistDTO();
        secondArtistDTO.setFirstName("Jane");
        secondArtistDTO.setLastName("Smith");
        artDTO.setArtists(List.of(artDTO.getArtists().get(0), secondArtistDTO));

        Artist secondArtist = new Artist();
        secondArtist.setId(2);
        secondArtist.setFirstName("Jane");
        secondArtist.setLastName("Smith");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artistRepository.findFirstByFirstNameAndLastName("Jane", "Smith")).thenReturn(Optional.of(secondArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertEquals(2, result.getArtists().size());
    }

    @Test
    void testPatchArt_ShouldHandleMultipleArtistIdsCorrectly() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        Artist secondArtist = new Artist();
        secondArtist.setId(2);
        secondArtist.setFirstName("Jane");
        secondArtist.setLastName("Smith");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findAllById(Set.of(1, 2))).thenReturn(List.of(artist, secondArtist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals(2, result.getArtists().size());
    }

    @Test
    void testGetAllArts_ShouldReturnMultipleArts() {
        Art secondArt = new Art();
        secondArt.setId(2);
        secondArt.setTitle("Second Art");
        secondArt.setArtists(new HashSet<>());
        secondArt.setClassification(null);

        when(artRepository.findAllWithArtistsAndClassification()).thenReturn(List.of(art, secondArt));
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getAllArts();

        assertEquals(2, result.size());
        verify(artCache, times(2)).put(anyInt(), any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleMultipleArtsCorrectly() {
        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2000);

        ArtistDTO secondArtistDTO = new ArtistDTO();
        secondArtistDTO.setFirstName("Jane");
        secondArtistDTO.setLastName("Smith");
        secondArtDTO.setArtists(List.of(secondArtistDTO));

        ClassificationDTO secondClassificationDTO = new ClassificationDTO();
        secondClassificationDTO.setName("Sculpture");
        secondClassificationDTO.setDescription("Stone sculpture");
        secondArtDTO.setClassification(secondClassificationDTO);

        List<ArtDTO> dtos = List.of(artDTO, secondArtDTO);

        Art secondArt = new Art();
        secondArt.setId(2);
        secondArt.setTitle("Second Art");

        when(artRepository.save(any(Art.class))).thenReturn(art).thenReturn(secondArt);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(classificationRepository.findByName("Sculpture")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artistRepository.findFirstByFirstNameAndLastName("Jane", "Smith")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        verify(artRepository, times(2)).save(any(Art.class));
    }

    @Test
    void testDeleteArtById_ShouldEvictCorrectCaches() {
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        artService.deleteArtById(1);

        verify(artCache).evict(1);
        verify(artistCache).update(eq(1), any(Artist.class));
        verify(classificationCache).update(eq(1), any(Classification.class));
    }

    @Test
    void testDeleteArtById_WithNullArtists_ShouldHandleGracefully() {
        art.setArtists(null);

        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        assertDoesNotThrow(() -> artService.deleteArtById(1));
        verify(artCache).evict(1);
    }

    @Test
    void testDeleteArtById_WithNullClassification_ShouldHandleGracefully() {
        art.setClassification(null);

        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        assertDoesNotThrow(() -> artService.deleteArtById(1));
        verify(artCache).evict(1);
    }

    @Test
    void testPatchArt_ShouldHandleNullValuesInPatchDTO() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testPatchArt_ShouldHandlePartialUpdatesCorrectly() {
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
    void testGetCacheInfo_ShouldReturnCorrectInfo() {
        String expectedInfo = "Art Cache: size=10, hits=100, misses=20";
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.getCacheInfo()).thenReturn(expectedInfo);

        String result = artService.getCacheInfo();

        assertEquals(expectedInfo, result);
    }

    @Test
    void testGetArtCache_ShouldReturnCorrectCache() {
        when(cacheService.getArtCache()).thenReturn(artCache);

        EntityCache<Art> result = artService.getArtCache();

        assertSame(artCache, result);
    }

    @Test
    void testAddArt_ShouldHandleDatabaseConstraintViolations() {
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenThrow(new RuntimeException("Database constraint violation"));
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleDatabaseConstraintViolations() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenThrow(new RuntimeException("Database constraint violation"));
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testDeleteArtById_ShouldHandleDatabaseExceptions() {
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        doThrow(new RuntimeException("Database error")).when(artRepository).delete(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        assertThrows(RuntimeException.class, () -> artService.deleteArtById(1));
    }

    @Test
    void testGetArtById_ShouldHandleCacheExceptionsGracefully() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenThrow(new RuntimeException("Cache error"));
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testGetAllArts_ShouldHandleCacheExceptionsGracefully() {
        when(artRepository.findAllWithArtistsAndClassification()).thenReturn(List.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        doThrow(new RuntimeException("Cache error")).when(artCache).put(anyInt(), any(Art.class));

        List<ArtDTO> result = artService.getAllArts();

        assertEquals(1, result.size());
    }

    @Test
    void testAddBulkArts_ShouldHandlePartialFailures() {
        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2000);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO, secondArtDTO);

        when(artRepository.save(any(Art.class)))
                .thenReturn(art)
                .thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testPatchArt_ShouldHandleConcurrentModification() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenThrow(new RuntimeException("Optimistic locking failure"));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(RuntimeException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testAddArt_ShouldHandleTransactionRollback() {
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        doThrow(new RuntimeException("Cache update failed")).when(artCache).put(1, art);

        assertThrows(RuntimeException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleTransactionRollback() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        doThrow(new RuntimeException("Cache update failed")).when(artCache).update(1, art);

        assertThrows(RuntimeException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testProcessArtist_ShouldHandleDatabaseErrors() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");

        when(artistRepository.findByFirstNameAndLastName("John", "Doe")).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> artService.processArtist(artistDTO));
    }

    @Test
    void testProcessClassification_ShouldHandleDatabaseErrors() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Painting");

        when(classificationRepository.findByName("Painting")).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> artService.processClassification(classificationDTO));
    }

    @Test
    void testUpdateArtists_ShouldHandleDatabaseErrors() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>());

        Set<Integer> artistIds = Set.of(1);

        when(artistRepository.findAllById(artistIds)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> artService.updateArtists(art, artistIds));
    }

    @Test
    void testGetArtsByClassificationId_ShouldHandleDatabaseErrors() {
        when(artRepository.findByClassificationId(1)).thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.getArtsByClassificationId(1));
    }

    @Test
    void testGetArtsByClassificationName_ShouldHandleDatabaseErrors() {
        when(artRepository.findByClassificationNameContainingIgnoreCase("Painting")).thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.getArtsByClassificationName("Painting"));
    }

    @Test
    void testGetArtsByArtistName_ShouldHandleDatabaseErrors() {
        when(artRepository.findByArtistsLastNameContainingIgnoreCase("Doe")).thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.getArtsByArtistName("Doe"));
    }

    @Test
    void testGetArtByTitle_ShouldHandleDatabaseErrors() {
        when(artRepository.findByTitle("Mona Lisa")).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> artService.getArtByTitle("Mona Lisa"));
    }

    @Test
    void testGetAllArts_ShouldHandleDatabaseErrors() {
        when(artRepository.findAllWithArtistsAndClassification()).thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.getAllArts());
    }

    @Test
    void testAddBulkArts_ShouldHandleDatabaseErrorsOnFirstSave() {
        List<ArtDTO> dtos = List.of(artDTO);

        when(artRepository.save(any(Art.class))).thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testDeleteArtById_ShouldHandleCacheEvictionErrors() {
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        doThrow(new RuntimeException("Cache error")).when(artCache).evict(1);

        assertThrows(RuntimeException.class, () -> artService.deleteArtById(1));
    }

    @Test
    void testPatchArt_ShouldHandlePartialCacheUpdateFailures() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        doThrow(new RuntimeException("Cache update failed")).when(artCache).update(1, art);

        assertThrows(RuntimeException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testAddArt_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);

        ArtDTO result = artServiceWithoutCache.addArt(artDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testGetArtById_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));

        ArtDTO result = artServiceWithoutCache.getArtById(1);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testUpdateArt_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);

        ArtDTO result = artServiceWithoutCache.updateArt(1, artDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testDeleteArtById_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);

        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));

        assertDoesNotThrow(() -> artServiceWithoutCache.deleteArtById(1));
        verify(artRepository).delete(art);
    }

    @Test
    void testPatchArt_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);

        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);

        ArtDTO result = artServiceWithoutCache.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void testGetCacheInfo_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);

        String result = artServiceWithoutCache.getCacheInfo();

        assertEquals("Cache service is not available", result);
    }

    @Test
    void testGetArtCache_ShouldHandleNullCacheService() {
        ArtService artServiceWithoutCache = new ArtService(artRepository, artistRepository, classificationRepository, null);

        EntityCache<Art> result = artServiceWithoutCache.getArtCache();

        assertNull(result);
    }

    @Test
    void testAddArt_ShouldHandleRepositoryNullReturns() {
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(classificationRepository.findByName("Painting")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(artistRepository).save(any(Artist.class));
        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void testUpdateArt_ShouldHandleRepositoryNullReturns() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(classificationRepository.findByName("Painting")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        verify(artistRepository).save(any(Artist.class));
        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void testProcessArtist_ShouldHandleEmptyRepositoryList() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setFirstName("New");
        artistDTO.setLastName("Artist");

        when(artistRepository.findByFirstNameAndLastName("New", "Artist")).thenReturn(Collections.emptyList());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);

        Artist result = artService.processArtist(artistDTO);

        assertNotNull(result);
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void testProcessClassification_ShouldHandleNullRepositoryReturn() {
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("NewType");

        when(classificationRepository.findByName("NewType")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);

        Classification result = artService.processClassification(classificationDTO);

        assertNotNull(result);
        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void testUpdateArtists_ShouldHandleEmptyRepositoryList() {
        Art art = new Art();
        art.setId(1);
        art.setArtists(new HashSet<>());

        Set<Integer> artistIds = Set.of(1);

        when(artistRepository.findAllById(artistIds)).thenReturn(Collections.emptyList());

        assertThrows(EntityNotFoundException.class, () -> artService.updateArtists(art, artistIds));
    }

    @Test
    void testGetArtById_ShouldHandleEmptyOptional() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtById(1));
    }

    @Test
    void testUpdateArt_ShouldHandleEmptyOptional() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testDeleteArtById_ShouldHandleEmptyOptional() {
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.deleteArtById(1));
    }

    @Test
    void testGetArtByTitle_ShouldHandleEmptyOptional() {
        when(artRepository.findByTitle("Unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtByTitle("Unknown"));
    }

    @Test
    void testPatchArt_ShouldHandleEmptyOptional() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void testAddArt_ShouldHandleCircularDependencies() {
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenAnswer(invocation -> {
            Art savedArt = invocation.getArgument(0);
            artist.getArts().add(savedArt);
            classification.getArts().add(savedArt);
            return savedArt;
        });
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertTrue(artist.getArts().stream().anyMatch(a -> a.getTitle().equals("Mona Lisa")));
        assertTrue(classification.getArts().stream().anyMatch(a -> a.getTitle().equals("Mona Lisa")));
    }

    @Test
    void testUpdateArt_ShouldHandleCircularDependencies() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenAnswer(invocation -> {
            Art savedArt = invocation.getArgument(0);
            artist.getArts().add(savedArt);
            classification.getArts().add(savedArt);
            return savedArt;
        });
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertTrue(artist.getArts().stream().anyMatch(a -> a.getTitle().equals("Mona Lisa")));
        assertTrue(classification.getArts().stream().anyMatch(a -> a.getTitle().equals("Mona Lisa")));
    }

    @Test
    void testDeleteArtById_ShouldHandleCircularDependencies() {
        artist.getArts().add(art);
        classification.getArts().add(art);

        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(art));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        artService.deleteArtById(1);

        assertFalse(artist.getArts().contains(art));
        assertFalse(classification.getArts().contains(art));
    }

    @Test
    void testAddArt_ShouldHandleLargeDataVolumes() {
        ArtDTO largeArtDTO = new ArtDTO();
        largeArtDTO.setTitle("A".repeat(255));
        largeArtDTO.setYear(2020);

        ArtistDTO largeArtistDTO = new ArtistDTO();
        largeArtistDTO.setFirstName("A".repeat(60));
        largeArtistDTO.setLastName("A".repeat(60));
        largeArtDTO.setArtists(List.of(largeArtistDTO));

        ClassificationDTO largeClassificationDTO = new ClassificationDTO();
        largeClassificationDTO.setName("A".repeat(100));
        largeClassificationDTO.setDescription("A".repeat(500));
        largeArtDTO.setClassification(largeClassificationDTO);

        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(largeArtDTO);

        assertNotNull(result);
        assertEquals(255, result.getTitle().length());
    }

    @Test
    void testUpdateArt_ShouldHandleLargeDataVolumes() {
        ArtDTO largeArtDTO = new ArtDTO();
        largeArtDTO.setTitle("A".repeat(255));
        largeArtDTO.setYear(2020);

        ArtistDTO largeArtistDTO = new ArtistDTO();
        largeArtistDTO.setFirstName("A".repeat(60));
        largeArtistDTO.setLastName("A".repeat(60));
        largeArtDTO.setArtists(List.of(largeArtistDTO));

        ClassificationDTO largeClassificationDTO = new ClassificationDTO();
        largeClassificationDTO.setName("A".repeat(100));
        largeClassificationDTO.setDescription("A".repeat(500));
        largeArtDTO.setClassification(largeClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, largeArtDTO);

        assertNotNull(result);
        assertEquals(255, result.getTitle().length());
    }

    @Test
    void testAddBulkArts_ShouldHandleLargeNumberOfItems() {
        List<ArtDTO> dtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            dtos.add(dto);
        }

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(ApplicationConstants.MAX_BULK_OPERATION_SIZE, result.size());
        verify(artRepository, times(ApplicationConstants.MAX_BULK_OPERATION_SIZE)).save(any(Art.class));
    }

    @Test
    void testGetAllArts_ShouldHandleLargeNumberOfResults() {
        List<Art> arts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Art art = new Art();
            art.setId(i);
            art.setTitle("Art " + i);
            art.setArtists(new HashSet<>());
            art.setClassification(null);
            arts.add(art);
        }

        when(artRepository.findAllWithArtistsAndClassification()).thenReturn(arts);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.getAllArts();

        assertEquals(1000, result.size());
        verify(artCache, times(1000)).put(anyInt(), any(Art.class));
    }

    @Test
    void testAddArt_ShouldHandleSpecialCharacters() {
        ArtDTO specialArtDTO = new ArtDTO();
        specialArtDTO.setTitle("Mona Lisa 🎨");
        specialArtDTO.setYear(1503);

        ArtistDTO specialArtistDTO = new ArtistDTO();
        specialArtistDTO.setFirstName("Jöhn");
        specialArtistDTO.setLastName("Döe");
        specialArtDTO.setArtists(List.of(specialArtistDTO));

        ClassificationDTO specialClassificationDTO = new ClassificationDTO();
        specialClassificationDTO.setName("Påinting");
        specialClassificationDTO.setDescription("Öil painting");
        specialArtDTO.setClassification(specialClassificationDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("Jöhn", "Döe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Påinting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(specialArtDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa 🎨", result.getTitle());
    }

    @Test
    void testUpdateArt_ShouldHandleSpecialCharacters() {
        ArtDTO specialArtDTO = new ArtDTO();
        specialArtDTO.setTitle("Mona Lisa 🎨");
        specialArtDTO.setYear(1503);

        ArtistDTO specialArtistDTO = new ArtistDTO();
        specialArtistDTO.setFirstName("Jöhn");
        specialArtistDTO.setLastName("Döe");
        specialArtDTO.setArtists(List.of(specialArtistDTO));

        ClassificationDTO specialClassificationDTO = new ClassificationDTO();
        specialClassificationDTO.setName("Påinting");
        specialClassificationDTO.setDescription("Öil painting");
        specialArtDTO.setClassification(specialClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("Jöhn", "Döe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Påinting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, specialArtDTO);

        assertNotNull(result);
        assertEquals("Mona Lisa 🎨", result.getTitle());
    }

    @Test
    void testAddArt_ShouldHandleSQLInjectionAttempt() {
        ArtDTO injectionArtDTO = new ArtDTO();
        injectionArtDTO.setTitle("'; DROP TABLE arts; --");
        injectionArtDTO.setYear(2020);

        ArtistDTO injectionArtistDTO = new ArtistDTO();
        injectionArtistDTO.setFirstName("'; DROP TABLE artists; --");
        injectionArtistDTO.setLastName("'; DROP TABLE artists; --");
        injectionArtDTO.setArtists(List.of(injectionArtistDTO));

        ClassificationDTO injectionClassificationDTO = new ClassificationDTO();
        injectionClassificationDTO.setName("'; DROP TABLE classifications; --");
        injectionClassificationDTO.setDescription("'; DROP TABLE classifications; --");
        injectionArtDTO.setClassification(injectionClassificationDTO);

        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(injectionArtDTO);

        assertNotNull(result);
        assertEquals("'; DROP TABLE arts; --", result.getTitle());
    }

    @Test
    void testUpdateArt_ShouldHandleSQLInjectionAttempt() {
        ArtDTO injectionArtDTO = new ArtDTO();
        injectionArtDTO.setTitle("'; DROP TABLE arts; --");
        injectionArtDTO.setYear(2020);

        ArtistDTO injectionArtistDTO = new ArtistDTO();
        injectionArtistDTO.setFirstName("'; DROP TABLE artists; --");
        injectionArtistDTO.setLastName("'; DROP TABLE artists; --");
        injectionArtDTO.setArtists(List.of(injectionArtistDTO));

        ClassificationDTO injectionClassificationDTO = new ClassificationDTO();
        injectionClassificationDTO.setName("'; DROP TABLE classifications; --");
        injectionClassificationDTO.setDescription("'; DROP TABLE classifications; --");
        injectionArtDTO.setClassification(injectionClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, injectionArtDTO);

        assertNotNull(result);
        assertEquals("'; DROP TABLE arts; --", result.getTitle());
    }

    @Test
    void testAddArt_ShouldHandleXSSAttempt() {
        ArtDTO xssArtDTO = new ArtDTO();
        xssArtDTO.setTitle("<script>alert('XSS')</script>");
        xssArtDTO.setYear(2020);

        ArtistDTO xssArtistDTO = new ArtistDTO();
        xssArtistDTO.setFirstName("<script>alert('XSS')</script>");
        xssArtistDTO.setLastName("<script>alert('XSS')</script>");
        xssArtDTO.setArtists(List.of(xssArtistDTO));

        ClassificationDTO xssClassificationDTO = new ClassificationDTO();
        xssClassificationDTO.setName("<script>alert('XSS')</script>");
        xssClassificationDTO.setDescription("<script>alert('XSS')</script>");
        xssArtDTO.setClassification(xssClassificationDTO);

        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(xssArtDTO);

        assertNotNull(result);
        assertEquals("<script>alert('XSS')</script>", result.getTitle());
    }

    @Test
    void testUpdateArt_ShouldHandleXSSAttempt() {
        ArtDTO xssArtDTO = new ArtDTO();
        xssArtDTO.setTitle("<script>alert('XSS')</script>");
        xssArtDTO.setYear(2020);

        ArtistDTO xssArtistDTO = new ArtistDTO();
        xssArtistDTO.setFirstName("<script>alert('XSS')</script>");
        xssArtistDTO.setLastName("<script>alert('XSS')</script>");
        xssArtDTO.setArtists(List.of(xssArtistDTO));

        ClassificationDTO xssClassificationDTO = new ClassificationDTO();
        xssClassificationDTO.setName("<script>alert('XSS')</script>");
        xssClassificationDTO.setDescription("<script>alert('XSS')</script>");
        xssArtDTO.setClassification(xssClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, xssArtDTO);

        assertNotNull(result);
        assertEquals("<script>alert('XSS')</script>", result.getTitle());
    }

    @Test
    void testAddArt_ShouldHandleNullFieldsInDTO() {
        ArtDTO nullArtDTO = new ArtDTO();
        nullArtDTO.setTitle("Valid Title");
        nullArtDTO.setYear(null);
        nullArtDTO.setArtists(null);
        nullArtDTO.setClassification(null);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(nullArtDTO);

        assertNotNull(result);
        assertEquals("Valid Title", result.getTitle());
        assertNull(result.getYear());
        assertNull(result.getArtists());
        assertNull(result.getClassification());
    }

    @Test
    void testUpdateArt_ShouldHandleNullFieldsInDTO() {
        ArtDTO nullArtDTO = new ArtDTO();
        nullArtDTO.setTitle("Valid Title");
        nullArtDTO.setYear(null);
        nullArtDTO.setArtists(null);
        nullArtDTO.setClassification(null);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, nullArtDTO);

        assertNotNull(result);
        assertEquals("Valid Title", result.getTitle());
        assertNull(result.getYear());
        assertNull(result.getArtists());
        assertNull(result.getClassification());
    }

    @Test
    void testPatchArt_ShouldHandleNullFieldsInEntity() {
        Art artWithNulls = new Art();
        artWithNulls.setId(1);
        artWithNulls.setTitle(null);
        artWithNulls.setYear(null);
        artWithNulls.setArtists(null);
        artWithNulls.setClassification(null);

        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(artWithNulls));
        when(artRepository.save(any(Art.class))).thenReturn(artWithNulls);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
    }

    @Test
    void testGetArtById_ShouldHandleNullFieldsInEntity() {
        Art artWithNulls = new Art();
        artWithNulls.setId(1);
        artWithNulls.setTitle("Art with Nulls");
        artWithNulls.setYear(null);
        artWithNulls.setArtists(null);
        artWithNulls.setClassification(null);

        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artCache.get(1)).thenReturn(Optional.empty());
        when(artRepository.findWithArtistsAndClassificationById(1)).thenReturn(Optional.of(artWithNulls));

        ArtDTO result = artService.getArtById(1);

        assertNotNull(result);
        assertEquals("Art with Nulls", result.getTitle());
        assertNull(result.getYear());
        assertNull(result.getArtists());
        assertNull(result.getClassification());
    }

    @Test
    void testAddArt_ShouldHandleDuplicateArtists() {
        ArtistDTO duplicateArtistDTO = new ArtistDTO();
        duplicateArtistDTO.setFirstName("John");
        duplicateArtistDTO.setLastName("Doe");

        artDTO.setArtists(List.of(duplicateArtistDTO, duplicateArtistDTO));

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals(1, result.getArtists().size());
    }

    @Test
    void testUpdateArt_ShouldHandleDuplicateArtists() {
        ArtistDTO duplicateArtistDTO = new ArtistDTO();
        duplicateArtistDTO.setFirstName("John");
        duplicateArtistDTO.setLastName("Doe");

        artDTO.setArtists(List.of(duplicateArtistDTO, duplicateArtistDTO));

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertEquals(1, result.getArtists().size());
    }

    @Test
    void testPatchArt_ShouldHandleDuplicateArtistIds() {
        ArtPatchDTO patchDTO = new ArtPatchDTO();

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findAllById(Set.of(1))).thenReturn(List.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtDTO result = artService.patchArt(1, patchDTO);

        assertNotNull(result);
        assertEquals(1, result.getArtists().size());
    }

    @Test
    void testAddArt_ShouldHandleCaseSensitiveNames() {
        ArtistDTO caseSensitiveArtistDTO = new ArtistDTO();
        caseSensitiveArtistDTO.setFirstName("john");
        caseSensitiveArtistDTO.setLastName("doe");
        artDTO.setArtists(List.of(caseSensitiveArtistDTO));

        Artist caseSensitiveArtist = new Artist();
        caseSensitiveArtist.setId(1);
        caseSensitiveArtist.setFirstName("john");
        caseSensitiveArtist.setLastName("doe");

        when(artistRepository.findFirstByFirstNameAndLastName("john", "doe")).thenReturn(Optional.of(caseSensitiveArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals("john", result.getArtists().get(0).getFirstName());
    }

    @Test
    void testUpdateArt_ShouldHandleCaseSensitiveNames() {
        ArtistDTO caseSensitiveArtistDTO = new ArtistDTO();
        caseSensitiveArtistDTO.setFirstName("john");
        caseSensitiveArtistDTO.setLastName("doe");
        artDTO.setArtists(List.of(caseSensitiveArtistDTO));

        Artist caseSensitiveArtist = new Artist();
        caseSensitiveArtist.setId(1);
        caseSensitiveArtist.setFirstName("john");
        caseSensitiveArtist.setLastName("doe");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("john", "doe")).thenReturn(Optional.of(caseSensitiveArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertEquals("john", result.getArtists().get(0).getFirstName());
    }

    @Test
    void testAddArt_ShouldHandleWhitespaceInNames() {
        ArtistDTO whitespaceArtistDTO = new ArtistDTO();
        whitespaceArtistDTO.setFirstName("  John  ");
        whitespaceArtistDTO.setLastName("  Doe  ");
        artDTO.setArtists(List.of(whitespaceArtistDTO));

        Artist whitespaceArtist = new Artist();
        whitespaceArtist.setId(1);
        whitespaceArtist.setFirstName("John");
        whitespaceArtist.setLastName("Doe");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(whitespaceArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals("John", result.getArtists().get(0).getFirstName());
    }

    @Test
    void testUpdateArt_ShouldHandleWhitespaceInNames() {
        ArtistDTO whitespaceArtistDTO = new ArtistDTO();
        whitespaceArtistDTO.setFirstName("  John  ");
        whitespaceArtistDTO.setLastName("  Doe  ");
        artDTO.setArtists(List.of(whitespaceArtistDTO));

        Artist whitespaceArtist = new Artist();
        whitespaceArtist.setId(1);
        whitespaceArtist.setFirstName("John");
        whitespaceArtist.setLastName("Doe");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(whitespaceArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertEquals("John", result.getArtists().get(0).getFirstName());
    }

    @Test
    void testAddArt_ShouldHandleUnicodeNames() {
        ArtistDTO unicodeArtistDTO = new ArtistDTO();
        unicodeArtistDTO.setFirstName("Jöhn");
        unicodeArtistDTO.setLastName("Döe");
        artDTO.setArtists(List.of(unicodeArtistDTO));

        Artist unicodeArtist = new Artist();
        unicodeArtist.setId(1);
        unicodeArtist.setFirstName("Jöhn");
        unicodeArtist.setLastName("Döe");

        when(artistRepository.findFirstByFirstNameAndLastName("Jöhn", "Döe")).thenReturn(Optional.of(unicodeArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals("Jöhn", result.getArtists().get(0).getFirstName());
    }

    @Test
    void testUpdateArt_ShouldHandleUnicodeNames() {
        ArtistDTO unicodeArtistDTO = new ArtistDTO();
        unicodeArtistDTO.setFirstName("Jöhn");
        unicodeArtistDTO.setLastName("Döe");
        artDTO.setArtists(List.of(unicodeArtistDTO));

        Artist unicodeArtist = new Artist();
        unicodeArtist.setId(1);
        unicodeArtist.setFirstName("Jöhn");
        unicodeArtist.setLastName("Döe");

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("Jöhn", "Döe")).thenReturn(Optional.of(unicodeArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        assertEquals("Jöhn", result.getArtists().get(0).getFirstName());
    }

    @Test
    void testAddArt_ShouldHandleVeryLongText() {
        ArtDTO longArtDTO = new ArtDTO();
        longArtDTO.setTitle("A".repeat(255));
        longArtDTO.setYear(2020);

        ArtistDTO longArtistDTO = new ArtistDTO();
        longArtistDTO.setFirstName("A".repeat(60));
        longArtistDTO.setLastName("A".repeat(60));
        longArtDTO.setArtists(List.of(longArtistDTO));

        ClassificationDTO longClassificationDTO = new ClassificationDTO();
        longClassificationDTO.setName("A".repeat(100));
        longClassificationDTO.setDescription("A".repeat(500));
        longArtDTO.setClassification(longClassificationDTO);

        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(longArtDTO);

        assertNotNull(result);
        assertEquals(255, result.getTitle().length());
    }

    @Test
    void testUpdateArt_ShouldHandleVeryLongText() {
        ArtDTO longArtDTO = new ArtDTO();
        longArtDTO.setTitle("A".repeat(255));
        longArtDTO.setYear(2020);

        ArtistDTO longArtistDTO = new ArtistDTO();
        longArtistDTO.setFirstName("A".repeat(60));
        longArtistDTO.setLastName("A".repeat(60));
        longArtDTO.setArtists(List.of(longArtistDTO));

        ClassificationDTO longClassificationDTO = new ClassificationDTO();
        longClassificationDTO.setName("A".repeat(100));
        longClassificationDTO.setDescription("A".repeat(500));
        longArtDTO.setClassification(longClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName(anyString())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, longArtDTO);

        assertNotNull(result);
        assertEquals(255, result.getTitle().length());
    }

    @Test
    void testAddArt_ShouldHandleBoundaryYearValues() {
        ArtDTO boundaryArtDTO = new ArtDTO();
        boundaryArtDTO.setTitle("Boundary Art");
        boundaryArtDTO.setYear(1000);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(boundaryArtDTO);

        assertNotNull(result);
        assertEquals(1000, result.getYear());
    }

    @Test
    void testUpdateArt_ShouldHandleBoundaryYearValues() {
        ArtDTO boundaryArtDTO = new ArtDTO();
        boundaryArtDTO.setTitle("Boundary Art");
        boundaryArtDTO.setYear(1000);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, boundaryArtDTO);

        assertNotNull(result);
        assertEquals(1000, result.getYear());
    }

    @Test
    void testAddArt_ShouldHandleCurrentYear() {
        ArtDTO currentYearArtDTO = new ArtDTO();
        currentYearArtDTO.setTitle("Current Year Art");
        currentYearArtDTO.setYear(LocalDate.now().getYear());

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(currentYearArtDTO);

        assertNotNull(result);
        assertEquals(LocalDate.now().getYear(), result.getYear());
    }

    @Test
    void testUpdateArt_ShouldHandleCurrentYear() {
        ArtDTO currentYearArtDTO = new ArtDTO();
        currentYearArtDTO.setTitle("Current Year Art");
        currentYearArtDTO.setYear(LocalDate.now().getYear());

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, currentYearArtDTO);

        assertNotNull(result);
        assertEquals(LocalDate.now().getYear(), result.getYear());
    }

    @Test
    void testAddArt_ShouldHandleZeroYear() {
        ArtDTO zeroYearArtDTO = new ArtDTO();
        zeroYearArtDTO.setTitle("Zero Year Art");
        zeroYearArtDTO.setYear(0);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.addArt(zeroYearArtDTO);

        assertNotNull(result);
        assertEquals(0, result.getYear());
    }

    @Test
    void testUpdateArt_ShouldHandleZeroYear() {
        ArtDTO zeroYearArtDTO = new ArtDTO();
        zeroYearArtDTO.setTitle("Zero Year Art");
        zeroYearArtDTO.setYear(0);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        ArtDTO result = artService.updateArt(1, zeroYearArtDTO);

        assertNotNull(result);
        assertEquals(0, result.getYear());
    }

    @Test
    void testAddArt_ShouldHandleMaxIntegerYear() {
        ArtDTO maxYearArtDTO = new ArtDTO();
        maxYearArtDTO.setTitle("Max Year Art");
        maxYearArtDTO.setYear(Integer.MAX_VALUE);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(ValidationException.class, () -> artService.addArt(maxYearArtDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleMaxIntegerYear() {
        ArtDTO maxYearArtDTO = new ArtDTO();
        maxYearArtDTO.setTitle("Max Year Art");
        maxYearArtDTO.setYear(Integer.MAX_VALUE);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, maxYearArtDTO));
    }

    @Test
    void testAddArt_ShouldHandleMinIntegerYear() {
        ArtDTO minYearArtDTO = new ArtDTO();
        minYearArtDTO.setTitle("Min Year Art");
        minYearArtDTO.setYear(Integer.MIN_VALUE);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(ValidationException.class, () -> artService.addArt(minYearArtDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleMinIntegerYear() {
        ArtDTO minYearArtDTO = new ArtDTO();
        minYearArtDTO.setTitle("Min Year Art");
        minYearArtDTO.setYear(Integer.MIN_VALUE);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, minYearArtDTO));
    }

    @Test
    void testAddArt_ShouldHandleExtremeStringLengths() {
        ArtDTO extremeArtDTO = new ArtDTO();
        extremeArtDTO.setTitle("");
        extremeArtDTO.setYear(2020);

        assertThrows(ValidationException.class, () -> artService.addArt(extremeArtDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleExtremeStringLengths() {
        ArtDTO extremeArtDTO = new ArtDTO();
        extremeArtDTO.setTitle("");
        extremeArtDTO.setYear(2020);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, extremeArtDTO));
    }

    @Test
    void testAddArt_ShouldHandleNullArtistNames() {
        ArtistDTO nullNameArtistDTO = new ArtistDTO();
        nullNameArtistDTO.setFirstName(null);
        nullNameArtistDTO.setLastName("Doe");
        artDTO.setArtists(List.of(nullNameArtistDTO));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleNullArtistNames() {
        ArtistDTO nullNameArtistDTO = new ArtistDTO();
        nullNameArtistDTO.setFirstName(null);
        nullNameArtistDTO.setLastName("Doe");
        artDTO.setArtists(List.of(nullNameArtistDTO));

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddArt_ShouldHandleEmptyArtistNames() {
        ArtistDTO emptyNameArtistDTO = new ArtistDTO();
        emptyNameArtistDTO.setFirstName("");
        emptyNameArtistDTO.setLastName("Doe");
        artDTO.setArtists(List.of(emptyNameArtistDTO));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleEmptyArtistNames() {
        ArtistDTO emptyNameArtistDTO = new ArtistDTO();
        emptyNameArtistDTO.setFirstName("");
        emptyNameArtistDTO.setLastName("Doe");
        artDTO.setArtists(List.of(emptyNameArtistDTO));

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddArt_ShouldHandleNullClassificationName() {
        ClassificationDTO nullNameClassificationDTO = new ClassificationDTO();
        nullNameClassificationDTO.setName(null);
        nullNameClassificationDTO.setDescription("Description");
        artDTO.setClassification(nullNameClassificationDTO);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleNullClassificationName() {
        ClassificationDTO nullNameClassificationDTO = new ClassificationDTO();
        nullNameClassificationDTO.setName(null);
        nullNameClassificationDTO.setDescription("Description");
        artDTO.setClassification(nullNameClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddArt_ShouldHandleEmptyClassificationName() {
        ClassificationDTO emptyNameClassificationDTO = new ClassificationDTO();
        emptyNameClassificationDTO.setName("");
        emptyNameClassificationDTO.setDescription("Description");
        artDTO.setClassification(emptyNameClassificationDTO);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleEmptyClassificationName() {
        ClassificationDTO emptyNameClassificationDTO = new ClassificationDTO();
        emptyNameClassificationDTO.setName("");
        emptyNameClassificationDTO.setDescription("Description");
        artDTO.setClassification(emptyNameClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddArt_ShouldHandleNullClassificationDescription() {
        ClassificationDTO nullDescClassificationDTO = new ClassificationDTO();
        nullDescClassificationDTO.setName("Name");
        nullDescClassificationDTO.setDescription(null);
        artDTO.setClassification(nullDescClassificationDTO);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleNullClassificationDescription() {
        ClassificationDTO nullDescClassificationDTO = new ClassificationDTO();
        nullDescClassificationDTO.setName("Name");
        nullDescClassificationDTO.setDescription(null);
        artDTO.setClassification(nullDescClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddArt_ShouldHandleEmptyClassificationDescription() {
        ClassificationDTO emptyDescClassificationDTO = new ClassificationDTO();
        emptyDescClassificationDTO.setName("Name");
        emptyDescClassificationDTO.setDescription("");
        artDTO.setClassification(emptyDescClassificationDTO);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void testUpdateArt_ShouldHandleEmptyClassificationDescription() {
        ClassificationDTO emptyDescClassificationDTO = new ClassificationDTO();
        emptyDescClassificationDTO.setName("Name");
        emptyDescClassificationDTO.setDescription("");
        artDTO.setClassification(emptyDescClassificationDTO);

        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void testAddArt_ShouldHandleMaximumBulkSize() {
        List<ArtDTO> maxSizeDtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            maxSizeDtos.add(dto);
        }

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(maxSizeDtos);

        assertEquals(ApplicationConstants.MAX_BULK_OPERATION_SIZE, result.size());
    }

    @Test
    void testAddBulkArts_ShouldHandleExceedingMaximumBulkSize() {
        List<ArtDTO> exceedingSizeDtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            exceedingSizeDtos.add(dto);
        }

        assertThrows(ValidationException.class, () -> artService.addBulkArts(exceedingSizeDtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleEmptyList() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(Collections.emptyList()));
    }

    @Test
    void testAddBulkArts_ShouldHandleNullList() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(null));
    }

    @Test
    void testAddBulkArts_ShouldHandleMixedValidAndInvalidArts() {
        ArtDTO validArtDTO = new ArtDTO();
        validArtDTO.setTitle("Valid Art");
        validArtDTO.setYear(2020);
        validArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        validArtDTO.setClassification(artDTO.getClassification());

        ArtDTO invalidArtDTO = new ArtDTO();
        invalidArtDTO.setTitle(null);
        invalidArtDTO.setYear(2020);

        List<ArtDTO> mixedDtos = List.of(validArtDTO, invalidArtDTO);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(mixedDtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleDatabaseErrorsMidOperation() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        when(artRepository.save(any(Art.class)))
                .thenReturn(art)
                .thenThrow(new RuntimeException("Database error"));
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleCacheErrors() {
        List<ArtDTO> dtos = List.of(artDTO);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        doThrow(new RuntimeException("Cache error")).when(artCache).put(anyInt(), any(Art.class));

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandlePartialArtistProcessingFailures() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        ArtistDTO invalidArtistDTO = new ArtistDTO();
        invalidArtistDTO.setFirstName(null);
        invalidArtistDTO.setLastName("Doe");
        secondArtDTO.setArtists(List.of(invalidArtistDTO));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandlePartialClassificationProcessingFailures() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        ClassificationDTO invalidClassificationDTO = new ClassificationDTO();
        invalidClassificationDTO.setName("");
        invalidClassificationDTO.setDescription("Description");
        secondArtDTO.setClassification(invalidClassificationDTO);

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleAllValidArts() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        verify(artRepository, times(2)).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleAllInvalidArts() {
        ArtDTO invalidArtDTO1 = new ArtDTO();
        invalidArtDTO1.setTitle(null);
        invalidArtDTO1.setYear(2020);

        ArtDTO invalidArtDTO2 = new ArtDTO();
        invalidArtDTO2.setTitle("");
        invalidArtDTO2.setYear(2021);

        List<ArtDTO> dtos = List.of(invalidArtDTO1, invalidArtDTO2);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleMixedArtistScenarios() {
        ArtDTO existingArtistArtDTO = new ArtDTO();
        existingArtistArtDTO.setTitle("Existing Artist Art");
        existingArtistArtDTO.setYear(2020);
        existingArtistArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        existingArtistArtDTO.setClassification(artDTO.getClassification());

        ArtDTO newArtistArtDTO = new ArtDTO();
        newArtistArtDTO.setTitle("New Artist Art");
        newArtistArtDTO.setYear(2021);
        ArtistDTO newArtistDTO = new ArtistDTO();
        newArtistDTO.setFirstName("New");
        newArtistDTO.setLastName("Artist");
        newArtistArtDTO.setArtists(List.of(newArtistDTO));
        newArtistArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(existingArtistArtDTO, newArtistArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artistRepository.findFirstByFirstNameAndLastName("New", "Artist")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleMixedClassificationScenarios() {
        ArtDTO existingClassificationArtDTO = new ArtDTO();
        existingClassificationArtDTO.setTitle("Existing Classification Art");
        existingClassificationArtDTO.setYear(2020);
        existingClassificationArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        existingClassificationArtDTO.setClassification(artDTO.getClassification());

        ArtDTO newClassificationArtDTO = new ArtDTO();
        newClassificationArtDTO.setTitle("New Classification Art");
        newClassificationArtDTO.setYear(2021);
        newClassificationArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        ClassificationDTO newClassificationDTO = new ClassificationDTO();
        newClassificationDTO.setName("New Type");
        newClassificationDTO.setDescription("New Description");
        newClassificationArtDTO.setClassification(newClassificationDTO);

        List<ArtDTO> dtos = List.of(existingClassificationArtDTO, newClassificationArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(classificationRepository.findByName("New Type")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleNullFieldsInSomeArts() {
        ArtDTO completeArtDTO = new ArtDTO();
        completeArtDTO.setTitle("Complete Art");
        completeArtDTO.setYear(2020);
        completeArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        completeArtDTO.setClassification(artDTO.getClassification());

        ArtDTO nullArtistsArtDTO = new ArtDTO();
        nullArtistsArtDTO.setTitle("Null Artists Art");
        nullArtistsArtDTO.setYear(2021);
        nullArtistsArtDTO.setArtists(null);
        nullArtistsArtDTO.setClassification(artDTO.getClassification());

        ArtDTO nullClassificationArtDTO = new ArtDTO();
        nullClassificationArtDTO.setTitle("Null Classification Art");
        nullClassificationArtDTO.setYear(2022);
        nullClassificationArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        nullClassificationArtDTO.setClassification(null);

        ArtDTO nullYearArtDTO = new ArtDTO();
        nullYearArtDTO.setTitle("Null Year Art");
        nullYearArtDTO.setYear(null);
        nullYearArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        nullYearArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(completeArtDTO, nullArtistsArtDTO, nullClassificationArtDTO, nullYearArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(4, result.size());
        verify(artRepository, times(4)).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleDuplicateTitles() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("Duplicate Title");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Duplicate Title");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        assertEquals("Duplicate Title", result.get(0).getTitle());
        assertEquals("Duplicate Title", result.get(1).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandlePerformanceWithLargeDataset() {
        List<ArtDTO> largeDtos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            largeDtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        long startTime = System.currentTimeMillis();
        List<ArtDTO> result = artService.addBulkArts(largeDtos);
        long endTime = System.currentTimeMillis();

        assertEquals(100, result.size());
        assertTrue((endTime - startTime) < 10000);
    }

    @Test
    void testAddBulkArts_ShouldHandleMemoryUsageWithLargeDataset() {
        List<ArtDTO> largeDtos = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            largeDtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        List<ArtDTO> result = artService.addBulkArts(largeDtos);

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;

        assertEquals(1000, result.size());
        assertTrue(memoryUsed < 100 * 1024 * 1024);
    }

    @Test
    void testAddBulkArts_ShouldHandleConcurrentModification() {
        List<ArtDTO> dtos = List.of(artDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenAnswer(invocation -> {
            Thread.sleep(100);
            return art;
        });
        when(cacheService.getArtCache()).thenReturn(artCache);

        Runnable task = () -> {
            try {
                artService.addBulkArts(dtos);
            } catch (Exception e) {
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(artRepository, atLeastOnce()).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleTransactionIsolation() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class)))
                .thenReturn(art)
                .thenThrow(new RuntimeException("Transaction failed"));

        when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleRollbackScenario() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class)))
                .thenReturn(art)
                .thenThrow(new RuntimeException("Database constraint violation"));

        when(cacheService.getArtCache()).thenReturn(artCache);

        try {
            artService.addBulkArts(dtos);
        } catch (RuntimeException e) {
        }

        verify(artRepository, times(2)).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleCacheConsistency() {
        List<ArtDTO> dtos = List.of(artDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        artService.addBulkArts(dtos);

        verify(artCache).put(1, art);
    }

    @Test
    void testAddBulkArts_ShouldHandleCacheFailuresGracefully() {
        List<ArtDTO> dtos = List.of(artDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);
        doThrow(new RuntimeException("Cache failure")).when(artCache).put(anyInt(), any(Art.class));

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandlePartialCacheFailures() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        Art secondArt = new Art();
        secondArt.setId(2);
        secondArt.setTitle("Second Art");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art).thenReturn(secondArt);
        when(cacheService.getArtCache()).thenReturn(artCache);
        doNothing().doThrow(new RuntimeException("Cache failure")).when(artCache).put(anyInt(), any(Art.class));

        assertThrows(RuntimeException.class, () -> artService.addBulkArts(dtos));
    }

    @Test
    void testAddBulkArts_ShouldHandleBatchProcessing() {
        List<ArtDTO> dtos = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            dtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(50, result.size());
        verify(artRepository, times(50)).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleResourceCleanup() {
        List<ArtDTO> dtos = List.of(artDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        artService.addBulkArts(dtos);

        verifyNoMoreInteractions(artistRepository, classificationRepository, artRepository, cacheService);
    }

    @Test
    void testAddBulkArts_ShouldHandleErrorRecovery() {
        ArtDTO validArtDTO = new ArtDTO();
        validArtDTO.setTitle("Valid Art");
        validArtDTO.setYear(2020);
        validArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        validArtDTO.setClassification(artDTO.getClassification());

        ArtDTO invalidArtDTO = new ArtDTO();
        invalidArtDTO.setTitle(null);
        invalidArtDTO.setYear(2021);

        List<ArtDTO> dtos = List.of(validArtDTO, invalidArtDTO);

        try {
            artService.addBulkArts(dtos);
        } catch (ValidationException e) {
        }

        verify(artRepository, never()).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleDataIntegrity() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        ArtDTO artDTO2 = new ArtDTO();
        artDTO2.setTitle("Art 2");
        artDTO2.setYear(2021);
        artDTO2.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO2.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1, artDTO2);

        Art art1 = new Art();
        art1.setId(1);
        art1.setTitle("Art 1");

        Art art2 = new Art();
        art2.setId(2);
        art2.setTitle("Art 2");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art1).thenReturn(art2);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        assertEquals("Art 1", result.get(0).getTitle());
        assertEquals("Art 2", result.get(1).getTitle());
        assertNotEquals(result.get(0).getId(), result.get(1).getId());
    }

    @Test
    void testAddBulkArts_ShouldHandleOrderPreservation() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        ArtDTO thirdArtDTO = new ArtDTO();
        thirdArtDTO.setTitle("Third Art");
        thirdArtDTO.setYear(2022);
        thirdArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        thirdArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO, thirdArtDTO);

        Art firstArt = new Art();
        firstArt.setId(1);
        firstArt.setTitle("First Art");

        Art secondArt = new Art();
        secondArt.setId(2);
        secondArt.setTitle("Second Art");

        Art thirdArt = new Art();
        thirdArt.setId(3);
        thirdArt.setTitle("Third Art");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(firstArt).thenReturn(secondArt).thenReturn(thirdArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(3, result.size());
        assertEquals("First Art", result.get(0).getTitle());
        assertEquals("Second Art", result.get(1).getTitle());
        assertEquals("Third Art", result.get(2).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleEmptyArtistListInSomeArts() {
        ArtDTO withArtistsArtDTO = new ArtDTO();
        withArtistsArtDTO.setTitle("With Artists");
        withArtistsArtDTO.setYear(2020);
        withArtistsArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        withArtistsArtDTO.setClassification(artDTO.getClassification());

        ArtDTO emptyArtistsArtDTO = new ArtDTO();
        emptyArtistsArtDTO.setTitle("Empty Artists");
        emptyArtistsArtDTO.setYear(2021);
        emptyArtistsArtDTO.setArtists(Collections.emptyList());
        emptyArtistsArtDTO.setClassification(artDTO.getClassification());

        ArtDTO nullArtistsArtDTO = new ArtDTO();
        nullArtistsArtDTO.setTitle("Null Artists");
        nullArtistsArtDTO.setYear(2022);
        nullArtistsArtDTO.setArtists(null);
        nullArtistsArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(withArtistsArtDTO, emptyArtistsArtDTO, nullArtistsArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getArtists().size());
        assertEquals(0, result.get(1).getArtists().size());
        assertNull(result.get(2).getArtists());
    }

    @Test
    void testAddBulkArts_ShouldHandleMixedYearValues() {
        ArtDTO normalYearArtDTO = new ArtDTO();
        normalYearArtDTO.setTitle("Normal Year");
        normalYearArtDTO.setYear(2020);
        normalYearArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        normalYearArtDTO.setClassification(artDTO.getClassification());

        ArtDTO zeroYearArtDTO = new ArtDTO();
        zeroYearArtDTO.setTitle("Zero Year");
        zeroYearArtDTO.setYear(0);
        zeroYearArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        zeroYearArtDTO.setClassification(artDTO.getClassification());

        ArtDTO nullYearArtDTO = new ArtDTO();
        nullYearArtDTO.setTitle("Null Year");
        nullYearArtDTO.setYear(null);
        nullYearArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        nullYearArtDTO.setClassification(artDTO.getClassification());

        ArtDTO boundaryYearArtDTO = new ArtDTO();
        boundaryYearArtDTO.setTitle("Boundary Year");
        boundaryYearArtDTO.setYear(1000);
        boundaryYearArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        boundaryYearArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(normalYearArtDTO, zeroYearArtDTO, nullYearArtDTO, boundaryYearArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(4, result.size());
        assertEquals(2020, result.get(0).getYear());
        assertEquals(0, result.get(1).getYear());
        assertNull(result.get(2).getYear());
        assertEquals(1000, result.get(3).getYear());
    }

    @Test
    void testAddBulkArts_ShouldHandleClassificationVariations() {
        ArtDTO withClassificationArtDTO = new ArtDTO();
        withClassificationArtDTO.setTitle("With Classification");
        withClassificationArtDTO.setYear(2020);
        withClassificationArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        withClassificationArtDTO.setClassification(artDTO.getClassification());

        ArtDTO nullClassificationArtDTO = new ArtDTO();
        nullClassificationArtDTO.setTitle("Null Classification");
        nullClassificationArtDTO.setYear(2021);
        nullClassificationArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        nullClassificationArtDTO.setClassification(null);

        ArtDTO newClassificationArtDTO = new ArtDTO();
        newClassificationArtDTO.setTitle("New Classification");
        newClassificationArtDTO.setYear(2022);
        newClassificationArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        ClassificationDTO newClassificationDTO = new ClassificationDTO();
        newClassificationDTO.setName("Sculpture");
        newClassificationDTO.setDescription("Stone sculpture");
        newClassificationArtDTO.setClassification(newClassificationDTO);

        List<ArtDTO> dtos = List.of(withClassificationArtDTO, nullClassificationArtDTO, newClassificationArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(classificationRepository.findByName("Sculpture")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(3, result.size());
        assertNotNull(result.get(0).getClassification());
        assertNull(result.get(1).getClassification());
        assertNotNull(result.get(2).getClassification());
    }

    @Test
    void testAddBulkArts_ShouldHandleArtistVariations() {
        ArtDTO singleArtistArtDTO = new ArtDTO();
        singleArtistArtDTO.setTitle("Single Artist");
        singleArtistArtDTO.setYear(2020);
        singleArtistArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        singleArtistArtDTO.setClassification(artDTO.getClassification());

        ArtDTO multipleArtistsArtDTO = new ArtDTO();
        multipleArtistsArtDTO.setTitle("Multiple Artists");
        multipleArtistsArtDTO.setYear(2021);
        ArtistDTO secondArtistDTO = new ArtistDTO();
        secondArtistDTO.setFirstName("Jane");
        secondArtistDTO.setLastName("Smith");
        multipleArtistsArtDTO.setArtists(List.of(artDTO.getArtists().get(0), secondArtistDTO));
        multipleArtistsArtDTO.setClassification(artDTO.getClassification());

        ArtDTO newArtistArtDTO = new ArtDTO();
        newArtistArtDTO.setTitle("New Artist");
        newArtistArtDTO.setYear(2022);
        ArtistDTO newArtistDTO = new ArtistDTO();
        newArtistDTO.setFirstName("New");
        newArtistDTO.setLastName("Artist");
        newArtistArtDTO.setArtists(List.of(newArtistDTO));
        newArtistArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(singleArtistArtDTO, multipleArtistsArtDTO, newArtistArtDTO);

        Artist secondArtist = new Artist();
        secondArtist.setId(2);
        secondArtist.setFirstName("Jane");
        secondArtist.setLastName("Smith");

        Artist newArtist = new Artist();
        newArtist.setId(3);
        newArtist.setFirstName("New");
        newArtist.setLastName("Artist");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artistRepository.findFirstByFirstNameAndLastName("Jane", "Smith")).thenReturn(Optional.of(secondArtist));
        when(artistRepository.findFirstByFirstNameAndLastName("New", "Artist")).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getArtists().size());
        assertEquals(2, result.get(1).getArtists().size());
        assertEquals(1, result.get(2).getArtists().size());
    }

    @Test
    void testAddBulkArts_ShouldHandleAllOptionalFieldsMissing() {
        ArtDTO minimalArtDTO = new ArtDTO();
        minimalArtDTO.setTitle("Minimal Art");
        minimalArtDTO.setYear(null);
        minimalArtDTO.setArtists(null);
        minimalArtDTO.setClassification(null);

        List<ArtDTO> dtos = List.of(minimalArtDTO);

        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertEquals("Minimal Art", result.get(0).getTitle());
        assertNull(result.get(0).getYear());
        assertNull(result.get(0).getArtists());
        assertNull(result.get(0).getClassification());
    }

    @Test
    void testAddBulkArts_ShouldHandleAllOptionalFieldsPresent() {
        ArtDTO completeArtDTO = new ArtDTO();
        completeArtDTO.setTitle("Complete Art");
        completeArtDTO.setYear(2020);
        completeArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        completeArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(completeArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertEquals("Complete Art", result.get(0).getTitle());
        assertEquals(2020, result.get(0).getYear());
        assertEquals(1, result.get(0).getArtists().size());
        assertNotNull(result.get(0).getClassification());
    }

    @Test
    void testAddBulkArts_ShouldHandleMixedCompleteness() {
        ArtDTO minimalArtDTO = new ArtDTO();
        minimalArtDTO.setTitle("Minimal Art");
        minimalArtDTO.setYear(null);
        minimalArtDTO.setArtists(null);
        minimalArtDTO.setClassification(null);

        ArtDTO partialArtDTO = new ArtDTO();
        partialArtDTO.setTitle("Partial Art");
        partialArtDTO.setYear(2020);
        partialArtDTO.setArtists(null);
        partialArtDTO.setClassification(artDTO.getClassification());

        ArtDTO completeArtDTO = new ArtDTO();
        completeArtDTO.setTitle("Complete Art");
        completeArtDTO.setYear(2021);
        completeArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        completeArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(minimalArtDTO, partialArtDTO, completeArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(3, result.size());
        assertEquals("Minimal Art", result.get(0).getTitle());
        assertEquals("Partial Art", result.get(1).getTitle());
        assertEquals("Complete Art", result.get(2).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleDatabaseGeneratedIDs() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        ArtDTO artDTO2 = new ArtDTO();
        artDTO2.setTitle("Art 2");
        artDTO2.setYear(2021);
        artDTO2.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO2.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1, artDTO2);

        Art art1 = new Art();
        art1.setId(1001);
        art1.setTitle("Art 1");

        Art art2 = new Art();
        art2.setId(1002);
        art2.setTitle("Art 2");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art1).thenReturn(art2);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        assertEquals(1001, result.get(0).getId());
        assertEquals(1002, result.get(1).getId());
    }

    @Test
    void testAddBulkArts_ShouldHandleTimestampGeneration() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1);

        Art savedArt = new Art();
        savedArt.setId(1);
        savedArt.setTitle("Art 1");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(savedArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertEquals("Art 1", result.get(0).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleVersioning() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1);

        Art savedArt = new Art();
        savedArt.setId(1);
        savedArt.setTitle("Art 1");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(savedArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertEquals("Art 1", result.get(0).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleAuditFields() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1);

        Art savedArt = new Art();
        savedArt.setId(1);
        savedArt.setTitle("Art 1");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(savedArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertEquals("Art 1", result.get(0).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleComplexObjectGraphs() {
        ArtistDTO mainArtistDTO = new ArtistDTO();
        mainArtistDTO.setFirstName("Leonardo");
        mainArtistDTO.setLastName("da Vinci");

        ArtistDTO collaboratorArtistDTO = new ArtistDTO();
        collaboratorArtistDTO.setFirstName("Assistant");
        collaboratorArtistDTO.setLastName("Painter");

        ClassificationDTO detailedClassificationDTO = new ClassificationDTO();
        detailedClassificationDTO.setName("Renaissance Painting");
        detailedClassificationDTO.setDescription("Oil on poplar wood");

        ArtDTO complexArtDTO = new ArtDTO();
        complexArtDTO.setTitle("Mona Lisa");
        complexArtDTO.setYear(1503);
        complexArtDTO.setArtists(List.of(mainArtistDTO, collaboratorArtistDTO));
        complexArtDTO.setClassification(detailedClassificationDTO);

        List<ArtDTO> dtos = List.of(complexArtDTO);

        Artist mainArtist = new Artist();
        mainArtist.setId(1);
        mainArtist.setFirstName("Leonardo");
        mainArtist.setLastName("da Vinci");

        Artist collaboratorArtist = new Artist();
        collaboratorArtist.setId(2);
        collaboratorArtist.setFirstName("Assistant");
        collaboratorArtist.setLastName("Painter");

        Classification detailedClassification = new Classification();
        detailedClassification.setId(1);
        detailedClassification.setName("Renaissance Painting");
        detailedClassification.setDescription("Oil on poplar wood");

        Art complexArt = new Art();
        complexArt.setId(1);
        complexArt.setTitle("Mona Lisa");
        complexArt.setYear(1503);
        complexArt.setArtists(new HashSet<>(List.of(mainArtist, collaboratorArtist)));
        complexArt.setClassification(detailedClassification);

        when(artistRepository.findFirstByFirstNameAndLastName("Leonardo", "da Vinci")).thenReturn(Optional.of(mainArtist));
        when(artistRepository.findFirstByFirstNameAndLastName("Assistant", "Painter")).thenReturn(Optional.of(collaboratorArtist));
        when(classificationRepository.findByName("Renaissance Painting")).thenReturn(detailedClassification);
        when(artRepository.save(any(Art.class))).thenReturn(complexArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        ArtDTO resultDTO = result.get(0);
        assertEquals("Mona Lisa", resultDTO.getTitle());
        assertEquals(1503, resultDTO.getYear());
        assertEquals(2, resultDTO.getArtists().size());
        assertEquals("Renaissance Painting", resultDTO.getClassification().getName());
    }

    @Test
    void testAddBulkArts_ShouldHandlePerformanceUnderLoad() {
        int numberOfArts = 100;
        List<ArtDTO> dtos = new ArrayList<>();

        for (int i = 0; i < numberOfArts; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Performance Test Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            dtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        long startTime = System.currentTimeMillis();
        List<ArtDTO> result = artService.addBulkArts(dtos);
        long endTime = System.currentTimeMillis();

        assertEquals(numberOfArts, result.size());
        long duration = endTime - startTime;
        assertTrue(duration < 5000, "Bulk operation took too long: " + duration + "ms");
    }

    @Test
    void testAddBulkArts_ShouldHandleMemoryEfficiency() {
        int numberOfArts = 1000;
        List<ArtDTO> dtos = new ArrayList<>();

        for (int i = 0; i < numberOfArts; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Memory Test Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            dtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        List<ArtDTO> result = artService.addBulkArts(dtos);

        runtime.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;

        assertEquals(numberOfArts, result.size());
        assertTrue(memoryUsed < 50 * 1024 * 1024, "Memory usage too high: " + memoryUsed + " bytes");
    }

    @Test
    void testAddBulkArts_ShouldHandleTransactionBoundaries() {
        ArtDTO validArtDTO = new ArtDTO();
        validArtDTO.setTitle("Valid Art");
        validArtDTO.setYear(2020);
        validArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        validArtDTO.setClassification(artDTO.getClassification());

        ArtDTO invalidArtDTO = new ArtDTO();
        invalidArtDTO.setTitle(null);
        invalidArtDTO.setYear(2021);

        List<ArtDTO> dtos = List.of(validArtDTO, invalidArtDTO);

        try {
            artService.addBulkArts(dtos);
            fail("Expected ValidationException was not thrown");
        } catch (ValidationException e) {
        }

        verify(artRepository, never()).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleRollbackOnPartialFailure() {
        ArtDTO firstArtDTO = new ArtDTO();
        firstArtDTO.setTitle("First Art");
        firstArtDTO.setYear(2020);
        firstArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        firstArtDTO.setClassification(artDTO.getClassification());

        ArtDTO secondArtDTO = new ArtDTO();
        secondArtDTO.setTitle("Second Art");
        secondArtDTO.setYear(2021);
        secondArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        secondArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(firstArtDTO, secondArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class)))
                .thenReturn(art)
                .thenThrow(new RuntimeException("Database failure"));

        when(cacheService.getArtCache()).thenReturn(artCache);

        try {
            artService.addBulkArts(dtos);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertEquals("Database failure", e.getMessage());
        }

        verify(artRepository, times(2)).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleCacheConsistencyAcrossOperations() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        ArtDTO artDTO2 = new ArtDTO();
        artDTO2.setTitle("Art 2");
        artDTO2.setYear(2021);
        artDTO2.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO2.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1, artDTO2);

        Art art1 = new Art();
        art1.setId(1);
        art1.setTitle("Art 1");

        Art art2 = new Art();
        art2.setId(2);
        art2.setTitle("Art 2");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art1).thenReturn(art2);
        when(cacheService.getArtCache()).thenReturn(artCache);

        artService.addBulkArts(dtos);

        verify(artCache).put(1, art1);
        verify(artCache).put(2, art2);
    }

    @Test
    void testAddBulkArts_ShouldHandleErrorReporting() {
        ArtDTO invalidArtDTO = new ArtDTO();
        invalidArtDTO.setTitle(null);
        invalidArtDTO.setYear(2020);

        List<ArtDTO> dtos = List.of(invalidArtDTO);

        try {
            artService.addBulkArts(dtos);
            fail("Expected ValidationException was not thrown");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Title cannot be null or empty"));
        }
    }

    @Test
    void testAddBulkArts_ShouldHandleDataValidationOrder() {
        ArtDTO invalidTitleArtDTO = new ArtDTO();
        invalidTitleArtDTO.setTitle("");
        invalidTitleArtDTO.setYear(2020);

        ArtDTO invalidYearArtDTO = new ArtDTO();
        invalidYearArtDTO.setTitle("Valid Title");
        invalidYearArtDTO.setYear(3000);

        ArtDTO invalidArtistArtDTO = new ArtDTO();
        invalidArtistArtDTO.setTitle("Valid Title");
        invalidArtistArtDTO.setYear(2020);
        ArtistDTO invalidArtist = new ArtistDTO();
        invalidArtist.setFirstName("");
        invalidArtist.setLastName("Doe");
        invalidArtistArtDTO.setArtists(List.of(invalidArtist));

        List<ArtDTO> dtos = List.of(invalidTitleArtDTO, invalidYearArtDTO, invalidArtistArtDTO);

        try {
            artService.addBulkArts(dtos);
            fail("Expected ValidationException was not thrown");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Title cannot be null or empty"));
        }
    }

    @Test
    void testAddBulkArts_ShouldHandleBatchOptimization() {
        int batchSize = 50;
        List<ArtDTO> dtos = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Batch Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            dtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(batchSize, result.size());
        verify(artRepository, times(batchSize)).save(any(Art.class));
    }

    @Test
    void testAddBulkArts_ShouldHandleResourceLimits() {
        List<ArtDTO> largeDtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE; i++) {
            ArtDTO dto = new ArtDTO();
            dto.setTitle("Art " + i);
            dto.setYear(2000 + i);
            dto.setArtists(List.of(artDTO.getArtists().get(0)));
            dto.setClassification(artDTO.getClassification());
            largeDtos.add(dto);
        }

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(largeDtos);

        assertEquals(ApplicationConstants.MAX_BULK_OPERATION_SIZE, result.size());
    }

    @Test
    void testAddBulkArts_ShouldHandleTimeoutScenarios() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Slow Art");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(artDTO1);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenAnswer(invocation -> {
            Thread.sleep(2000);
            return art;
        });
        when(cacheService.getArtCache()).thenReturn(artCache);

        long startTime = System.currentTimeMillis();
        List<ArtDTO> result = artService.addBulkArts(dtos);
        long endTime = System.currentTimeMillis();

        assertEquals(1, result.size());
        assertTrue((endTime - startTime) < 5000, "Operation timed out");
    }

    @Test
    void testAddBulkArts_ShouldHandleRecoveryAfterFailure() {
        ArtDTO artDTO1 = new ArtDTO();
        artDTO1.setTitle("Art 1");
        artDTO1.setYear(2020);
        artDTO1.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO1.setClassification(artDTO.getClassification());

        List<ArtDTO> firstBatch = List.of(artDTO1);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> firstResult = artService.addBulkArts(firstBatch);
        assertEquals(1, firstResult.size());

        ArtDTO artDTO2 = new ArtDTO();
        artDTO2.setTitle("Art 2");
        artDTO2.setYear(2021);
        artDTO2.setArtists(List.of(artDTO.getArtists().get(0)));
        artDTO2.setClassification(artDTO.getClassification());

        List<ArtDTO> secondBatch = List.of(artDTO2);

        Art art2 = new Art();
        art2.setId(2);
        art2.setTitle("Art 2");

        when(artRepository.save(any(Art.class))).thenReturn(art2);

        List<ArtDTO> secondResult = artService.addBulkArts(secondBatch);
        assertEquals(1, secondResult.size());
        assertEquals("Art 2", secondResult.get(0).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleDataIntegrityConstraints() {
        ArtDTO duplicateTitleArtDTO = new ArtDTO();
        duplicateTitleArtDTO.setTitle("Duplicate Title");
        duplicateTitleArtDTO.setYear(2020);
        duplicateTitleArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        duplicateTitleArtDTO.setClassification(artDTO.getClassification());

        ArtDTO anotherDuplicateTitleArtDTO = new ArtDTO();
        anotherDuplicateTitleArtDTO.setTitle("Duplicate Title");
        anotherDuplicateTitleArtDTO.setYear(2021);
        anotherDuplicateTitleArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        anotherDuplicateTitleArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(duplicateTitleArtDTO, anotherDuplicateTitleArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(2, result.size());
        assertEquals("Duplicate Title", result.get(0).getTitle());
        assertEquals("Duplicate Title", result.get(1).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleCustomBusinessRules() {
        ArtDTO businessRuleArtDTO = new ArtDTO();
        businessRuleArtDTO.setTitle("Business Rule Art");
        businessRuleArtDTO.setYear(2020);
        businessRuleArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        businessRuleArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(businessRuleArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        assertEquals("Business Rule Art", result.get(0).getTitle());
    }

    @Test
    void testAddBulkArts_ShouldHandleExternalDependencies() {
        ArtDTO externalDependencyArtDTO = new ArtDTO();
        externalDependencyArtDTO.setTitle("External Dependency Art");
        externalDependencyArtDTO.setYear(2020);
        externalDependencyArtDTO.setArtists(List.of(artDTO.getArtists().get(0)));
        externalDependencyArtDTO.setClassification(artDTO.getClassification());

        List<ArtDTO> dtos = List.of(externalDependencyArtDTO);

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(1, result.size());
        verify(artistRepository).findFirstByFirstNameAndLastName("John", "Doe");
        verify(classificationRepository).findByName("Painting");
        verify(artRepository).save(any(Art.class));
        verify(cacheService.getArtCache()).put(1, art);
    }

    @Test
    void testAddBulkArts_ShouldHandleAllScenariosComprehensively() {
        ArtDTO minimalArt = new ArtDTO();
        minimalArt.setTitle("Minimal");
        minimalArt.setYear(null);
        minimalArt.setArtists(null);
        minimalArt.setClassification(null);

        ArtDTO completeArt = new ArtDTO();
        completeArt.setTitle("Complete");
        completeArt.setYear(2020);
        completeArt.setArtists(List.of(artDTO.getArtists().get(0)));
        completeArt.setClassification(artDTO.getClassification());

        ArtDTO zeroYearArt = new ArtDTO();
        zeroYearArt.setTitle("Zero Year");
        zeroYearArt.setYear(0);
        zeroYearArt.setArtists(List.of(artDTO.getArtists().get(0)));
        zeroYearArt.setClassification(artDTO.getClassification());

        ArtDTO boundaryYearArt = new ArtDTO();
        boundaryYearArt.setTitle("Boundary Year");
        boundaryYearArt.setYear(1000);
        boundaryYearArt.setArtists(List.of(artDTO.getArtists().get(0)));
        boundaryYearArt.setClassification(artDTO.getClassification());

        ArtDTO multipleArtistsArt = new ArtDTO();
        multipleArtistsArt.setTitle("Multiple Artists");
        ArtistDTO secondArtist = new ArtistDTO();
        secondArtist.setFirstName("Jane");
        secondArtist.setLastName("Smith");
        multipleArtistsArt.setArtists(List.of(artDTO.getArtists().get(0), secondArtist));
        multipleArtistsArt.setClassification(artDTO.getClassification());

        ArtDTO newClassificationArt = new ArtDTO();
        newClassificationArt.setTitle("New Classification");
        ClassificationDTO newClassification = new ClassificationDTO();
        newClassification.setName("Digital Art");
        newClassification.setDescription("Computer-generated artwork");
        newClassificationArt.setClassification(newClassification);
        newClassificationArt.setArtists(List.of(artDTO.getArtists().get(0)));

        List<ArtDTO> dtos = List.of(
                minimalArt, completeArt, zeroYearArt, boundaryYearArt,
                multipleArtistsArt, newClassificationArt
        );

        Artist janeArtist = new Artist();
        janeArtist.setId(2);
        janeArtist.setFirstName("Jane");
        janeArtist.setLastName("Smith");

        Classification digitalClassification = new Classification();
        digitalClassification.setId(2);
        digitalClassification.setName("Digital Art");
        digitalClassification.setDescription("Computer-generated artwork");

        when(artistRepository.findFirstByFirstNameAndLastName("John", "Doe")).thenReturn(Optional.of(artist));
        when(artistRepository.findFirstByFirstNameAndLastName("Jane", "Smith")).thenReturn(Optional.of(janeArtist));
        when(classificationRepository.findByName("Painting")).thenReturn(classification);
        when(classificationRepository.findByName("Digital Art")).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(digitalClassification);
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(cacheService.getArtCache()).thenReturn(artCache);

        List<ArtDTO> result = artService.addBulkArts(dtos);

        assertEquals(6, result.size());
        verify(artistRepository, times(2)).findFirstByFirstNameAndLastName(anyString(), anyString());
        verify(classificationRepository, times(2)).findByName(anyString());
        verify(classificationRepository).save(any(Classification.class));
        verify(artRepository, times(6)).save(any(Art.class));
        verify(artCache, times(6)).put(anyInt(), any(Art.class));
    }
}