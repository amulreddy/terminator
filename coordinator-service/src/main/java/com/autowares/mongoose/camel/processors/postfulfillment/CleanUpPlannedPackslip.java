package com.autowares.mongoose.camel.processors.postfulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.SupplyChainService;

@Component
public class CleanUpPlannedPackslip implements Processor {

	@Autowired
	SupplyChainService transactionalStateManager;
	

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext document = exchange.getIn().getBody(FulfillmentLocationContext.class);
		transactionalStateManager.getClient().deleteDocument(document.getReferenceDocument());
	}
}
