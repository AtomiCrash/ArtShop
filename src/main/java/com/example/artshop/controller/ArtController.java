package com.example.artshop.controller;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.dto.ArtPatchDTO;
import com.example.artshop.service.ArtService;
import com.example.artshop.service.ArtServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/art")
@Tag(name = "Art Management", description = "Operations related to artworks")
public class ArtController {
    private final ArtServiceInterface artService;

    @Autowired
    public ArtController(ArtService artService) {
        this.artService = artService;
    }

    @Operation(summary = "Get all artworks", description = "Returns a list of all artworks")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtDTO.class))))
    @GetMapping("/all")
    public ResponseEntity<List<ArtDTO>> getAllArts() {
        List<ArtDTO> arts = artService.getAllArts();
        return ResponseEntity.ok(arts);
    }

    @Operation(summary = "Add multiple artworks",
            description = "Creates multiple artworks in one request (max 10 items)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Artworks created successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input (empty list or more than 10 items)")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<ArtDTO>> addBulkArts(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "List of exactly 3 ArtDTO objects",
                    required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtDTO.class))))
            @RequestBody List<ArtDTO> artDTOs) {
        List<ArtDTO> createdArts = artService.addBulkArts(artDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdArts);
    }

    @Operation(summary = "Get artwork by title", description = "Returns a single artwork by its title")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Artwork found",
                    content = @Content(schema = @Schema(implementation = ArtDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artwork not found")
    })
    @GetMapping("/title")
    public ResponseEntity<ArtDTO> getArtByTitle(
            @Parameter(description = "Title of the artwork to be retrieved", required = true)
            @RequestParam String title) {
        ArtDTO art = artService.getArtByTitle(title);
        return ResponseEntity.ok(art);
    }

    @Operation(summary = "Get artwork by ID", description = "Returns a single artwork by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Artwork found",
                    content = @Content(schema = @Schema(implementation = ArtDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artwork not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ArtDTO> getArtById(
            @Parameter(description = "ID of the artwork to be retrieved", required = true)
            @PathVariable int id) {
        ArtDTO art = artService.getArtById(id);
        return ResponseEntity.ok(art);
    }

    @Operation(summary = "Get artworks by artist name",
            description = "Returns artworks created by artist with specified name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artworks found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtDTO.class)))),
            @ApiResponse(responseCode = "200", description = "No artworks found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/by-artist")
    public ResponseEntity<?> getArtsByArtistName(
            @Parameter(description = "Name of the artist to search by", required = true)
            @RequestParam String artistName) {
        List<ArtDTO> arts = artService.getArtsByArtistName(artistName);
        if (arts.isEmpty()) {
            return ResponseEntity.ok("No artworks found for artist: " + artistName);
        }
        return ResponseEntity.ok(arts);
    }

    @Operation(summary = "Get artworks by classification ID",
            description = "Returns artworks with specified classification ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artworks found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtDTO.class)))),
            @ApiResponse(responseCode = "200", description = "No artworks found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/by-classificationid")
    public ResponseEntity<Object> getArtsByClassificationId(
            @Parameter(description = "ID of the classification to search by", required = true)
            @RequestParam Integer id) {
        List<ArtDTO> arts = artService.getArtsByClassificationId(id);
        if (arts.isEmpty()) {
            return ResponseEntity.ok("No artworks found for classification ID: " + id);
        }
        return ResponseEntity.ok(arts);
    }

    @Operation(summary = "Get artworks by classification name",
            description = "Returns artworks with classification containing specified name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artworks found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtDTO.class)))),
            @ApiResponse(responseCode = "200", description = "No artworks found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/by-classification")
    public ResponseEntity<?> getArtsByClassificationName(
            @Parameter(description = "Name of the classification to search by", required = true)
            @RequestParam String name) {
        List<ArtDTO> arts = artService.getArtsByClassificationName(name);
        if (arts.isEmpty()) {
            return ResponseEntity.ok("No artworks found for classification name containing: " + name);
        }
        return ResponseEntity.ok(arts);
    }

    @Operation(summary = "Partially update artwork",
            description = "Updates specific fields of an artwork")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artwork updated successfully",
                    content = @Content(schema = @Schema(implementation = ArtDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artwork not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ArtDTO> patchArt(
            @Parameter(description = "ID of the artwork to be updated", required = true)
            @PathVariable int id,
            @RequestBody ArtPatchDTO artPatchDTO) {
        ArtDTO updatedArt = artService.patchArt(id, artPatchDTO);
        return ResponseEntity.ok(updatedArt);
    }

    @Operation(summary = "Update artwork", description = "Updates existing artwork by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artwork updated successfully",
                    content = @Content(schema = @Schema(implementation = ArtDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artwork not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ArtDTO> updateArt(
            @Parameter(description = "ID of the artwork to be updated", required = true)
            @PathVariable int id,
            @RequestBody ArtDTO artDTO) {
        ArtDTO updatedArt = artService.updateArt(id, artDTO);
        return ResponseEntity.ok(updatedArt);
    }

    @Operation(summary = "Add new artwork", description = "Creates a new artwork")
    @ApiResponse(responseCode = "200", description = "Artwork created successfully",
            content = @Content(schema = @Schema(implementation = ArtDTO.class)))
    @PostMapping("/add")
    public ResponseEntity<ArtDTO> addArt(
            @RequestBody @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Artwork object to be added",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ArtDTO.class)))
            ArtDTO artDTO) {
        ArtDTO savedArt = artService.addArt(artDTO);
        return ResponseEntity.ok(savedArt);
    }

    @Operation(summary = "Delete artwork", description = "Deletes artwork by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Artwork deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Artwork not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtById(
            @Parameter(description = "ID of the artwork to be deleted", required = true)
            @PathVariable int id) {
        artService.deleteArtById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get cache info", description = "Returns artwork cache statistics")
    @ApiResponse(responseCode = "200", description = "Cache info retrieved",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping("/cache-info")
    public ResponseEntity<String> getArtCacheInfo() {
        return ResponseEntity.ok(artService.getCacheInfo());
    }
}