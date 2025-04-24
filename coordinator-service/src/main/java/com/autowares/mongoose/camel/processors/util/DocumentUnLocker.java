package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.service.LockedDocuments;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.DocumentImpl;

@Component
public class DocumentUnLocker implements Processor {

	private static Logger log = LoggerFactory.getLogger(DocumentUnLocker.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		String lockedDocumentId = exchange.getIn().getHeader("__locked_document_id__", String.class);
		
		if (lockedDocumentId == null) {
			if (exchange.getIn().getBody() instanceof Document) {
				Document document = exchange.getIn().getBody(Document.class);
				if (document != null && document.getDocumentId() != null) {
					lockedDocumentId = document.getDocumentId();
				}

			}
		}
		
		if (lockedDocumentId != null) {
			Document document = new DocumentImpl();
			document.setDocumentId(lockedDocumentId);
			if (LockedDocuments.isLocked(document)) {
				try {
					LockedDocuments.unlockDocument(document);
					log.info("Document unlocked " + document.getDocumentId());
				} catch (Exception e) {
					log.error("Unable to unlock " + document.getDocumentId() + " got: " + e.getMessage());
				}

			} else {
				log.error("Trying to unlock Document: " + document.getDocumentId() + " we dont have locked");
			}
		} else {
			log.error("Trying to unlock a: " + exchange.getIn().getBody().getClass().getSimpleName()
					+ " which isnt a Document");
		}
	}
}
