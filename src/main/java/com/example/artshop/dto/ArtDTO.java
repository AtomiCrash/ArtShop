package com.example.artshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ArtDTO {
    private Integer id;

    @Schema(description = "Title of the artwork", example = "Starry Night", required = true)
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "Year when artwork was created", example = "1889")
    @Max(value = 2025, message = "Year cannot be in the future")
    private Integer year;

    @Schema(description = "List of artists who created the artwork")
    @Valid
    private List<ArtistDTO> artists;

    @Schema(description = "Classification of the artwork")
    @Valid
    private ClassificationDTO classification;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<ArtistDTO> getArtists() {
        return artists;
    }

    public void setArtists(List<ArtistDTO> artists) {
        this.artists = artists;
    }

    public ClassificationDTO getClassification() {
        return classification;
    }

    public void setClassification(ClassificationDTO classification) {
        this.classification = classification;
    }
}