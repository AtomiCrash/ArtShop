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
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private EntityCache<Classification> classificationCache;
    @Mock
    private EntityCache<Artist> artistCache;
    @Mock
    private EntityCache<Art> artCache;

    @InjectMocks
    private ArtService artService;

    @Test
    void addBulkArts_WithValidData_ReturnsSavedArts() {
        ArtDTO artDTO = createValidArtDTO();
        Artist artist = createArtist("Artist");
        Classification classification = new Classification();
        Art expectedArt = createArt("Test Art", 2020, classification, Set.of(artist));

        when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Art", result.get(0).getTitle());
        assertEquals(2020, result.get(0).getYear());
    }

    @Test
    void addBulkArts_WithNonExistingArtist_ThrowsNotFoundException() {
        ArtDTO artDTO = createValidArtDTO();
        when(artistRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addBulkArts_WithoutTitle_ThrowsValidationException() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setYear(2020);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addBulkArts_WithEmptyTitle_ThrowsValidationException() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("");
        artDTO.setYear(2020);

        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addBulkArts_WithoutClassification_SavesArtWithoutClassification() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);

        Art expectedArt = new Art();
        expectedArt.setTitle("Test Art");
        expectedArt.setYear(2020);

        when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertNotNull(result);
        assertNull(result.get(0).getClassification());
    }

    @Test
    void addBulkArts_WithoutArtists_SavesArtWithoutArtists() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);

        Art expectedArt = new Art();
        expectedArt.setTitle("Test Art");
        expectedArt.setYear(2020);

        when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertNotNull(result);
        assertTrue(result.get(0).getArtists().isEmpty());
    }

    @Test
    void addBulkArts_WithEmptyArtistsList_SavesArtWithoutArtists() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);
        artDTO.setArtists(Collections.emptyList());

        Art expectedArt = new Art();
        expectedArt.setTitle("Test Art");
        expectedArt.setYear(2020);

        when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertNotNull(result);
        assertTrue(result.get(0).getArtists().isEmpty());
    }

    @Test
    void addBulkArts_WithNullList_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(null));
    }

    @Test
    void addBulkArts_WithEmptyList_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artService.addBulkArts(Collections.emptyList()));
    }

    @Test
    void addBulkArts_WithTooManyItems_ThrowsValidationException() {
        List<ArtDTO> largeList = Collections.nCopies(101, new ArtDTO());
        assertThrows(ValidationException.class, () -> artService.addBulkArts(largeList));
    }

    @Test
    void addArt_WithValidData_ReturnsSavedArt() {
        ArtDTO artDTO = createTestArtDTO();
        Artist artist = createTestArtist("Artist");
        Classification classification = createTestClassification("Test Classification");
        Art expectedArt = createTestArt("Test Art", 2020, classification, Set.of(artist));

        when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        assertNotNull(result);
        assertEquals("Test Art", result.getTitle());
        assertEquals(2020, result.getYear());
    }

    @Test
    void addArt_WithNewClassification_CreatesAndSavesClassification() {
        ArtDTO artDTO = createTestArtDTO();
        artDTO.getClassification().setId(null);

        Artist artist = createTestArtist("Artist");
        Classification newClassification = createTestClassification("New Classification");

        when(classificationRepository.findByName(anyString())).thenReturn(null);
        when(classificationRepository.save(any(Classification.class))).thenReturn(newClassification);
        when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(createTestArt("Test Art", 2020, newClassification, Set.of(artist)));
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void addArt_WithNewArtist_CreatesAndSavesArtist() {
        ArtDTO artDTO = createTestArtDTO();
        artDTO.getArtists().get(0).setId(null);
        Artist newArtist = createTestArtist("New Artist");
        Classification classification = createTestClassification("Test Classification");

        when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);
        when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        when(artRepository.save(any(Art.class))).thenReturn(createTestArt("Test Art", 2020, classification, Set.of(newArtist)));
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void addArt_WithNullClassification_DoesNotUpdateClassificationCache() {
        ArtDTO artDTO = createTestArtDTO();
        artDTO.setClassification(null);
        Artist artist = createTestArtist("Artist");
        Art expectedArt = createTestArt("Test Art", 2020, null, Set.of(artist));

        when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        when(cacheService.getArtistCache()).thenReturn(artistCache);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        assertNotNull(result);
        verify(cacheService, never()).getClassificationCache();
    }

    private ArtDTO createValidArtDTO() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);

        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Test Classification");
        artDTO.setClassification(classificationDTO);

        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setLastName("Artist");
        artDTO.setArtists(List.of(artistDTO));

        return artDTO;
    }

    private Artist createArtist(String lastName) {
        Artist artist = new Artist();
        artist.setLastName(lastName);
        return artist;
    }

    private Art createArt(String title, int year, Classification classification, Set<Artist> artists) {
        Art art = new Art();
        art.setTitle(title);
        art.setYear(year);
        art.setClassification(classification);
        art.setArtists(artists);
        return art;
    }

    private ArtDTO createTestArtDTO() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);

        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setName("Test Classification");
        classificationDTO.setDescription("Test Description");
        artDTO.setClassification(classificationDTO);

        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setLastName("Artist");
        artDTO.setArtists(Collections.singletonList(artistDTO));

        return artDTO;
    }

    private Artist createTestArtist(String lastName) {
        Artist artist = new Artist();
        artist.setLastName(lastName);
        return artist;
    }

    private Classification createTestClassification(String name) {
        Classification classification = new Classification();
        classification.setName(name);
        classification.setDescription("Test Description");
        return classification;
    }

    private Art createTestArt(String title, int year, Classification classification, Set<Artist> artists) {
        Art art = new Art();
        art.setTitle(title);
        art.setYear(year);
        art.setClassification(classification);
        art.setArtists(artists);
        return art;
    }


    @Test
    void patchArt_WithValidUpdates_ReturnsUpdatedArt() {
        // Create a mock ArtPatchDTO with setters
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            private String title;
            private Integer year;
            private int classificationId;
            private Set<Integer> artistIds;

            @Override
            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            @Override
            public Integer getYear() {
                return year;
            }

            public void setYear(Integer year) {
                this.year = year;
            }

            @Override
            public int getClassificationId() {
                return classificationId;
            }

            public void setClassificationId(int classificationId) {
                this.classificationId = classificationId;
            }

            @Override
            public Set<Integer> getArtistIds() {
                return artistIds;
            }

            public void setArtistIds(Set<Integer> artistIds) {
                this.artistIds = artistIds;
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };
        Art existingArt = createTestArt("Original Title", 2020, null, new HashSet<>());
        when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.patchArt(1, patchDTO);

        assertEquals("Updated Title", result.getTitle());
        assertEquals(2021, result.getYear());
        verify(artCache).update(1, result);
    }

    @Test
    void patchArt_WithNullPatchDTO_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artService.patchArt(1, null));
    }

    @Test
    void patchArt_WithNoUpdates_ThrowsValidationException() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            @Override
            public boolean hasUpdates() {
                return false;
            }
        };
        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithEmptyTitle_ThrowsValidationException() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            private String title = "";

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };
        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithInvalidYear_ThrowsValidationException() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            private Integer year = 999;

            @Override
            public Integer getYear() {
                return year;
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };
        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithClassificationUpdate_UpdatesClassification() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            private int classificationId = 1;

            @Override
            public int getClassificationId() {
                return classificationId;
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };

        Classification classification = createTestClassification("New Classification");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        when(classificationRepository.findById(anyInt())).thenReturn(classification);
        when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Art result = artService.patchArt(1, patchDTO);

        assertNotNull(result.getClassification());
        verify(classificationCache).update(1, classification);
    }

    @Test
    void patchArt_WithArtistUpdates_UpdatesArtists() {
        ArtPatchDTO patchDTO = new ArtPatchDTO() {
            private Set<Integer> artistIds = new HashSet<>(Arrays.asList(1, 2));

            @Override
            public Set<Integer> getArtistIds() {
                return artistIds;
            }

            @Override
            public boolean hasUpdates() {
                return true;
            }
        };

        Artist artist1 = createTestArtist("Artist1");
        Artist artist2 = createTestArtist("Artist2");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        when(artistRepository.findAllById(any())).thenReturn(Arrays.asList(artist1, artist2));
        when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        when(cacheService.getArtCache()).thenReturn(artCache);
        when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.patchArt(1, patchDTO);

        assertEquals(2, result.getArtists().size());
        verify(artistCache, times(2)).update(anyInt(), any(Artist.class));
    }
}