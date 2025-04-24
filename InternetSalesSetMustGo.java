package com.autowares.mongoose.camel.processors.conditiondetection;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessAttribute;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.xmlgateway.model.RequestItem;

@Component
public class InternetSalesSetMustGo implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
		Object o = exchange.getIn().getBody(Object.class);
		CoordinatorContext context = null;
		if (o instanceof CoordinatorContext) {
			context = exchange.getIn().getBody(CoordinatorContext.class);
		}
		if (o instanceof FulfillmentLocationContext) {
			context = exchange.getIn().getBody(FulfillmentLocationContext.class).getOrder();
		}
		if (o instanceof DocumentContext) {
			context = exchange.getIn().getBody(DocumentContext.class).getContext();
		}
		

        Optional<BusinessAttribute> internetAccount = context.getBusinessContext().getBusinessDetail()
                .getBusinessAttributes().stream().filter(i -> i.getType().getBusAttributeId().equals(27L)).findAny();
        if (internetAccount.isPresent()) {
            for (LineItemContext lineitem : context.getLineItems()) {
                lineitem.setMustGo(true);
            }
            ;
            for (RequestItem requestItem : context.getTransactionContext().getRequest().getLineItems()) {
                requestItem.getHandlingOptions().setMustGoActive(true);
            }
        }

    }

}
