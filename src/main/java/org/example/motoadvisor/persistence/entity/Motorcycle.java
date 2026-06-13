package org.example.motoadvisor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a motorcycle in the reference catalogue.
 * Retained as an auxiliary persistence model, while the active search catalog is stored in OWL.
 */
@Entity
@Table(name = "motorcycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motorcycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int engineSizeCc;

    private int priceEur;

    private int seatHeightMm;

    private int weightKg;

    @Column(nullable = false)
    private String experienceLevel;
}

