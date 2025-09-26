package com.example.artshop.service;

import com.example.artshop.constants.ApplicationConstants;
import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.exception.NotFoundException;
import com.example.artshop.exception.ValidationException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtRepository;
import com.example.artshop.repository.ArtistRepository;
import com.example.artshop.service.cache.EntityCache;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;
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
    private final ArtRepository artRepository;

    @Autowired
    public ArtistService(ArtistRepository artistRepository, CacheService cacheService, ArtRepository artRepository) {
        this.artistRepository = artistRepository;
        this.cacheService = cacheService;
        this.artRepository = artRepository;
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

        if (artistDTO.getFirstName() != null && artistDTO.getLastName() != null) {
            Optional<Artist> existingArtist = artistRepository.findFirstByFirstNameAndLastName(
                    artistDTO.getFirstName(),
                    artistDTO.getLastName()
            );

            if (existingArtist.isPresent()) {
                Artist artist = existingArtist.get();
                LOGGER.warn("Artist already exists with name: {} {} (ID: {})",
                        artist.getFirstName(), artist.getLastName(), artist.getId());
                return convertToDTO(artist); // Возвращаем существующего художника
            }
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

        LOGGER.info("Created new artist: {} {} (ID: {})",
                savedArtist.getFirstName(), savedArtist.getLastName(), savedArtist.getId());

        return convertToDTO(savedArtist);
    }

    @Transactional
    public List<ArtistDTO> getAllArtists() {
        List<Artist> artists = artistRepository.findAllWithArts();
        LOGGER.debug("Artists loaded: {}", artists.size());
        artists.forEach(artist -> cacheService.getArtistCache().put(artist.getId(), artist));
        return artists.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public Optional<ArtistDTO> getArtistById(Integer id) {
        return cacheService.getArtistCache().get(id)
                .map(this::convertToDTO)
                .or(() -> {
                    Optional<Artist> artist = artistRepository.findWithArtsById(id);
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
        try {
            Artist artist = artistRepository.findWithArtsById(id)
                    .orElseThrow(() -> new NotFoundException(ARTIST_NOT_FOUND + id));

            List<Art> artsCopy = new ArrayList<>(artist.getArts());

            for (Art art : artsCopy) {
                art.getArtists().remove(artist);
                artRepository.save(art);
                cacheService.getArtCache().update(art.getId(), art);
            }

            artist.getArts().clear();
            artistRepository.delete(artist);
            cacheService.getArtistCache().evict(id);

        } catch (Exception e) {
            LOGGER.error("Error deleting artist with id {}: {}", id, e.getMessage(), e);
            throw new ValidationException("Failed to delete artist: " + e.getMessage());
        }
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
        if (!artistPatchDTO.hasUpdates()) throw new ValidationException("No fields to update");
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

        if (artist.getArts() != null && !artist.getArts().isEmpty()) {
            List<String> titles = artist.getArts().stream()
                    .map(Art::getTitle)
                    .collect(Collectors.toList());
            dto.setArtworkTitles(titles);
            dto.setArtworkCount(titles.size());
        } else {
            dto.setArtworkCount(0);
        }

        return dto;
    }
}