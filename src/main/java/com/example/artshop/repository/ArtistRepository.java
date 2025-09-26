package com.example.artshop.repository;

import com.example.artshop.model.Artist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Integer> {
    @Query("SELECT a FROM Artist a WHERE a.firstName = :firstName AND a.lastName = :lastName")
    List<Artist> findByFirstNameAndLastName(@Param("firstName") String firstName,
                                            @Param("lastName") String lastName);

    default Optional<Artist> findFirstByFirstNameAndLastName(String firstName, String lastName) {
        List<Artist> artists = findByFirstNameAndLastName(firstName, lastName);
        return artists.isEmpty() ? Optional.empty() : Optional.of(artists.get(0));
    }

    @Query("SELECT a FROM Artist a LEFT JOIN FETCH a.arts WHERE a.id = :id")
    Optional<Artist> findWithArtsById(@Param("id") Integer id);

    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN FETCH a.arts")
    List<Artist> findAllWithArts();

    @Query("SELECT a FROM Artist a JOIN a.arts art WHERE art.title LIKE %:artTitle%")
    List<Artist> findByArtTitleContaining(@Param("artTitle") String artTitle);

    List<Artist> findByFirstNameContainingIgnoreCase(String firstName);

    List<Artist> findByLastNameContainingIgnoreCase(String lastName);

    List<Artist> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName, String lastName);
}