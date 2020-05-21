package org.hotecosystem.terminology.fhir.cadsr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

import javax.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns = {"/*"}, displayName = "FHIR Terminology Server on caDSR")
public class CadsrServer extends RestfulServer {
    private static final long serialVersionUID = 1L;

    public CadsrServer() {
        super(FhirContext.forR5());
    }

    @Override
    public void initialize() {
        List<IResourceProvider> providers = new ArrayList<>();
        providers.add(new CadsrValueSetProvider());
        setResourceProviders(providers);

        INarrativeGenerator narrativeGenerator = new DefaultThymeleafNarrativeGenerator();
        getFhirContext().setNarrativeGenerator(narrativeGenerator);

        registerInterceptor(new ResponseHighlighterInterceptor());
    }
}
