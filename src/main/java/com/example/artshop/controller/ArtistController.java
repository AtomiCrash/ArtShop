package com.example.artshop.controller;

import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.model.Artist;
import com.example.artshop.service.ArtistService;
import com.example.artshop.service.ArtistServiceInterface;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/artist")
public class ArtistController {
    private final ArtistServiceInterface artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Artist>> getAllArtists() {
        return ResponseEntity.ok(artistService.getAllArtists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtistById(@PathVariable Integer id) {
        Optional<Artist> artist = artistService.getArtistById(id);
        return artist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-atr")
    public ResponseEntity<?> getArtistsByArtTitle(@RequestParam String artTitle) {
        List<Artist> artists = artistService.getArtistsByArtTitle(artTitle);
        if (artists.isEmpty()) {
            return ResponseEntity.ok("No artists found for artwork title: " + artTitle);
        }
        return ResponseEntity.ok(artists);
    }

    @GetMapping("/name")
    public ResponseEntity<List<Artist>> searchArtists(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        List<Artist> artists = artistService.searchArtists(firstName, lastName);
        return ResponseEntity.ok(artists);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Artist> updateArtist(
            @PathVariable Integer id,
            @RequestBody ArtistDTO artistDTO) {
        Artist updatedArtist = artistService.updateArtist(id, artistDTO);
        return ResponseEntity.ok(updatedArtist);
    }

    @PostMapping("/add")
    public ResponseEntity<Artist> createArtist(@RequestBody ArtistDTO artistDTO) {
        Artist createdArtist = artistService.createArtist(artistDTO);
        return ResponseEntity.ok(createdArtist);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Integer id) {
        artistService.deleteArtist(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Artist> patchArtist(
            @PathVariable Integer id,
            @RequestBody ArtistPatchDTO artistPatchDTO) {
        Artist patchedArtist = artistService.patchArtist(id, artistPatchDTO);
        return ResponseEntity.ok(patchedArtist);
    }

    @GetMapping("/cache-info")
    public ResponseEntity<String> getArtistCacheInfo() {
        return ResponseEntity.ok(artistService.getCacheInfo());
    }
}