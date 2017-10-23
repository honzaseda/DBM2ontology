package DBM2ontology;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.apache.xerces.util.URI;

import java.io.*;

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
            ontModel.setNsPrefixes(model.getNsPrefixMap());
            model.read(new FileInputStream(inputFileName), null, "TTL");

            System.out.println("Generating classes");
            ResIterator resIterator = model.listResourcesWithProperty(nodeType);
            while (resIterator.hasNext()) {
                ontModel.createClass(resIterator.next().getProperty(nodeType).getObject().toString());
            }
            System.out.println("Classes generated");
            StmtIterator stmtIterator = model.listStatements();
            System.out.println("Generating objects and their properties");
            while (stmtIterator.hasNext()) {
                Statement stmt = stmtIterator.nextStatement();
                RDFNode object = stmt.getObject();
                if (object instanceof Resource) {
                    ObjectProperty objectProperty = ontModel.createObjectProperty(stmt.getPredicate().toString());
                    objectProperty.addDomain(stmt.getSubject().getProperty(RDF.type).getObject().asResource());
                    objectProperty.addRange(getRange(object.toString()));
                } else {
                    DatatypeProperty datatypeProperty = ontModel.createDatatypeProperty(stmt.getPredicate().toString());
                    datatypeProperty.addDomain(stmt.getSubject().getProperty(RDF.type).getObject().asResource());
                    datatypeProperty.addRange(getRange(object.toString()));
                }
            }
            System.out.print("Done. ");
            FileOutputStream f = new FileOutputStream(new File(outputFileName));
            ontModel.write(f);
            System.out.println("Saved output to file.");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static Resource getRange(String objectValue){
        if (objectValue.contains(".")) {
            try {
                double dbl = Double.parseDouble(objectValue);
                return XSD.xdouble;
            } catch (Exception ex) {/* .. */}
        }

        try {
            int integer = Integer.parseInt(objectValue);
            return XSD.integer;
        } catch (Exception ex) {/* .. */}

        try {
            long lng = Long.parseLong(objectValue);
            return XSD.xlong;
        } catch (Exception ex) {/* .. */}

        try{
            if(objectValue.contains(":") ||  objectValue.contains(".") || objectValue.contains("/")) {
                URI uri = new URI(objectValue);
                return XSD.anyURI;
            }
        } catch (Exception ex){/* .. */}
        return XSD.xstring;
    }

}
