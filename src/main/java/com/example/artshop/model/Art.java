package com.example.artshop.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@Table(name = "Arts")
//@AllArgsConstructor
public class Art {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    @Column(unique = true)
    private String artist;
    private int year;

    public Art(String title, String artist, int year) {
        this.title = title;
        this.artist = artist;
        this.year = year;
    }
    public Art(int id, String title, String artist, int year) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.year = year;
    }

    public Art() {
    }

    @Override
    public String toString() {
        return "Art{" + '\n' +
                "id=" + id + '\n' +
                "title='" + title + '\'' + '\n' +
                "artist='" + artist + '\'' + '\n' +
                "year=" + year + '\n' +
                '}';
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getYear() {
        return year;
    }

    // Сеттеры
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setYear(int year) {
        this.year = year;
    }
}