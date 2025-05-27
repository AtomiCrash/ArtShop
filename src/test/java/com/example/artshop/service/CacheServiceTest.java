package com.example.artshop.service;

import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CacheServiceTest {  // ← ВАЖНО: открытие класса

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService();
    }

    @Test
    void testGetArtistCache() {
        EntityCache<Artist> artistCache = cacheService.getArtistCache();
        assertNotNull(artistCache);
        assertEquals("Artist", artistCache.getName());
    }

    @Test
    void testGetArtCache() {
        EntityCache<Art> artCache = cacheService.getArtCache();
        assertNotNull(artCache);
        assertEquals("Art", artCache.getName());
    }

    @Test
    void testGetClassificationCache() {
        EntityCache<Classification> classificationCache = cacheService.getClassificationCache();
        assertNotNull(classificationCache);
        assertEquals("Classification", classificationCache.getName());
    }
}
