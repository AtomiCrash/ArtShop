package com.example.artshop.controller;

import com.example.artshop.model.Art;
import com.example.artshop.repository.ArtRepository;
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
    public ArtRepository artRepository;

    @PostMapping("/api/art/add")
    public void addart(@RequestBody Art art) {
        LOG.info("New row: {}", artRepository.save(art));
    }

    @GetMapping("/api/art/all")
    public String getallarts() {
        List<Art> arts = artRepository.findAll();
        return arts.toString();
    }

    @GetMapping("/api/art/id")
    public String getartbyid(@RequestParam int id) {
        return artRepository.findById(id).toString();
    }

    @DeleteMapping("/api/artdel/id")
    public void deleteartbyid(@RequestParam int id) {
        artRepository.deleteById(id);
    }

    @PutMapping("/api/artput")
    public String changeart(@RequestBody final Art art) {
        if (!artRepository.existsById(art.getId())) {
            return "Not Found";
        }
        return artRepository.save(art).toString();
    }
}
