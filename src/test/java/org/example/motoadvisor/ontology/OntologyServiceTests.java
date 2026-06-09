package org.example.motoadvisor.ontology;

import org.example.motoadvisor.ontology.model.MotorcycleOntologyRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OntologyServiceTests {

    @Autowired
    private OntologyService ontologyService;

    @Test
    void loadsSeedOntologyAndReturnsMotorcycles() {
        var rows = ontologyService.findAllMotorcycles();

        assertNotNull(rows);
        // Seed target is 100+ bikes in this phase.
        assertTrue(rows.size() >= 100,
                "Expected seed size >= 100, got " + rows.size());
    }

    @Test
    void upsertAndUpdatePriceAvailability() {
        MotorcycleOntologyRecord record = new MotorcycleOntologyRecord(
                "http://example.org/moto-advisor#TEST900",
                "TEST900",
                "TestBrand",
                "Test 900",
                "NAKED",
                900,
                9900,
                810,
                190,
                "INTERMEDIATE",
                true
        );

        ontologyService.upsertMotorcycle(record);

        Optional<MotorcycleOntologyRecord> created = ontologyService.findByModelCode("TEST900");
        assertTrue(created.isPresent());
        assertEquals(9900, created.get().priceEur());
        assertTrue(created.get().available());

        ontologyService.updatePrice("TEST900", 10500);
        ontologyService.updateAvailability("TEST900", false);

        Optional<MotorcycleOntologyRecord> updated = ontologyService.findByModelCode("TEST900");
        assertTrue(updated.isPresent());
        assertEquals(10500, updated.get().priceEur());
        assertFalse(updated.get().available());
    }
}

