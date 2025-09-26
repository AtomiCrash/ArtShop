package com.example.artshop.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "classification")
public class Classification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "classification")
    @JsonBackReference
    private Set<Art> arts = new HashSet<>();

    public Classification() {}

    public Classification(String name, String description) {
        this.name = name;
        this.description = description;
    }

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

    public Set<Art> getArts() {
        return arts;
    }

    public void setArts(Set<Art> arts) {
        this.arts = arts;
    }

    @Override
    public String toString() {
        return "Classification{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", artsCount=" + (arts != null ? arts.size() : 0) +
                '}';
    }

    public void addArt(Art art) {
        this.arts.add(art);
        art.setClassification(this);
    }

    public void removeArt(Art art) {
        this.arts.remove(art);
        art.setClassification(null);
    }
}
