package com.autowares.mongoose.camel.processors.fulfillmentplanning;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.FulfillmentLocationContext;

@Component
public class PartAvailabilityProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {

		FulfillmentLocationContext fulfillmentLocation = exchange.getIn().getBody(FulfillmentLocationContext.class);

		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			Part part = availability.getLineItem().getPart();
			if (part != null && part.getAvailability() != null) {
				Optional<PartAvailability> optionalPartAvailability = part.getAvailability().stream()
						.filter(p -> p.getBuildingCode().equals(fulfillmentLocation.getLocation())).findAny();
				if (optionalPartAvailability.isPresent()) {
					PartAvailability partAvailability = optionalPartAvailability.get();
					availability.setPartAvailability(partAvailability);
					availability.setQuantityOnHand(partAvailability.getQuantityOnHand());
				}
			}
		}
	}

}
