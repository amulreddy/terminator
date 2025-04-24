package com.autowares.mongoose.camel.processors.fulfillmentplanning;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.processors.postfulfillment.PickupWarehouseVendorIntegration;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.criteria.PkCriteria;

@Component
@Deprecated
public class OrderFulfillmentPlanning implements Processor {
	
	@Autowired
	PickupWarehouseVendorIntegration pickupWarehouseVendorIntegration;

	private static Logger log = LoggerFactory.getLogger(OrderFulfillmentPlanning.class);

	/*
	 * Per line item, consider the source scores, to create the fulfillment plan
	 * 
	 * 0. Given no availability, alternate sourcing? 1. Given full/partial
	 * availability in one location, use that location 2. Given full availability in
	 * multiple locations, if express use the logistical source scoring falling back
	 * to load balancing if tied if standard, logistical weighted less than load 3.
	 * Given partial availability in multiple locations, if express, logistical if
	 * standard, try and group to the smallest number of sources and use the
	 * logistical source scoring to pick them
	 */

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext orderContext = exchange.getIn().getBody(CoordinatorContext.class);
		LinkedHashSet<FulfillmentLocationContext> fulfillmentSequence = orderContext.getFulfillmentSequence();
		Boolean consolidationEnabled = true;
		for (LineItemContext orderDetail : orderContext.getLineItems()) {
			// TODO apply PK Vendor Rule Logic
			ArrayList<FulfillmentLocationContext> pkFulfillmentContext = applyPkRules(orderDetail,fulfillmentSequence);
			getFulfillmentDetails(orderDetail, pkFulfillmentContext, consolidationEnabled);
		}
	}

	private ArrayList<FulfillmentLocationContext> applyPkRules(LineItemContext orderDetail,
			LinkedHashSet<FulfillmentLocationContext> fulfillmentSequence) {
		
		ArrayList<FulfillmentLocationContext> pkFillContext = new ArrayList<>();
		pkFillContext.addAll(fulfillmentSequence);
		
		for (FulfillmentLocationContext sequence: fulfillmentSequence) {
		
			Optional<PkCriteria> pkResults = pickupWarehouseVendorIntegration.getPkCriteriaList().stream()
					.filter(i -> i.getBuildingCode().equals(sequence.getLocation()))
					//TODO should this be the part?
					.filter(i -> i.getVendorCode().equals(orderDetail.getVendorCode())).findAny();
			if (pkResults.isPresent()) {
				pkFillContext.remove(sequence);
				int indexToInsertAt = pkFillContext.size()-1;
				for (int i=0; i<pkFillContext.size(); i++) {
					if (pkFillContext.get(i).getDaysToDeliver()>sequence.getDaysToDeliver()) {
						indexToInsertAt=i-1;
						break;
					}
				}

				pkFillContext.add(Math.max(indexToInsertAt,0), sequence);
				log.info("pkFillContext generated " + orderDetail.getContext().getCustomerNumber() + " / " + orderDetail.getPartNumber() + " index: " + indexToInsertAt);
			
			}
		}
		return pkFillContext;
	}

	public void getFulfillmentDetails(LineItemContext orderDetail,
			Collection<FulfillmentLocationContext> fulfillmentSequence, Boolean consolidationEnabled) {
		
		Map<FulfillmentLocationContext, Integer> logisticalFulfillmentPlan = fulfillmentPlanning(orderDetail, fulfillmentSequence);

		/*
		 * Consolidation currently only occurs if there is no impact to the customer
		 */
		if (consolidationEnabled) {
			//  split fulfillment.
			if (logisticalFulfillmentPlan.entrySet().size() > 1 ) {
				// TODO Move consolidation logic into a WHEN rule
				List<FulfillmentLocationContext> orderFillContextList = orderDetail.getFulfillmentDetails()
						.stream()
						.sorted(Comparator.comparingDouble(FulfillmentContext::getScoreValue)
						.reversed())
						.map(i -> i.getFulfillmentLocation())
						.collect(Collectors.toList());
				Map<FulfillmentLocationContext, Integer> availabilityFulfillmentPlan = fulfillmentPlanning(orderDetail, orderFillContextList);	
				if (availabilityFulfillmentPlan.entrySet().size() < logisticalFulfillmentPlan.entrySet().size() ) {
					log.info("Consolidation available.");
					OptionalLong maxLogisticTimeToDeliver = getMaxArrivalDateAsMillis(logisticalFulfillmentPlan.keySet());
					OptionalLong maxAvailabilityTimeToDeliver = getMaxArrivalDateAsMillis(availabilityFulfillmentPlan.keySet());
					if (maxAvailabilityTimeToDeliver.isPresent() && maxLogisticTimeToDeliver.isPresent() && 
							maxAvailabilityTimeToDeliver.getAsLong() <= maxLogisticTimeToDeliver.getAsLong() ) {
						log.info("Consolidating.");
						assignValues(orderDetail, availabilityFulfillmentPlan);
						return;
					} else {
						log.info("Not consolidateable, consolidation would impact customer's order fulfillment time");
					}
				}
				log.info("Consolidating wouldnt reduce the number of jobs");
			} 
		}
		
		assignValues(orderDetail, logisticalFulfillmentPlan);
		
	}
	
	private OptionalLong getMaxArrivalDateAsMillis(Collection<FulfillmentLocationContext> orderFillContexts) {
		return orderFillContexts.stream()
				.filter(i -> i.getArrivalDate() != null)
				.map(i -> i.getArrivalDate().toInstant())
				.mapToLong(Instant::toEpochMilli)
				.max();
	}

	private Map<FulfillmentLocationContext, Integer> fulfillmentPlanning(LineItemContext orderDetail,
			Collection<FulfillmentLocationContext> fulfillmentSequence) {
		
		Map<FulfillmentLocationContext, Integer> fulfillmentMap = new HashMap<>();
		Integer requestQuantity = orderDetail.getQuantity();
		Integer amountLeftToFill = requestQuantity;

		for (FulfillmentLocationContext fillSequence : fulfillmentSequence) {
			for (FulfillmentContext orderFillDetail : orderDetail.getFulfillmentDetails()) {
				if (fillSequence.equals(orderFillDetail.getFulfillmentLocation())) {
					Integer amountFilled = Math.min(orderFillDetail.getQuantityOnHand(), amountLeftToFill);
					
					amountLeftToFill = amountLeftToFill - amountFilled;

					if (amountFilled > 0) {
						fulfillmentMap.put(fillSequence, amountFilled);
					}
					if (amountLeftToFill <= 0) {
						break;
					}
				}
			}
		}
		return fulfillmentMap;
	}
	
	private void assignValues(LineItemContext orderDetail, Map<FulfillmentLocationContext, Integer> fulfillmentMap) {
		
		for (FulfillmentContext orderFillDetail : orderDetail.getFulfillmentDetails()) {
			
			for (Entry<FulfillmentLocationContext, Integer> entrySet : fulfillmentMap.entrySet()) {
				if (orderFillDetail.getFulfillmentLocation().equals(entrySet.getKey())) {
//					log.info("filling order: " + orderDetail.getContext().getXmlOrderId() + " line: " + orderDetail.getLineNumber() + " detail: " + orderDetail.getOrderId() 
//					       + " at " + orderFillDetail.getLocation() + " amount: " +  entrySet.getValue());
					orderFillDetail.setFillQuantity(entrySet.getValue());
				}
			}
		}
	}

}
