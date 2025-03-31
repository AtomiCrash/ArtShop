package com.example.artshop.repository;

import com.example.artshop.model.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassificationRepository extends JpaRepository<Classification, Integer> {
    Classification findByName(String name);

    Classification findById(int id);

    Classification save(Classification classification);
}
