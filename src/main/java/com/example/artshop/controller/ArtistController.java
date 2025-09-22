package com.example.artshop.controller;

import com.example.artshop.dto.ArtistDTO;
import com.example.artshop.dto.ArtistPatchDTO;
import com.example.artshop.service.ArtistService;
import com.example.artshop.service.ArtistServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
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
@RequestMapping("/api/artist")
@Tag(name = "Artist Management", description = "Operations related to artists")
public class ArtistController {
    private final ArtistServiceInterface artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @Operation(summary = "Get all artists", description = "Returns list of all artists")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtistDTO.class))))
    @GetMapping("/all")
    public ResponseEntity<List<ArtistDTO>> getAllArtists() {
        return ResponseEntity.ok(artistService.getAllArtists());
    }

    @Operation(summary = "Get artist by ID", description = "Returns single artist by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artist found",
                    content = @Content(schema = @Schema(implementation = ArtistDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artist not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ArtistDTO> getArtistById(
            @Parameter(description = "ID of artist to be retrieved", required = true)
            @PathVariable Integer id) {
        Optional<ArtistDTO> artist = artistService.getArtistById(id);
        return artist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search artists by name",
            description = "Returns artists filtered by first and/or last name")
    @ApiResponse(responseCode = "200", description = "Artists found",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtistDTO.class))))
    @GetMapping("/name")
    public ResponseEntity<List<ArtistDTO>> searchArtists(
            @Parameter(description = "First name to search by (optional)")
            @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name to search by (optional)")
            @RequestParam(required = false) String lastName) {
        List<ArtistDTO> artists = artistService.searchArtists(firstName, lastName);
        return ResponseEntity.ok(artists);
    }

    @Operation(summary = "Add multiple artists",
            description = "Creates multiple artists in one request (max 10 items)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Artists created successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtistDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input (empty list or more than 10 items)")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<ArtistDTO>> addBulkArtists(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "List of ArtistDTO objects (max 10 items)",
                    required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArtistDTO.class))))
            @RequestBody List<ArtistDTO> artistDTOs) {
        List<ArtistDTO> createdArtists = artistService.addBulkArtists(artistDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdArtists);
    }

    @Operation(summary = "Update artist", description = "Updates existing artist by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artist updated successfully",
                    content = @Content(schema = @Schema(implementation = ArtistDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artist not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ArtistDTO> updateArtist(
            @Parameter(description = "ID of artist to be updated", required = true)
            @PathVariable Integer id,
            @RequestBody ArtistDTO artistDTO) {
        ArtistDTO updatedArtist = artistService.updateArtist(id, artistDTO);
        return ResponseEntity.ok(updatedArtist);
    }

    @Operation(summary = "Create artist", description = "Creates a new artist")
    @ApiResponse(responseCode = "200", description = "Artist created successfully",
            content = @Content(schema = @Schema(implementation = ArtistDTO.class)))
    @PostMapping("/add")
    public ResponseEntity<ArtistDTO> createArtist(
            @RequestBody ArtistDTO artistDTO) {
        ArtistDTO createdArtist = artistService.createArtist(artistDTO);
        return ResponseEntity.ok(createdArtist);
    }

    @Operation(summary = "Delete artist", description = "Deletes artist by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Artist deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Artist not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(
            @Parameter(description = "ID of artist to be deleted", required = true)
            @PathVariable Integer id) {
        artistService.deleteArtist(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Partially update artist",
            description = "Updates specific fields of an artist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artist patched successfully",
                    content = @Content(schema = @Schema(implementation = ArtistDTO.class))),
            @ApiResponse(responseCode = "404", description = "Artist not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ArtistDTO> patchArtist(
            @Parameter(description = "ID of artist to be patched", required = true)
            @PathVariable Integer id,
            @RequestBody ArtistPatchDTO artistPatchDTO) {
        ArtistDTO patchedArtist = artistService.patchArtist(id, artistPatchDTO);
        return ResponseEntity.ok(patchedArtist);
    }

    @Operation(summary = "Get cache info", description = "Returns artist cache statistics")
    @ApiResponse(responseCode = "200", description = "Cache info retrieved",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping("/cache-info")
    public ResponseEntity<String> getArtistCacheInfo() {
        return ResponseEntity.ok(artistService.getCacheInfo());
    }
}