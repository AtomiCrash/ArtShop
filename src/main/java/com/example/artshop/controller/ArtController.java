package com.example.artshop.controller;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.model.Art;
import com.example.artshop.service.ArtService;
import com.example.artshop.service.ArtServiceInterface;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/art")
@Tag(name = "Art Management", description = "Operations related to artworks")
public class ArtController {
    private final ArtServiceInterface artService;

    @Autowired
    public ArtController(ArtService artService) {
        this.artService = artService;
    }

    @Operation(summary = "Get all artworks", description = "Returns a list of all artworks")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping("/all")
    public ResponseEntity<List<Art>> getAllArts() {
        List<Art> arts = artService.getAllArts();
        return ResponseEntity.ok(arts);
    }

    @Operation(summary = "Get artwork by title", description = "Returns a single artwork by its title")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Artwork found"),
            @ApiResponse(responseCode = "404", description = "Artwork not found")
    })
    @GetMapping("/title")
    public ResponseEntity<Art> getArtByTitle(@Parameter(description = "Title of the artwork to be retrieved")
                                                 @RequestParam String title) {
        Art art = artService.getArtByTitle(title);
        return ResponseEntity.ok(art);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Art> getArtById(@PathVariable int id) {
        Art art = artService.getArtById(id);
        return ResponseEntity.ok(art);
    }

    @GetMapping("/by-artist")
    public ResponseEntity<?> getArtsByArtistName(@RequestParam String artistName) {
        List<Art> arts = artService.getArtsByArtistName(artistName);
        if (arts.isEmpty()) {
            return ResponseEntity.ok("No artworks found for artist: " + artistName);
        }
        return ResponseEntity.ok(arts);
    }

    @GetMapping("/by-classificationid")
    public ResponseEntity<?> getArtsByClassificationId(@RequestParam Integer id) {
        List<Art> arts = artService.getArtsByClassificationId(id);
        if (arts.isEmpty()) {
            return ResponseEntity.ok("No artworks found for classification ID: " + id);
        }
        return ResponseEntity.ok(arts);
    }

    @GetMapping("/by-classification")
    public ResponseEntity<?> getArtsByClassificationName(@RequestParam String name) {
        List<Art> arts = artService.getArtsByClassificationName(name);
        if (arts.isEmpty()) {
            return ResponseEntity.ok("No artworks found for classification name containing: " + name);
        }
        return ResponseEntity.ok(arts);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ArtDTO> patchArt(
            @PathVariable int id,
            @RequestBody ArtPatchDTO artPatchDTO) {
        Art updatedArt = artService.patchArt(id, artPatchDTO);
        ArtDTO artDTO = convertToDto(updatedArt);
        return ResponseEntity.ok(artDTO);
    }

    private ArtDTO convertToDto(Art art) {
        ArtDTO dto = new ArtDTO();
        dto.setTitle(art.getTitle());
        dto.setYear(art.getYear());

        if (art.getClassification() != null) {
            dto.setClassificationId(art.getClassification().getId());
            dto.setClassificationName(art.getClassification().getName());
        }

        return dto;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Art> updateArt(
            @PathVariable int id,
            @RequestBody ArtDTO artDTO) {
        Art updatedArt = artService.updateArt(id, artDTO);
        return ResponseEntity.ok(updatedArt);
    }

    @Operation(summary = "Add new artwork", description = "Creates a new artwork")
    @ApiResponse(responseCode = "200", description = "Artwork created successfully")
    @PostMapping("/add")
    public ResponseEntity<Art> addArt(@RequestBody @Valid
                                          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                  description = "Artwork object to be added",
                                                  required = true)
                                          ArtDTO artDTO) {
        Art savedArt = artService.addArt(artDTO);
        return ResponseEntity.ok(savedArt);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtById(@PathVariable int id) {
        artService.deleteArtById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cache-info")
    public ResponseEntity<String> getArtCacheInfo() {
        return ResponseEntity.ok(artService.getCacheInfo());
    }
}