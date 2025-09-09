package com.example.artshop.service;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.model.Art;
import com.example.artshop.service.cache.EntityCache;
import java.util.List;

public interface ArtServiceInterface {
    List<ArtDTO> getArtsByArtistName(String artistName);

    ArtDTO addArt(ArtDTO artDTO);

    ArtDTO patchArt(int id, ArtPatchDTO artPatchDTO);

    List<ArtDTO> getAllArts();

    ArtDTO getArtById(int id);

    ArtDTO updateArt(int id, ArtDTO artDTO);

    List<ArtDTO> getArtsByClassificationId(Integer classificationId);

    List<ArtDTO> getArtsByClassificationName(String classificationName);

    void deleteArtById(int id);

    ArtDTO getArtByTitle(String title);

    String getCacheInfo();

    EntityCache<Art> getArtCache();

    List<ArtDTO> addBulkArts(List<ArtDTO> artDTOs);
}