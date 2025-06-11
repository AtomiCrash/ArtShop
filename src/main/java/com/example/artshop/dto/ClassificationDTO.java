package com.example.artshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ClassificationDTO {
    private int id;
    @Size(max = 60, message = "Classification name must be 60 characters or less")
    @NotBlank(message = "Classification name is required")
    private String name;
    @Size(max = 120, message = "Description must be 60 characters or less")
    @NotBlank(message = "Description is required")
    private String description;
    private List<ArtDTO> works;

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
}
