package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private EntityCache<Artist> artistCache;

    @Mock
    private EntityCache<Art> artCache;

    @InjectMocks
    private ArtistService artistService;

    private ArtistDTO artistDTO;
    private Artist artist;
    private Art art;

    @BeforeEach
    void setUp() {
        artistDTO = new ArtistDTO();
        artistDTO.setId(1);
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");

        artist = new Artist();
        //artist.setId(1);
        artist.setFirstName("John");
        artist.setLastName("Doe");
        artist.setArts(new HashSet<>());

        art = new Art();
        art.setId(1);
        art.setTitle("Mona Lisa");

        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> {
            Artist savedArtist = invocation.getArgument(0);
            //savedArtist.setId(1); // Simulate ID assignment
            return savedArtist;
        });
    }

    @Test
    void createArtist_ValidDTO_SavesAndCachesArtist() {
        Artist result = artistService.createArtist(artistDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(artistRepository).save(any(Artist.class));
        verify(artistCache).put(eq(1), any(Artist.class));
    }

    @Test
    void createArtist_NullDTO_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> artistService.createArtist(null));
        assertEquals("Artist data cannot be null", exception.getMessage());
    }

    @Test
    void createArtist_EmptyNames_ThrowsValidationException() {
        artistDTO.setFirstName("");
        artistDTO.setLastName("");
        ValidationException exception = assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
        assertEquals("Artist must have at least first name or last name", exception.getMessage());
    }

    @Test
    void createArtist_LongFirstName_ThrowsValidationException() {
        artistDTO.setFirstName("a".repeat(61));
        ValidationException exception = assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
        assertEquals("First name must be 60 characters or less", exception.getMessage());
    }

    @Test
    void addBulkArtists_ValidDTOs_SavesAndCachesArtists() {
        List<ArtistDTO> dtos = Collections.singletonList(artistDTO);
        List<Artist> result = artistService.addBulkArtists(dtos);

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(artistRepository).save(any(Artist.class));
        verify(artistCache).put(eq(1), any(Artist.class));
    }

    @Test
    void addBulkArtists_EmptyList_ThrowsValidationException() {
        ValidationException exception = assertThrows(ValidationException.class, () -> artistService.addBulkArtists(Collections.emptyList()));
        assertEquals("Artist list cannot be null or empty", exception.getMessage());
    }

    @Test
    void addBulkArtists_TooManyItems_ThrowsValidationException() {
        List<ArtistDTO> dtos = new ArrayList<>();
        for (int i = 0; i < ApplicationConstants.MAX_BULK_OPERATION_SIZE + 1; i++) {
            dtos.add(new ArtistDTO());
        }
        ValidationException exception = assertThrows(ValidationException.class, () -> artistService.addBulkArtists(dtos));
        assertEquals("Cannot add more than " + ApplicationConstants.MAX_BULK_OPERATION_SIZE + " artists at once", exception.getMessage());
    }

    @Test
    void getArtistById_FromCache_ReturnsArtist() {
        when(artistCache.get(1)).thenReturn(Optional.of(artist));

        Optional<Artist> result = artistService.getArtistById(1);

        assertTrue(result.isPresent());
        assertEquals("John", result.get().getFirstName());
        verify(artistRepository, never()).findById(anyInt());
        verify(artistCache).get(1);
    }

    @Test
    void getArtistById_NotInCache_FetchesFromRepository() {
        when(artistCache.get(1)).thenReturn(Optional.empty());
        when(artistRepository.findById(1)).thenReturn(Optional.of(artist));

        Optional<Artist> result = artistService.getArtistById(1);

        assertTrue(result.isPresent());
        assertEquals("John", result.get().getFirstName());
        verify(artistRepository).findById(1);
        verify(artistCache).put(1, artist);
    }

    @Test
    void updateArtist_ValidDTO_UpdatesArtist() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);

        Artist result = artistService.updateArtist(1, artistDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(artistRepository).save(any(Artist.class));
        verify(artistCache).update(eq(1), any(Artist.class));
    }

    @Test
    void updateArtist_NotFound_ThrowsNotFoundException() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> artistService.updateArtist(1, artistDTO));
        assertEquals(ArtistService.ARTIST_NOT_FOUND + 1, exception.getMessage());
    }

    @Test
    void patchArtist_ValidPatch_UpdatesArtist() {
        ArtistPatchDTO patchDTO = new ArtistPatchDTO();
        patchDTO.setFirstName("Jane");
        patchDTO.setLastName("Smith");

        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);

        Artist result = artistService.patchArtist(1, patchDTO);

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        verify(artistRepository).save(any(Artist.class));
        verify(artistCache).update(eq(1), any(Artist.class));
    }

    @Test
    void patchArtist_NoUpdates_ThrowsIllegalArgumentException() {
        ArtistPatchDTO patchDTO = new ArtistPatchDTO();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> artistService.patchArtist(1, patchDTO));
        assertEquals("No fields to update", exception.getMessage());
    }

    @Test
    void deleteArtist_ExistingArtist_RemovesAndUpdatesCache() {
        artist.setArts(new HashSet<>(Collections.singletonList(art)));
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));

        artistService.deleteArtist(1);

        verify(artistRepository).delete(artist);
        verify(artistCache).evict(1);
        verify(artCache).update(eq(1), eq(art));
    }

    @Test
    void deleteArtist_NotFound_ThrowsNotFoundException() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> artistService.deleteArtist(1));
        assertEquals(ArtistService.ARTIST_NOT_FOUND + 1, exception.getMessage());
    }

    @Test
    void searchArtists_BothNames_ReturnsArtists() {
        when(artistRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("John", "Doe"))
                .thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.searchArtists("John", "Doe");

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(artistRepository).findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("John", "Doe");
    }

    @Test
    void searchArtists_FirstNameOnly_ReturnsArtists() {
        when(artistRepository.findByFirstNameContainingIgnoreCase("John")).thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.searchArtists("John", null);

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(artistRepository).findByFirstNameContainingIgnoreCase("John");
    }

    @Test
    void searchArtists_LastNameOnly_ReturnsArtists() {
        when(artistRepository.findByLastNameContainingIgnoreCase("Doe")).thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.searchArtists(null, "Doe");

        assertEquals(1, result.size());
        assertEquals("Doe", result.get(0).getLastName());
        verify(artistRepository).findByLastNameContainingIgnoreCase("Doe");
    }

    @Test
    void searchArtists_NoNames_ReturnsEmptyList() {
        List<Artist> result = artistService.searchArtists(null, null);

        assertTrue(result.isEmpty());
        verify(artistRepository, never()).findByFirstNameContainingIgnoreCase(anyString());
        verify(artistRepository, never()).findByLastNameContainingIgnoreCase(anyString());
    }

    @Test
    void getArtistsByArtTitle_ValidTitle_ReturnsArtists() {
        when(artistRepository.findByArtTitleContaining("Mona Lisa")).thenReturn(Collections.singletonList(artist));

        List<Artist> result = artistService.getArtistsByArtTitle("Mona Lisa");

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(artistRepository).findByArtTitleContaining("Mona Lisa");
    }
}