package com.example.artshop.service;

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
import com.example.artshop.service.CacheService;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtServiceInterfaceTest {

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

    private ArtDTO artDTO;
    private Art art;
    private Artist artist;
    private Classification classification;

    @BeforeEach
    void setUp() {
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        artist = new Artist();
        artist.setFirstName("Vincent");
        artist.setLastName("van Gogh");

        classification = new Classification();
        classification.setName("Painting");
        classification.setDescription("Oil on canvas");

        art = new Art();
        art.setTitle("Starry Night");
        art.setYear(1889);
        art.setArtists(Set.of(artist));
        art.setClassification(classification);

        artDTO = new ArtDTO();
        artDTO.setTitle("Starry Night");
        artDTO.setYear(1889);
        artDTO.setArtists(List.of(new ArtistDTO()));
        artDTO.setClassification(new ClassificationDTO());
    }

    @Test
    void getArtsByArtistName_ShouldReturnListOfArts() {
        lenient().when(artRepository.findByArtistsLastNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(art));

        List<Art> result = artService.getArtsByArtistName("Gogh");

        assertEquals(1, result.size());
        assertEquals("Starry Night", result.get(0).getTitle());
    }

    @Test
    void addArt_ShouldSaveArt() {
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findByName(anyString())).thenReturn(classification);
        lenient().when(artRepository.save(any(Art.class))).thenReturn(art);

        Art result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals("Starry Night", result.getTitle());
        verify(artCache).put(anyInt(), any(Art.class));
    }

    @Test
    void addArt_ShouldThrowValidationException_WhenTitleIsNull() {
        artDTO.setTitle(null);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_ShouldThrowValidationException_WhenYearIsInFuture() {
        artDTO.setYear(LocalDate.now().getYear() + 1);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void patchArt_ShouldUpdateArt() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            @Override
            public String getTitle() {
                return "New Title";
            }

            @Override
            public Integer getYear() {
                return 1890;
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(art));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(art);

        Art result = artService.patchArt(1, patchDTO);

        assertEquals("New Title", result.getTitle());
        assertEquals(1890, result.getYear());
        verify(artCache).update(anyInt(), any(Art.class));
    }

    @Test
    void patchArt_ShouldThrowNotFoundException_WhenArtNotFound() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            @Override
            public String getTitle() {
                return "New Title";
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void getAllArts_ShouldReturnAllArts() {
        lenient().when(artRepository.findAll()).thenReturn(List.of(art));

        List<Art> result = artService.getAllArts();

        assertEquals(1, result.size());
        assertEquals("Starry Night", result.get(0).getTitle());
    }

    @Test
    void getArtById_ShouldReturnArtFromCache() {
        lenient().when(artCache.get(anyInt())).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertEquals("Starry Night", result.getTitle());
        verify(artRepository, never()).findById(anyInt());
    }

    @Test
    void getArtById_ShouldReturnArtFromRepository_WhenNotInCache() {
        lenient().when(artCache.get(anyInt())).thenReturn(Optional.empty());
        lenient().when(artRepository.findById(anyInt())).thenReturn(Optional.of(art));

        ArtDTO result = artService.getArtById(1);

        assertEquals("Starry Night", result.getTitle());
        verify(artCache).put(anyInt(), any(Art.class));
    }

    @Test
    void getArtById_ShouldThrowNotFoundException_WhenArtNotFound() {
        lenient().when(artCache.get(anyInt())).thenReturn(Optional.empty());
        lenient().when(artRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtById(1));
    }

    @Test
    void updateArt_ShouldUpdateArt() {
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(art));
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(art);

        Art result = artService.updateArt(1, artDTO);

        assertNotNull(result);
        verify(artCache).update(anyInt(), any(Art.class));
    }

    @Test
    void getArtsByClassificationId_ShouldReturnArts() {
        lenient().when(artRepository.findByClassificationId(anyInt())).thenReturn(List.of(art));

        List<Art> result = artService.getArtsByClassificationId(1);

        assertEquals(1, result.size());
        assertEquals("Starry Night", result.get(0).getTitle());
        verify(artCache, times(1)).put(anyInt(), any(Art.class));
    }

    @Test
    void getArtsByClassificationName_ShouldReturnArts() {
        lenient().when(artRepository.findByClassificationNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(art));

        List<Art> result = artService.getArtsByClassificationName("Painting");

        assertEquals(1, result.size());
        assertEquals("Starry Night", result.get(0).getTitle());
        verify(artCache, times(1)).put(anyInt(), any(Art.class));
    }

    @Test
    void deleteArtById_ShouldDeleteArt() {
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(art));

        artService.deleteArtById(1);

        verify(artRepository).delete(any(Art.class));
        verify(artCache).evict(anyInt());
    }

    @Test
    void getArtByTitle_ShouldReturnArt() {
        lenient().when(artRepository.findByTitle(anyString())).thenReturn(Optional.of(art));

        Art result = artService.getArtByTitle("Starry Night");

        assertEquals(1, result.getId());
        assertEquals("Starry Night", result.getTitle());
    }

    @Test
    void addBulkArts_ShouldAddMultipleArts() {
        List<ArtDTO> artDTOs = List.of(artDTO, artDTO);
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findByName(anyString())).thenReturn(classification);
        lenient().when(artRepository.save(any(Art.class))).thenReturn(art);

        List<Art> result = artService.addBulkArts(artDTOs);

        assertEquals(2, result.size());
        verify(artRepository, times(2)).save(any(Art.class));
    }

    @Test
    void addBulkArts_ShouldThrowValidationException_WhenListIsEmpty() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(Collections.emptyList()));
    }

    @Test
    void getCacheInfo_ShouldReturnCacheInfo() {
        lenient().when(artCache.getCacheInfo()).thenReturn("Cache info");

        String result = artService.getCacheInfo();

        assertEquals("Cache info", result);
    }

    @Test
    void getArtCache_ShouldReturnCache() {
        EntityCache<Art> result = artService.getArtCache();

        assertEquals(artCache, result);
    }
}