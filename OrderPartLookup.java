package com.autowares.mongoose.camel.processors.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.MultiPartRequest;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.command.PartClient;
import com.autowares.apis.partservice.model.OptionalPartField2;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.utils.MatchingUtils;
import com.google.common.collect.Lists;

@Component
public class OrderPartLookup implements Processor {

	private static PartClient partClient = new PartClient();
	// Connection to the partsservice for looking up parts.
	private static Logger log = LoggerFactory.getLogger(OrderPartLookup.class);

	@PostConstruct
	private void init() {
//		partClient.withServiceDomain("consul");
	}

	@Override
	public void process(Exchange exchange) throws Exception {

		Object o = exchange.getIn().getBody(Object.class);
		CoordinatorContext context = null;
		List<CoordinatorContext> contexts = new ArrayList<>();
		if (o instanceof CoordinatorContext) {
			context = exchange.getIn().getBody(CoordinatorContext.class);
		}
		if (o instanceof FulfillmentLocationContext) {
			context = exchange.getIn().getBody(FulfillmentLocationContext.class).getOrder();
		}
		if (o instanceof DocumentContext) {
			context = exchange.getIn().getBody(DocumentContext.class).getContext();
			for (ProcurementGroupContext procurementGroupContext : exchange.getIn().getBody(DocumentContext.class)
					.getProcurementGroupContexts()) {
				CoordinatorContext customerContext = procurementGroupContext.getCustomerContext().getOrderContext();
				CoordinatorContext supplierContext = procurementGroupContext.getSupplierContext().getOrderContext();
				contexts.add(customerContext);
				contexts.add(supplierContext);
			}
		}

		Boolean supersede = exchange.getIn().getHeader("replaceSupersededParts", false, Boolean.class);

		log.info("looking up " + context.getLineItems().size() + " parts");
		MultiPartRequest request = buildRequest(context);
		request = partClient.lookupParts(request);

		if (request != null) {
			for (LineItemContext lineItem : context.getLineItems()) {

				Optional<Part> optionalPart = request.getParts().stream()
						.filter(p -> p.getLineNumber() != null && p.getLineNumber().equals(lineItem.getLineNumber()))
						.findFirst();
				if (optionalPart.isPresent()) {
					Part part = optionalPart.get();

					if (supersede && part.getSupersededBy() != null
							&& part.getAvailability().stream().mapToInt(i -> i.getQuantityOnHand()).sum() == 0) {
						Part supersededPart = part.getSupersededBy();
						supersededPart.setLineNumber(lineItem.getLineNumber());
						MultiPartRequest superSededRequest = buildRequest(context);
						superSededRequest.setParts(Lists.newArrayList(supersededPart));
						try {
							String message = "Superseding line number " + lineItem.getLineNumber() + " from part "
									+ part.getCounterWorksLineCode() + " " + part.getPartNumber() + " to part "
									+ supersededPart.getCounterWorksLineCode() + " " + supersededPart.getPartNumber();
							part = partClient.lookupParts(superSededRequest).getParts().get(0);
							context.updateProcessingLog(message);
							log.info(message);
						} catch (Exception e) {
							log.error("Unable to replace part with supersede");
						}
					}

					lineItem.setPart(part);

					for (CoordinatorContext otherContext : contexts) {
						Optional<LineItemContext> otherLine = MatchingUtils.matchByProduct(lineItem,
								otherContext.getLineItems());
						if (otherLine.isPresent()) {
							otherLine.get().setPart(part);
						}
					}
				}
			}
		} else {
			log.error("Part lookup failed, request = null - order: " + context.getDocumentId());
		}

	}

	private MultiPartRequest buildRequest(CoordinatorContext context) {
		MultiPartRequest request = new MultiPartRequest(context);
		Predicate<Part> invalidPart = p -> p.getMoaPartHeaderId() == null;
		invalidPart = invalidPart.and(p -> p.getLineCode() == null);
		invalidPart = invalidPart.and(p -> p.getVendorCodeSubCode() == null);
		invalidPart = invalidPart.and(p -> p.getBrandAaiaId() == null);
		// Filter out invalid parts that could cause multiple parts to come back later.
		List<Part> validParts = request.getParts().stream().filter(invalidPart.negate()).collect(Collectors.toList());
		request.setParts(validParts);
		request.getFields().add(OptionalPartField2.origCodes);
		// TODO Check for inquiry options so that these are specifically requested.
		request.getFields().add(OptionalPartField2.gcomReceiverId);
		request.getFields().add(OptionalPartField2.activeProductLine);
		request.getFields().add(OptionalPartField2.hazmatDetails);
		request.getFields().add(OptionalPartField2.supersededby);
		request.getFields().add(OptionalPartField2.priceLevels);
		request.getFields().add(OptionalPartField2.productBuyer);
		if (context.getInquiryOptions().getIncludeInterchanges()) {
			request.getFields().add(OptionalPartField2.interchanges);
			request.getInterchangefields().add(OptionalPartField2.availability);
		}

		request.setCustomerNumber(String.valueOf(context.getCustomerNumber()));
		request.setVisibleInViper(true);
		return request;
	}

}
