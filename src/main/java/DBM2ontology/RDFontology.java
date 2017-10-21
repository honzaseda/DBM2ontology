package DBM2ontology;

import org.apache.jena.graph.Node;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import org.apache.jena.vocabulary.RDF;

import java.io.*;
import java.util.Properties;
import java.util.Set;

/**
 * Created by seda on 19/10/17.
 */
public class RDFontology {
    static private Property nodeType = RDF.type;
    static private String inputFileName = "data/20171009-eu-6.ttl";
    static private String outputFileName = "data/ontology.xml";

    public static void main(String[] args) {
        try {

            Model model = ModelFactory.createDefaultModel();
            OntModel ontModel = ModelFactory.createOntologyModel();

            model.read(new FileInputStream(inputFileName), null, "TTL");
            ResIterator resIterator = model.listResourcesWithProperty(nodeType);

            ontModel.setNsPrefixes(model.getNsPrefixMap());

            while (resIterator.hasNext()) {
                ontModel.createClass(resIterator.next().getProperty(nodeType).getObject().toString());
            }

//            ontModel.createObjectProperty()

            StmtIterator stmtIterator = model.listStatements();
            while (stmtIterator.hasNext()) {
                Statement stmt = stmtIterator.nextStatement();
                RDFNode object = stmt.getObject();
                ResIterator propertySubject = model.listSubjectsWithProperty(stmt.getPredicate());
                if (object instanceof Resource) {
                    ObjectProperty objectProperty = ontModel.createObjectProperty(stmt.getPredicate().toString());
                    while(propertySubject.hasNext()){
                        objectProperty.addDomain(propertySubject.next().getProperty(RDF.type).getObject().asResource());
                    }
//                    System.out.println(object.toString() + " is an object type");
                } else {
                    DatatypeProperty datatypeProperty = ontModel.createDatatypeProperty(stmt.getPredicate().toString());
                    while(propertySubject.hasNext()){
                        datatypeProperty.addDomain(propertySubject.next().getProperty(RDF.type).getObject().asResource());
                    }

//                    System.out.println(object.toString() + " is an data type");
                }
            }

            Properties properties = new Properties();

            FileOutputStream f = new FileOutputStream(new File(outputFileName));
            ontModel.write(f);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
