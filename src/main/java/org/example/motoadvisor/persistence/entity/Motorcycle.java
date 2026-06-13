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

    /** Make / brand name, e.g. "Honda", "Yamaha". */
    @Column(nullable = false)
    private String brand;

    /** Model name, e.g. "CB500F". */
    @Column(nullable = false)
    private String name;

    /**
     * Riding category.
     * Allowed values: SPORT, NAKED, TOURING, ADVENTURE, CRUISER, COMMUTER
     */
    @Column(nullable = false)
    private String category;

    /** Engine displacement in cubic centimetres. */
    @Column(nullable = false)
    private int engineSizeCc;

    /** Approximate price in EUR. */
    private int priceEur;

    /** Seat height in millimetres. */
    private int seatHeightMm;

    /** Wet weight in kilograms. */
    private int weightKg;

    /**
     * Minimum recommended experience level.
     * Allowed values: BEGINNER, INTERMEDIATE, ADVANCED
     */
    @Column(nullable = false)
    private String experienceLevel;
}

