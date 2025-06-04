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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private ArtistPatchDTO artistPatchDTO;

    @BeforeEach
    void setUp() {
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        artist = new Artist("Vincent", "van Gogh");

        artistDTO = new ArtistDTO();
        artistDTO.setFirstName("Vincent");
        artistDTO.setLastName("van Gogh");

        artistPatchDTO = new ArtistPatchDTO();
        artistPatchDTO.setFirstName("Updated");
    }

    @Test
    void createArtist_ValidData_ReturnsArtist() {
        Artist savedArtist = new Artist("Vincent", "van Gogh");
        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);

        Artist result = artistService.createArtist(artistDTO);

        assertNotNull(result);
        assertEquals("Vincent", result.getFirstName());
        assertEquals("van Gogh", result.getLastName());
        verify(artistCache).put(anyInt(), eq(savedArtist));
    }

    @Test
    void createArtist_NullData_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artistService.createArtist(null));
    }

    @Test
    void createArtist_InvalidFirstName_ThrowsValidationException() {
        artistDTO.setFirstName(null);
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void createArtist_InvalidLastName_ThrowsValidationException() {
        artistDTO.setLastName(null);
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void createArtist_FirstNameTooLong_ThrowsValidationException() {
        artistDTO.setFirstName("a".repeat(61));
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void createArtist_MiddleNameTooLong_ThrowsValidationException() {
        artistDTO.setMiddleName("a".repeat(61));
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void createArtist_LastNameTooLong_ThrowsValidationException() {
        artistDTO.setLastName("a".repeat(61));
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void getArtistById_FoundInCache_ReturnsArtist() {
        Artist cachedArtist = new Artist("Vincent", "van Gogh");
        lenient().when(artistCache.get(anyInt())).thenReturn(Optional.of(cachedArtist));

        Optional<Artist> result = artistService.getArtistById(1);

        assertTrue(result.isPresent());
        assertEquals("Vincent", result.get().getFirstName());
        verify(artistRepository, never()).findById(anyInt());
    }

    @Test
    void getArtistById_NotFoundInCache_FetchesFromRepository() {
        Artist dbArtist = new Artist("Vincent", "van Gogh");
        lenient().when(artistCache.get(anyInt())).thenReturn(Optional.empty());
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(dbArtist));

        Optional<Artist> result = artistService.getArtistById(1);

        assertTrue(result.isPresent());
        assertEquals("Vincent", result.get().getFirstName());
        verify(artistCache).put(anyInt(), eq(dbArtist));
    }

    @Test
    void getArtistById_NotFound_ReturnsEmpty() {
        lenient().when(artistCache.get(anyInt())).thenReturn(Optional.empty());
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.empty());

        Optional<Artist> result = artistService.getArtistById(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateArtist_ValidData_ReturnsUpdatedArtist() {
        Artist existingArtist = new Artist("Old", "Artist");
        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.of(existingArtist));
        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(existingArtist);

        Artist result = artistService.updateArtist(1, artistDTO);

        assertNotNull(result);
        assertEquals("Vincent", result.getFirstName());
        assertEquals("van Gogh", result.getLastName());
        verify(artistCache).update(anyInt(), eq(existingArtist));
    }

    @Test
    void updateArtist_NotFound_ThrowsNotFoundException() {
        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.updateArtist(1, artistDTO));
    }

    @Test
    void deleteArtist_ValidId_DeletesArtist() {
        Artist artistToDelete = new Artist("ToDelete", "Artist");
        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.of(artistToDelete));

        artistService.deleteArtist(1);

        verify(artistRepository).delete(artistToDelete);
        verify(artistCache).evict(anyInt());
    }

    @Test
    void deleteArtist_NotFound_ThrowsNotFoundException() {
        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.deleteArtist(1));
    }

    @Test
    void patchArtist_ValidData_ReturnsPatchedArtist() {
        Artist existingArtist = new Artist("Original", "Artist");
        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.of(existingArtist));
        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(existingArtist);

        Artist result = artistService.patchArtist(1, artistPatchDTO);

        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        verify(artistCache).update(anyInt(), eq(existingArtist));
    }

    @Test
    void patchArtist_NoUpdates_ThrowsIllegalArgumentException() {
        ArtistPatchDTO emptyPatch = new ArtistPatchDTO();
        assertThrows(IllegalArgumentException.class, () -> artistService.patchArtist(1, emptyPatch));
    }

    @Test
    void patchArtist_NotFound_ThrowsNotFoundException() {
        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.patchArtist(1, artistPatchDTO));
    }

    @Test
    void searchArtists_ByFirstName_ReturnsList() {
        lenient().when(artistRepository.findByFirstNameContainingIgnoreCase("Vincent"))
                .thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.searchArtists("Vincent", null);

        assertEquals(1, result.size());
        assertEquals("Vincent", result.get(0).getFirstName());
    }

    @Test
    void searchArtists_ByLastName_ReturnsList() {
        lenient().when(artistRepository.findByLastNameContainingIgnoreCase("van Gogh"))
                .thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.searchArtists(null, "van Gogh");

        assertEquals(1, result.size());
        assertEquals("van Gogh", result.get(0).getLastName());
    }

    @Test
    void searchArtists_ByBothNames_ReturnsList() {
        lenient().when(artistRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("Vincent", "van Gogh"))
                .thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.searchArtists("Vincent", "van Gogh");

        assertEquals(1, result.size());
    }

    @Test
    void searchArtists_NoParams_ReturnsEmptyList() {
        List<Artist> result = artistService.searchArtists(null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getArtistsByArtTitle_Found_ReturnsList() {
        lenient().when(artistRepository.findByArtTitleContaining("Starry Night"))
                .thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.getArtistsByArtTitle("Starry Night");

        assertEquals(1, result.size());
    }

    @Test
    void getArtistsByArtTitle_NotFound_ReturnsEmptyList() {
        lenient().when(artistRepository.findByArtTitleContaining("Unknown")).thenReturn(Collections.emptyList());

        List<Artist> result = artistService.getArtistsByArtTitle("Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllArtists_ReturnsList() {
        lenient().when(artistRepository.findAll()).thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.getAllArtists();

        assertEquals(1, result.size());
    }

    @Test
    void getCacheInfo_ReturnsCacheInfo() {
        lenient().when(artistCache.getCacheInfo()).thenReturn("Cache info");

        String result = artistService.getCacheInfo();

        assertEquals("Cache info", result);
    }

    @Test
    void getArtistCache_ReturnsCache() {
        EntityCache<Artist> result = artistService.getArtistCache();

        assertEquals(artistCache, result);
    }

    @Test
    void addBulkArtists_ValidList_ReturnsList() {
        List<ArtistDTO> dtos = Arrays.asList(artistDTO, artistDTO);
        Artist savedArtist1 = new Artist("Vincent", "van Gogh");
        Artist savedArtist2 = new Artist("Pablo", "Picasso");

        lenient().when(artistRepository.save(any(Artist.class)))
                .thenReturn(savedArtist1)
                .thenReturn(savedArtist2);

        List<Artist> result = artistService.addBulkArtists(dtos);

        assertEquals(2, result.size());
        verify(artistCache, times(2)).put(anyInt(), any(Artist.class));
    }

    @Test
    void addBulkArtists_NullList_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(null));
    }

    @Test
    void addBulkArtists_EmptyList_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(Collections.emptyList()));
    }

    @Test
    void addBulkArtists_TooLargeList_ThrowsValidationException() {
        List<ArtistDTO> largeList = Collections.nCopies(ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1, artistDTO);
        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(largeList));
    }

    @Test
    void addBulkArtists_InvalidArtist_ThrowsValidationException() {
        artistDTO.setFirstName(null);
        artistDTO.setLastName(null);
        List<ArtistDTO> dtos = Collections.singletonList(artistDTO);

        assertThrows(ValidationException.class, () -> artistService.addBulkArtists(dtos));
    }

    @Test
    void addBulkArtists_ArtistWithOnlyFirstName_Valid() {
        artistDTO.setLastName(null);
        List<ArtistDTO> dtos = Collections.singletonList(artistDTO);
        Artist savedArtist = new Artist("Vincent", null);

        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);

        List<Artist> result = artistService.addBulkArtists(dtos);

        assertEquals(1, result.size());
        verify(artistCache).put(anyInt(), eq(savedArtist));
    }

    @Test
    void addBulkArtists_ArtistWithOnlyLastName_Valid() {
        artistDTO.setFirstName(null);
        List<ArtistDTO> dtos = Collections.singletonList(artistDTO);
        Artist savedArtist = new Artist(null, "van Gogh");

        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);

        List<Artist> result = artistService.addBulkArtists(dtos);

        assertEquals(1, result.size());
        verify(artistCache).put(anyInt(), eq(savedArtist));
    }

    @Test
    void patchArtist_UpdateMiddleName_ReturnsPatchedArtist() {
        Artist existingArtist = new Artist("Original", "Artist");
        ArtistPatchDTO patch = new ArtistPatchDTO();
        patch.setMiddleName("New Middle Name");

        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.of(existingArtist));
        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(existingArtist);

        Artist result = artistService.patchArtist(1, patch);

        assertNotNull(result);
        verify(artistCache).update(anyInt(), eq(existingArtist));
    }

    @Test
    void patchArtist_UpdateLastName_ReturnsPatchedArtist() {
        Artist existingArtist = new Artist("Original", "Artist");
        ArtistPatchDTO patch = new ArtistPatchDTO();
        patch.setLastName("New Last Name");

        lenient().when(artistRepository.findWithArtsById(anyInt())).thenReturn(Optional.of(existingArtist));
        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(existingArtist);

        Artist result = artistService.patchArtist(1, patch);

        assertNotNull(result);
        verify(artistCache).update(anyInt(), eq(existingArtist));
    }
}