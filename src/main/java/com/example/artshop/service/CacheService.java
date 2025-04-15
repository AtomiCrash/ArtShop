package com.example.artshop.service;

import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.service.cache.EntityCache;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final EntityCache<Artist> artistCache = new EntityCache<>("Artist");

    private final EntityCache<Art> artCache = new EntityCache<>("Art");

    private final EntityCache<Classification> classificationCache = new EntityCache<>("Classification");

    public EntityCache<Artist> getArtistCache() {
        return artistCache;
    }

    public EntityCache<Art> getArtCache() {
        return artCache;
    }

    public EntityCache<Classification> getClassificationCache() {
        return classificationCache;
    }
}