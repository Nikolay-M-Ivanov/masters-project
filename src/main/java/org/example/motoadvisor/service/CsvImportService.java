package org.example.motoadvisor.service;

import org.example.motoadvisor.dto.CsvImportResult;

/**
 * Service responsible for importing motorcycle data from CSV.
 */
public interface CsvImportService {

    /**
     * Import motorcycle records from configured startup CSV path.
     *
     * @return number of rows imported/upserted
     */
    int importMotorcycles();

    /**
     * Parse raw CSV string and return valid row count.
     */
    int parseCsvContent(String csvContent);

    /**
     * Full import pipeline used by admin upload.
     * Validates required headers + row constraints, upserts DB rows,
     * upserts ontology individuals, and saves ontology when requested.
     */
    CsvImportResult importCsv(String sourcePath, String csvContent, boolean saveOntology);
}
