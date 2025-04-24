package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.xmlgateway.edi.EdiSourceDocument;
import com.autowares.xmlgateway.edi.base.EdiDocument;

@Component
public class FileToEDI implements Processor {
	
	
	@Override
	public void process(Exchange exchange) throws Exception {
		String edi = exchange.getIn().getBody(String.class);
		EdiDocument doc = new EdiDocument(edi, null, null);
		EdiSourceDocument sd = EdiSourceDocument.builder().fromEdiDocument(doc).build();
		PrettyPrint.print(sd);
	}

}
