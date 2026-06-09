package org.example.motoadvisor.ontology.model;

/**
 * Projection model for motorcycle data stored in ontology.
 *
 * <p>Used by UI and search flows to avoid leaking OWLAPI types outside
 * OntologyService.
 */
public record MotorcycleOntologyRecord(
        String iri,
        String modelCode,
        String brand,
        String modelName,
        String category,
        int engineSizeCc,
        int priceEur,
        int seatHeightMm,
        int weightKg,
        String experienceLevel,
        boolean available
) {
}

