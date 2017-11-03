package DBM2ontology;

import org.apache.jena.ext.com.google.common.primitives.Doubles;
import org.apache.jena.ext.com.google.common.primitives.Ints;
import org.apache.jena.ext.com.google.common.primitives.Longs;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.apache.xerces.util.URI;

import java.io.*;
import java.util.*;

/**
 * Created by Seda on 19/10/17.
 */
public class RDFontology {
    static private Property nodeType = RDF.type;
    static private String inputFileName;
    static private String outputFileName;

    static private Model model;
    static private OntModel ontModel;

    public static void main(String[] args) {
        if(!validArgs(args)){
            return;
        }

        model = ModelFactory.createDefaultModel();
        ontModel = ModelFactory.createOntologyModel();
        ontModel.setNsPrefixes(model.getNsPrefixMap());

        model.read(inputFileName);

        System.out.println("Creating classes...");
        createClassesFromResources(model.listResourcesWithProperty(nodeType));

        System.out.println("Creating Properties...");
        createPropertiesFromStatements(model.listStatements());

        System.out.print("Done. ");
        writeOntology(outputFileName);
    }

    private static boolean validArgs(String[] args){
        if(args.length < 1){
            System.out.println("You must specify an input file name as argument.");
            return false;
        }
        else{
            inputFileName = args[0];
        }

        if(args.length > 1){
            outputFileName = args[1];
        }
        else{
            outputFileName = "data/ontology.owl";
        }
        return true;
    }

    private static void writeOntology(String fileName) {
        try {
            FileOutputStream f = new FileOutputStream(new File(fileName));
            ontModel.write(f);
            System.out.println("Ontology saved to file.");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static void createClassesFromResources(ResIterator resIterator) {
        while (resIterator.hasNext()) {
            ontModel.createClass(resIterator.next().getProperty(nodeType).getObject().toString());
        }
    }

    private static void createPropertiesFromStatements(StmtIterator stmtIterator) {
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.next();
            RDFNode object = stmt.getObject();
            if (object instanceof Resource) {
                ObjectProperty objectProperty = ontModel.createObjectProperty(stmt.getPredicate().toString(), checkPropertyFunctionality(stmt.getPredicate()));
                objectProperty.addDomain(stmt.getSubject().getProperty(nodeType).getObject().asResource());
                objectProperty.setRange(getObjectRange(stmt.getPredicate().getLocalName()));
            } else {
                DatatypeProperty datatypeProperty = ontModel.createDatatypeProperty(stmt.getPredicate().toString(), checkPropertyFunctionality(stmt.getPredicate()));
                datatypeProperty.addDomain(stmt.getSubject().getProperty(nodeType).getObject().asResource());
                datatypeProperty.setRange(getDataRange(object.toString()));
            }
        }
    }

    //    A property can be functional if an instance of the domain is associated with at most one instance of range.
    private static boolean checkPropertyFunctionality(Property property) {
        RDFNode o = null;
        StmtIterator stmtIterator = model.listStatements(null, property, o);
        Map<Resource, Integer> map = new HashMap<>();

        while (stmtIterator.hasNext()) {
            Resource res = stmtIterator.next().getSubject();
            if (map.get(res) == null) {
                map.put(res, 1);
            } else {
                map.replace(res, map.get(res) + 1);
            }
        }

        for (Resource r : map.keySet()) {
            if (map.get(r) != 1) {
                return false;
            }
        }
        return true;
    }

    private static Resource getObjectRange(String objectValue) {
        ExtendedIterator<OntClass> ontClasses = ontModel.listClasses();
        while (ontClasses.hasNext()) {
            OntClass ontClass = ontClasses.next();
            if (ontClass.getLocalName().toLowerCase().equals(objectValue.toLowerCase())) {
                return ontClass;
            }
        }
        return XSD.anyURI;
    }

    private static Resource getDataRange(String objectValue) {
        if (objectValue.contains(".")) {
            if (Doubles.tryParse(objectValue) != null) {
                return XSD.xdouble;
            }
        }

        if (Ints.tryParse(objectValue) != null) {
            return XSD.integer;
        }

        if (Longs.tryParse(objectValue) != null) {
            return XSD.xlong;
        }

        return XSD.xstring;
    }

    private static Resource oldGetDataRange(String objectValue) {
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

        try {
            if (objectValue.contains(":") || objectValue.contains(".") || objectValue.contains("/")) {
                URI uri = new URI(objectValue);
                return XSD.anyURI;
            }
        } catch (Exception ex) {/* .. */}
        return XSD.xstring;
    }
}
