package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RDFUtils {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.RDFUtils");

    public static final Model parse(String data, String contentType, String baseUri) throws IOException {
        RDFFormat dataFormat = null;
        switch (contentType) {
            case "text/turtle":
                dataFormat = RDFFormat.TURTLE;
                break;
            case "text/n-triples":
                dataFormat = RDFFormat.NTRIPLES;
                break;
            case "text/plain":
                dataFormat = RDFFormat.NTRIPLES;
                break;
        }
        return Rio.parse(new StringReader(data), baseUri, dataFormat);
    }

//    public static final String getValue(String data, String contentType, String subject, String predicate) throws IOException {
//        Model model = parse(data, contentType);
//        return model.filter(Values.iri(subject), Values.iri(predicate), null).objects().toArray(new Value[0])[0].stringValue();
//    }

    public static final List<String> turtleToTripleArray(String data, String baseUri) throws IOException {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static final List<String> triplesToTripleArray(String data, String baseUri) throws IOException {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.NTRIPLES);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static final List<String> jsonLdToTripleArray(String data, String baseUri) throws IOException {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.JSONLD);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static final String turtleToJsonLd(String data, String baseUri) throws IOException {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.JSONLD);
        return sw.toString();
    }

    public static final Boolean isTurtle(String data, String baseUri) {
        try {
            Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
            return model != null && model.size() != 0;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            logger.debug("Input is not in Turtle format", e);
            return false;
        }
    }

    public static final Boolean isJsonLD(String data, String baseUri) {
        try {
            Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.JSONLD);
            return model != null && model.size() != 0;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            logger.debug("Input is not in JSON-LD format", e);
            return false;
        }
    }

    public static final Boolean isNTriples(String data, String baseUri) {
        try {
            Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.NTRIPLES);
            return model != null && model.size() != 0;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            logger.debug("Input is not in N-Triples format", e);
            return false;
        }
    }

    public static final String getAclLink(Map<String, Object> headers) {
        if (headers == null) return null;
        List<String> links = (List<String>) headers.get("Link");
        if (links.size() == 0) return null;
        Optional<String> aclLink = links.stream().filter(link -> link.contains("; rel=\"acl\"")).findFirst();
        if (aclLink.isEmpty()) return null;
        return aclLink.get().split("[<>]")[1];
    }
}
