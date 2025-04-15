package com.example.artshop.service;

import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.service.cache.EntityCache;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArtistService implements ArtistServiceInterface {
    private final ArtistRepository artistRepository;
    private final CacheService cacheService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistService.class);
    public static final String ARTIST_NOT_FOUND = "Artist not found with id: ";

    @Autowired
    public ArtistService(ArtistRepository artistRepository, CacheService cacheService) {
        this.artistRepository = artistRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public List<Artist> getArtistsByArtTitle(String artTitle) {
        List<Artist> artists = artistRepository.findByArtTitleContaining(artTitle);
        if (artists.isEmpty()) LOGGER.warn("No artists found for artwork title: {}", artTitle);
        return artists;
    }

    @Transactional
    public Artist createArtist(ArtistDTO artistDTO) {
        Artist artist = new Artist();
        artist.setFirstName(artistDTO.getFirstName());
        artist.setMiddleName(artistDTO.getMiddleName());
        artist.setLastName(artistDTO.getLastName());
        Artist savedArtist = artistRepository.save(artist);
        cacheService.getArtistCache().put(savedArtist.getId(), savedArtist);
        return savedArtist;
    }

    @Transactional
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    @Transactional
    public Optional<Artist> getArtistById(Integer id) {
        return cacheService.getArtistCache().get(id)
                .map(Optional::of)
                .orElseGet(() -> {
                    Optional<Artist> artist = artistRepository.findById(id);
                    artist.ifPresent(a -> cacheService.getArtistCache().put(a.getId(), a));
                    return artist;
                });
    }

    @Transactional
    public Artist updateArtist(Integer id, ArtistDTO artistDTO) {
        Artist artist = artistRepository.findWithArtsById(id)
                .orElseThrow(() -> new NotFoundException(ARTIST_NOT_FOUND + id));
        artist.setFirstName(artistDTO.getFirstName());
        artist.setMiddleName(artistDTO.getMiddleName());
        artist.setLastName(artistDTO.getLastName());
        Artist updatedArtist = artistRepository.save(artist);
        cacheService.getArtistCache().update(id, updatedArtist);
        artist.getArts().forEach(art -> cacheService.getArtCache().update(art.getId(), art));
        return updatedArtist;
    }

    @Transactional
    public void deleteArtist(Integer id) {
        Artist artist = artistRepository.findWithArtsById(id)
                .orElseThrow(() -> new NotFoundException(ARTIST_NOT_FOUND + id));
        artist.getArts().forEach(art -> {
            art.getArtists().remove(artist);
            cacheService.getArtCache().update(art.getId(), art);
        });
        artistRepository.delete(artist);
        cacheService.getArtistCache().evict(id);
    }

    @Transactional
    public List<Artist> searchArtists(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return artistRepository
                    .findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName);
        } else if (firstName != null) {
            return artistRepository.findByFirstNameContainingIgnoreCase(firstName);
        } else if (lastName != null) {
            return artistRepository.findByLastNameContainingIgnoreCase(lastName);
        }
        return Collections.emptyList();
    }

    @Transactional
    public Artist patchArtist(Integer id, ArtistPatchDTO artistPatchDTO) {
        if (!artistPatchDTO.hasUpdates()) throw new IllegalArgumentException("No fields to update");
        Artist artist = artistRepository.findWithArtsById(id)
                .orElseThrow(() -> new NotFoundException(ARTIST_NOT_FOUND + id));
        if (artistPatchDTO.getFirstName() != null) artist.setFirstName(artistPatchDTO.getFirstName());
        if (artistPatchDTO.getMiddleName() != null) artist.setMiddleName(artistPatchDTO.getMiddleName());
        if (artistPatchDTO.getLastName() != null) artist.setLastName(artistPatchDTO.getLastName());
        Artist patchedArtist = artistRepository.save(artist);
        cacheService.getArtistCache().update(id, patchedArtist);
        artist.getArts().forEach(art -> cacheService.getArtCache().update(art.getId(), art));
        return patchedArtist;
    }

    public String getCacheInfo() {
        return cacheService.getArtistCache().getCacheInfo();
    }

    public EntityCache<Artist> getArtistCache() {
        return cacheService.getArtistCache();
    }
}