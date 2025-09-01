package in.manipalhospitals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class PrettyLogger implements Processor {

    private static final ObjectWriter jsonWriter =
            new ObjectMapper().writerWithDefaultPrettyPrinter();
    private static final Logger LOG = LoggerFactory.getLogger(PrettyLogger.class);


    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);

        String prettyBody;

        try {
            if (contentType != null && contentType.toLowerCase().contains("json")) {
                // Pretty print JSON
                Object json = new ObjectMapper().readValue(body, Object.class);
                prettyBody = jsonWriter.writeValueAsString(json);
            } else if (contentType != null && contentType.toLowerCase().contains("xml")) {
                // Pretty print XML
                prettyBody = formatXml(body);
            } else {
                // Fallback: just raw string
                prettyBody = body;
            }
        } catch (Exception e) {
            prettyBody = body; // fallback if parsing fails
        }

//        exchange.getContext().getLogger().info(prettyBody);
        LOG.info(prettyBody);

    }

    private String formatXml(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }
}
