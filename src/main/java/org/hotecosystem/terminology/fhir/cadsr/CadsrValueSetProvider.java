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
import java.util.stream.Collectors;

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

        String mdq = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX cmdr: <http://cbiit.nci.nih.gov/caDSR#>\n" +
                "PREFIX isomdr: <http://www.iso.org/11179/MDR#>\n" +
                "PREFIX ncit: <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "\n" +
                "select DISTINCT *  where {?url cmdr:publicId \"" + id + "\" .\n" +
                "    ?url cmdr:short_name ?name ;\n" +
                "         cmdr:publicId ?identifier1 ; \n" +
                "         isomdr:VD_publicId ?identifier2 ;\n" +
                "         isomdr:VD_version ?version ;\n" +
                "         cmdr:short_name ?name ;\n" +
                "         rdfs:label ?title ;\n" +
                "         skos:definition ?description .\n" +
                "}\n";


        ValueSet vs = new ValueSet();
        vs.setUrl(uri)
                .setContact(Arrays.asList(contact))
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .setPublisher("National Cancer Institute")
                .setDate(new Date());
        RepositoryConnection connection = repository.getConnection();

        String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX cmdr: <http://cbiit.nci.nih.gov/caDSR#>\n" +
                "PREFIX isomdr: <http://www.iso.org/11179/MDR#>\n" +
                "PREFIX ncit: <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "select DISTINCT ?s ?value ?label ?concept_type ?concept_code ?concept_order where { \n" +
                "    ?s cmdr:publicId \"" + id + "\" .\n" +
                "    ?s isomdr:permitted_value ?pv .\n" +
                "    ?pv isomdr:value ?value .\n" +
                "    ?pv rdfs:label ?label. \n" +
                "    { \n" +
                "       ?pv cmdr:has_concept ?c . \n" +
                "       ?c  cmdr:main_concept ?concept_code ; \n" +
                "           cmdr:display_order ?order  . \n" +
                "       BIND(cmdr:main_concept as $concept_type) \n" +
                "    } \n" +
                "    UNION \n" +
                "    {	\n" +
                "       ?pv cmdr:has_concept ?c . \n" +
                "       ?c  cmdr:minor_concept ?concept_code ; \n" +
                "           cmdr:display_order ?concept_order . \n" +
                "       BIND(cmdr:minor_concept as $concept_type) \n" +
                "    } \n" +
                "}";

        try {
            TupleQuery mdquery = connection.prepareTupleQuery(QueryLanguage.SPARQL, mdq);
            TupleQueryResult mdResult = mdquery.evaluate();
            if (mdResult.hasNext()) {
                BindingSet bindings = mdResult.next();
                vs.setTitle(bindings.getValue("title").stringValue())
                        .setName(bindings.getValue("name").stringValue())
                        .setDescription(bindings.getValue("description").stringValue())
                        .setVersion(bindings.getValue("version").stringValue());
                List<Identifier> identifiers = new ArrayList<>();
                identifiers.add(new Identifier()
                        .setSystem("http://www.iso.org/11179/MDR#VD_publicId")
                        .setValue(bindings.getValue("identifier2").stringValue()));
                identifiers.add(new Identifier()
                        .setSystem("http://cbiit.nci.nih.gov/caDSR#")
                        .setValue(bindings.getValue("identifier1").stringValue()));
                vs.setIdentifier(identifiers);
            }

            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
            TupleQueryResult tupleQueryResult = query.evaluate();
            Set<String> codes = new HashSet();
            ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
            expansion.setIdentifier(uri)
                    .setTimestamp(new Date());
            Map<String, ValueSet.ValueSetExpansionContainsComponent> topContains = new HashMap<>();
            while (tupleQueryResult.hasNext()) {
                BindingSet bindings = tupleQueryResult.next();
                String code = bindings.getValue("value").stringValue();
                ValueSet.ValueSetExpansionContainsComponent topContain = topContains.getOrDefault(code,
                        new ValueSet.ValueSetExpansionContainsComponent());
                topContain.setSystem(bindings.getValue("s").stringValue())
                        .setCode(code)
                        .setDisplay(bindings.getValue("label").stringValue());
                ValueSet.ValueSetExpansionContainsComponent childContain = new ValueSet.ValueSetExpansionContainsComponent();
                String concept = bindings.getValue("concept_code").stringValue();
                int bp = concept.lastIndexOf("#");
                String prefix = concept.substring(0, bp+1);
                String conceptCode = concept.substring(bp+1);
                childContain.setSystem(prefix).setCode(conceptCode);
                Extension extension = new Extension("http://hotecosystem.org/fhir/StructureDefinition/cadsr-concept-type");
                extension.setValue(new StringType(bindings.getValue("concept_type").stringValue()));
                childContain.addExtension(extension);
                topContain.getContains().add(childContain);
                topContains.put(code, topContain);
            }
            expansion.setTotal(topContains.size());
            expansion.setContains(topContains.values().stream().collect(Collectors.toList()));
            tupleQueryResult.close();
            vs.setExpansion(expansion);
        } finally {
            connection.close();
        }
        return vs;
    }
}
