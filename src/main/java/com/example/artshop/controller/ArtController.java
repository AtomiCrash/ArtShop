package com.example.artshop.controller;

import com.example.artshop.model.Art;
import com.example.artshop.service.ArtService;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/art")
public class ArtController {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ArtController.class);

    @Autowired
    private ArtService artService;

    @PostMapping("/add")
    public void addArt(@RequestBody Art art) {
        LOG.info("New row: {}", artService.addArt(art));
    }

    @GetMapping("/all")
    public List<Art> getAllArts() {
        return artService.getAllArts();
    }

    @GetMapping("/")
    public Art getArtById(@RequestParam String title) {
        return artService.getArtByTitle(title);
    }

    @GetMapping("/{id}")
    public Art getArtById(@PathVariable int id) {
        return artService.getArtById(id);
    }

    @DeleteMapping("/id")
    public void deleteArtById(@RequestParam int id) {
        artService.deleteArtById(id);
    }

    @PutMapping("/artput")
    public Art changeArt(@RequestBody final Art art) {
        return artService.updateArt(art);
    }
}
