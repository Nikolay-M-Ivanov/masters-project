package org.example.motoadvisor.service;

/**
 * Service for ontology-backed admin operations.
 */
public interface AdminService {

    /**
     * Update motorcycle price and availability by model code.
     * Persists changes in the ontology so the next search reflects them.
     *
     * @param modelCode the motorcycle model code
     * @param priceEur new price in EUR
     * @param available availability status
     * @throws Exception if operation fails
     */
    void updateMotorcyclePrice(String modelCode, int priceEur, boolean available) throws Exception;
}

