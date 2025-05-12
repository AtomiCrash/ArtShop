package com.example.artshop.service;

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
        artist = new Artist();
        artist.setId(1);
        artist.setFirstName("John");
        artist.setLastName("Doe");
        
        artistDTO = new ArtistDTO();
        artistDTO.setFirstName("John");
        artistDTO.setLastName("Doe");
        
        artistPatchDTO = new ArtistPatchDTO();
        artistPatchDTO.setFirstName("Updated");
        
        when(cacheService.getArtistCache()).thenReturn(artistCache);
    }

    @Test
    void getArtistsByArtTitle_ShouldReturnArtists() {
        when(artistRepository.findByArtTitleContaining("art")).thenReturn(List.of(artist));
        
        List<Artist> result = artistService.getArtistsByArtTitle("art");
        
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
    }

    @Test
    void createArtist_ShouldCreateArtist() {
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        
        Artist result = artistService.createArtist(artistDTO);
        
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(artistCache).put(1, artist);
    }

    @Test
    void createArtist_ShouldThrowValidationException_WhenDataIsNull() {
        assertThrows(ValidationException.class, () -> artistService.createArtist(null));
    }

    @Test
    void createArtist_ShouldThrowValidationException_WhenNoNames() {
        artistDTO.setFirstName(null);
        artistDTO.setLastName(null);
        
        assertThrows(ValidationException.class, () -> artistService.createArtist(artistDTO));
    }

    @Test
    void getAllArtists_ShouldReturnAllArtists() {
        when(artistRepository.findAll()).thenReturn(List.of(artist));
        
        List<Artist> result = artistService.getAllArtists();
        
        assertEquals(1, result.size());
    }

    @Test
    void getArtistById_ShouldReturnArtistFromCache() {
        when(artistCache.get(1)).thenReturn(Optional.of(artist));
        
        Optional<Artist> result = artistService.getArtistById(1);
        
        assertTrue(result.isPresent());
        assertEquals("John", result.get().getFirstName());
    }

    @Test
    void getArtistById_ShouldReturnArtistFromRepository() {
        when(artistCache.get(1)).thenReturn(Optional.empty());
        when(artistRepository.findById(1)).thenReturn(Optional.of(artist));
        
        Optional<Artist> result = artistService.getArtistById(1);
        
        assertTrue(result.isPresent());
        verify(artistCache).put(1, artist);
    }

    @Test
    void updateArtist_ShouldUpdateArtist() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        
        Artist result = artistService.updateArtist(1, artistDTO);
        
        assertNotNull(result);
        verify(artistCache).update(1, artist);
    }

    @Test
    void updateArtist_ShouldThrowNotFoundException() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> artistService.updateArtist(1, artistDTO));
    }

    @Test
    void deleteArtist_ShouldDeleteArtist() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        
        artistService.deleteArtist(1);
        
        verify(artistRepository).delete(artist);
        verify(artistCache).evict(1);
    }

    @Test
    void searchArtists_ShouldSearchByFirstNameAndLastName() {
        when(artistRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase("John", "Doe"))
            .thenReturn(List.of(artist));
        
        List<Artist> result = artistService.searchArtists("John", "Doe");
        
        assertEquals(1, result.size());
    }

    @Test
    void patchArtist_ShouldPatchArtist() {
        when(artistRepository.findWithArtsById(1)).thenReturn(Optional.of(artist));
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        
        Artist result = artistService.patchArtist(1, artistPatchDTO);
        
        assertEquals("Updated", result.getFirstName());
        verify(artistCache).update(1, artist);
    }

    @Test
    void patchArtist_ShouldThrowException_WhenNoUpdates() {
        artistPatchDTO.setFirstName(null);
        
        assertThrows(IllegalArgumentException.class, () -> artistService.patchArtist(1, artistPatchDTO));
    }
}