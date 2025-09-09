package com.example.artshop.service;

import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.model.Artist;
import com.example.artshop.service.cache.EntityCache;
import java.util.List;
import java.util.Optional;

public interface ArtistServiceInterface {
    List<ArtistDTO> getArtistsByArtTitle(String artTitle);

    ArtistDTO createArtist(ArtistDTO artistDTO);

    List<ArtistDTO> getAllArtists();

    Optional<ArtistDTO> getArtistById(Integer id);

    ArtistDTO updateArtist(Integer id, ArtistDTO artistDTO);

    void deleteArtist(Integer id);

    List<ArtistDTO> searchArtists(String firstName, String lastName);

    ArtistDTO patchArtist(Integer id, ArtistPatchDTO artistPatchDTO);

    String getCacheInfo();

    EntityCache<Artist> getArtistCache();

    List<ArtistDTO> addBulkArtists(List<ArtistDTO> artistDTOs);
}