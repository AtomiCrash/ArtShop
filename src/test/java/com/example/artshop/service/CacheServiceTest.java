package com.example.artshop.service;

import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.service.cache.EntityCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @InjectMocks
    private CacheService cacheService;
    
    @Mock
    private EntityCache<Artist> artistCache;
    
    @Mock
    private EntityCache<Art> artCache;
    
    @Mock
    private EntityCache<Classification> classificationCache;

    @Test
    void getArtistCache_ShouldReturnCache() {
        EntityCache<Artist> result = cacheService.getArtistCache();
        assertNotNull(result);
    }

    @Test
    void getArtCache_ShouldReturnCache() {
        EntityCache<Art> result = cacheService.getArtCache();
        assertNotNull(result);
    }

    @Test
    void getClassificationCache_ShouldReturnCache() {
        EntityCache<Classification> result = cacheService.getClassificationCache();
        assertNotNull(result);
    }
}