package com.example.artshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ArtDTO {
    private Integer id;
    @Schema(description = "Title of the artwork", example = "Starry Night", required = true)
    private String title;
    @Schema(description = "Year when artwork was created", example = "1889")
    private Integer year;
    @Schema(description = "List of artists who created the artwork")
    @Valid
    private List<ArtistDTO> artists;
    @Schema(description = "Classification of the artwork")
    @Valid
    private ClassificationDTO classification;
    private int classificationId;
    private String classificationName;

    public int getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(int classificationId) {
        this.classificationId = classificationId;
    }

    public String getClassificationName() {
        return classificationName;
    }

    public void setClassificationName(String classificationName) {
        this.classificationName = classificationName;
    }

    public ClassificationDTO getClassification() {
        return classification;
    }

    public void setClassification(ClassificationDTO classification) {
        this.classification = classification;
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

    public void setYear(int year) {
        this.year = year;
    }

    public List<ArtistDTO> getArtists() {
        return artists;
    }

    public void setArtists(List<ArtistDTO> artists) {
        this.artists = artists;
    }
}

