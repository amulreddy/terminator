package com.autowares.mongoose.camel.processors.lookup;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.UnimplementedException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.DocumentModification;
import com.autowares.servicescommon.model.LineItemModification;

@Component
public class ResolveLineItemDocumentReference implements Processor {

	Logger log = LoggerFactory.getLogger(ResolveLineItemDocumentReference.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		log.info("Resolve Line Item Document References");
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		Map<String, List<LineItemContext>> supplierMap = context.getLineItems().stream()
				.filter(i -> i.getDocumentReference() != null)
				.collect(Collectors.groupingBy(LineItemContext::getDocumentReference));
		
		if (supplierMap.keySet().size() > 1) {
			throw new UnimplementedException("Cant deal with multiple documentReferences at this time");
		}

		for (Entry<String, List<LineItemContext>> key : supplierMap.entrySet()) {
			String documentId = key.getKey();
			List<LineItemContext> lines = supplierMap.get(documentId);
			DocumentModification dm = new DocumentModification();
			dm.setAction("quote");
			dm.setDocumentId(documentId);
			for (LineItemContext l : lines) {
				LineItemModification line = new LineItemModification(l);
				dm.getLineItems().add(line);
			}
			DocumentContext dc = new DocumentContext(dm);
			dc.setContext(context);
			exchange.getIn().setBody(dc);
		}

	}
}
