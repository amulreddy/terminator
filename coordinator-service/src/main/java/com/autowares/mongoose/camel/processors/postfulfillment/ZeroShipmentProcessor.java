package com.autowares.mongoose.camel.processors.postfulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.MoaOperationalStateManager;
import com.autowares.orders.model.Item;
import com.autowares.orders.model.ProcessStage;

@Component
public class ZeroShipmentProcessor implements Processor {

    @Autowired
    private MoaOperationalStateManager stateManager;

    @Override
    public void process(Exchange exchange) throws Exception {
        if (exchange.getIn().getBody() instanceof FulfillmentLocationContext) {
            FulfillmentLocationContext fulfillmentLocationContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
        
                for (Availability availability : fulfillmentLocationContext.getLineItemAvailability()) {
                    Integer shipQuantity = availability.getFillQuantity();
                    if (shipQuantity == 0) {
                        Item item = availability.getMoaOrderDetail();
                        
                        item.setProcessStage(ProcessStage.Packed);
                        // Change the process state to packed (5). No need to change the shipqty to 0
                        // as it's already 0.

                        if (item.getWarehouse().equals(6)) {
                            // TODO If this is a Motorstate record, set the MS fields in the moa_orders
                            // records
                            // as well as the error_flag ('Y') and process_stage = 8 (canceled).
                            item.setMsErrorCode("4");
                            item.setMsErrorCodeDescription(
                                    "The ship quantity requested for the order is not available (via coordinator).");
                            item.setProcessStage(ProcessStage.Canceled);
                            item.setErrorFlag("Y");
                        }
                        stateManager.persist(item);
                    }
                }
            }
    }
}
