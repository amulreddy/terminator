package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.LockedDocumentException;
import com.autowares.mongoose.service.LockedDocuments;
import com.autowares.servicescommon.model.Document;

@Component
public class DocumentLocker implements Processor {

	Logger log = LoggerFactory.getLogger(DocumentLocker.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		Document context = exchange.getIn().getBody(Document.class);
		if (LockedDocuments.isLocked(context)) {
			log.info("Document already locked by us, stopped processing " + context.getDocumentId() + " for "
					+ LockedDocuments.instanceId);
			exchange.setRouteStop(true);
			return;
		}
		if (context.getDocumentId() != null) {
			try {
				LockedDocuments.lockDocument(context);
				exchange.getIn().setHeader("__locked_document_id__", context.getDocumentId());
			} catch (Exception e) {
				throw new LockedDocumentException("Unable to process " + context.getDocumentId() + " is locked", e);
			}
			log.info("Achieved locked state " + context.getDocumentId() + " for " + LockedDocuments.instanceId);
			return;
		}
		throw new AbortProcessingException("Unable lock document");
	}

}
