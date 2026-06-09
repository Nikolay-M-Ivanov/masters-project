package org.example.motoadvisor.persistence.repository;

import org.example.motoadvisor.persistence.entity.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    Optional<ImportBatch> findTopByOrderByStartedAtDesc();

    List<ImportBatch> findTop20ByOrderByStartedAtDesc();

    /**
     * Helper method: open a new import batch as STARTED.
     */
    default ImportBatch startBatch(String sourcePath) {
        ImportBatch batch = ImportBatch.builder()
                .sourcePath(sourcePath)
                .status(ImportBatch.STATUS_STARTED)
                .build();
        return save(batch);
    }

    /**
     * Helper method: close an import batch as COMPLETED.
     */
    @Transactional
    default ImportBatch completeBatch(Long batchId, int totalRows, int importedRows, int skippedRows) {
        ImportBatch batch = findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Import batch not found: " + batchId));

        batch.setStatus(ImportBatch.STATUS_COMPLETED);
        batch.setTotalRows(totalRows);
        batch.setImportedRows(importedRows);
        batch.setSkippedRows(skippedRows);
        batch.setFinishedAt(LocalDateTime.now());
        batch.setErrorMessage(null);

        return save(batch);
    }

    /**
     * Helper method: close an import batch as FAILED.
     */
    @Transactional
    default ImportBatch failBatch(Long batchId, String errorMessage) {
        ImportBatch batch = findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Import batch not found: " + batchId));

        batch.setStatus(ImportBatch.STATUS_FAILED);
        batch.setFinishedAt(LocalDateTime.now());
        batch.setErrorMessage(errorMessage);

        return save(batch);
    }
}
