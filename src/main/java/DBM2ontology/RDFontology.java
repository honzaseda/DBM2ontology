package DBM2ontology;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import org.apache.jena.vocabulary.RDF;

import java.io.*;

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
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
