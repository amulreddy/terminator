package com.autowares.mongoose.camel.processors.lookup;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.MoaOperationalStateManager;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.orders.model.Item;
import com.autowares.servicescommon.model.Document;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class LookupSupplyChainPurchaseOrder implements Processor {

	@Autowired
	private SupplyChainService transactionalStateManager;

	@Autowired
	private MoaOperationalStateManager operationalStateManager;

	@Override
	public void process(Exchange exchange) throws Exception {

		Object object = exchange.getIn().getBody();
		DocumentContext document = null;
		CoordinatorContext context = null;

		if (object instanceof DocumentContext) {
			document = (DocumentContext) object;
			context = document.getContext();
		} else if (object instanceof Document) {
			document = new DocumentContext((Document) object);
			if (object instanceof SupplyChainSourceDocument) {
				document.setSourceDocument((SupplyChainSourceDocument) object);
			}
		}

		if (object instanceof CoordinatorContext) {
			context = (CoordinatorContext) object;
			document.setContext(context);
		}

		if (!document.isResolved()) {
			document = transactionalStateManager.resolveDocumentContext(document);
		}

		if (document.getFulfillmentLocationContext() != null) {
			FulfillmentLocationContext fulfillmentLocationContext = document.getFulfillmentLocationContext();
			for (Availability availability : fulfillmentLocationContext.getLineItemAvailability()) {
				if (availability.getFillQuantity() > 0) {
					OperationalContext oc = operationalStateManager.getOperationalContext(fulfillmentLocationContext.getDocumentId(), availability.getLineItem().getLineNumber());
					if (oc != null) {
						availability.setOperationalContext(oc);
						Optional<OperationalItem> optionalItem = oc.getItems().stream().findAny();
						if (optionalItem.isPresent()) {
							Object source = optionalItem.get().getSource();
							if (source instanceof Item) {
								availability.setMoaOrderDetail((Item) source);
							}
						}
					}
				}
			}
		}

		exchange.getIn().setBody(document);
	}

}