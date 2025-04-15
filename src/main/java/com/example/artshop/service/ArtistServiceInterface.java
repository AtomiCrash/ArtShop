package com.example.artshop.service;

import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.model.Artist;
import com.example.artshop.service.cache.EntityCache;
import java.util.List;
import java.util.Optional;

public interface ArtistServiceInterface {
    List<Artist> getArtistsByArtTitle(String artTitle);

    Artist createArtist(ArtistDTO artistDTO);

    List<Artist> getAllArtists();

    Optional<Artist> getArtistById(Integer id);

    Artist updateArtist(Integer id, ArtistDTO artistDTO);

    void deleteArtist(Integer id);

    List<Artist> searchArtists(String firstName, String lastName);

    Artist patchArtist(Integer id, ArtistPatchDTO artistPatchDTO);

    String getCacheInfo();

    EntityCache<Artist> getArtistCache();
}