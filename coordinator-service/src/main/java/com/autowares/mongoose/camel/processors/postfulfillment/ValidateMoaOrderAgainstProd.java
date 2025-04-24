package com.autowares.mongoose.camel.processors.postfulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.ViperToWmsCoreUtils;
import com.autowares.orders.model.Item;
import com.vividsolutions.jts.util.AssertionFailedException;

@Component
public class ValidateMoaOrderAgainstProd implements Processor {

    @Autowired
    ViperToWmsCoreUtils coreUtils;

    Logger log = LoggerFactory.getLogger(ValidateMoaOrderAgainstProd.class);

    @Override
    public void process(Exchange exchange) throws Exception {

        FulfillmentLocationContext fulfillmentLocationContext = exchange.getIn()
                .getBody(FulfillmentLocationContext.class);

        for (Availability availability : fulfillmentLocationContext.getLineItemAvailability()) {
            Item testItem = availability.getMoaOrderDetail();
            if (testItem != null) {
                Item prodItem = null;
                try {
                    prodItem = coreUtils.lookupProdOrderByXmlOrderIdLineNumberBuilding(
                            fulfillmentLocationContext.getOrder().getDocumentId(),
                            availability.getLineItem().getLineNumber(), fulfillmentLocationContext.getLocation());
                } catch (Exception e) {
                }
                if (prodItem != null) {
                    validate(prodItem, testItem, fulfillmentLocationContext.getOrder());
                } else {
                    log.error("unable to lookup moa order: " + fulfillmentLocationContext.getOrder().getDocumentId()
                            + " " + availability.getLineItem().getLineNumber() + " "
                            + fulfillmentLocationContext.getLocation());
                }
            }
        }

    }

    private void validate(Item prodItem, Item testItem, CoordinatorContext coordinatorContext) {
        try {
            if (!prodItem.getBuilding().equals("PER")) {
                assertEquals("Truck Run", prodItem.getCustomerRun().getTruckRun(),
                        testItem.getCustomerRun().getTruckRun());
            }
            assertEquals("Cut off slot", prodItem.getCutOffSlot(), testItem.getCutOffSlot());
            assertEquals("OrderType", prodItem.getOrderType(), testItem.getOrderType());
            assertEquals("Must Go", prodItem.getMustGo(), testItem.getMustGo());
            assertEquals("Bill Price", prodItem.getBillprice().setScale(2), testItem.getBillprice().setScale(2));
            assertEquals("Shipping Method", prodItem.getShippingMethod(), testItem.getShippingMethod());
        } catch (AssertionFailedException e) {
            AssertionFailedException failed = (AssertionFailedException) e;
            String message = failed.getMessage() + " for customer: " + coordinatorContext.getCustomerNumber()
                    + " for xmlOrderId: " + coordinatorContext.getDocumentId() + " lineNumber: "
                    + prodItem.getCustomerLineNumber() + " in building: " + prodItem.getBuilding();
            throw new AssertionFailedException(message);
        }
    }

    private void assertEquals(String field, Object expected, Object actual) {
        String message = field + " Expected: " + expected + " got: " + actual;
        if (expected != null) {
            if (actual != null) {
                if (expected.equals(actual)) {
                    return;
                }
            }
        } else {
            if (actual == null) {
                return;
            }
        }

        throw new AssertionFailedException(message);
    }

}
