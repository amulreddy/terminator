package com.autowares.mongoose.camel.processors.fulfillmentplanning;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.SequenceGeneratorService;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;

@Component
public class EdiLocationCreator implements Processor {
	
	@Autowired
	private SequenceGeneratorService sequenceGeneratorService ; 

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		if (context.getDocumentId() == null) {
			context.setDocumentId(sequenceGeneratorService.getNextSequence());
		}
		FulfillmentOptions fulfillmentOptions = context.getFulfillmentOptions();
		fulfillmentOptions.setAllowSupplierInventory(true);
		fulfillmentOptions.setAllowAWILocations(false);
		fulfillmentOptions.setPreferSupplierInventory(true);
		fulfillmentOptions.getExcludedLocations().add("PER");
		for( FulfillmentLocationContext fulfillmentLocation : context.getFulfillmentSequence()) {
			if (LocationType.Vendor.equals(fulfillmentLocation.getLocationType())) {
				createOrUpdateFulfillmentLocationContext(fulfillmentLocation);
			}
		}

	}

	private void createOrUpdateFulfillmentLocationContext(FulfillmentLocationContext fulfillmentLocation) {
//		fulfillmentLocation.setSystemType(SystemType.GCommerceEDI);
		fulfillmentLocation.setTransactionStage(TransactionStage.Open);
		fulfillmentLocation.setTransactionStatus(TransactionStatus.Processing);
		fulfillmentLocation.setQuoted(true);
//		fulfillmentLocation.getNonStockContext().setSystemType(SystemType.GCommerceEDI);
		for ( Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			availability.setFillQuantity(availability.getLineItem().getQuantity());
		}
	}
}
