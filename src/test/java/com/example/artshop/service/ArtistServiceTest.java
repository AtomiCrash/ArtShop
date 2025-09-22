package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtistRepository;
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

class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private EntityCache<Artist> artistCache;

    @InjectMocks
    private ArtistService artistService;

    private Artist artist;
    private ArtistDTO artistDTO;

    @BeforeEach
    void setUp() {
        artist = new Artist();
        artist.setId(1);
        artist.setFirstName("John");
        artist.setLastName("Doe");

        artistDTO = new ArtistDTO();
        artistDTO.setId(1);
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");
    }

    @Test
    void testCreateArtist_Success() {
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.createArtist(artistDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(artistCache).put(1, artist);
    }

    @Test
    void testCreateArtist_NullData() {
        assertThrows(ValidationException.class, () -> artistService.createArtist(null));
    }

    @Test
    void testCreateArtist_NoName() {
        ArtistDTO invalidDTO = new ArtistDTO();
        assertThrows(ValidationException.class, () -> artistService.createArtist(invalidDTO));
    }

    @Test
    void testCreateArtist_FirstNameTooLong() {
        artistDTO.setFirstName("A".repeat(61));
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void testCreateArtist_MiddleNameTooLong() {
        artistDTO.setMiddleName("A".repeat(61));
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void testCreateArtist_LastNameTooLong() {
        artistDTO.setLastName("A".repeat(61));
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void testCreateArtist_OnlyFirstName() {
        artistDTO.setLastName(null);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.createArtist(artistDTO);
        assertNotNull(result);
    }

    @Test
    void testCreateArtist_OnlyLastName() {
        artistDTO.setFirstName(null);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.createArtist(artistDTO);
        assertNotNull(result);
    }

    @Test
    void testGetArtistById_FoundInCache() {
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(artistCache.get(1)).thenReturn(Optional.of(artist));

        Optional<ArtistDTO> result = artistService.getArtistById(1);

        assertTrue(result.isPresent());
        assertEquals("John", result.get().getFirstName());
    }

    @Test
    void testGetArtistById_NotFoundInCacheFoundInRepo() {
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(artistCache.get(1)).thenReturn(Optional.empty());
        when(artistRepository.findById(1)).thenReturn(Optional.of(artist));

        Optional<ArtistDTO> result = artistService.getArtistById(1);

        assertTrue(result.isPresent());
        verify(artistCache).put(1, artist);
    }

    @Test
    void testGetArtistById_NotFound() {
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(artistCache.get(1)).thenReturn(Optional.empty());
        when(artistRepository.findById(1)).thenReturn(Optional.empty());

        Optional<ArtistDTO> result = artistService.getArtistById(1);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateArtist_Success() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO updatedDTO = new ArtistDTO();
        updatedDTO.setFirstName("Jane");
        updatedDTO.setLastName("Smith");

        ArtistDTO result = artistService.updateArtist(1, updatedDTO);

        assertEquals("Jane", result.getFirstName());
        verify(artistCache).update(1, artist);
    }

    @Test
    void testUpdateArtist_NotFound() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.updateArtist(1, artistDTO));
    }

    @Test
    void testDeleteArtist_Success() {
        Artist artistWithArts = new Artist();
        artistWithArts.setId(1);
        artistWithArts.setArts(new HashSet<>());

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artistWithArts));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        artistService.deleteArtist(1);

        verify(artistRepository).delete(artistWithArts);
        verify(artistCache).evict(1);
    }

    @Test
    void testDeleteArtist_NotFound() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.deleteArtist(1));
    }

    @Test
    void testSearchArtists_ByFirstName() {
        when(artistRepository.findByFirstNameContainingIgnoreCase("John")).thenReturn(List.of(artist));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.searchArtists("John", null);

        assertEquals(1, result.size());
        verify(artistCache).put(1, artist);
    }

    @Test
    void testSearchArtists_ByLastName() {
        when(artistRepository.findByLastNameContainingIgnoreCase("Doe")).thenReturn(List.of(artist));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.searchArtists(null, "Doe");

        assertEquals(1, result.size());
    }

    @Test
    void testSearchArtists_ByBothNames() {
        when(artistRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("John", "Doe"))
                .thenReturn(List.of(artist));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.searchArtists("John", "Doe");

        assertEquals(1, result.size());
    }

    @Test
    void testSearchArtists_NoNames() {
        List<ArtistDTO> result = artistService.searchArtists(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testPatchArtist_Success() {
        ArtistPatchDTO patchDTO = new ArtistPatchDTO();
        patchDTO.setFirstName("Jane");

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.patchArtist(1, patchDTO);

        assertEquals("Jane", result.getFirstName());
        verify(artistCache).update(1, artist);
    }

    @Test
    void testPatchArtist_NoUpdates() {
        ArtistPatchDTO patchDTO = new ArtistPatchDTO();

        assertThrows(IllegalArgumentException.class, () -> artistService.patchArtist(1, patchDTO));
    }

    @Test
    void testPatchArtist_NotFound() {
        ArtistPatchDTO patchDTO = new ArtistPatchDTO();
        patchDTO.setFirstName("Jane");

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.patchArtist(1, patchDTO));
    }

    @Test
    void testAddBulkArtists_Success() {
        List<ArtistDTO> dtos = List.of(artistDTO);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.addBulkArtists(dtos);

        assertEquals(1, result.size());
    }

    @Test
    void testAddBulkArtists_EmptyList() {
        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(List.of()));
    }

    @Test
    void testAddBulkArtists_NullList() {
        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(null));
    }

    @Test
    void testAddBulkArtists_TooManyItems() {
        List<ArtistDTO> dtos = Arrays.asList(new ArtistDTO[ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1]);
        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(dtos));
    }

    @Test
    void testAddBulkArtists_InvalidArtist() {
        ArtistDTO invalidDTO = new ArtistDTO();
        List<ArtistDTO> dtos = List.of(invalidDTO);

        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(dtos));
    }

    @Test
    void testGetArtistsByArtTitle_Success() {
        when(artistRepository.findByArtTitleContaining("Mona Lisa")).thenReturn(List.of(artist));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.getArtistsByArtTitle("Mona Lisa");

        assertEquals(1, result.size());
        verify(artistCache).put(1, artist);
    }

    @Test
    void testGetArtistsByArtTitle_EmptyResult() {
        when(artistRepository.findByArtTitleContaining("Unknown")).thenReturn(List.of());
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.getArtistsByArtTitle("Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllArtists_Success() {
        when(artistRepository.findAllWithArts()).thenReturn(List.of(artist));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.getAllArtists();

        assertEquals(1, result.size());
        verify(artistCache).put(1, artist);
    }

    @Test
    void testGetCacheInfo() {
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(artistCache.getCacheInfo()).thenReturn("Cache info");

        String result = artistService.getCacheInfo();

        assertEquals("Cache info", result);
    }

    @Test
    void testGetArtistCache() {
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        EntityCache<Artist> result = artistService.getArtistCache();

        assertEquals(artistCache, result);
    }

    @Test
    void testCreateArtist_OnlyMiddleName_ShouldThrowValidationException() {
        ArtistDTO invalidDTO = new ArtistDTO();
        invalidDTO.setMiddleName("OnlyMiddle");

        assertThrows(ValidationException.class, () -> artistService.createArtist(invalidDTO));
    }

    @Test
    void testUpdateArtist_WithNullMiddleName_ShouldUpdateSuccessfully() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO updatedDTO = new ArtistDTO();
        updatedDTO.setFirstName("Jane");
        updatedDTO.setMiddleName(null);
        updatedDTO.setLastName("Smith");

        ArtistDTO result = artistService.updateArtist(1, updatedDTO);

        assertEquals("Jane", result.getFirstName());
        assertNull(result.getMiddleName());
        verify(artistCache).update(1, artist);
    }

    @Test
    void testPatchArtist_UpdateMiddleNameToNull_ShouldUpdateSuccessfully() {
        ArtistPatchDTO patchDTO = new ArtistPatchDTO();
        patchDTO.setMiddleName(null);

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.patchArtist(1, patchDTO);

        assertNull(result.getMiddleName());
        verify(artistCache).update(1, artist);
    }

    @Test
    void testGetAllArtists_WithNullArts_ShouldHandleGracefully() {
        Artist artistWithNullArts = new Artist();
        artistWithNullArts.setId(2);
        artistWithNullArts.setFirstName("Test");
        artistWithNullArts.setLastName("Artist");
        artistWithNullArts.setArts(null);

        when(artistRepository.findAllWithArts()).thenReturn(List.of(artistWithNullArts));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.getAllArtists();

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getFirstName());
    }

    @Test
    void testSearchArtists_EmptyResults_ShouldReturnEmptyList() {
        when(artistRepository.findByFirstNameContainingIgnoreCase("Nonexistent")).thenReturn(Collections.emptyList());
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.searchArtists("Nonexistent", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAddBulkArtists_WithNullMiddleName_ShouldSucceed() {
        ArtistDTO artistWithNullMiddle = new ArtistDTO();
        artistWithNullMiddle.setFirstName("John");
        artistWithNullMiddle.setMiddleName(null);
        artistWithNullMiddle.setLastName("Doe");

        List<ArtistDTO> dtos = List.of(artistWithNullMiddle);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.addBulkArtists(dtos);

        assertEquals(1, result.size());
    }

    @Test
    void testCreateArtist_WithOnlyLastName_ShouldSucceed() {
        ArtistDTO artistOnlyLastName = new ArtistDTO();
        artistOnlyLastName.setLastName("Doe");

        Artist savedArtist = new Artist();
        savedArtist.setId(1);
        savedArtist.setLastName("Doe");

        when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.createArtist(artistOnlyLastName);

        assertNotNull(result);
        assertEquals("Doe", result.getLastName());
        assertNull(result.getFirstName());
        verify(artistCache).put(1, savedArtist);
    }

    @Test
    void testCreateArtist_WithOnlyFirstName_ShouldSucceed() {
        ArtistDTO artistOnlyFirstName = new ArtistDTO();
        artistOnlyFirstName.setFirstName("John");

        Artist savedArtist = new Artist();
        savedArtist.setId(1);
        savedArtist.setFirstName("John");

        when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.createArtist(artistOnlyFirstName);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertNull(result.getLastName());
        verify(artistCache).put(1, savedArtist);
    }

    @Test
    void testUpdateArtist_RemoveMiddleName_ShouldSucceed() {
        Artist artistWithMiddleName = new Artist();
        artistWithMiddleName.setId(1);
        artistWithMiddleName.setFirstName("John");
        artistWithMiddleName.setMiddleName("Middle");
        artistWithMiddleName.setLastName("Doe");

        ArtistDTO updateDTO = new ArtistDTO();
        updateDTO.setFirstName("John");
        updateDTO.setMiddleName(null);
        updateDTO.setLastName("Doe");

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artistWithMiddleName));
        when(artistRepository.save(any(Artist.class))).thenReturn(artistWithMiddleName);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.updateArtist(1, updateDTO);

        assertNotNull(result);
        assertNull(result.getMiddleName());
        verify(artistCache).update(1, artistWithMiddleName);
    }

    @Test
    void testPatchArtist_RemoveMiddleName_ShouldSucceed() {
        Artist artistWithMiddleName = new Artist();
        artistWithMiddleName.setId(1);
        artistWithMiddleName.setFirstName("John");
        artistWithMiddleName.setMiddleName("Middle");
        artistWithMiddleName.setLastName("Doe");

        ArtistPatchDTO patchDTO = new ArtistPatchDTO();
        patchDTO.setMiddleName(null);

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artistWithMiddleName));
        when(artistRepository.save(any(Artist.class))).thenReturn(artistWithMiddleName);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        ArtistDTO result = artistService.patchArtist(1, patchDTO);

        assertNotNull(result);
        assertNull(result.getMiddleName());
        verify(artistCache).update(1, artistWithMiddleName);
    }

    @Test
    void testGetArtistsByArtTitle_WithNullArts_ShouldHandleGracefully() {
        Artist artistWithNullArts = new Artist();
        artistWithNullArts.setId(2);
        artistWithNullArts.setFirstName("Test");
        artistWithNullArts.setLastName("Artist");
        artistWithNullArts.setArts(null);

        when(artistRepository.findByArtTitleContaining("Mona Lisa")).thenReturn(List.of(artistWithNullArts));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.getArtistsByArtTitle("Mona Lisa");

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getFirstName());
    }

    @Test
    void testGetAllArtists_WithEmptyArts_ShouldHandleGracefully() {
        Artist artistWithEmptyArts = new Artist();
        artistWithEmptyArts.setId(2);
        artistWithEmptyArts.setFirstName("Test");
        artistWithEmptyArts.setLastName("Artist");
        artistWithEmptyArts.setArts(new HashSet<>());

        when(artistRepository.findAllWithArts()).thenReturn(List.of(artistWithEmptyArts));
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.getAllArtists();

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getFirstName());
    }

    @Test
    void testSearchArtists_WithEmptyString_ShouldReturnEmptyList() {
        when(artistRepository.findByFirstNameContainingIgnoreCase("")).thenReturn(Collections.emptyList());
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        List<ArtistDTO> result = artistService.searchArtists("", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAddBulkArtists_WithEmptyNames_ShouldThrowValidationException() {
        ArtistDTO invalidDTO = new ArtistDTO();
        invalidDTO.setFirstName("");
        invalidDTO.setLastName("");

        List<ArtistDTO> dtos = List.of(invalidDTO);

        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(dtos));
    }

    @Test
    void testAddBulkArtists_WithWhitespaceNames_ShouldThrowValidationException() {
        ArtistDTO invalidDTO = new ArtistDTO();
        invalidDTO.setFirstName("   ");
        invalidDTO.setLastName("   ");

        List<ArtistDTO> dtos = List.of(invalidDTO);

        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(dtos));
    }
}