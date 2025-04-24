package com.autowares.mongoose.vipersoap;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.time.Duration;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.client.DiscoverService;

@DiscoverService(name = "soap", path = "/soap/Mongoose")
public class ViperMongooseUpdateClient extends BaseResillience4JClient {
	
	private static Logger log = LoggerFactory.getLogger(ViperMongooseUpdateClient.class);

    public ViperMongooseUpdateClient() {
    }
    
    public String putawayUpdate(PutawayUpdate request) {
    	Supplier<String> supplier = ()->this.putawayUpdateUnprotected(request); 
    	return decorate(supplier, Duration.ofSeconds(5));
    }

    private String putawayUpdateUnprotected(PutawayUpdate request) {
    	UriBuilder uriBuilder = getUriBuilder();
    	try {
        JAXBContext requestCtx = JAXBContext.newInstance(PutawayUpdate.class);
        JAXBContext responseCtx = JAXBContext.newInstance(PutawayUpdateResponse.class);

        Marshaller marshaller = requestCtx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream requestData = new ByteArrayOutputStream();
        marshaller.marshal(request, requestData);
        log.info("Request: " + new String(requestData.toByteArray()));
        
        WebServiceTemplate ws = new WebServiceTemplate();
        ws.setDefaultUri(uriBuilder.build().toString());

        StreamSource source = new StreamSource(new StringReader(new String(requestData.toByteArray())));
        ByteArrayOutputStream bin = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(bin);
        ws.sendSourceAndReceiveToResult(source, result);

        log.info("Response: " + new String(bin.toByteArray()));
        Unmarshaller unmarshaller = responseCtx.createUnmarshaller();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(bin.toByteArray()));
        doc.createElementNS("http://www.w3.org/2001/XMLSchema", "xsd:string");
        cleanResponse(doc.getFirstChild());

        PutawayUpdateResponse response = (PutawayUpdateResponse) unmarshaller.unmarshal(doc.getFirstChild());
        return response.getResult();
    	} catch (Exception e) {
    		log.error("Error updating putaway file", e);
    		return null;
    	}
    }

    private void cleanResponse(Node node) {
        if (node != null && node.getAttributes() != null) {
            for (int a = 0; a < node.getAttributes().getLength(); a++) {
                Node item = node.getAttributes().item(a);
                if(item.getPrefix().equals("xsi")) {
                    node.getAttributes().removeNamedItem(item.getNodeName());
                }
            }
        }
        if (node != null && node.getChildNodes() != null) {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                cleanResponse(node.getChildNodes().item(i));
            }
        }
    }
    

}
