package com.example.artshop.service;

import com.example.artshop.exception.ArtNotFoundException;
import com.example.artshop.model.Art;
import com.example.artshop.repository.ArtRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArtService {
    @Autowired
    private ArtRepository artRepository;
    public static final String ART_NOT_FOUND = "Art with id %d not found";
    public static final String ART_NOT_FOUNDSTRING = "Art with title %s not found";

    public Art addArt(Art art) {
        return artRepository.save(art);
    }

    public List<Art> getAllArts() {
        return artRepository.findAll();
    }

    public Art getArtById(int id) {
        return artRepository.findById(id)
                .orElseThrow(() -> new ArtNotFoundException(String.format(ART_NOT_FOUND, id)));
    }

    public void deleteArtById(int id) {
        if (!artRepository.existsById(id)) {
            throw new ArtNotFoundException(String.format(ART_NOT_FOUND, id));
        }
        artRepository.deleteById(id);
    }

    public Art updateArt(Art art) {
        if (!artRepository.existsById(art.getId())) {
            throw new ArtNotFoundException(String.format(ART_NOT_FOUND, art.getId()));
        }
        return artRepository.save(art);
    }

    public Art getArtByTitle(String title) {
        return artRepository.findByTitle(title)
                .orElseThrow(() -> new ArtNotFoundException(String.format(ART_NOT_FOUNDSTRING, title)));
    }
}