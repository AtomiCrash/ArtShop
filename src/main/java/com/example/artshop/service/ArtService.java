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

    public Art addArt(Art art) {
        return artRepository.save(art);
    }

    public List<Art> getAllArts() {
        return artRepository.findAll();
    }

    public Art getArtById(int id) {
        return artRepository.findById(id)
                .orElseThrow(() -> new ArtNotFoundException("Art with id " + id + " not found"));
    }

    public void deleteArtById(int id) {
        if (!artRepository.existsById(id)) {
            throw new ArtNotFoundException("Art with id " + id + " not found");
        }
        artRepository.deleteById(id);
    }

    public Art updateArt(Art art) {
        if (!artRepository.existsById(art.getId())) {
            throw new ArtNotFoundException("Art with id " + art.getId() + " not found");
        }
        return artRepository.save(art);
    }
}