package dev.agiro.healthygation.controller;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import dev.agiro.healthygation.FhirClientService;
import dev.agiro.healthygation.domain.PatientBrief;
import org.hl7.fhir.r5.model.*;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final FhirClientService fhirClientService;

    public PatientController(FhirClientService fhirClientService) {
        this.fhirClientService = fhirClientService;
    }

    @GetMapping("/{id}")
    public Patient getAllPatients(@PathVariable String id) {
        return fhirClientService.searchPatient(id);
    }

    @GetMapping
    public List<PatientBrief> getPatientsByName(@RequestParam String name) {

        Bundle bundle = fhirClientService.searchPatientByName(name);

        if (bundle.hasEntry()) {
            return bundle.getEntry().stream()
                    .map(entry -> {
                        if (entry.getResource() instanceof Patient patient ) {
                            final HumanName humanName = patient.getName().getFirst();

                            return new PatientBrief(patient.getIdentifier().stream().map(Identifier::getValue).toList(),
                                                    humanName.getNameAsSingleString(),
                                                    humanName.getFamily(),
                                                    ObjectUtils.nullSafeToString(patient.getGender()),
                                                    patient.getBirthDate());
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

        }
        throw new ResourceNotFoundException("Patient not found");
    }
}
