package dev.agiro.healthygation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class FhirClientService {

	private final IGenericClient fhirClient;

	public FhirClientService() {
		FhirContext ctx = FhirContext.forR4();
		this.fhirClient = ctx.newRestfulGenericClient("https://hapi.fhir.org/baseR4");
	}

	public Bundle cercaPacientsPerCognom(String cognom) {
		return fhirClient.search()
				.forResource(Patient.class)
				.where(Patient.FAMILY.matches().value(cognom))
				.returnBundle(Bundle.class)
				.execute();
	}

}
