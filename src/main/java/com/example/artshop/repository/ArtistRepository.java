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
    Optional<Artist> findByFirstNameAndLastName(String firstName, String lastName);

    List<Artist> findByFirstNameContaining(String firstName);

    List<Artist> findByLastNameContaining(String lastName);

    List<Artist> findByFirstNameContainingAndLastNameContaining(String firstName, String lastName);

    @Query("SELECT a FROM Artist a WHERE LOWER(a.firstName) LIKE LOWER(concat('%', :query, '%')) OR " +
            "LOWER(a.lastName) LIKE LOWER(concat('%', :query, '%'))")
    List<Artist> searchByName(@Param("query") String query);

    List<Artist> findByFirstNameContainingIgnoreCase(String firstName);

    List<Artist> findByLastNameContainingIgnoreCase(String lastName);

    List<Artist> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(String firstName, String lastName);

    @Query("SELECT a FROM Artist a LEFT JOIN FETCH a.arts WHERE a.id = :id")
    Optional<Artist> findWithArtsById(@Param("id") Integer id);

    @Query("SELECT DISTINCT a FROM Artist a JOIN a.arts art WHERE LOWER(art.title) " +
            "LIKE LOWER(concat('%', :artTitle, '%'))")
    List<Artist> findByArtTitleContaining(@Param("artTitle") String artTitle);
}