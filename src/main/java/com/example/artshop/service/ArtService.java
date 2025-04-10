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
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtService {
    private final ArtRepository artRepository;
    private final ArtistRepository artistRepository;
    private final EntityCache<Art> artCache;
    private final ClassificationRepository classificationRepository;

    public static final String ART_NOT_FOUND = "Art with id %d not found";
    public static final String ART_NOT_FOUNDSTRING = "Art with title %s not found";

    @Autowired
    public ArtService(ArtRepository artRepository, ArtistRepository artistRepository,
                      ClassificationRepository classificationRepository) {
        this.artRepository = artRepository;
        this.artistRepository = artistRepository;
        this.classificationRepository = classificationRepository;
        this.artCache = new EntityCache<>("Art");
    }

    @Transactional(readOnly = true)
    public List<Art> getArtsByArtistName(String artistName) {
        List<Art> arts = artRepository.findByArtistsLastNameContainingIgnoreCase(artistName);
        if (arts.isEmpty()) {
            System.out.println("No artworks found for artist: " + artistName);
        }
        return arts;
    }

    @Transactional
    public Art addArt(ArtDTO artDTO) {
        if (artDTO == null) {
            throw new IllegalArgumentException("ArtDTO cannot be null");
        }

        Art art = new Art();
        art.setTitle(artDTO.getTitle());
        art.setYear(artDTO.getYear());

        if (artDTO.getClassification() != null) {
            Classification classification = processClassification(artDTO.getClassification());
            art.setClassification(classification);
        }

        if (artDTO.getArtists() != null && !artDTO.getArtists().isEmpty()) {
            Set<Artist> artists = new HashSet<>();
            for (ArtistDTO artistDTO : artDTO.getArtists()) {
                Artist artist = processArtist(artistDTO);
                artists.add(artist);
            }
            art.setArtists(artists);
        }

        Art savedArt = artRepository.save(art);
        artCache.put(savedArt.getId(), savedArt);
        return savedArt;
    }

    private Classification processClassification(ClassificationDTO classificationDTO) {
        if (classificationDTO == null) {
            return null;
        }

        Classification classification = null;

        if (classificationDTO.getId() != null) {
            classification = classificationRepository.findById(classificationDTO.getId())
                    .orElse(null);
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
                    .orElseThrow(() -> new NotFoundException("Artist not found with id: " + artistDTO.getId()));
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
        if (!artPatchDTO.hasUpdates()) {
            throw new IllegalArgumentException("No fields to update");
        }

        Art art = artRepository.findWithArtistsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Art not found with id: " + id));

        if (artPatchDTO.getTitle() != null) {
            art.setTitle(artPatchDTO.getTitle());
        }

        if (artPatchDTO.getYear() != null) {
            art.setYear(artPatchDTO.getYear());
        }

        if (artPatchDTO.getClassificationId() != 0) {
            Classification classification = classificationRepository.findById(artPatchDTO.getClassificationId());
            if (classification == null) {
                throw new EntityNotFoundException("Classification not found");
            }
            art.setClassification(classification);
        }

        if (artPatchDTO.getArtistIds() != null) {
            updateArtists(art, artPatchDTO.getArtistIds());
        }

        Art updatedArt = artRepository.save(art);
        artCache.update(id, updatedArt);
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
        return artCache.get(id)
                .orElseGet(() -> {
                    Art art = artRepository.findById(id)
                            .orElseThrow(() -> new NotFoundException(String.format(ART_NOT_FOUND, id)));
                    artCache.put(id, art);
                    return art;
                });
    }

    @Transactional
    public Art updateArt(int id, ArtDTO artDTO) {
        Art art = artRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Art not found with id: " + id));

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
                            .orElseThrow(() -> new NotFoundException(
                                    "Artist not found with id: " + artistDTO.getId()));
                } else {
                    artist = new Artist();
                    artist.setFirstName(artistDTO.getFirstName());
                    artist.setMiddleName(artistDTO.getMiddleName());
                    artist.setLastName(artistDTO.getLastName());
                    artist = artistRepository.save(artist);
                }

                updatedArtists.add(artist);
                artist.getArts().add(art);
            }

            art.setArtists(updatedArtists);
        }

        Art updatedArt = artRepository.save(art);
        artCache.update(id, updatedArt);
        return updatedArt;
    }

    @Transactional
    public void deleteArtById(int id) {
        Art art = artRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Art not found with id: " + id));

        for (Artist artist : art.getArtists()) {
            artist.getArts().remove(art);
        }
        art.getArtists().clear();

        artRepository.delete(art);
        artCache.evict(id);
    }

    public Art getArtByTitle(String title) {
        return artRepository.findByTitle(title)
                .orElseThrow(() -> new NotFoundException(ART_NOT_FOUNDSTRING + title));
    }

    public ClassificationRepository getСlassificationRepository() {
        return classificationRepository;
    }

    public String getCacheInfo() {
        return artCache.getCacheInfo();
    }
}