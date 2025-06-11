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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtServiceTest {
    @Mock private ArtRepository artRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private ClassificationRepository classificationRepository;
    @Mock private CacheService cacheService;
    @Mock private EntityCache<Classification> classificationCache;
    @Mock private EntityCache<Artist> artistCache;
    @Mock private EntityCache<Art> artCache;

    @InjectMocks private ArtService artService;

    private ArtDTO createValidArtDTO() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setTitle("Test Art");
        artDTO.setYear(2020);
        ClassificationDTO classificationDTO = new ClassificationDTO();
        classificationDTO.setId(1);
        classificationDTO.setName("Test Classification");
        classificationDTO.setDescription("Test Description");
        artDTO.setClassification(classificationDTO);
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setId(1);
        artistDTO.setLastName("Test Artist");
        artDTO.setArtists(List.of(artistDTO));
        return artDTO;
    }

    private Art createTestArt(String title, Integer year, Classification classification, Set<Artist> artists) {
        Art art = new Art();
        art.setId(1);
        art.setTitle(title);
        art.setYear(year);
        art.setClassification(classification);
        art.setArtists(artists);
        return art;
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

    @Test
    void addBulkArts_WithValidData_ReturnsSavedArts() {
        ArtDTO artDTO = createValidArtDTO();
        Artist artist = createTestArtist("Test Artist");
        Classification classification = createTestClassification("Test Classification");
        Art expectedArt = createTestArt("Test Art", 2020, classification, Set.of(artist));

        lenient().when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertEquals(1, result.size());
        assertEquals("Test Art", result.get(0).getTitle());
    }

    @Test
    void addBulkArts_WithNonExistingArtist_ThrowsNotFoundException() {
        ArtDTO artDTO = createValidArtDTO();
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.empty());
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
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setClassification(null);
        Artist artist = createTestArtist("Test Artist");
        Art expectedArt = createTestArt("Test Art", 2020, null, Set.of(artist));

        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertNull(result.get(0).getClassification());
    }

    @Test
    void addBulkArts_WithoutArtists_SavesArtWithoutArtists() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(null);
        Art expectedArt = createTestArt("Test Art", 2020, null, new HashSet<>());

        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

        assertTrue(result.get(0).getArtists().isEmpty());
    }

    @Test
    void addBulkArts_WithEmptyArtistsList_SavesArtWithoutArtists() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(Collections.emptyList());
        Art expectedArt = createTestArt("Test Art", 2020, null, new HashSet<>());

        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        List<Art> result = artService.addBulkArts(List.of(artDTO));

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
        ArtDTO artDTO = createValidArtDTO();
        Artist artist = createTestArtist("Test Artist");
        Classification classification = createTestClassification("Test Classification");
        Art expectedArt = createTestArt("Test Art", 2020, classification, Set.of(artist));

        lenient().when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        assertEquals("Test Art", result.getTitle());
        verify(artCache).put(anyInt(), any(Art.class));
    }

    @Test
    void addArt_WithNewClassification_CreatesAndSavesClassification() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setId(null);
        artDTO.getClassification().setName("New Classification");
        Artist artist = createTestArtist("Test Artist");
        Classification newClassification = createTestClassification("New Classification");
        Art expectedArt = createTestArt("Test Art", 2020, newClassification, Set.of(artist));

        lenient().when(classificationRepository.findByName(anyString())).thenReturn(null);
        lenient().when(classificationRepository.save(any(Classification.class))).thenReturn(newClassification);
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        verify(classificationRepository).save(any(Classification.class));
    }

    @Test
    void addArt_WithExistingClassificationByName_DoesNotCreateNew() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setId(null);
        artDTO.getClassification().setName("Existing Classification");
        Artist artist = createTestArtist("Test Artist");
        Classification existingClassification = createTestClassification("Existing Classification");
        Art expectedArt = createTestArt("Test Art", 2020, existingClassification, Set.of(artist));

        lenient().when(classificationRepository.findByName(anyString())).thenReturn(existingClassification);
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        verify(classificationRepository, never()).save(any(Classification.class));
    }

    @Test
    void addArt_WithNewArtist_CreatesAndSavesArtist() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getArtists().get(0).setId(null);
        Artist newArtist = createTestArtist("New Artist");
        Classification classification = createTestClassification("Test Classification");
        Art expectedArt = createTestArt("Test Art", 2020, classification, Set.of(newArtist));

        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void addArt_WithNullClassification_DoesNotUpdateClassificationCache() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setClassification(null);
        Artist artist = createTestArtist("Test Artist");
        Art expectedArt = createTestArt("Test Art", 2020, null, Set.of(artist));

        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        verify(cacheService, never()).getClassificationCache();
    }

    @Test
    void addArt_WithNullArtDTO_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artService.addArt(null));
    }

    @Test
    void addArt_WithEmptyTitle_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setTitle("");
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithFutureYear_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(LocalDate.now().getYear() + 1);
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithLongTitle_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setTitle("a".repeat(61));
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void patchArt_WithValidUpdates_ReturnsUpdatedArt() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getTitle()).thenReturn("Updated Title");
        lenient().when(patchDTO.getYear()).thenReturn(2021);

        Art existingArt = createTestArt("Original Title", 2020, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.patchArt(1, patchDTO);

        assertEquals("Updated Title", result.getTitle());
        verify(artCache).update(1, result);
    }

    @Test
    void patchArt_WithNullPatchDTO_ThrowsValidationException() {
        assertThrows(ValidationException.class, () -> artService.patchArt(1, null));
    }

    @Test
    void patchArt_WithNoUpdates_ThrowsValidationException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(false);
        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithEmptyTitle_ThrowsValidationException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getTitle()).thenReturn("");
        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithInvalidYear_ThrowsValidationException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getYear()).thenReturn(999);
        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithClassificationUpdate_UpdatesClassification() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getClassificationId()).thenReturn(1);
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Classification classification = createTestClassification("New Classification");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(classificationRepository.findById(anyInt())).thenReturn(classification);
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);

        Art result = artService.patchArt(1, patchDTO);

        verify(classificationCache).update(1, classification);
    }

    @Test
    void patchArt_WithArtistUpdates_UpdatesArtists() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getArtistIds()).thenReturn(new HashSet<>(Set.of(1, 2)));
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Artist artist1 = createTestArtist("Artist1");
        Artist artist2 = createTestArtist("Artist2");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findAllById(any(Iterable.class))).thenReturn(Arrays.asList(artist1, artist2));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.patchArt(1, patchDTO);

        verify(artistCache, times(2)).update(anyInt(), any(Artist.class));
    }

    @Test
    void patchArt_WithEmptyArtistIds_ClearsArtists() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getArtistIds()).thenReturn(new HashSet<>());
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Artist existingArtist = createTestArtist("Existing Artist");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>(Set.of(existingArtist)));

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.patchArt(1, patchDTO);

        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void patchArt_WithNullArtistIds_ClearsArtists() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getArtistIds()).thenReturn(null);
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Artist existingArtist = createTestArtist("Existing Artist");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>(Set.of(existingArtist)));

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.patchArt(1, patchDTO);

        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void patchArt_WithNonExistingArt_ThrowsNotFoundException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getYear()).thenReturn(2020);
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithNonExistingClassification_ThrowsNotFoundException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getClassificationId()).thenReturn(1);
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(classificationRepository.findById(anyInt())).thenReturn(null);

        assertThrows(NotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithSomeInvalidArtistIds_ThrowsNotFoundException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getArtistIds()).thenReturn(new HashSet<>(Set.of(1, 2)));
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Artist artist1 = createTestArtist("Artist1");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findAllById(any(Iterable.class))).thenReturn(Arrays.asList(artist1));

        assertThrows(NotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithSomeNonExistingArtists_ThrowsNotFoundException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getArtistIds()).thenReturn(new HashSet<>(Set.of(1, 2)));
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Artist artist1 = createTestArtist("Artist1");
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findAllById(any(Iterable.class))).thenReturn(Arrays.asList(artist1));

        assertThrows(NotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithInvalidClassificationId_ThrowsNotFoundException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getClassificationId()).thenReturn(1);
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(classificationRepository.findById(anyInt())).thenReturn(null);

        assertThrows(NotFoundException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithLongTitle_ThrowsValidationException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getTitle()).thenReturn("a".repeat(61));
        lenient().when(patchDTO.getYear()).thenReturn(2020);

        Art existingArt = createTestArt("Original Title", 2020, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void patchArt_WithFutureYear_ThrowsValidationException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getYear()).thenReturn(LocalDate.now().getYear() + 1);

        Art existingArt = createTestArt("Original Title", 2020, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void getArtById_ReturnsCachedArt() {
        Art expectedArt = createTestArt("Cached Art", 2020, null, new HashSet<>());
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(artCache.get(anyInt())).thenReturn(Optional.of(expectedArt));

        ArtDTO result = artService.getArtById(1);

        assertEquals("Cached Art", result.getTitle());
    }

    @Test
    void getArtById_ReturnsFromRepositoryWhenNotCached() {
        Art expectedArt = createTestArt("Repository Art", 2020, null, new HashSet<>());
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(artCache.get(anyInt())).thenReturn(Optional.empty());
        lenient().when(artRepository.findById(anyInt())).thenReturn(Optional.of(expectedArt));

        ArtDTO result = artService.getArtById(1);

        assertEquals("Repository Art", result.getTitle());
    }

    @Test
    void getArtById_ThrowsNotFoundWhenNotExists() {
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(artCache.get(anyInt())).thenReturn(Optional.empty());
        lenient().when(artRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtById(1));
    }

    @Test
    void deleteArtById_RemovesArtAndUpdatesCache() {
        Artist artist = createTestArtist("Test Artist");
        Art art = createTestArt("Test Art", 2020, null, new HashSet<>(Set.of(artist)));

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(art));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        artService.deleteArtById(1);

        verify(artRepository).delete(art);
        verify(artCache).evict(1);
        verify(artistCache).update(artist.getId(), artist);
    }

    @Test
    void deleteArtById_WithNonExistingArt_ThrowsNotFound() {
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.deleteArtById(1));
    }

    @Test
    void getArtsByArtistName_ReturnsMatchingArts() {
        Artist artist = createTestArtist("Van Gogh");
        Art art1 = createTestArt("Starry Night", 1889, null, Set.of(artist));
        Art art2 = createTestArt("Sunflowers", 1888, null, Set.of(artist));

        lenient().when(artRepository.findByArtistsLastNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(art1, art2));

        List<Art> result = artService.getArtsByArtistName("Van");

        assertEquals(2, result.size());
    }

    @Test
    void getArtsByArtistName_ReturnsEmptyListWhenNoMatches() {
        lenient().when(artRepository.findByArtistsLastNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());

        List<Art> result = artService.getArtsByArtistName("NonExisting");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateArt_WithValidData_ReturnsUpdatedArt() {
        ArtDTO artDTO = createValidArtDTO();
        Artist artist = createTestArtist("Updated Artist");
        Classification classification = createTestClassification("Updated Classification");
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        Art updatedArt = createTestArt("Updated Title", 2020, classification, Set.of(artist));

        lenient().when(artRepository.findWithArtistsById(any())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findById(any())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(updatedArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.updateArt(1, artDTO);

        assertEquals("Updated Title", result.getTitle());
        verify(artCache).update(1, updatedArt);
    }

    @Test
    void updateArt_WithNonExistingArt_ThrowsNotFound() {
        ArtDTO artDTO = createValidArtDTO();
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void updateArt_WithNonExistingArtist_ThrowsNotFound() {
        ArtDTO artDTO = createValidArtDTO();
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void updateArt_WithNonExistingClassification_ThrowsNotFound() {
        ArtDTO artDTO = createValidArtDTO();
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(any())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findById(any())).thenReturn(Optional.of(createTestArtist("Artist")));
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void updateArt_WithNullArtists_ClearsArtists() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(null);
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>(Set.of(createTestArtist("Old Artist"))));
        Art updatedArt = createTestArt("Updated Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(updatedArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.updateArt(1, artDTO);

        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void updateArt_WithEmptyArtistsList_ClearsArtists() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(Collections.emptyList());
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>(Set.of(createTestArtist("Old Artist"))));
        Art updatedArt = createTestArt("Updated Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(any())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(updatedArt);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        Art result = artService.updateArt(1, artDTO);

        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void updateArt_WithLongTitle_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setTitle("a".repeat(61));
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void updateArt_WithFutureYear_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(LocalDate.now().getYear() + 1);
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void updateArt_WithLongArtistName_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        ArtistDTO artistDTO = artDTO.getArtists().get(0);
        artistDTO.setFirstName("a".repeat(61));
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void updateArt_WithInvalidClassification_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setName("");
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void getAllArts_ReturnsAllArts() {
        Art art1 = createTestArt("Art 1", 2020, null, new HashSet<>());
        Art art2 = createTestArt("Art 2", 2021, null, new HashSet<>());

        lenient().when(artRepository.findAll()).thenReturn(List.of(art1, art2));

        List<Art> result = artService.getAllArts();

        assertEquals(2, result.size());
    }

    @Test
    void getArtsByClassificationId_ReturnsMatchingArts() {
        Classification classification = createTestClassification("Test Classification");
        Art art1 = createTestArt("Art 1", 2020, classification, new HashSet<>());
        Art art2 = createTestArt("Art 2", 2021, classification, new HashSet<>());

        lenient().when(artRepository.findByClassificationId(anyInt())).thenReturn(List.of(art1, art2));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        List<Art> result = artService.getArtsByClassificationId(1);

        assertEquals(2, result.size());
    }

    @Test
    void getArtsByClassificationId_ReturnsEmptyListWhenNoMatches() {
        lenient().when(artRepository.findByClassificationId(anyInt())).thenReturn(Collections.emptyList());
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        List<Art> result = artService.getArtsByClassificationId(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getArtsByClassificationName_ReturnsMatchingArts() {
        Classification classification = createTestClassification("Impressionism");
        Art art1 = createTestArt("Art 1", 2020, classification, new HashSet<>());
        Art art2 = createTestArt("Art 2", 2021, classification, new HashSet<>());

        lenient().when(artRepository.findByClassificationNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(art1, art2));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        List<Art> result = artService.getArtsByClassificationName("Impression");

        assertEquals(2, result.size());
    }

    @Test
    void getArtsByClassificationName_ReturnsEmptyListWhenNoMatches() {
        lenient().when(artRepository.findByClassificationNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        List<Art> result = artService.getArtsByClassificationName("NonExisting");

        assertTrue(result.isEmpty());
    }

    @Test
    void getArtByTitle_ReturnsArt() {
        Art expectedArt = createTestArt("Specific Art", 2020, null, new HashSet<>());
        lenient().when(artRepository.findByTitle(anyString())).thenReturn(Optional.of(expectedArt));

        Art result = artService.getArtByTitle("Specific Art");

        assertEquals("Specific Art", result.getTitle());
    }

    @Test
    void getArtByTitle_ThrowsNotFound() {
        lenient().when(artRepository.findByTitle(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artService.getArtByTitle("Non-existent Art"));
    }

    @Test
    void getCacheInfo_ReturnsCacheInfo() {
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(artCache.getCacheInfo()).thenReturn("Cache info");

        String result = artService.getCacheInfo();

        assertEquals("Cache info", result);
    }

    @Test
    void getArtCache_ReturnsArtCache() {
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        EntityCache<Art> result = artService.getArtCache();

        assertSame(artCache, result);
    }

    @Test
    void addBulkArts_WithLongTitle_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setTitle("a".repeat(61));
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addBulkArts_WithFutureYear_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(LocalDate.now().getYear() + 1);
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addBulkArts_WithInvalidClassification_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setName("");
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addBulkArts_WithInvalidArtist_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        ArtistDTO artistDTO = artDTO.getArtists().get(0);
        artistDTO.setFirstName(null);
        artistDTO.setLastName(null);
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addArt_WithLongArtistName_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        ArtistDTO artistDTO = artDTO.getArtists().get(0);
        artistDTO.setFirstName("a".repeat(61));
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithLongClassificationName_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setName("a".repeat(61));
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithLongClassificationDescription_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setDescription("a".repeat(121));
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithNullClassification_ProcessesCorrectly() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setClassification(null);
        Artist artist = createTestArtist("Test Artist");
        Art expectedArt = createTestArt("Test Art", 2020, null, Set.of(artist));

        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        assertNull(result.getClassification());
        verify(artRepository).save(any(Art.class));
    }

    @Test
    void addArt_WithNewArtist_ProcessesCorrectly() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getArtists().get(0).setId(null);
        artDTO.getArtists().get(0).setLastName("New Artist");
        Classification classification = createTestClassification("Test Classification");
        Artist newArtist = createTestArtist("New Artist");
        Art expectedArt = createTestArt("Test Art", 2020, classification, Set.of(newArtist));

        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artistRepository.save(any(Artist.class))).thenReturn(newArtist);
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        verify(artistRepository).save(any(Artist.class));
        assertEquals("New Artist", result.getArtists().iterator().next().getLastName());
    }

    @Test
    void addArt_WithClassificationNotFoundByIdOrName_CreatesNewClassification() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getClassification().setId(null);
        artDTO.getClassification().setName("New Classification");
        Classification newClassification = createTestClassification("New Classification");
        Artist artist = createTestArtist("Test Artist");
        Art expectedArt = createTestArt("Test Art", 2020, newClassification, Set.of(artist));

        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(classificationRepository.findByName("New Classification")).thenReturn(null);
        lenient().when(classificationRepository.save(any(Classification.class))).thenReturn(newClassification);
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        Art result = artService.addArt(artDTO);

        verify(classificationRepository).save(any(Classification.class));
        assertEquals("New Classification", result.getClassification().getName());
    }

    @Test
    void processArtist_WithInvalidId_ThrowsNotFoundException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getArtists().get(0).setId(999);
        lenient().when(artistRepository.findById(999)).thenReturn(Optional.empty());
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(createTestClassification("Test Classification")));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(createTestArt("Test Art", 2020, null, new HashSet<>()));
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(NotFoundException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void patchArt_WithArtistIdsAsList_ThrowsClassCastException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        when(patchDTO.hasUpdates()).thenReturn(true);
        when(patchDTO.getArtistIds()).thenReturn(new HashSet<>(Arrays.asList(1, 2)));
        when(patchDTO.getYear()).thenReturn(2020);

        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findAllById(any(Iterable.class))).thenReturn(Collections.emptyList());
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ClassCastException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void updateArt_WithArtistIdsAsList_ThrowsClassCastException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(Arrays.asList(new ArtistDTO(), new ArtistDTO()));
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ClassCastException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void addBulkArts_WithMultipleValidArts_ReturnsSavedArts() {
        ArtDTO artDTO1 = createValidArtDTO();
        ArtDTO artDTO2 = createValidArtDTO();
        artDTO2.setTitle("Test Art 2");
        Artist artist = createTestArtist("Test Artist");
        Classification classification = createTestClassification("Test Classification");
        Art expectedArt1 = createTestArt("Test Art", 2020, classification, Set.of(artist));
        Art expectedArt2 = createTestArt("Test Art 2", 2020, classification, Set.of(artist));

        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt1, expectedArt2);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);
        lenient().when(cacheService.getClassificationCache()).thenReturn(classificationCache);
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        List<Art> result = artService.addBulkArts(Arrays.asList(artDTO1, artDTO2));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getTitle().equals("Test Art")));
        assertTrue(result.stream().anyMatch(a -> a.getTitle().equals("Test Art 2")));
    }

    @Test
    void patchArt_WithNullCache_ThrowsNullPointerException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        when(patchDTO.hasUpdates()).thenReturn(true);
        when(patchDTO.getYear()).thenReturn(2020);

        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void updateArt_WithNullCache_ThrowsNullPointerException() {
        ArtDTO artDTO = createValidArtDTO();
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        Artist artist = createTestArtist("Test Artist");
        Classification classification = createTestClassification("Test Classification");

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findById(anyInt())).thenReturn(Optional.of(artist));
        lenient().when(classificationRepository.findById(any())).thenReturn(Optional.of(classification));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);
        lenient().when(cacheService.getArtCache()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void addArt_WithInvalidYearRange_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(0);
        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void patchArt_WithInvalidYearRange_ThrowsValidationException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getYear()).thenReturn(0);

        Art existingArt = createTestArt("Original Title", 2020, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);

        assertThrows(ValidationException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void updateArt_WithInvalidYearRange_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(0);
        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(cacheService.getArtCache()).thenReturn(artCache);
        lenient().when(cacheService.getArtistCache()).thenReturn(artistCache);

        assertThrows(ValidationException.class, () -> artService.updateArt(1, artDTO));
    }

    @Test
    void addBulkArts_WithInvalidYearRange_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(0);
        assertThrows(ValidationException.class, () -> artService.addBulkArts(List.of(artDTO)));
    }

    @Test
    void addArt_WithNullCacheService_ThrowsNullPointerException() {
        ArtDTO artDTO = createValidArtDTO();
        lenient().when(cacheService.getArtistCache()).thenReturn(null);
        lenient().when(cacheService.getClassificationCache()).thenReturn(null);
        lenient().when(cacheService.getArtCache()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithYearOutOfRange_ThrowsValidationException() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setYear(0);

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void addArt_WithNullArtists_ProcessesCorrectly() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(null);
        Artist artist = createTestArtist("Test Artist");
        Art expectedArt = createTestArt("Test Art", 2020, null, new HashSet<>());

        lenient().when(artRepository.save(any(Art.class))).thenReturn(expectedArt);

        Art result = artService.addArt(artDTO);

        assertTrue(result.getArtists().isEmpty());
    }

    @Test
    void patchArt_WithListArtistIds_ThrowsClassCastException() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getArtistIds()).thenReturn(new HashSet<>(Arrays.asList(1, 2)));
        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));

        assertThrows(ClassCastException.class, () -> artService.patchArt(1, patchDTO));
    }

    @Test
    void addArt_WithLongArtistName_ThrowsValidationExceptions() {
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getArtists().get(0).setFirstName("a".repeat(61));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void validateArtists_WithInvalidMiddleName_ThrowsValidationException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setMiddleName("a".repeat(61));
        artistDTO.setLastName("Doe");

        assertThrows(ValidationException.class, () -> artService.addArt(new ArtDTO() {{
            setArtists(List.of(artistDTO));
        }}));
    }

    @Test
    void updateArt_WithNullArtists_DoesNotClearArtists() {
        ArtDTO artDTO = new ArtDTO();
        artDTO.setArtists(null);
        Art existingArt = createTestArt("Original Title", 2019, null, Set.of(createTestArtist("Artist")));

        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);

        Art result = artService.updateArt(1, artDTO);

        assertFalse(result.getArtists().isEmpty());
    }

    @Test
    void patchArt_WithZeroClassificationId_DoesNotUpdateClassification() {
        ArtPatchDTO patchDTO = mock(ArtPatchDTO.class);
        lenient().when(patchDTO.hasUpdates()).thenReturn(true);
        lenient().when(patchDTO.getClassificationId()).thenReturn(0);

        Art existingArt = createTestArt("Title", 2020, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);

        Art result = artService.patchArt(1, patchDTO);

        assertNull(result.getClassification());
    }

    @Test
    void addArt_WithMiddleNameExceedingLimit_ThrowsValidationException() {
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setMiddleName("a".repeat(61));
        artistDTO.setLastName("Doe");
        ArtDTO artDTO = createValidArtDTO();
        artDTO.setArtists(List.of(artistDTO));

        assertThrows(ValidationException.class, () -> artService.addArt(artDTO));
    }

    @Test
    void updateArt_WithExistingArtist_UpdatesCorrectly() {
        Artist existingArtist = createTestArtist("Existing Artist");
        ArtDTO artDTO = createValidArtDTO();
        artDTO.getArtists().get(0).setId(1);

        Art existingArt = createTestArt("Original Title", 2019, null, new HashSet<>());
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(existingArt));
        lenient().when(artistRepository.findById(1)).thenReturn(Optional.of(existingArtist));
        lenient().when(artRepository.save(any(Art.class))).thenReturn(existingArt);

        Art result = artService.updateArt(1, artDTO);

        assertEquals(1, result.getArtists().size());
        assertEquals("Existing Artist", result.getArtists().iterator().next().getLastName());
    }

    @Test
    void deleteArtById_UpdatesArtistCache() {
        Artist artist = createTestArtist("Artist");
        Art art = createTestArt("Art", 2020, null, Set.of(artist));
        lenient().when(artRepository.findWithArtistsById(anyInt())).thenReturn(Optional.of(art));
        artService.deleteArtById(1);
        verify(artistCache).update(artist.getId(), artist);
    }
}