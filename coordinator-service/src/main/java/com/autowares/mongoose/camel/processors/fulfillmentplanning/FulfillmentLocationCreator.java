package com.autowares.mongoose.camel.processors.fulfillmentplanning;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.SystemType;

@Component
public class FulfillmentLocationCreator implements Processor {

	private static Logger log = LoggerFactory.getLogger(FulfillmentLocationCreator.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		for (LineItemContext lineItem : context.getLineItems()) {

			if (!lineItem.getInvalid()) {
				com.autowares.apis.partservice.Part part = lineItem.getPart();

				if (part != null && part.getAvailability() != null) {
					for (PartAvailability availability : part.getAvailability()) {
						if (availability.getBuildingCode() != null && !"XXX".equals(availability.getBuildingCode())) {
								//TODO should we create these if there is 0 on hand?
								createOrUpdateFulfillmentLocationContext(availability.getBuildingCode(), lineItem,
										availability, exchange);
						}
					}
				} else {
					String message = "Unable to get part availability!";
					log.error(message);
					lineItem.updateOrderLog(message);
				}
			}
		}
		if (context.getFulfillmentSequence() == null || context.getFulfillmentSequence().isEmpty()) {
			log.info("Unable to determine any availability");
			throw new ContinueProcessingException("Unable to determine any availability");
		}
	}

	private void createOrUpdateFulfillmentLocationContext(String location, LineItemContext request,
			PartAvailability partAvailability, Exchange exchange) {

		if (partAvailability != null) {
			FulfillmentLocationContext fulfillmentLocation = null;
			Optional<FulfillmentLocationContext> optionalFillContext = request.getContext().getFulfillmentSequence()
					.stream().filter(i -> location.equals(i.getLocation())).findAny();
			if (optionalFillContext.isPresent()) {
				fulfillmentLocation = optionalFillContext.get();
			} else {
				fulfillmentLocation = new FulfillmentLocationContext(request.getContext(), location);
				// TODO not great but currently we create these from availability
				if (partAvailability.getWarehouseNumber() == 0) {
					fulfillmentLocation.setLocationType(LocationType.Store);
					fulfillmentLocation.setSystemType(SystemType.CounterWorks);
				} else if (partAvailability.getWarehouseNumber() == 6) {
					fulfillmentLocation.setLocationType(LocationType.Vendor);
					fulfillmentLocation.setSystemType(SystemType.MotorState);
					FulfillmentOptions fulfillmentOptions = request.getContext().getFulfillmentOptions();
					fulfillmentOptions.getExcludedLocations().add("PER");
					exchange.getIn().setHeader("stockedInWarehouse", true);
				} else if (partAvailability.getWarehouseNumber() == 10) {
					fulfillmentLocation.setLocationType(LocationType.Vendor);
				} else {
					fulfillmentLocation.setLocationType(LocationType.Warehouse);
					fulfillmentLocation.setSystemType(SystemType.AwiWarehouse);
					exchange.getIn().setHeader("stockedInWarehouse", true);
				}
			}

			BusinessDetail businessDetail = request.getContext().getBusinessContext().getBusinessDetail();
			if (businessDetail != null) {
				if (businessDetail.getServicingWarehouse() != null) {
					if (fulfillmentLocation.getLocation()
							.equals(businessDetail.getServicingWarehouse().getBuildingMnemonic())) {
						fulfillmentLocation.setServicingLocation(true);
					}
				}
			}

			for (FulfillmentContext fulfillment : request.getFulfillmentDetails()) {
				Integer qoh = fulfillment.getQuantityOnHand();
				if (qoh == null) {
					qoh = 0;
				}
				qoh += partAvailability.getQuantityOnHand();
				fulfillment.setQuantityOnHand(qoh);
			}

			Optional<Availability> optionalAvailability = fulfillmentLocation.getLineItemAvailability().stream()
					.filter(i -> i.getLineItem().getLineNumber().equals(request.getLineNumber())).findAny();
			if (!optionalAvailability.isPresent()) {
				new Availability(request, fulfillmentLocation);
			}

		}
	}

}
