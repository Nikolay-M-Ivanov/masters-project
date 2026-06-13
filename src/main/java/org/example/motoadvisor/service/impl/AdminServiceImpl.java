package org.example.motoadvisor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.ontology.OntologyService;
import org.example.motoadvisor.service.AdminService;
import org.springframework.stereotype.Service;

/**
 * Admin operations service for ontology-backed motorcycle maintenance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final OntologyService ontologyService;

    @Override
    public void updateMotorcyclePrice(String modelCode, int priceEur, boolean available) throws Exception {
        boolean priceUpdated = ontologyService.updatePrice(modelCode, priceEur).isPresent();
        boolean availabilityUpdated = ontologyService.updateAvailability(modelCode, available).isPresent();
        if (!priceUpdated && !availabilityUpdated) {
            throw new IllegalArgumentException("Model code not found in ontology: " + modelCode);
        }

        ontologyService.save();
        log.info("[ADMIN] Ontology updated for modelCode={} with priceEur={} available={}", modelCode, priceEur, available);
    }
}

