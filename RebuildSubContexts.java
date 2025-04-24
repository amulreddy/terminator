package com.autowares.mongoose.camel.processors.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderContextTypeConverter;
import com.autowares.mongoose.camel.components.QuoteContextTypeConverter;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.supplychain.commands.SourceDocumentClient;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.OrderRequest;
import com.autowares.xmlgateway.model.RequestItem;

@Component
public class RebuildSubContexts implements Processor {

	private SourceDocumentClient sourceDocumentClient = new SourceDocumentClient();
	Logger log = LoggerFactory.getLogger(RebuildSubContexts.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		
		Collection<? extends LineItemContext> lineItems = context.getLineItems();

		
		Optional<SupplyChainSourceDocumentImpl> sourceDocument = sourceDocumentClient.findSourceDocumentByDocumentId(context.getSourceContext().getDocumentId(),
				SourceDocumentType.PurchaseOrder);
		if (sourceDocument.isPresent()) {
			CoordinatorOrderContext newOrderContext = OrderContextTypeConverter.supplyChainDocumentToOrderContext(sourceDocument.get());
			newOrderContext.setSourceContext(context.getSourceContext());
			InquiryRequest inquiryRequest = QuoteContextTypeConverter.sourceDocumentToInquiryRequest(sourceDocument.get());
			inquiryRequest.setAccountNumber(String.valueOf(newOrderContext.getCustomerNumber()));
			inquiryRequest.getFulfillmentOptions().setExcludedLocations(context.getFulfillmentOptions().getExcludedLocations());
			// Remove original fulfillment locations and recreate later.
			List<RequestItem> orderLineItemContexts = new ArrayList<RequestItem>();
			for( LineItemContext lineItem : lineItems) {
				Optional<RequestItem> optionalOrderLine = inquiryRequest.getLineItems().stream()
				        .filter(i -> i.getLineNumber().equals(lineItem.getLineNumber())).findAny();
				if (optionalOrderLine.isPresent()) {
					RequestItem orderLine = optionalOrderLine.get();
					orderLine.setQuantity(lineItem.getQuantity());
					orderLineItemContexts.add(orderLine);
				}
			}
			inquiryRequest.setLineItems(orderLineItemContexts);
			CoordinatorOrderContext subContext = OrderContextTypeConverter.orderRequestToCoordinatorOrderContext(new OrderRequest(inquiryRequest));
			subContext.setSourceContext(newOrderContext);
			subContext.setTransactionContext(newOrderContext.getTransactionContext());
			exchange.getIn().setBody(subContext);
		}
		
		
	}

}
