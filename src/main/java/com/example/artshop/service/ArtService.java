package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
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
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Transactional
    public List<Art> addBulkArts(List<ArtDTO> artDTOs) {
        if (artDTOs == null || artDTOs.isEmpty()) {
            throw new ValidationException("Art list cannot be null or empty");
        }
        if (artDTOs.size() > ApplicationConstants.MAX_BULK_OPERATION_SIZE) {
            throw new ValidationException("Cannot add more than " +
                    ApplicationConstants.MAX_BULK_OPERATION_SIZE + " artworks at once");
        }

        return artDTOs.stream()
                .peek(dto -> {
                    if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                        throw new ValidationException("Art title is required for all items");
                    }
                })
                .map(this::addSingleArt)
                .collect(Collectors.toList());
    }

    private Art addSingleArt(ArtDTO artDTO) {
        Art art = new Art();
        art.setTitle(artDTO.getTitle());
        art.setYear(artDTO.getYear());

        if (artDTO.getClassification() != null) {
            Classification classification = processClassification(artDTO.getClassification());
            art.setClassification(classification);
        }

        if (artDTO.getArtists() != null && !artDTO.getArtists().isEmpty()) {
            Set<Artist> artists = artDTO.getArtists().stream()
                    .map(this::processArtist)
                    .collect(Collectors.toSet());
            art.setArtists(artists);
        }

        return artRepository.save(art);
    }

    @Transactional(readOnly = true)
    public List<Art> getArtsByArtistName(String artistName) {
        List<Art> arts = artRepository.findByArtistsLastNameContainingIgnoreCase(artistName);
        if (arts.isEmpty()) LOGGER.warn("No artworks found for artist: {}", artistName);
        return arts;
    }

    @Transactional
    public Art addArt(ArtDTO artDTO) {
        if (artDTO == null) {
            throw new ValidationException("Art data cannot be null");
        }
        if (artDTO.getTitle() == null || artDTO.getTitle().trim().isEmpty()) {
            throw new ValidationException("Art title is required");
        }
        if (artDTO.getYear() != null) {
            int currentYear = LocalDate.now().getYear();
            if (artDTO.getYear() > currentYear) {
                throw new ValidationException("Year cannot be in the future");
            }
        }
        if (artDTO.getClassification() != null) {
            validateClassification(artDTO.getClassification());
        }
        if (artDTO.getArtists() != null && !artDTO.getArtists().isEmpty()) {
            validateArtists(artDTO.getArtists());
        }

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

    private void validateClassification(ClassificationDTO classification) {
        if (classification.getName() == null || classification.getName().trim().isEmpty()) {
            throw new ValidationException("Classification name is required");
        }
        if (classification.getDescription() == null || classification.getDescription().trim().isEmpty()) {
            throw new ValidationException("Classification description is required");
        }
    }

    private void validateArtists(List<ArtistDTO> artists) {
        for (ArtistDTO artist : artists) {
            if ((artist.getFirstName() == null || artist.getFirstName().trim().isEmpty()) &&
                    (artist.getLastName() == null || artist.getLastName().trim().isEmpty())) {
                throw new ValidationException("Artist must have at least first name or last name");
            }
            if (artist.getFirstName() != null && artist.getFirstName().length() > 60) {
                throw new ValidationException("First name must be 60 characters or less");
            }
            if (artist.getMiddleName() != null && artist.getMiddleName().length() > 60) {
                throw new ValidationException("Middle name must be 60 characters or less");
            }
            if (artist.getLastName() != null && artist.getLastName().length() > 60) {
                throw new ValidationException("Last name must be 60 characters or less");
            }
        }
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
        if (artPatchDTO == null) {
            throw new ValidationException("ArtPatchDTO cannot be null");
        }
        if (!artPatchDTO.hasUpdates()) {
            throw new ValidationException("No fields to update");
        }
        if (artPatchDTO.getTitle() != null && artPatchDTO.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }
        if (artPatchDTO.getYear() != null && artPatchDTO.getYear() < 1000) {
            throw new ValidationException("Year must be greater than 1000");
        }

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