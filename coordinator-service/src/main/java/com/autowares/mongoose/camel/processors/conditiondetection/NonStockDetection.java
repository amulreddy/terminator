package com.autowares.mongoose.camel.processors.conditiondetection;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.PurchaseOrderType;

@Component
public class NonStockDetection implements Processor {

	private static Logger log = LoggerFactory.getLogger(NonStockDetection.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		Optional<? extends LineItemContext> nonStockLineItems = context.getLineItems().stream()
				.filter(i -> !i.getDocumentReferenceIds().isEmpty()).findAny();
		if (nonStockLineItems.isPresent() && context instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext orderContext = (CoordinatorOrderContext) context;
			orderContext.setOrderType(PurchaseOrderType.SpecialOrder);
			log.info("Special Order detected");
		}

	}

}
