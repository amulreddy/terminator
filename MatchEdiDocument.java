package com.autowares.mongoose.camel.processors.lookup;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.Part;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.utils.MatchingUtils;

@Component
public class MatchEdiDocument implements Processor {

	Logger log = LoggerFactory.getLogger(MatchEdiDocument.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		DocumentContext documentContext = exchange.getIn().getBody(DocumentContext.class);
		if (documentContext != null && documentContext.getContext() != null) {
			CoordinatorContext orderContext = documentContext.getContext();
			for (LineItemContextImpl lineItem : documentContext.getLineItems()) {

				// PartNumber is the only thing we get on a Packslip currently
				Optional<LineItemContext> matchingLineItem = Optional.empty();
				if(lineItem.getManufacturerLineCode() != null) {
					matchingLineItem = MatchingUtils.matchByPartNumberManufacturerLineCode(lineItem,
						orderContext.getLineItems());
				} 
				if (!matchingLineItem.isPresent()) {
					matchingLineItem = MatchingUtils.matchByPartNumber(lineItem,
							orderContext.getLineItems());
				}
				if (!matchingLineItem.isPresent()) {
					matchingLineItem = MatchingUtils.matchByOrigCodePartNumber(lineItem,
							orderContext.getLineItems());
				}
				if (matchingLineItem.isPresent()) {
					LineItemContext sourceLine = matchingLineItem.get();
					Part part = new Part();
					part.setProductId(sourceLine.getProductId());
					part.setPartNumber(sourceLine.getPartNumber());
					part.setVendorCodeSubCode(sourceLine.getVendorCodeSubCode());
					part.setCounterWorksLineCode(sourceLine.getCounterWorksLineCode());
					lineItem.setPart(part);
					lineItem.setLineNumber(sourceLine.getLineNumber());
				}

			}
		}
	}
}
