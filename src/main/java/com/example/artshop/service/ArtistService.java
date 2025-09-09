package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.service.cache.EntityCache;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
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
    public List<ArtistDTO> addBulkArtists(List<ArtistDTO> artistDTOs) {
        if (artistDTOs == null || artistDTOs.isEmpty()) {
            throw new ValidationException("Artist list cannot be null or empty");
        }
        if (artistDTOs.size() > ApplicationConstants.MAX_BULK_OPERATION_SIZE) {
            throw new ValidationException("Cannot add more than " +
                    ApplicationConstants.MAX_BULK_OPERATION_SIZE + " artists at once");
        }
        return artistDTOs.stream()
                .peek(dto -> {
                    if ((dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) &&
                            (dto.getLastName() == null || dto.getLastName().trim().isEmpty())) {
                        throw new ValidationException("Artist must have at least first name or last name");
                    }
                })
                .map(this::createArtist)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ArtistDTO> getArtistsByArtTitle(String artTitle) {
        List<Artist> artists = artistRepository.findByArtTitleContaining(artTitle);
        artists.forEach(artist -> cacheService.getArtistCache().put(artist.getId(), artist));
        if (artists.isEmpty()) LOGGER.warn("No artists found for artwork title: {}", artTitle);
        return artists.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public ArtistDTO createArtist(ArtistDTO artistDTO) {
        if (artistDTO == null) {
            throw new ValidationException("Artist data cannot be null");
        }
        if ((artistDTO.getFirstName() == null || artistDTO.getFirstName().trim().isEmpty()) &&
                (artistDTO.getLastName() == null || artistDTO.getLastName().trim().isEmpty())) {
            throw new ValidationException("Artist must have at least first name or last name");
        }
        if (artistDTO.getFirstName() != null && artistDTO.getFirstName().length() > 60) {
            throw new ValidationException("First name must be 60 characters or less");
        }
        if (artistDTO.getMiddleName() != null && artistDTO.getMiddleName().length() > 60) {
            throw new ValidationException("Middle name must be 60 characters or less");
        }
        if (artistDTO.getLastName() != null && artistDTO.getLastName().length() > 60) {
            throw new ValidationException("Last name must be 60 characters or less");
        }

        Artist artist = new Artist();
        artist.setFirstName(artistDTO.getFirstName());
        artist.setMiddleName(artistDTO.getMiddleName());
        artist.setLastName(artistDTO.getLastName());
        Artist savedArtist = artistRepository.save(artist);
        cacheService.getArtistCache().put(savedArtist.getId(), savedArtist);
        return convertToDTO(savedArtist);
    }

    @Transactional
    public List<ArtistDTO> getAllArtists() {
        List<Artist> artists = artistRepository.findAllWithArts();
        LOGGER.debug("Artists loaded: {}", artists.size());
        artists.forEach(artist -> {
            LOGGER.debug("Artist {} has {} arts", artist.getId(), (artist.getArts() != null ? artist.getArts().size() : "null"));
            if (artist.getArts() != null) {
                artist.getArts().forEach(art -> {
                    LOGGER.debug("Art: {}", art.getTitle());
                    if (Hibernate.isInitialized(art.getClassification()) && art.getClassification() != null) {
                        LOGGER.debug("Classification: {}", art.getClassification().getName());
                    } else {
                        Hibernate.initialize(art.getClassification());
                    }
                });
            }
        });
        artists.forEach(artist -> cacheService.getArtistCache().put(artist.getId(), artist));
        return artists.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public Optional<ArtistDTO> getArtistById(Integer id) {
        return cacheService.getArtistCache().get(id)
                .map(this::convertToDTO)
                .or(() -> {
                    Optional<Artist> artist = artistRepository.findById(id);
                    artist.ifPresent(a -> cacheService.getArtistCache().put(a.getId(), a));
                    return artist.map(this::convertToDTO);
                });
    }

    @Transactional
    public ArtistDTO updateArtist(Integer id, ArtistDTO artistDTO) {
        Artist artist = artistRepository.findWithArtsById(id)
                .orElseThrow(() -> new NotFoundException(ARTIST_NOT_FOUND + id));
        artist.setFirstName(artistDTO.getFirstName());
        artist.setMiddleName(artistDTO.getMiddleName());
        artist.setLastName(artistDTO.getLastName());
        Artist updatedArtist = artistRepository.save(artist);
        cacheService.getArtistCache().update(id, updatedArtist);
        artist.getArts().forEach(art -> cacheService.getArtCache().update(art.getId(), art));
        return convertToDTO(updatedArtist);
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
    public List<ArtistDTO> searchArtists(String firstName, String lastName) {
        List<Artist> artists;
        if (firstName != null && lastName != null) {
            artists = artistRepository
                    .findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(firstName, lastName);
        } else if (firstName != null) {
            artists = artistRepository.findByFirstNameContainingIgnoreCase(firstName);
        } else if (lastName != null) {
            artists = artistRepository.findByLastNameContainingIgnoreCase(lastName);
        } else {
            artists = Collections.emptyList();
        }
        artists.forEach(artist -> cacheService.getArtistCache().put(artist.getId(), artist));
        return artists.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public ArtistDTO patchArtist(Integer id, ArtistPatchDTO artistPatchDTO) {
        if (!artistPatchDTO.hasUpdates()) throw new IllegalArgumentException("No fields to update");
        Artist artist = artistRepository.findWithArtsById(id)
                .orElseThrow(() -> new NotFoundException(ARTIST_NOT_FOUND + id));
        if (artistPatchDTO.getFirstName() != null) artist.setFirstName(artistPatchDTO.getFirstName());
        if (artistPatchDTO.getMiddleName() != null) artist.setMiddleName(artistPatchDTO.getMiddleName());
        if (artistPatchDTO.getLastName() != null) artist.setLastName(artistPatchDTO.getLastName());
        Artist patchedArtist = artistRepository.save(artist);
        cacheService.getArtistCache().update(id, patchedArtist);
        artist.getArts().forEach(art -> cacheService.getArtCache().update(art.getId(), art));
        return convertToDTO(patchedArtist);
    }

    public String getCacheInfo() {
        return cacheService.getArtistCache().getCacheInfo();
    }

    public EntityCache<Artist> getArtistCache() {
        return cacheService.getArtistCache();
    }

    private ArtistDTO convertToDTO(Artist artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setFirstName(artist.getFirstName());
        dto.setMiddleName(artist.getMiddleName());
        dto.setLastName(artist.getLastName());
        return dto;
    }
}