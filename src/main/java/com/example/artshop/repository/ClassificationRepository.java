package com.example.artshop.repository;

import com.example.artshop.model.Classification;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassificationRepository extends JpaRepository<Classification, Integer> {
    Classification findByName(String name);

    Classification findById(int id);

    Classification save(Classification classification);

    @Query("SELECT DISTINCT c FROM Classification c JOIN c.arts a WHERE LOWER(a.title) " +
            "LIKE LOWER(concat('%', :artTitle, '%'))")
    List<Classification> findByArtTitleContaining(@Param("artTitle") String artTitle);

    @Query("SELECT c FROM Classification c WHERE LOWER(c.name) LIKE LOWER(concat('%', :name, '%'))")
    List<Classification> findByNameContainingIgnoreCase(@Param("name") String name);
}