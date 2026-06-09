package org.example.motoadvisor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.service.CsvImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup.
 * Triggers CSV import when {@code app.csv.import.enabled=true} (default).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {

    private final CsvImportService csvImportService;

    @Value("${app.csv.import.enabled:true}")
    private boolean csvImportEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (csvImportEnabled) {
            csvImportService.importMotorcycles();
        } else {
            log.info("[STARTUP] CSV import disabled via app.csv.import.enabled=false");
        }
    }
}

