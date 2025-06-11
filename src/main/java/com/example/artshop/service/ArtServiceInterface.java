package com.example.artshop.service;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.model.Art;
import com.example.artshop.service.cache.EntityCache;
import java.util.List;

public interface ArtServiceInterface {
    List<Art> getArtsByArtistName(String artistName);

    Art addArt(ArtDTO artDTO);

    Art patchArt(int id, ArtPatchDTO artPatchDTO);

    List<Art> getAllArts();

    ArtDTO getArtById(int id);

    Art updateArt(int id, ArtDTO artDTO);

    List<Art> getArtsByClassificationId(Integer classificationId);

    List<Art> getArtsByClassificationName(String classificationName);

    void deleteArtById(int id);

    Art getArtByTitle(String title);

    String getCacheInfo();

    EntityCache<Art> getArtCache();

    List<Art> addBulkArts(List<ArtDTO> artDTOs);
}