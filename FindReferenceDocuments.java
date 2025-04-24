package com.autowares.mongoose.camel.processors.lookup;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderContextTypeConverter;
import com.autowares.mongoose.camel.components.QuoteContextTypeConverter;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.commands.SourceDocumentClient;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.xmlgateway.edi.EdiSourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Deprecated
public class FindReferenceDocuments implements Processor {

	@Autowired
	private SupplyChainService supplyChain;

	@Autowired
	private ObjectMapper objectMapper;

	Logger log = LoggerFactory.getLogger(FindReferenceDocuments.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		log.info("FindReferenceDocuments");
		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		SourceDocumentClient sourceDocumentClient = supplyChain.getClient();

		Map<String, List<LineItemContext>> supplierMap = context.getLineItems().stream()
				.filter(i -> i.getDocumentReference() != null)
				.collect(Collectors.groupingBy(LineItemContext::getDocumentReference));

		for (Entry<String, List<LineItemContext>> key : supplierMap.entrySet()) {
			String documentId = key.getKey();

			/**
			 * All these lines need to be on the quote. 100%, no extras but we will have to
			 * do product Id matching.
			 */

			/**
			 * ReferenceDocumentId's come in on an inquiry or order request. The reference
			 * document is the customer quote Customer Quote becomes a fulfillmentContext
			 * for the affected line items and quantities Supplier Quote becomes the
			 * nonStockContext
			 */

			// TODO: Validate that the supplier quote is populated in the non-stock context
			// in the "RebuildNonstockContext" processor
			List<LineItemContext> lines = supplierMap.get(documentId);

			Optional<SupplyChainSourceDocumentImpl> optionalQuote = sourceDocumentClient
					.findSourceDocumentByDocumentId(documentId, SourceDocumentType.Quote);

			if (optionalQuote.isPresent()) {
				SupplyChainSourceDocument quote = optionalQuote.get();
				log.info("Found quote: " + quote.getDocumentId());
				context.getProcurementGroups().addAll(quote.getProcurementGroups());
				if (quote.getTransactionContext() != null) {
					if (context.getTransactionContext() != null) {
						quote.getTransactionContext().setTransactionReferenceId(context.getTransactionContext().getTransactionReferenceId());
					}
					context.setTransactionContext(quote.getTransactionContext());
				}

				/**
				 * Turn the quote into a coordinator context and re-inflate a fill location
				 * context. The order lines are already isolated to what we need.
				 */
				CoordinatorContext referenceContext = QuoteContextTypeConverter
						.sourceDocumentToCoordinatorContext(quote);

				if (referenceContext.getLineItems().size() != lines.size()) {
					log.error("Quote to Order/Inquiry line item mis-match");
				}

				FulfillmentLocationContext fulfillmentLocationContext = null;

				// going to loop through but there should only be one on the quote.
				// also all the fulfillmentContexts should line up for quantity
				for (FulfillmentLocationContext fillLocation : referenceContext.getFulfillmentSequence()) {
					// Create a new fulfillment location context;
					try {
						fulfillmentLocationContext = objectMapper.readValue(
								objectMapper.writeValueAsString(fillLocation), FulfillmentLocationContext.class);
					} catch (Exception e) {
						throw e;
					}
//					fulfillmentLocationContext = new FulfillmentLocationContext(context, fillLocation.getLocation());
					fulfillmentLocationContext.setOrder(context);
					context.getFulfillmentSequence().add(fulfillmentLocationContext);
					fulfillmentLocationContext.getNonStockContext()
							.setDocumentId(fillLocation.getNonStockContext().getDocumentId());
					fulfillmentLocationContext.getNonStockContext().getFulfillmentSequence()
							.add(fulfillmentLocationContext);
					/** Set the non-stock context */
//					String docId = fillLocation.getNonStockContext().getDocumentId();
//					Optional<SupplyChainQuote> supplierQuote =  sourceDocumentClient.findSourceDocumentByDocumentId(docId, SourceDocumentType.Quote);
//					if (supplierQuote.isPresent()) {
//						log.info("Found supplier quote: " + quote.getDocumentId());
//					    fulfillmentLocationContext.setNonStockContext(QuoteContextTypeConverter.supplyChainQuoteToCoordinatorQuoteContext(supplierQuote.get()));
//					}
					fulfillmentLocationContext.setReferenceDocument(quote);

					fulfillmentLocationContext.setTransactionStage(TransactionStage.Open);
					fulfillmentLocationContext.setTransactionStatus(TransactionStatus.Pending);

					if (fulfillmentLocationContext.getLocationType() == LocationType.Vendor) {
						context.getInquiryOptions().getLocationTypes().add(LocationType.Vendor);
					}

					// TODO Clone other fields...
				}

				for (LineItemContext referenceLine : referenceContext.getLineItems()) {
					for (LineItemContext orderLine : lines) {
						if (orderLine.getProductId().equals(referenceLine.getProductId())) {
							log.info("Matched item: " + orderLine.getLineCode() + " " + orderLine.getPartNumber());
							for (Availability referenceAvalability : referenceLine.getAvailability()) {
								referenceAvalability.setLineItem(orderLine);
								orderLine.getAvailability().add(referenceAvalability);
								referenceAvalability.setFulfillmentLocation(fulfillmentLocationContext);
								fulfillmentLocationContext.getLineItemAvailability().add(referenceAvalability);
							}
						}
					}
				}

				if (context instanceof CoordinatorOrderContext) {
					quote.setTransactionStage(TransactionStage.Complete);
					quote.setTransactionStatus(TransactionStatus.Accepted);
					sourceDocumentClient.saveSourceDocument(quote);
				}

			}
		}

		if (context.getReferenceDocument() != null && context.getReferenceDocument() instanceof EdiSourceDocument) {
			EdiSourceDocument ediSourceDoc = (EdiSourceDocument) context.getReferenceDocument();
			Optional<SupplyChainSourceDocumentImpl> existingOrder = supplyChain.getProdClient()
					.findSourceDocumentByDocumentId(ediSourceDoc.getTransactionReferenceId(),
							SourceDocumentType.PurchaseOrder);
			if (existingOrder.isPresent()) {
				context.setSourceContext(
						OrderContextTypeConverter.supplyChainDocumentToOrderContext(existingOrder.get()));
				context.getSourceContext().getRelatedContexts().add(context);
			}

			if (SourceDocumentType.PackSlip.equals(context.getSourceDocumentType())) {
				if (!existingOrder.isPresent()) {
					throw new AbortProcessingException("Cannot process an ASN without a reference order");
				}
				Optional<SupplyChainSourceDocumentImpl> existingPackslip = supplyChain.getClient()
						.findSourceDocumentByDocumentId(context.getDocumentId(), SourceDocumentType.PackSlip);
				if (existingPackslip.isPresent()) {
					throw new AbortProcessingException("Already processed ASN");
				}
			}
		}

	}
}
