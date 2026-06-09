package org.example.motoadvisor.service;

import org.example.motoadvisor.dto.CsvImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CsvImportServiceTests {

    @Autowired
    private CsvImportService csvImportService;

    @Test
    void importCsvFailsWhenRequiredHeaderMissing() {
        String csv = "brand,name,category,priceEur\n" +
                "Honda,CB500F,NAKED,6700\n";

        CsvImportResult result = csvImportService.importCsv("test-missing-header.csv", csv, false);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().getFirst().contains("Missing required CSV header"));
    }

    @Test
    void importCsvValidatesRowsAndUpserts() {
        String csv = "brand,name,category,engineSizeCc,priceEur,seatHeightMm,weightKg,experienceLevel\n" +
                "Honda,CB500F,NAKED,471,6700,790,189,BEGINNER\n" +
                "Yamaha,MT-07,NAKED,689,8000,805,184,INTERMEDIATE\n" +
                "BadBrand,Bad Bike,UNKNOWN,900,10000,810,200,INTERMEDIATE\n"; // invalid category

        CsvImportResult result = csvImportService.importCsv("test-valid-upsert.csv", csv, false);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRows());
        assertEquals(2, result.getImportedRows());
        assertEquals(1, result.getSkippedRows());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void parseCsvContentReturnsOnlyValidRowCount() {
        String csv = "brand,name,category,engineSizeCc,experienceLevel\n" +
                "Honda,CB500F,NAKED,471,BEGINNER\n" +
                "Honda,,NAKED,471,BEGINNER\n"; // blank name -> invalid

        int valid = csvImportService.parseCsvContent(csv);

        assertEquals(1, valid);
    }
}

