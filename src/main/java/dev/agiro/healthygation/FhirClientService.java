package dev.agiro.healthygation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class FhirClientService {

	private final IGenericClient fhirClient;

	public FhirClientService() {
		FhirContext ctx = FhirContext.forR5Cached();
        ctx.setRestfulClientFactory(new ApacheRestfulClientFactory(ctx));

		this.fhirClient = ctx.newRestfulGenericClient("http://localhost:8080/hapi-fhir-jpaserver/fhir/");
        this.fhirClient.registerInterceptor(new LoggingInterceptor(true));
	}

	public Patient searchPatient(String identifier) {
		return fhirClient.read()
                .resource(Patient.class)
                .withId(identifier)
				.execute();
	}
	public Bundle searchPatientByName(String identifier) {
		return fhirClient.search()
				.forResource(Patient.class)
				.where(Patient.FAMILY.matches().value(identifier))
				.returnBundle(Bundle.class)
				.execute();
	}

}
