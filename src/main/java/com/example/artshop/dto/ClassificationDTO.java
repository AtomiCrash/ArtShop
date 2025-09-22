package com.example.artshop.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ClassificationDTO {
    private Integer id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    private List<String> artworkTitles;
    private Integer artworkCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getArtworkTitles() {
        return artworkTitles;
    }

    public void setArtworkTitles(List<String> artworkTitles) {
        this.artworkTitles = artworkTitles;
    }

    public Integer getArtworkCount() {
        return artworkCount;
    }

    public void setArtworkCount(Integer artworkCount) {
        this.artworkCount = artworkCount;
    }
}