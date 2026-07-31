package dev.agiro.healthygation.graphrag.domain;

import dev.agiro.healthygation.FhirClientService;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FhirGraphIngesterTest {

    private final FhirClientService fhirClientService = Mockito.mock(FhirClientService.class);
    private final Neo4jGraphStore graphStore = Mockito.mock(Neo4jGraphStore.class);
    private final EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);

    private final FhirGraphIngester ingester = new FhirGraphIngester(fhirClientService, graphStore, embeddingModel);

    @Test
    void ingestAllProcessesOnePatientWithConditions() {
        Patient patient = new Patient();
        patient.setId("Patient/1");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setBirthDate(Date.from(LocalDate.of(1980, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        condition.setId("Condition/1");
        condition.setCode(new CodeableConcept().setText("Diabetes"));

        Bundle patientBundle = new Bundle();
        patientBundle.addEntry().setResource(patient);

        Bundle conditionBundle = new Bundle();
        conditionBundle.addEntry().setResource(condition);

        Bundle emptyBundle = new Bundle();

        when(fhirClientService.fetchPage(0, 1)).thenReturn(patientBundle);
        when(fhirClientService.fetchPage(1, 1)).thenReturn(emptyBundle);
        when(fhirClientService.searchConditionsByPatient("Patient/1")).thenReturn(conditionBundle);
        when(fhirClientService.searchMedicationRequestsByPatient(anyString())).thenReturn(emptyBundle);
        when(fhirClientService.searchObservationsByPatient(anyString())).thenReturn(emptyBundle);
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});

        ingester.ingestAll(1, 1);

        verify(graphStore).savePatients(anyList());
        verify(graphStore).saveConcepts(anyList());
        verify(graphStore).saveRelationships(anyList(), eq("HAS_CONDITION"), eq("Condition"));
    }
}
