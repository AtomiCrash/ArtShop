package com.example.artshop.service;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ArtServiceInterfaceTest {

    private ArtServiceInterface artServiceInterface;

    @Test
    void interfaceShouldContainRequiredMethods() {
        assertDoesNotThrow(() -> {
            artServiceInterface.getArtsByArtistName("");
            artServiceInterface.addArt(new ArtDTO());
            artServiceInterface.patchArt(1, new ArtPatchDTO());
            artServiceInterface.getAllArts();
            artServiceInterface.getArtById(1);
            artServiceInterface.updateArt(1, new ArtDTO());
            artServiceInterface.getArtsByClassificationId(1);
            artServiceInterface.getArtsByClassificationName("");
            artServiceInterface.deleteArtById(1);
            artServiceInterface.getArtByTitle("");
            artServiceInterface.getCacheInfo();
            artServiceInterface.getArtCache();
            artServiceInterface.addBulkArts(List.of(new ArtDTO()));
        });
    }
}