package com.example.artshop.dto;

import com.example.artshop.model.Art;
import java.util.List;

public class ArtRequest {
    private Art art;
    private List<Integer> artistIds;

    // Геттеры и сеттеры
    public Art getArt() {
        return art;
    }

    public void setArt(Art art) {
        this.art = art;
    }

    public List<Integer> getArtistIds() {
        return artistIds;
    }

    public void setArtistIds(List<Integer> artistIds) {
        this.artistIds = artistIds;
    }
}
