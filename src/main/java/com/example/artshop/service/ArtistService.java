package com.example.artshop.service;

import com.example.artshop.service.cache.EntityCache;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtistRepository;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArtistService {
    private final ArtistRepository artistRepository;
    private final EntityCache<Artist> artistCache;

    @Autowired
    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
        this.artistCache = new EntityCache<>("Artist");
    }

    @Transactional
    public List<Artist> getArtistsByArtTitle(String artTitle) {
        return artistRepository.findByArtTitleContaining(artTitle);
    }

    @Transactional
    public Artist createArtist(ArtistDTO artistDTO) {
        Artist artist = new Artist();
        artist.setFirstName(artistDTO.getFirstName());
        artist.setMiddleName(artistDTO.getMiddleName());
        artist.setLastName(artistDTO.getLastName());

        Artist savedArtist = artistRepository.save(artist);
        artistCache.put(savedArtist.getId(), savedArtist);
        return savedArtist;
    }

    @Transactional
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    @Transactional
    public Optional<Artist> getArtistById(Integer id) {
        return artistCache.get(id)
                .map(Optional::of)
                .orElseGet(() -> {
                    Optional<Artist> artist = artistRepository.findById(id);
                    artist.ifPresent(a -> artistCache.put(a.getId(), a));
                    return artist;
                });
    }

    @Transactional
    public Artist updateArtist(Integer id, ArtistDTO artistDTO) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artist not found with id: " + id));

        artist.setFirstName(artistDTO.getFirstName());
        artist.setMiddleName(artistDTO.getMiddleName());
        artist.setLastName(artistDTO.getLastName());

        Artist updatedArtist = artistRepository.save(artist);
        artistCache.update(id, updatedArtist);
        return updatedArtist;
    }

    @Transactional
    public void deleteArtist(Integer id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artist not found with id: " + id));

        // Разрываем связи перед удалением
        artist.getArts().forEach(art -> art.getArtists().remove(artist));
        artistRepository.delete(artist);
        artistCache.evict(id);
    }

    @Transactional
    public List<Artist> searchArtists(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return artistRepository.findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName,
                    lastName);
        } else if (firstName != null) {
            return artistRepository.findByFirstNameContainingIgnoreCase(firstName);
        } else if (lastName != null) {
            return artistRepository.findByLastNameContainingIgnoreCase(lastName);
        }
        return Collections.emptyList();
    }

    @Transactional
    public Artist patchArtist(Integer id, ArtistPatchDTO artistPatchDTO) {
        if (!artistPatchDTO.hasUpdates()) {
            throw new IllegalArgumentException("No fields to update");
        }

        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artist not found with id: " + id));

        if (artistPatchDTO.getFirstName() != null) {
            artist.setFirstName(artistPatchDTO.getFirstName());
        }
        if (artistPatchDTO.getMiddleName() != null) {
            artist.setMiddleName(artistPatchDTO.getMiddleName());
        }
        if (artistPatchDTO.getLastName() != null) {
            artist.setLastName(artistPatchDTO.getLastName());
        }

        Artist patchedArtist = artistRepository.save(artist);
        artistCache.update(id, patchedArtist);
        return patchedArtist;
    }

    public String getCacheInfo() {
        return artistCache.getCacheInfo();
    }
}