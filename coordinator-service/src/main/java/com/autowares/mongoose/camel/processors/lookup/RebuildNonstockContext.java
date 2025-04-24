package com.autowares.mongoose.camel.processors.lookup;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderContextTypeConverter;
import com.autowares.mongoose.camel.components.QuoteContextTypeConverter;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.request.SourceDocumentRequest;

@Component
@Deprecated
public class RebuildNonstockContext implements Processor {

	@Autowired
	private SupplyChainService supplyChainService;
	
	Logger log = LoggerFactory.getLogger(RebuildNonstockContext.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);

		List<FulfillmentLocationContext> vendors = context.getFulfillmentSequence().stream()
				.filter(i -> LocationType.Vendor.equals(i.getLocationType())).collect(Collectors.toList());

		for (FulfillmentLocationContext fulfillmentLocationContext : vendors) {
			
			log.info("Rebuilding Nonstock Context for supplier: " + fulfillmentLocationContext.getDescription());

			if (fulfillmentLocationContext.getNonStockContext().getDocumentId() != null) {

				SourceDocumentRequest request = SourceDocumentRequest.builder()
						.withDocumentId(fulfillmentLocationContext.getNonStockContext().getDocumentId())
						.withSourceDocumentType(SourceDocumentType.Quote).build();
				Page<SupplyChainSourceDocumentImpl> quotes = supplyChainService.getClient().findSourceDocument(request);
				Optional<SupplyChainSourceDocumentImpl> supplyChainQuote = quotes.stream().findAny();

				if (supplyChainQuote.isPresent()) {
					SupplyChainSourceDocumentImpl quote = supplyChainQuote.get();
					log.info("Found Supplier quote: " + quote.getDocumentId());
					if (context instanceof CoordinatorOrderContext) {
						CoordinatorOrderContext nonStockContext = OrderContextTypeConverter
								.supplyChainDocumentToOrderContext(quote);
						nonStockContext.setFulfillmentSequence(fulfillmentLocationContext.getNonStockContext().getFulfillmentSequence());
						fulfillmentLocationContext.setNonStockContext(nonStockContext);
					} else {
						CoordinatorContext nonStockContext = QuoteContextTypeConverter.sourceDocumentToCoordinatorContext(quote);
						fulfillmentLocationContext.setNonStockContext(nonStockContext);
						CoordinatorContextImpl impl = (CoordinatorContextImpl) context;
						impl.getProcurementGroupContext().getSupplierContext().setQuoteContext(nonStockContext);
					}
				}
			} else {
				log.info("No Supplier quote documentId found");
			}
		}
	}
}
