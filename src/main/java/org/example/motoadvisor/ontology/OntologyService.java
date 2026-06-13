package org.example.motoadvisor.ontology;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.ontology.model.MotorcycleOntologyRecord;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Ontology service for motorcycle records.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load ontology from configured path</li>
 *   <li>Save ontology to file path</li>
 *   <li>Find records (all/by model code)</li>
 *   <li>Upsert records</li>
 *   <li>Update availability and price fields</li>
 * </ul>
 */
@Slf4j
@Service
public class OntologyService {

    private static volatile OntologyService INSTANCE;

    private static final String BASE = "http://example.org/moto-advisor#";

    private static final String CLASS_MOTORCYCLE = "Motorcycle";

    private static final String P_MODEL_CODE = "hasModelCode";
    private static final String P_BRAND = "hasBrand";
    private static final String P_MODEL_NAME = "hasModelName";
    private static final String P_CATEGORY = "hasCategory";
    private static final String P_ENGINE = "engineSizeCc";
    private static final String P_PRICE = "priceEur";
    private static final String P_SEAT = "seatHeightMm";
    private static final String P_WEIGHT = "weightKg";
    private static final String P_EXPERIENCE = "experienceLevel";
    private static final String P_AVAILABLE = "available";

    @Value("${app.ontology.path:classpath:ontology/moto-advisor.owl}")
    private String ontologyPath;

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLDataFactory df;

    @PostConstruct
    public void init() {
        INSTANCE = this;
        manager = OWLManager.createOWLOntologyManager();
        df = manager.getOWLDataFactory();

        try (InputStream is = openOntologyStream(ontologyPath)) {
            if (is != null) {
                ontology = manager.loadOntologyFromOntologyDocument(is);
                log.info("[ONTOLOGY] Loaded '{}' - {} axioms.", ontologyPath, ontology.getAxiomCount());
            } else {
                log.warn("[ONTOLOGY] Resource '{}' not found - starting with empty ontology.", ontologyPath);
                ontology = manager.createOntology(IRI.create(BASE.replace("#", "")));
            }
        } catch (Exception e) {
            log.error("[ONTOLOGY] Failed to load ontology from '{}': {}. Starting with empty ontology.",
                    ontologyPath, e.getMessage());
            try {
                ontology = manager.createOntology(IRI.create(BASE.replace("#", "")));
            } catch (OWLOntologyCreationException ex) {
                throw new IllegalStateException("Cannot create fallback empty ontology", ex);
            }
        }
    }

    /** Persist ontology changes to file system path. */
    public synchronized void save() {
        if (ontologyPath.startsWith("classpath:")) {
            log.warn("[ONTOLOGY] Save skipped: classpath resource '{}' is read-only at runtime.", ontologyPath);
            return;
        }

        try {
            Path target = Path.of(ontologyPath);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            manager.saveOntology(ontology, IRI.create(target.toUri()));
            log.info("[ONTOLOGY] Saved ontology to '{}'.", target);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save ontology to " + ontologyPath, e);
        }
    }

    /** Returns all motorcycle records from ontology, ordered by model code. */
    public List<MotorcycleOntologyRecord> findAllMotorcycles() {
        List<MotorcycleOntologyRecord> rows = new ArrayList<>();
        ontology.individualsInSignature().forEach(ind -> {
            MotorcycleOntologyRecord row = toRecord(ind.asOWLNamedIndividual());
            if (row != null && row.modelCode() != null && !row.modelCode().isBlank()) {
                rows.add(row);
            }
        });
        rows.sort(Comparator.comparing(MotorcycleOntologyRecord::modelCode, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /** Returns only ontology records currently marked as available. */
    public List<MotorcycleOntologyRecord> findAvailableMotorcycles() {
        return findAllMotorcycles().stream()
                .filter(MotorcycleOntologyRecord::available)
                .toList();
    }

    /** Find one motorcycle record by model code (case-insensitive). */
    public Optional<MotorcycleOntologyRecord> findByModelCode(String modelCode) {
        return findAllMotorcycles().stream()
                .filter(r -> r.modelCode().equalsIgnoreCase(modelCode))
                .findFirst();
    }

    /**
     * Insert or update a motorcycle individual by model code.
     * Existing individual data properties are replaced by provided values.
     */
    public synchronized MotorcycleOntologyRecord upsertMotorcycle(MotorcycleOntologyRecord row) {
        String code = normalizeCode(row.modelCode());
        OWLNamedIndividual ind = individual(code);

        // Ensure type assertion exists.
        addAxiom(df.getOWLClassAssertionAxiom(owlClass(CLASS_MOTORCYCLE), ind));

        setString(ind, P_MODEL_CODE, code);
        setString(ind, P_BRAND, row.brand());
        setString(ind, P_MODEL_NAME, row.modelName());
        setString(ind, P_CATEGORY, row.category());
        setInt(ind, P_ENGINE, row.engineSizeCc());
        setInt(ind, P_PRICE, row.priceEur());
        setInt(ind, P_SEAT, row.seatHeightMm());
        setInt(ind, P_WEIGHT, row.weightKg());
        setString(ind, P_EXPERIENCE, row.experienceLevel());
        setBoolean(ind, P_AVAILABLE, row.available());

        return toRecord(ind);
    }

    /** Update availability flag by model code. */
    public synchronized Optional<MotorcycleOntologyRecord> updateAvailability(String modelCode, boolean available) {
        OWLNamedIndividual ind = findIndividualByModelCode(modelCode).orElse(null);
        if (ind == null) {
            return Optional.empty();
        }
        setBoolean(ind, P_AVAILABLE, available);
        return Optional.ofNullable(toRecord(ind));
    }

    /** Update price (EUR) by model code. */
    public synchronized Optional<MotorcycleOntologyRecord> updatePrice(String modelCode, int priceEur) {
        OWLNamedIndividual ind = findIndividualByModelCode(modelCode).orElse(null);
        if (ind == null) {
            return Optional.empty();
        }
        setInt(ind, P_PRICE, priceEur);
        return Optional.ofNullable(toRecord(ind));
    }

    /** Returns loaded ontology instance. */
    public OWLOntology getOntology() {
        return ontology;
    }

    /** Static accessor used by JADE agents that are not Spring-managed beans. */
    public static OntologyService getInstance() {
        return INSTANCE;
    }

    /** Returns ontology manager. */
    public OWLOntologyManager getManager() {
        return manager;
    }

    /** Backward-compatible simple class lookup by local name. */
    public Optional<OWLClass> findClass(String localName) {
        return ontology.classesInSignature()
                .filter(c -> c.getIRI().getShortForm().equalsIgnoreCase(localName))
                .findFirst();
    }

    private InputStream openOntologyStream(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String resource = path.substring("classpath:".length());
            return getClass().getClassLoader().getResourceAsStream(resource);
        }

        Path file = Path.of(path);
        if (Files.exists(file)) {
            return Files.newInputStream(file);
        }
        return null;
    }

    private OWLClass owlClass(String localName) {
        return df.getOWLClass(IRI.create(BASE + localName));
    }

    private OWLDataProperty dataProperty(String localName) {
        return df.getOWLDataProperty(IRI.create(BASE + localName));
    }

    private OWLNamedIndividual individual(String code) {
        return df.getOWLNamedIndividual(IRI.create(BASE + code));
    }

    private void addAxiom(OWLAxiom axiom) {
        manager.addAxiom(ontology, axiom);
    }

    private void setString(OWLNamedIndividual ind, String prop, String value) {
        replaceDataProperty(ind, prop);
        if (value != null) {
            addAxiom(df.getOWLDataPropertyAssertionAxiom(dataProperty(prop), ind, value));
        }
    }

    private void setInt(OWLNamedIndividual ind, String prop, int value) {
        replaceDataProperty(ind, prop);
        addAxiom(df.getOWLDataPropertyAssertionAxiom(dataProperty(prop), ind, value));
    }

    private void setBoolean(OWLNamedIndividual ind, String prop, boolean value) {
        replaceDataProperty(ind, prop);
        addAxiom(df.getOWLDataPropertyAssertionAxiom(dataProperty(prop), ind, value));
    }

    private void replaceDataProperty(OWLNamedIndividual ind, String prop) {
        OWLDataProperty p = dataProperty(prop);
        ontology.dataPropertyAssertionAxioms(ind)
                .filter(ax -> ax.getProperty().asOWLDataProperty().equals(p))
                .toList()
                .forEach(ax -> manager.removeAxiom(ontology, ax));
    }

    private Optional<OWLNamedIndividual> findIndividualByModelCode(String modelCode) {
        String wanted = normalizeCode(modelCode);

        Optional<OWLNamedIndividual> byIri = ontology.individualsInSignature()
                .filter(i -> i.isNamed())
                .map(OWLIndividual::asOWLNamedIndividual)
                .filter(i -> i.getIRI().getShortForm().equalsIgnoreCase(wanted))
                .findFirst();
        if (byIri.isPresent()) {
            return byIri;
        }

        return ontology.individualsInSignature()
                .filter(i -> i.isNamed())
                .map(OWLIndividual::asOWLNamedIndividual)
                .filter(i -> getString(i, P_MODEL_CODE).map(v -> v.equalsIgnoreCase(wanted)).orElse(false))
                .findFirst();
    }

    private MotorcycleOntologyRecord toRecord(OWLNamedIndividual ind) {
        String code = getString(ind, P_MODEL_CODE).orElse(ind.getIRI().getShortForm());
        String brand = getString(ind, P_BRAND).orElse("");
        String modelName = getString(ind, P_MODEL_NAME).orElse(code);
        String category = getString(ind, P_CATEGORY).orElse("");
        int engine = getInt(ind, P_ENGINE).orElse(0);
        int price = getInt(ind, P_PRICE).orElse(0);
        int seat = getInt(ind, P_SEAT).orElse(0);
        int weight = getInt(ind, P_WEIGHT).orElse(0);
        String exp = getString(ind, P_EXPERIENCE).orElse("");
        boolean available = getBoolean(ind, P_AVAILABLE).orElse(true);

        return new MotorcycleOntologyRecord(
                ind.getIRI().toString(),
                code,
                brand,
                modelName,
                category,
                engine,
                price,
                seat,
                weight,
                exp,
                available
        );
    }

    private Optional<String> getString(OWLNamedIndividual ind, String prop) {
        return ontology.dataPropertyAssertionAxioms(ind)
                .filter(ax -> ax.getProperty().asOWLDataProperty().equals(dataProperty(prop)))
                .map(ax -> ax.getObject().getLiteral())
                .findFirst();
    }

    private Optional<Integer> getInt(OWLNamedIndividual ind, String prop) {
        return getString(ind, prop).flatMap(v -> {
            try {
                return Optional.of(Integer.parseInt(v));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    private Optional<Boolean> getBoolean(OWLNamedIndividual ind, String prop) {
        return getString(ind, prop).map(Boolean::parseBoolean);
    }

    private String normalizeCode(String modelCode) {
        return modelCode == null ? "UNKNOWN" : modelCode.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
