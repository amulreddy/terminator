package com.autowares.mongoose.camel.processors.postfulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderFillContextTypeConverter;
import com.autowares.mongoose.camel.components.QuoteContextTypeConverter;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class SaveSourceDocument implements Processor {

	@Autowired
	private SupplyChainService supplyChainService;

	private static Logger log = LoggerFactory.getLogger(SaveSourceDocument.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		Object object = exchange.getIn().getBody(Object.class);
		SupplyChainSourceDocument sourceDocument = null;
		CoordinatorContext context = null;
		DocumentContext documentContext = null;
		if (object != null) {

			if (object instanceof DocumentContext) {
				documentContext = (DocumentContext) object;
				if (SourceDocumentType.PackSlip.equals(documentContext.getSourceDocument().getSourceDocumentType())) {
					object = documentContext.getFulfillmentLocationContext();
				} else {
					// TODO: Verify this is the right thing to do.
					object = documentContext.getContext();
				}
			}

			if (object instanceof FulfillmentLocationContext) {
				FulfillmentLocationContext fulfillmentLocationContext = (FulfillmentLocationContext) object;
				if (!(fulfillmentLocationContext.getOrder() instanceof CoordinatorOrderContext)) {
					//TODO -- First pass - CoordinatorContextImpl.
					//TODO -- Second Pass - FulfillmentLocationContext - Order is not CoordinatorOrderContext.
					log.info("Save shipment estimate.");
					sourceDocument = OrderFillContextTypeConverter
							.orderFillContextToShipmentEstimate(fulfillmentLocationContext);
				} else {
					sourceDocument = OrderFillContextTypeConverter
							.orderFillContextToSupplyChainPackslip(fulfillmentLocationContext);
				}
			} else if (object instanceof CoordinatorContext) {
				context = (CoordinatorContext) object;
				sourceDocument = QuoteContextTypeConverter.coordinatorContextToSupplyChainSourceDocument(context);
			} else if (object instanceof SupplyChainSourceDocument) {
				sourceDocument = (SupplyChainSourceDocument) object;
			}

		}

		if (sourceDocument != null) {
			sourceDocument = (SupplyChainSourceDocument) supplyChainService.persist(sourceDocument);
			if (context != null) {
				context.setReferenceDocument(sourceDocument);
				context.setDocumentId(sourceDocument.getDocumentId());
			}

			if (documentContext != null) {
				documentContext.setSourceDocument(sourceDocument);
			}

			log.info("Document ID = " + sourceDocument.getDocumentId());
		} else {
			log.warn("Unable to save null source document");
		}
	}

}
