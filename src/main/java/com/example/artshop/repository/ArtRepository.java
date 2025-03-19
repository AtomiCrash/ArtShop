package com.example.artshop.repository;

import com.example.artshop.model.Art;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtRepository extends JpaRepository<Art, Integer> {
    Optional<Art> findByTitle(String title);
}
