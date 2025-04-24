package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.supplychain.commands.GeneratePOClient;
import com.autowares.supplychain.model.PurchaseOrderNumber;

@Component
public class GenerateSupplierPurchaseOrder implements Processor {
	
	private static GeneratePOClient generatePOClient = new GeneratePOClient();

	@Override
	public void process(Exchange exchange) throws Exception {
		
		CoordinatorOrderContext context = exchange.getIn().getBody(CoordinatorOrderContext.class);
		for (FulfillmentLocationContext sequence : context.getFulfillmentSequence()) {
			CoordinatorContext nonStockContext = sequence.getNonStockContext();
			if (PurchaseOrderType.SpecialOrder.equals(nonStockContext.getOrderType())) {
				if (context.getCustomerDocumentId() != null) {
						String customerDocumentId = context.getCustomerDocumentId();
						PurchaseOrderNumber poNumberResponse = generatePOClient.generatePurchaseOrderNumber(customerDocumentId);
						nonStockContext.setDocumentId(poNumberResponse.getPurchaseOrderNumber());
				}
			}
			nonStockContext.getDocumentId();
		}
	}
}
