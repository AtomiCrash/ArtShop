package com.example.artshop.service;

import com.example.artshop.exception.ArtistNotFoundException;
import com.example.artshop.model.Artist;
import com.example.artshop.repository.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ArtistService {
    @Autowired
    private ArtistRepository artistRepository;

    public static final String ARTIST_NOT_FOUND = "Artist with id %d not found";

    // Добавление художника
    public Artist addArtist(Artist artist) {
        return artistRepository.save(artist);
    }

    // Получение всех художников
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    // Получение художника по ID
    public Artist getArtistById(int id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ArtistNotFoundException(String.format(ARTIST_NOT_FOUND, id)));
    }

    // Удаление художника по ID
    public void deleteArtistById(int id) {
        if (!artistRepository.existsById(id)) {
            throw new ArtistNotFoundException(String.format(ARTIST_NOT_FOUND, id));
        }
        artistRepository.deleteById(id);
    }

    // Обновление художника
    public Artist updateArtist(int id, Artist artist) {
        Artist existingArtist = artistRepository.findById(id)
                .orElseThrow(() -> new ArtistNotFoundException(String.format(ARTIST_NOT_FOUND, id)));

        existingArtist.setName(artist.getName());
        return artistRepository.save(existingArtist);
    }
}
