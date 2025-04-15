package com.example.artshop.service;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ClassificationDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.model.Classification;
import com.example.artshop.repository.ArtRepository;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.repository.ClassificationRepository;
import com.example.artshop.service.cache.EntityCache;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtService implements ArtServiceInterface {
    private final ArtRepository artRepository;
    private final ArtistRepository artistRepository;
    private final ClassificationRepository classificationRepository;
    private final CacheService cacheService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtService.class);
    public static final String ART_NOT_FOUND = "Art with id %d not found";
    public static final String ART_NOT_FOUNDSTRING = "Art with title %s not found";
    public static final String ART_NOT_FOUNDARTIST = "Artist not found with id: ";

    @Autowired
    public ArtService(ArtRepository artRepository,
                      ArtistRepository artistRepository,
                      ClassificationRepository classificationRepository,
                      CacheService cacheService) {
        this.artRepository = artRepository;
        this.artistRepository = artistRepository;
        this.classificationRepository = classificationRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public List<Art> getArtsByArtistName(String artistName) {
        List<Art> arts = artRepository.findByArtistsLastNameContainingIgnoreCase(artistName);
        if (arts.isEmpty()) LOGGER.warn("No artworks found for artist: {}", artistName);
        return arts;
    }

    @Transactional
    public Art addArt(ArtDTO artDTO) {
        if (artDTO == null) throw new IllegalArgumentException("ArtDTO cannot be null");
        Art art = new Art();
        art.setTitle(artDTO.getTitle());
        art.setYear(artDTO.getYear());
        if (artDTO.getClassification() != null) {
            Classification classification = processClassification(artDTO.getClassification());
            art.setClassification(classification);
            cacheService.getClassificationCache().update(classification.getId(), classification);
        }
        if (artDTO.getArtists() != null && !artDTO.getArtists().isEmpty()) {
            Set<Artist> artists = new HashSet<>();
            for (ArtistDTO artistDTO : artDTO.getArtists()) {
                Artist artist = processArtist(artistDTO);
                artists.add(artist);
                cacheService.getArtistCache().update(artist.getId(), artist);
            }
            art.setArtists(artists);
        }
        Art savedArt = artRepository.save(art);
        cacheService.getArtCache().put(savedArt.getId(), savedArt);
        return savedArt;
    }

    private Classification processClassification(ClassificationDTO classificationDTO) {
        if (classificationDTO == null) return null;
        Classification classification = null;
        if (classificationDTO.getId() != null) {
            classification = classificationRepository.findById(classificationDTO.getId()).orElse(null);
        }
        if (classification == null && classificationDTO.getName() != null) {
            classification = classificationRepository.findByName(classificationDTO.getName());
        }
        if (classification == null) {
            classification = new Classification();
            classification.setName(classificationDTO.getName());
            classification.setDescription(classificationDTO.getDescription());
            classification = classificationRepository.save(classification);
        }
        return classification;
    }

    private Artist processArtist(ArtistDTO artistDTO) {
        if (artistDTO.getLastName() == null || artistDTO.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Artist last name is required");
        }
        if (artistDTO.getId() != null) {
            return artistRepository.findById(artistDTO.getId())
                    .orElseThrow(() -> new NotFoundException(ART_NOT_FOUNDARTIST + artistDTO.getId()));
        } else {
            Artist artist = new Artist();
            artist.setFirstName(artistDTO.getFirstName());
            artist.setLastName(artistDTO.getLastName());
            artist.setMiddleName(artistDTO.getMiddleName());
            return artistRepository.save(artist);
        }
    }

    @Transactional
    public Art patchArt(int id, ArtPatchDTO artPatchDTO) {
        if (!artPatchDTO.hasUpdates()) throw new IllegalArgumentException("No fields to update");
        Art art = artRepository.findWithArtistsById(id)
                .orElseThrow(() -> new EntityNotFoundException(ART_NOT_FOUND + id));
        if (artPatchDTO.getTitle() != null) art.setTitle(artPatchDTO.getTitle());
        if (artPatchDTO.getYear() != null) art.setYear(artPatchDTO.getYear());
        if (artPatchDTO.getClassificationId() != 0) {
            Classification classification = classificationRepository.findById(artPatchDTO.getClassificationId());
            if (classification == null) throw new EntityNotFoundException("Classification not found");
            art.setClassification(classification);
            cacheService.getClassificationCache().update(classification.getId(), classification);
        }
        if (artPatchDTO.getArtistIds() != null) {
            updateArtists(art, artPatchDTO.getArtistIds());
        }
        Art updatedArt = artRepository.save(art);
        cacheService.getArtCache().update(id, updatedArt);
        art.getArtists().forEach(artist -> cacheService.getArtistCache().update(artist.getId(), artist));
        return updatedArt;
    }

    private void updateArtists(Art art, Set<Integer> newArtistIds) {
        art.getArtists().clear();
        if (!newArtistIds.isEmpty()) {
            Set<Artist> artists = (Set<Artist>) artistRepository.findAllById(newArtistIds);
            if (artists.size() != newArtistIds.size()) {
                throw new EntityNotFoundException("Some artists not found");
            }
            art.setArtists(artists);
        }
    }

    @Transactional(readOnly = true)
    public List<Art> getAllArts() {
        return artRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Art getArtById(int id) {
        return cacheService.getArtCache().get(id)
                .orElseGet(() -> {
                    Art art = artRepository.findById(id)
                            .orElseThrow(() -> new NotFoundException(String.format(ART_NOT_FOUND, id)));
                    cacheService.getArtCache().put(id, art);
                    return art;
                });
    }

    @Transactional
    public Art updateArt(int id, ArtDTO artDTO) {
        Art art = artRepository.findWithArtistsById(id)
                .orElseThrow(() -> new NotFoundException(ART_NOT_FOUND + id));
        art.setTitle(artDTO.getTitle());
        art.setYear(artDTO.getYear());
        if (artDTO.getArtists() != null) {
            art.getArtists().forEach(artist -> artist.getArts().remove(art));
            art.getArtists().clear();
            Set<Artist> updatedArtists = new HashSet<>();
            for (ArtistDTO artistDTO : artDTO.getArtists()) {
                Artist artist;
                if (artistDTO.getId() != null) {
                    artist = artistRepository.findById(artistDTO.getId())
                            .orElseThrow(() -> new NotFoundException(ART_NOT_FOUNDARTIST + artistDTO.getId()));
                } else {
                    artist = new Artist();
                    artist.setFirstName(artistDTO.getFirstName());
                    artist.setMiddleName(artistDTO.getMiddleName());
                    artist.setLastName(artistDTO.getLastName());
                    artist = artistRepository.save(artist);
                }
                updatedArtists.add(artist);
                artist.getArts().add(art);
                cacheService.getArtistCache().update(artist.getId(), artist);
            }
            art.setArtists(updatedArtists);
        }
        Art updatedArt = artRepository.save(art);
        cacheService.getArtCache().update(id, updatedArt);
        return updatedArt;
    }

    @Transactional(readOnly = true)
    public List<Art> getArtsByClassificationId(Integer classificationId) {
        List<Art> arts = artRepository.findByClassificationId(classificationId);
        if (arts.isEmpty()) {
            System.out.println("No artworks found for classification ID: " + classificationId);
        } else {
            arts.forEach(a -> cacheService.getArtCache().put(a.getId(), a));
        }
        return arts;
    }

    @Transactional(readOnly = true)
    public List<Art> getArtsByClassificationName(String classificationName) {
        List<Art> arts = artRepository.findByClassificationNameContainingIgnoreCase(classificationName);
        if (arts.isEmpty()) {
            System.out.println("No artworks found for classification name containing: " + classificationName);
        } else {
            arts.forEach(a -> cacheService.getArtCache().put(a.getId(), a));
        }
        return arts;
    }

    @Transactional
    public void deleteArtById(int id) {
        Art art = artRepository.findWithArtistsById(id)
                .orElseThrow(() -> new NotFoundException(ART_NOT_FOUND + id));
        for (Artist artist : art.getArtists()) {
            artist.getArts().remove(art);
            cacheService.getArtistCache().update(artist.getId(), artist);
        }
        art.getArtists().clear();
        artRepository.delete(art);
        cacheService.getArtCache().evict(id);
    }

    public Art getArtByTitle(String title) {
        return artRepository.findByTitle(title)
                .orElseThrow(() -> new NotFoundException(ART_NOT_FOUNDSTRING + title));
    }

    public String getCacheInfo() {
        return cacheService.getArtCache().getCacheInfo();
    }

    public EntityCache<Art> getArtCache() {
        return cacheService.getArtCache();
    }
}