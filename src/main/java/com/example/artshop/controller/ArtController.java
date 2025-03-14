package com.example.artshop.controller;

import com.example.artshop.model.Art;
import com.example.artshop.repository.ArtRepository;
import com.example.artshop.service.ArtService;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArtController {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ArtController.class);

    @Autowired
    private ArtService artService;

    @PostMapping("/api/art/add")
    public void addArt(@RequestBody Art art) {
        LOG.info("New row: {}", artService.addArt(art));
    }

    @GetMapping("/api/art/all")
    public String getAllArts() {
        List<Art> arts = artService.getAllArts();
        return arts.toString();
    }

    @GetMapping("/api/art/id")
    public String getArtById(@RequestParam int id) {
        Art art = artService.getArtById(id);
        return art.toString();
    }

    @DeleteMapping("/api/art/id")
    public void deleteArtById(@RequestParam int id) {
        artService.deleteArtById(id);
    }

    @PutMapping("/api/artput")
    public String changeArt(@RequestBody final Art art) {
        Art updatedArt = artService.updateArt(art);
        return "Updated: " + updatedArt.toString();
    }
}
