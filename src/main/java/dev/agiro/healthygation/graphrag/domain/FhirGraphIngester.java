package dev.agiro.healthygation.graphrag.domain;

import dev.agiro.healthygation.FhirClientService;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.*;

@Service
public class FhirGraphIngester {

    private static final Logger log = LoggerFactory.getLogger(FhirGraphIngester.class);

    private final FhirClientService fhirClientService;
    private final Neo4jGraphStore graphStore;
    private final EmbeddingModel embeddingModel;

    public FhirGraphIngester(FhirClientService fhirClientService, Neo4jGraphStore graphStore, EmbeddingModel embeddingModel) {
        this.fhirClientService = fhirClientService;
        this.graphStore = graphStore;
        this.embeddingModel = embeddingModel;
    }

    public void ingestAll(int pageSize, int maxPatients) {
        int page = 0;
        int total = 0;

        while (total < maxPatients) {
            Bundle bundle = fhirClientService.fetchPage(page, pageSize);
            if (!bundle.hasEntry() || bundle.getEntry().isEmpty()) {
                break;
            }

            List<GraphNode> pagePatients = new ArrayList<>();
            List<GraphNode> pageConcepts = new ArrayList<>();
            List<Relationship> pageRelations = new ArrayList<>();

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Patient patient) {
                    processPatient(patient, pagePatients, pageConcepts, pageRelations);
                    total++;
                    if (total >= maxPatients) {
                        break;
                    }
                }
            }

            savePage(pagePatients, pageConcepts, pageRelations);
            log.info("Ingested page {} with {} patients (total {})", page, pagePatients.size(), total);
            page++;
        }
    }

    private void processPatient(Patient patient, List<GraphNode> patients, List<GraphNode> concepts, List<Relationship> relations) {
        String patientId = patient.getIdElement().toUnqualifiedVersionless().getValue();
        Integer birthYear = Optional.ofNullable(patient.getBirthDate())
                .map(d -> d.toInstant().atZone(ZoneOffset.UTC).getYear())
                .orElse(null);
        String gender = Optional.ofNullable(patient.getGender()).map(Object::toString).orElse("unknown");

        GraphNode patientNode = new GraphNode(
                patientId,
                "Patient",
                "Patient born in " + Optional.ofNullable(birthYear).orElse(0) + ", gender " + gender,
                null,
                Map.of("gender", gender, "birthYear", birthYear)
        );
        patients.add(patientNode);

        Bundle conditions = fhirClientService.searchConditionsByPatient(patientId);
        if (conditions.hasEntry()) {
            for (Bundle.BundleEntryComponent e : conditions.getEntry()) {
                if (e.getResource() instanceof Condition condition) {
                    String id = condition.getIdElement().toUnqualifiedVersionless().getValue();
                    String text = extractText(condition.getCode());
                    List<Float> embedding = embed(text);
                    GraphNode concept = new GraphNode(id, "Condition", text, embedding, Map.of());
                    concepts.add(concept);
                    relations.add(new Relationship(patientId, id, "HAS_CONDITION"));
                }
            }
        }

        Bundle medications = fhirClientService.searchMedicationRequestsByPatient(patientId);
        if (medications.hasEntry()) {
            for (Bundle.BundleEntryComponent e : medications.getEntry()) {
                if (e.getResource() instanceof MedicationRequest medication) {
                    String id = medication.getIdElement().toUnqualifiedVersionless().getValue();
                    String text = extractMedicationText(medication);
                    List<Float> embedding = embed(text);
                    GraphNode concept = new GraphNode(id, "Medication", text, embedding, Map.of());
                    concepts.add(concept);
                    relations.add(new Relationship(patientId, id, "TAKES"));
                }
            }
        }

        Bundle observations = fhirClientService.searchObservationsByPatient(patientId);
        if (observations.hasEntry()) {
            for (Bundle.BundleEntryComponent e : observations.getEntry()) {
                if (e.getResource() instanceof Observation observation) {
                    String id = observation.getIdElement().toUnqualifiedVersionless().getValue();
                    String text = extractObservationText(observation);
                    List<Float> embedding = embed(text);
                    GraphNode concept = new GraphNode(id, "Observation", text, embedding, Map.of());
                    concepts.add(concept);
                    relations.add(new Relationship(patientId, id, "HAS_OBSERVATION"));
                }
            }
        }
    }

    private void savePage(List<GraphNode> patients, List<GraphNode> concepts, List<Relationship> relations) {
        graphStore.savePatients(patients);
        graphStore.saveConcepts(concepts);
        graphStore.saveRelationships(filterRelations(relations, "HAS_CONDITION"), "HAS_CONDITION", "Condition");
        graphStore.saveRelationships(filterRelations(relations, "TAKES"), "TAKES", "Medication");
        graphStore.saveRelationships(filterRelations(relations, "HAS_OBSERVATION"), "HAS_OBSERVATION", "Observation");
    }

    private List<Relationship> filterRelations(List<Relationship> all, String type) {
        return all.stream().filter(r -> r.type().equals(type)).toList();
    }

    private List<Float> embed(String text) {
        float[] vector = embeddingModel.embed(text);
        List<Float> embedding = new ArrayList<>(vector.length);
        for (float value : vector) {
            embedding.add(value);
        }
        return embedding;
    }

    private String extractText(CodeableConcept code) {
        if (code == null) {
            return "unknown";
        }
        if (code.hasText()) {
            return code.getText();
        }
        Coding coding = code.getCodingFirstRep();
        if (coding != null && coding.hasDisplay()) {
            return coding.getDisplay();
        }
        if (coding != null) {
            return coding.getSystem() + "|" + coding.getCode();
        }
        return "unknown";
    }

    private String extractMedicationText(MedicationRequest medication) {
        if (medication.hasMedicationCodeableConcept()) {
            return extractText(medication.getMedicationCodeableConcept());
        }
        if (medication.hasMedicationReference()) {
            return medication.getMedicationReference().getReference();
        }
        return "unknown";
    }

    private String extractObservationText(Observation observation) {
        String code = extractText(observation.getCode());
        String value = "unknown";
        if (observation.hasValueStringType()) {
            value = observation.getValueStringType().getValue();
        } else if (observation.hasValueQuantity()) {
            value = observation.getValueQuantity().getValue().toPlainString() + " " + observation.getValueQuantity().getUnit();
        } else if (observation.hasValueCodeableConcept()) {
            value = extractText(observation.getValueCodeableConcept());
        }
        return code + ": " + value;
    }
}
