package com.example.artshop.service;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ArtRepository;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.repository.ClassificationRepository;
import com.example.artshop.service.cache.EntityCache;
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
    private ArtPatchDTO artPatchDTO;
    private Artist artist;
    private Classification classification;

    @BeforeEach
    void setUp() {
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        
        artist = new Artist();
        artist.setId(1);
        artist.setFirstName("John");
        artist.setLastName("Doe");
        
        classification = new Classification();
        classification.setId(1);
        classification.setName("Painting");
        
        art = new Art();
        art.setId(1);
        art.setTitle("Mona Lisa");
        art.setYear(1503);
        art.setArtists(new HashSet<>(Collections.singletonList(artist)));
        art.setClassification(classification);
        
        artDTO = new ArtDTO();
        artDTO.setTitle("Mona Lisa");
        artDTO.setYear(1503);
        artDTO.setArtists(List.of(new ArtistDTO()));
        artDTO.setClassification(new ClassificationDTO());
        
        artPatchDTO = new ArtPatchDTO();
        artPatchDTO.setTitle("Updated");
    }

    @Test
    void getArtsByArtistName_ShouldReturnArts() {
        when(artRepository.findByArtistsLastNameContainingIgnoreCase("Doe")).thenReturn(List.of(art));
        
        List<Art> result = artService.getArtsByArtistName("Doe");
        
        assertEquals(1, result.size());
    }

    @Test
    void addArt_ShouldAddArt() {
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        when(classificationRepository.save(any(Classification.class))).thenReturn(classification);
        
        Art result = artService.addArt(artDTO);
        
        assertNotNull(result);
        verify(artCache).put(1, art);
    }

    @Test
    void addArt_ShouldThrowValidationException_WhenTitleIsNull() {
        artDTO.setTitle(null);
        
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_ShouldThrowValidationException_WhenYearInFuture() {
        artDTO.setYear(LocalDate.now().getYear() + 1);
        
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void patchArt_ShouldPatchArt() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        
        Art result = artService.patchArt(1, artPatchDTO);
        
        assertEquals("Updated", result.getTitle());
        verify(artCache).update(1, art);
    }

    @Test
    void patchArt_ShouldThrowValidationException_WhenNoUpdates() {
        artPatchDTO.setTitle(null);
        artPatchDTO.setYear(null);
        
        assertThrows(ValidationException.class, () -> artService.patchArt(1, artPatchDTO));
    }

    @Test
    void getAllArts_ShouldReturnAllArts() {
        when(artRepository.findAll()).thenReturn(List.of(art));
        
        List<Art> result = artService.getAllArts();
        
        assertEquals(1, result.size());
    }

    @Test
    void getArtById_ShouldReturnFromCache() {
        when(artCache.get(1)).thenReturn(Optional.of(art));
        
        Art result = artService.getArtById(1);
        
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void updateArt_ShouldUpdateArt() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        when(artRepository.save(any(Art.class))).thenReturn(art);
        when(artistRepository.save(any(Artist.class))).thenReturn(artist);
        
        Art result = artService.updateArt(1, artDTO);
        
        assertNotNull(result);
        verify(artCache).update(1, art);
    }

    @Test
    void getArtsByClassificationId_ShouldReturnArts() {
        when(artRepository.findByClassificationId(1)).thenReturn(List.of(art));
        
        List<Art> result = artService.getArtsByClassificationId(1);
        
        assertEquals(1, result.size());
        verify(artCache).put(1, art);
    }

    @Test
    void deleteArtById_ShouldDeleteArt() {
        when(artRepository.findWithArtistsById(1)).thenReturn(Optional.of(art));
        
        artService.deleteArtById(1);
        
        verify(artRepository).delete(art);
        verify(artCache).evict(1);
    }

    @Test
    void getArtByTitle_ShouldReturnArt() {
        when(artRepository.findByTitle("Mona Lisa")).thenReturn(Optional.of(art));
        
        Art result = artService.getArtByTitle("Mona Lisa");
        
        assertEquals("Mona Lisa", result.getTitle());
    }

    @Test
    void addBulkArts_ShouldAddThreeArts() {
        List<ArtDTO> artDTOs = List.of(new ArtDTO(), new ArtDTO(), new ArtDTO());
        when(artRepository.save(any(Art.class))).thenReturn(art);
        
        List<Art> result = artService.addBulkArts(artDTOs);
        
        assertEquals(3, result.size());
    }

    @Test
    void addBulkArts_ShouldThrowException_WhenNotThreeItems() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(new ArtDTO())));
    }
}