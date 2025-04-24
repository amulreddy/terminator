package com.autowares.mongoose.camel.processors.conditiondetection;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.SourceDocumentType;

@Component
public class ReturnDetectionProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(ReturnDetectionProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		if (context instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext orderContext = (CoordinatorOrderContext) context;

			List<LineItemContext> returns = orderContext.getLineItems().stream().filter(o -> o.getQuantity() < 0)
					.collect(Collectors.toList());
			// Save records that have getQuantity<0 (returns) to the returns variable array.
			if (!returns.isEmpty()) {
				// If we have found returns records (returns array is NOT empty) :
				log.info("**** Returns detected for xmlOrderId: " + orderContext.getXmlOrderId());
				orderContext.setSourceDocumentType(SourceDocumentType.Return);
			}
		}
	}

}
