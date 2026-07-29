package dev.agiro.healthygation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FhirClientService {

	private final IGenericClient fhirClient;

	public FhirClientService(@Value("${healthygation.fhir.base-url:http://localhost:8085/hapi-fhir-jpaserver/fhir/}")
                             String fhirServerUrl) {
		FhirContext ctx = FhirContext.forR4Cached();
        ctx.setRestfulClientFactory(new ApacheRestfulClientFactory(ctx));

		this.fhirClient = ctx.newRestfulGenericClient(fhirServerUrl);
        this.fhirClient.registerInterceptor(new LoggingInterceptor(true));
	}


    public Bundle fetchPage(int pageNumber, int pageSize) {
        return fhirClient.search()
                .forResource(Patient.class)
                .offset(pageNumber * pageSize)
                .count(pageSize)
                .returnBundle(Bundle.class)
                .execute();
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
