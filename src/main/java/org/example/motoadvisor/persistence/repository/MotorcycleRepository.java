package org.example.motoadvisor.persistence.repository;

import org.example.motoadvisor.persistence.entity.Motorcycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotorcycleRepository extends JpaRepository<Motorcycle, Long> {

    List<Motorcycle> findByCategory(String category);

    List<Motorcycle> findByExperienceLevel(String experienceLevel);

    List<Motorcycle> findByCategoryAndExperienceLevel(String category, String experienceLevel);

    List<Motorcycle> findByEngineSizeCcBetween(int minCc, int maxCc);

    boolean existsByBrandAndName(String brand, String name);

    Optional<Motorcycle> findByBrandAndName(String brand, String name);
}
