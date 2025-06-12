package com.example.artshop.repository;

import com.example.artshop.model.Art;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtRepository extends JpaRepository<Art, Integer> {

    List<Art> findByTitleContainingIgnoreCase(String title);

    List<Art> findByYear(Integer year);

    List<Art> findByTitleContainingIgnoreCaseAndYear(String title, Integer year);

    @EntityGraph(attributePaths = {"artists"})
    Optional<Art> findWithArtistsById(Integer id);

    @Query("SELECT a FROM Art a JOIN a.artists ar WHERE LOWER(ar.lastName) LIKE LOWER(concat('%', :artistName, '%'))")
    List<Art> findByArtistsLastNameContainingIgnoreCase(@Param("artistName") String artistName);

    @Query("SELECT a FROM Art a JOIN a.classification c WHERE c.id = :classificationId")
    List<Art> findByClassificationId(@Param("classificationId") Integer classificationId);

    @Query("SELECT a FROM Art a JOIN a.classification c WHERE LOWER(c.name) " +
            "LIKE LOWER(concat('%', :classificationName, '%'))")
    List<Art> findByClassificationNameContainingIgnoreCase(@Param("classificationName") String classificationName);

    @Query("SELECT DISTINCT a FROM Art a LEFT JOIN FETCH a.artists LEFT JOIN FETCH a.classification WHERE a.id = :id")
    Optional<Art> findWithArtistsAndClassificationById(@Param("id") Integer id);

    @Query("SELECT a FROM Art a WHERE a.title = :title")
    Optional<Art> findByTitle(@Param("title") String title);

    @Query("SELECT DISTINCT a FROM Art a LEFT JOIN FETCH a.artists LEFT JOIN FETCH a.classification")
    List<Art> findAllWithArtistsAndClassification();
}
