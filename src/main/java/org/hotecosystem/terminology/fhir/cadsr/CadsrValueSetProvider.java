package org.hotecosystem.terminology.fhir.cadsr;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;

import java.util.*;

public class CadsrValueSetProvider implements IResourceProvider {
    HTTPRepository repository;
    public CadsrValueSetProvider() {
        String repoUrl = "http://graph.hotecosystem.org:7200/repositories/crfcde";
        repository = new HTTPRepository(repoUrl);
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return ValueSet.class;
    }

    @Read
    public ValueSet read(@IdParam IdType theId) {
        String id = theId.getIdPart().trim();
        ContactDetail contact = new ContactDetail();
        contact.setName("National Cancer Institute");
        String uri = "http://cbiit.nci.nih.gov/caDSR#" + id;



        ValueSet vs = new ValueSet();
        vs.setUrl(uri)
                .setContact(Arrays.asList(contact))
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .setPublisher("National Cancer Institute");
        RepositoryConnection connection = repository.getConnection();

        String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX cmdr: <http://cbiit.nci.nih.gov/caDSR#>\n" +
                "PREFIX isomdr: <http://www.iso.org/11179/MDR#>\n" +
                "PREFIX ncit: <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "select DISTINCT ?s ?value ?label where {?s cmdr:publicId \"" + id + "\" .\n" +
                "    ?s isomdr:permitted_value ?pv .\n" +
                "    ?pv isomdr:value ?value .\n" +
                "    ?pv rdfs:label ?label.\n" +
                "}";

        try {
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
            TupleQueryResult tupleQueryResult = query.evaluate();
            Set<String> codes = new HashSet();
            ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
            expansion.setIdentifier(uri)
                    .setTimestamp(new Date());
            List<ValueSet.ValueSetExpansionContainsComponent> vseccs = new ArrayList<>();
            while (tupleQueryResult.hasNext()) {
                BindingSet bindings = tupleQueryResult.next();
                ValueSet.ValueSetExpansionContainsComponent vsecc = new ValueSet.ValueSetExpansionContainsComponent();
                vsecc.setSystem(bindings.getValue("s").stringValue())
                        .setCode(bindings.getValue("value").stringValue())
                        .setDisplay(bindings.getValue("label").stringValue());
                vseccs.add(vsecc);
            }
            expansion.setTotal(vseccs.size());
            expansion.setContains(vseccs);
            tupleQueryResult.close();
            vs.setExpansion(expansion);
        } finally {
            connection.close();
        }
        return vs;
    }
}
