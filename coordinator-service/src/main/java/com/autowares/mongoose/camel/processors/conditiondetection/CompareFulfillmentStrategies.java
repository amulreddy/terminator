package com.autowares.mongoose.camel.processors.conditiondetection;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.TransactionalStateManager;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;

@Component
public class CompareFulfillmentStrategies implements Processor {
	
    
    @Autowired
    TransactionalStateManager transactionalStateManager;

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		IncompleteFulfillmentStrategy incompleteFulfillmentStrategy = IncompleteFulfillmentStrategy.canNotFill;
	
        CoordinatorContext source2Context = context.getSourceContext().getSourceContext();

		String excludedLocation = context.getFulfillmentOptions().getExcludedLocations().get(0);
		boolean canTransfer = false;
		boolean recheckedAlready = false;
        for(LineItemContext lineItem : context.getLineItems()) {
            @SuppressWarnings("unused")
			Optional<? extends LineItemContext> originalOptionalLineItemContext = source2Context.getLineItems().stream()
                    .filter(i -> i.getLineNumber().equals(lineItem.getLineNumber())).findAny();
            if (!recheckedAlready) {
                incompleteFulfillmentStrategy = IncompleteFulfillmentStrategy.recheck;
            }
			for(LineItemContext sourceLineItem : context.getSourceContext().getLineItems()) {
				if(sourceLineItem.getLineNumber().equals(lineItem.getLineNumber())) {
					FulfillmentLocationContext zeroedSourceLocation = sourceLineItem
							.getAvailability()
							.stream()
							.map(i -> i.getFulfillmentLocation()).filter(j -> j.getLocation().equals(excludedLocation)).findAny().get();
					ZonedDateTime originalArrivalDate = zeroedSourceLocation.getArrivalDate();
					List<ZonedDateTime> availableArrivalDates = lineItem.getAvailability().stream().filter(i -> i.getFillQuantity() > 0)
                            .map(a -> a.getFulfillmentLocation().getArrivalDate()).collect(Collectors.toList());
					if(!availableArrivalDates.isEmpty()) {
						canTransfer = availableArrivalDates.stream().allMatch(a -> a.equals(originalArrivalDate));
						//  **  allMatch will return true if nothing is found.  **
					}
                    if (canTransfer) {
                        incompleteFulfillmentStrategy = IncompleteFulfillmentStrategy.transfer;
                    }
					if(sourceLineItem.getMustGo() && inOperationalHours()) {
						incompleteFulfillmentStrategy = IncompleteFulfillmentStrategy.managerHandle;
					}
				}
			}
		}
		if(DeliveryMethod.CustomerPickUp.equals(context.getDeliveryMethod()) || ServiceClass.Express.equals(context.getServiceClass())) {
            incompleteFulfillmentStrategy = IncompleteFulfillmentStrategy.managerHandle;
		}
        
        exchange.getIn().setHeader("incompleteFulfillmentStrategy", incompleteFulfillmentStrategy);
		Object purchaseOrderObject = context.getSourceContext().getReferenceDocument();
		if (purchaseOrderObject instanceof SupplyChainSourceDocumentImpl) {
		    SupplyChainSourceDocumentImpl purchaseOrder = (SupplyChainSourceDocumentImpl) purchaseOrderObject;
		    for(LineItemContext lineItem : context.getLineItems()) {
	            for(GenericLine supplyChainLine : purchaseOrder.getLineItems()) {
	                if (lineItem.getLineNumber().equals(supplyChainLine.getLineNumber())) {
	                    String message = null;
	                    switch (incompleteFulfillmentStrategy) {
                            case managerHandle:
                                message = "Order set for manager callback";
                                break;
                            case recheck:
                                message = "Order set for recheck";
                                break;
                            case transfer:
                            	message = "Order transfered from " + excludedLocation + " to" ;
                            	for (Availability availability : lineItem.getAvailability()) {
                            		if (availability.getFillQuantity()>0) {
                            			message += " " + availability.getFulfillmentLocation().getLocation() + " quantity = " + availability.getFillQuantity();
                            		}
                            	}
                                
                                break;
                            case canNotFill:
                            default:
                                message = "Unable to fill order";
                                break;
                        }
	                    supplyChainLine.getNotes().add(SupplyChainNote.builder().withMessage(message).build());
	                    transactionalStateManager.persist(supplyChainLine);
	                }
	            }
		    }
		}
	}
	
	public enum IncompleteFulfillmentStrategy {
	    canNotFill,
	    transfer,
	    recheck,
	    managerHandle;
	}
	
	private Boolean inOperationalHours() {
		ZonedDateTime sixAM = ZonedDateTime.of(ZonedDateTime.now().toLocalDate(),LocalTime.of(6, 0),ZoneId.systemDefault());
		ZonedDateTime sixPM = ZonedDateTime.of(ZonedDateTime.now().toLocalDate(),LocalTime.of(18, 0),ZoneId.systemDefault());
		return ZonedDateTime.now().isAfter(sixAM) && ZonedDateTime.now().isBefore(sixPM);
		
	}

}
