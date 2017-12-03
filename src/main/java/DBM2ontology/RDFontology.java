package DBM2ontology;

import org.apache.jena.ext.com.google.common.primitives.Doubles;
import org.apache.jena.ext.com.google.common.primitives.Ints;
import org.apache.jena.ext.com.google.common.primitives.Longs;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import java.io.*;
import java.util.*;

/**
 * Created by Seda on 19/10/17.
 */
public class RDFontology {
    static private Property nodeType = RDF.type;
    static private String inputFileName;
    static private String outputFileName;
    static private int individualsRatio = 20;

    static private Model model;
    static private OntModel ontModel;

    /**
     * Main function, reads input file, initializes models
     *
     * @param args Arguments array
     */
    public static void main(String[] args) {
        if (!validArgs(args)) {
            return;
        }

        model = ModelFactory.createDefaultModel();
        ontModel = ModelFactory.createOntologyModel();

        model.read(inputFileName);
        ontModel.setNsPrefixes(model.getNsPrefixMap());

        createClassesFromResources(model.listResourcesWithProperty(nodeType));

        createPropertiesFromStatements(model.listStatements());

        createIndividuals(individualsRatio);
        System.out.println("Done analyzing input file.");
        writeOntology(outputFileName);
    }

    /**
     * Checks validity of input arguments
     *
     * @param args Input arguments
     * @return Boolean value of test result
     */
    private static boolean validArgs(String[] args) {
//        parameters example
//        "data/ibds.ttl" "data/ibdsontology.xml" 5
//        "data/20171009-eu-6.ttl" "data/euontology.xml" 20

        if (args.length < 1) {
            System.out.println("You must specify an input file name as argument.");
            return false;
        } else {
            inputFileName = args[0];
        }

        if (args.length > 1) {
            outputFileName = args[1];
        } else {
            outputFileName = "Ontology.owl";
        }
        if (args.length > 2 && Ints.tryParse(args[2]) != null) {
            individualsRatio = Integer.parseInt(args[2]);
            if (individualsRatio < 1){
                System.out.println("Ratio parameter should be an integer bigger than 0");
                return false;
            }
        }
        return true;
    }

    /**
     * Writes created ontology model into an output file
     *
     * @param fileName Path to file
     */
    private static void writeOntology(String fileName) {
        try {
            FileOutputStream f = new FileOutputStream(new File(fileName));
            ontModel.write(f);
            System.out.println("Ontology saved to file.");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Adds all found classes into ontology model
     *
     * @param resIterator Iterator of all resources taken from model
     */
    private static void createClassesFromResources(ResIterator resIterator) {
        System.out.print("Creating classes... ");
        int numOfResources = 0, numOfClasses = 0;
        while (resIterator.hasNext()) {
            RDFNode node = resIterator.next().getProperty(nodeType).getObject();
            OntClass ontClass = ontModel.createClass(node.toString());
            ontClass.addProperty(RDFS.label, node.asResource().getLocalName());
            numOfResources++;
        }
        ExtendedIterator c = ontModel.listClasses();
        while(c.hasNext()){
            numOfClasses++;
            c.next();
        }
        System.out.println("Created " + numOfClasses + " distinct classes from total of " + numOfResources + " resources");
    }

    /**
     * Iterates all model statements, adds all existing properties into ontology model, also sets the property type, range and domains
     *
     * @param stmtIterator Iterator of all statements found in model
     */
    private static void createPropertiesFromStatements(StmtIterator stmtIterator) {
        System.out.print("Creating Properties... ");
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.next();
            RDFNode object = stmt.getObject();
            if (stmt.getPredicate() != null && !stmt.getPredicate().toString().equals(nodeType.toString())) {
                if (object instanceof Resource) {
                    ObjectProperty objectProperty = ontModel.createObjectProperty(stmt.getPredicate().toString(), checkPropertyFunctionality(stmt.getPredicate()));
                    if (stmt.getSubject().getProperty(nodeType) != null) {
                        objectProperty.addDomain(stmt.getSubject().getProperty(nodeType).getObject().asResource());
                    }
                    objectProperty.setRange(getObjectRange(stmt.getPredicate().getLocalName()));
                    objectProperty.addProperty(RDFS.label, stmt.getPredicate().getLocalName());
                } else {
                    DatatypeProperty datatypeProperty = ontModel.createDatatypeProperty(stmt.getPredicate().toString(), checkPropertyFunctionality(stmt.getPredicate()));
                    if (stmt.getSubject().getProperty(nodeType) != null) {
                        datatypeProperty.addDomain(stmt.getSubject().getProperty(nodeType).getObject().asResource());
                    }
                    datatypeProperty.setRange(getDataRange(object.toString()));
                    datatypeProperty.addProperty(RDFS.label, stmt.getPredicate().getLocalName());
                }
            }
        }
    }

    /**
     * Checks if given property of model is functional.
     * Note: A property can be functional if an instance of the domain is associated with at most one instance of range
     *
     * @param property Checked property
     * @return Boolean value, true if property is functional
     */
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

    /**
     * Gets range of an Object property
     *
     * @param objectValue String name of property
     * @return Resource of property range
     */
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

    /**
     * Predicts range of a DataType property
     *
     * @param objectValue String name of property
     * @return Resource of property range
     */
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

    /**
     * Creates individuals of classes
     */
    private static void createIndividuals(int ratio) {
        int numOfDataProps = 0, numOfObjectProps = 0, numOfIndividuals = 0;
        ExtendedIterator<DatatypeProperty> d = ontModel.listDatatypeProperties();
        ExtendedIterator<ObjectProperty> o = ontModel.listObjectProperties();
        List<OntProperty> list = new ArrayList<>();
        while (o.hasNext()) {
            numOfObjectProps++;
            list.add(o.next());
        }
        while (d.hasNext()) {
            numOfDataProps++;
            list.add(d.next());
        }

        System.out.println("Created total of " + numOfObjectProps + " Object properties and " + numOfDataProps + " DataType properties");
        System.out.println("Creating Individuals...");

        for (OntProperty property : list) {
            RDFNode nullNode = null;
            StmtIterator stmtIterator = model.listStatements(null, property, nullNode);
            Set<RDFNode> set = new HashSet<>();
            int numOfInstances = 0;
            while (stmtIterator.hasNext()) {
                numOfInstances++;
                set.add(stmtIterator.next().getObject());
            }

            if (numOfInstances / set.size() > ratio) {
                ExtendedIterator<? extends OntResource> domainsIter = property.listDomain();
                List<OntResource> domains = new ArrayList<>();
                while (domainsIter.hasNext()) {
                    domains.add(domainsIter.next());
                }
                for (OntResource ontClass : domains) {
                    for (RDFNode node : set) {
                        if(!node.toString().equals("")) {
                            if (node.isResource()) {
                                Resource res = ontModel.createResource(node.toString());
                                Individual in = ontModel.createIndividual(ontClass.toString() + "-" + res.getLocalName(), OWL2.NamedIndividual);
                                in.addProperty(RDFS.label, res.getLocalName());
                            } else {
                                Individual in = ontModel.createIndividual(ontClass.toString() + "-" + node.toString(), OWL2.NamedIndividual);
                                in.addProperty(RDFS.label, node.toString());
                            }
                        }
                    }
                }
            }
        }
//        ExtendedIterator<Individual> i = ontModel.listIndividuals();
//        while(i.hasNext()){
//            numOfIndividuals++;
//            i.next();
//        }
//        System.out.println("Found " + numOfIndividuals + " class individuals");
    }
}