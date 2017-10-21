package DBM2ontology;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by seda on 19/10/17.
 */
public class RDFontology {
    static Property nodeType = RDF.type;
    static String classURI = "http://test/class/";

    public static void main(String[] args){
        try {
            String inputFileName = "data/20171009-eu-6.ttl";
            Model model = ModelFactory.createDefaultModel();
            model.read(new FileInputStream(inputFileName),null,"TTL");

            ResIterator resIterator = model.listResourcesWithProperty(RDF.type);

            OntModel ontModel = ModelFactory.createOntologyModel();
            ontModel.setNsPrefixes(model.getNsPrefixMap());

            while(resIterator.hasNext()){
                ontModel.createClass(resIterator.next().getProperty(RDF.type).getObject().toString());
            }

            FileOutputStream f = new FileOutputStream(new File("data/ontology.xml"));
            ontModel.write(f);

//            StmtIterator resIterator = model.listStatements();
//            while(resIterator.hasNext()){
//                Statement statement = iter.nextStatement();
//                Resource  subject   = statement.getSubject();     // get the subject
//                Property  predicate = statement.getPredicate();   // get the predicate
//                RDFNode   object    = statement.getObject();      // get the object
//
//                System.out.print(subject.toString());
//                System.out.print(" " + predicate.toString() + " ");
//                if (object instanceof Resource) {
//                    System.out.print(object.toString());
//                } else {
//                    System.out.print(" \"" + object.toString() + "\"");
//                }
//                System.out.println(" .");
//            }
            System.out.println("Blboune");


        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
