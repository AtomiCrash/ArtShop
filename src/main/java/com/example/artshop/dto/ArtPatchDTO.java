package com.example.artshop.dto;

import java.util.Set;

public class ArtPatchDTO {
    private String title;
    private Integer year;
    private int classificationId;
    private Set<Integer> artistIds;

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public int getClassificationId() {
        return classificationId;
    }

    public Set<Integer> getArtistIds() {
        return artistIds;
    }

    public boolean hasUpdates() {
        return title != null || year != null ||
                classificationId != '0' || artistIds != null;
    }
}