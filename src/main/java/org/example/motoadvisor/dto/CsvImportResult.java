package org.example.motoadvisor.dto;

import lombok.*;

import java.util.List;

/**
 * Outcome of one CSV import execution.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportResult {

    private Long batchId;
    private String sourcePath;

    @Builder.Default
    private int totalRows = 0;

    @Builder.Default
    private int importedRows = 0;

    @Builder.Default
    private int skippedRows = 0;

    @Builder.Default
    private List<String> errors = List.of();

    @Builder.Default
    private boolean success = true;
}

