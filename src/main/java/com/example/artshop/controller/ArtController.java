package com.example.artshop.controller;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtRequest;
import com.example.artshop.model.Art;
import com.example.artshop.repository.ArtRepository;
import com.example.artshop.service.ArtService;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping("/arts")
public class ArtController {
    @Autowired
    private ArtService artService;

    @PostMapping("/api/art/add")
    public Art addArt(@RequestBody ArtDTO artDTO) {
        return artService.addArt(artDTO);
    }

    @GetMapping
    public List<Art> getAllArts() {
        return artService.getAllArts();
    }

    @GetMapping("/{id}")
    public Art getArtById(@PathVariable int id) {
        return artService.getArtById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteArtById(@PathVariable int id) {
        artService.deleteArtById(id);
    }

    @PutMapping("/{id}")
    public Art updateArt(@PathVariable int id, @RequestBody ArtRequest artRequest) {
        return artService.updateArt(id, artRequest.getArt(), artRequest.getArtistIds());
    }

    @PostMapping("/{artId}/artists/{artistId}")
    public Art addArtistToArt(@PathVariable int artId, @PathVariable int artistId) {
        return artService.addArtistToArt(artId, artistId);
    }

    @DeleteMapping("/{artId}/artists/{artistId}")
    public Art removeArtistFromArt(@PathVariable int artId, @PathVariable int artistId) {
        return artService.removeArtistFromArt(artId, artistId);
    }
}