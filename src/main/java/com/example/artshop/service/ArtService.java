package com.example.artshop.service;

import com.example.artshop.dto.ArtDTO;
import com.example.artshop.exception.ArtNotFoundException;
import com.example.artshop.exception.ArtistNotFoundException;
import com.example.artshop.model.Art;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.artshop.repository.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArtService {
    @Autowired
    private ArtRepository artRepository;

    @Autowired
    private ArtistRepository artistRepository;

    public static final String ART_NOT_FOUND = "Art with id %d not found";
    public static final String ARTIST_NOT_FOUND = "Artist with id %d not found";

    // Добавление картины с возможностью указать художников
    public Art addArt(ArtDTO artDTO) {
        Art art = new Art();
        art.setTitle(artDTO.getTitle());
        art.setYear(artDTO.getYear());

        Set<Artist> artists = new HashSet<>();
        for (String artistName : artDTO.getArtist()) {
            Artist artist = artistRepository.findByName(artistName)
                    .orElseGet(() -> {
                        Artist newArtist = new Artist(artistName);
                        return artistRepository.save(newArtist);
                    });
            artists.add(artist);
        }
        art.setArtists(artists);

        return artRepository.save(art);
    }

    // Получение всех картин
    public List<Art> getAllArts() {
        return artRepository.findAll();
    }

    // Получение картины по ID
    public Art getArtById(int id) {
        return artRepository.findById(id)
                .orElseThrow(() -> new ArtNotFoundException(String.format(ART_NOT_FOUND, id)));
    }

    // Удаление картины по ID
    public void deleteArtById(int id) {
        if (!artRepository.existsById(id)) {
            throw new ArtNotFoundException(String.format(ART_NOT_FOUND, id));
        }
        artRepository.deleteById(id);
    }

    // Обновление картины с возможностью обновить список художников
    public Art updateArt(int id, Art art, List<Integer> artistIds) {
        Art existingArt = artRepository.findById(id)
                .orElseThrow(() -> new ArtNotFoundException(String.format(ART_NOT_FOUND, id)));

        existingArt.setTitle(art.getTitle());
        existingArt.setYear(art.getYear());

        Set<Artist> artists = new HashSet<>();
        for (Integer artistId : artistIds) {
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new ArtistNotFoundException(String.format(ARTIST_NOT_FOUND, artistId)));
            artists.add(artist);
        }
        existingArt.setArtists(artists);

        return artRepository.save(existingArt);
    }

    // Добавление художника к картине
    public Art addArtistToArt(int artId, int artistId) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new ArtNotFoundException(String.format(ART_NOT_FOUND, artId)));
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistNotFoundException(String.format(ARTIST_NOT_FOUND, artistId)));

        art.getArtists().add(artist);
        return artRepository.save(art);
    }

    // Удаление художника из картины
    public Art removeArtistFromArt(int artId, int artistId) {
        Art art = artRepository.findById(artId)
                .orElseThrow(() -> new ArtNotFoundException(String.format(ART_NOT_FOUND, artId)));
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistNotFoundException(String.format(ARTIST_NOT_FOUND, artistId)));

        art.getArtists().remove(artist);
        return artRepository.save(art);
    }
}