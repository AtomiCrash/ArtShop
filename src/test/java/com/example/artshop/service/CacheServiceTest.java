package com.example.artshop.service;

import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService();
    }

    @Test
    void testGetArtistCache() {
        EntityCache<Artist> artistCache = cacheService.getArtistCache();

        assertNotNull(artistCache);
    }

    @Test
    void testGetArtCache() {
        EntityCache<Art> artCache = cacheService.getArtCache();

        assertNotNull(artCache);
    }

    @Test
    void testGetClassificationCache() {
        EntityCache<Classification> classificationCache = cacheService.getClassificationCache();

        assertNotNull(classificationCache);

    }

    @Test
    void testCacheIndependence() {
        EntityCache<Artist> artistCache = cacheService.getArtistCache();
        EntityCache<Art> artCache = cacheService.getArtCache();

        Artist artist = new Artist();
        artist.setId(1);
        artist.setFirstName("Test");

        Art art = new Art();
        art.setId(1);
        art.setTitle("Test Art");

        artistCache.put(1, artist);
        artCache.put(1, art);

        assertTrue(artistCache.get(1).isPresent());
        assertTrue(artCache.get(1).isPresent());
        assertEquals("Test", artistCache.get(1).get().getFirstName());
        assertEquals("Test Art", artCache.get(1).get().getTitle());
    }

    @Test
    void testCacheServiceSingletonBehavior() {
        CacheService anotherInstance = new CacheService();

        assertNotSame(
                cacheService.getArtistCache(),
                anotherInstance.getArtistCache()
        );
    }
}