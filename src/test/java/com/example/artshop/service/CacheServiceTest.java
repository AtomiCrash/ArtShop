package com.example.artshop.service;

import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    private CacheService cacheService = new CacheService();

    @Test
    void testGetArtistCache_ReturnsArtistCache() {
        EntityCache<Artist> cache = cacheService.getArtistCache();
        assertNotNull(cache);
        assertEquals("Artist", cache.getName());
    }

    @Test
    void testGetArtCache_ReturnsArtCache() {
        EntityCache<Art> cache = cacheService.getArtCache();
        assertNotNull(cache);
        assertEquals("Art", cache.getName());
    }

    @Test
    void testGetClassificationCache_ReturnsClassificationCache() {
        EntityCache<Classification> cache = cacheService.getClassificationCache();
        assertNotNull(cache);
        assertEquals("Classification", cache.getName());
    }
}