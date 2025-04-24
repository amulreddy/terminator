package com.autowares.mongoose.camel.components;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.model.Origcode;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.CoordinatorOrderContextImpl;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.motorstateservice.model.Line;
import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FulfillmentLocation;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.Note;
import com.autowares.servicescommon.model.NoteImpl;
import com.autowares.servicescommon.model.PartLineItem;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.util.DateConversion;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.servicescommon.util.SequenceGenerator;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.SupplyChainLine;
import com.autowares.supplychain.model.SupplyChainLineType;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainParty;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl.Builder;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;
import com.autowares.xmlgateway.model.GatewayOrder;
import com.autowares.xmlgateway.model.GatewayOrderPlacedEvent;
import com.autowares.xmlgateway.model.GenericGatewayPartRequest;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.OrderRequest;
import com.autowares.xmlgateway.model.RequestItem;
import com.autowares.xmlgateway.model.ResponseItem;

@Component
@Converter(generateLoader = true)
public class OrderContextTypeConverter implements TypeConverters {

	private static final SequenceGenerator sequenceGenerator = new SequenceGenerator();
	private static Logger log = LoggerFactory.getLogger(OrderContextTypeConverter.class);

	@Converter
	public static CoordinatorOrderContext orderRequestToCoordinatorOrderContext(OrderRequest request) {
//		if (request.getDocumentId() == null) {
//			request.setDocumentId(UUID.randomUUID().toString());
//		}
		CoordinatorOrderContextImpl orderContext = new CoordinatorOrderContextImpl();

		TransactionContext transactionContext = TransactionContext.builder().withTransactionContextId(UUID.randomUUID())
				.withRequest(request).withTransactionReferenceId(request.getDocumentId())
				.withDeliveryMethod(request.getFulfillmentOptions().getDeliveryMethod())
				.withServiceClass(request.getFulfillmentOptions().getServiceClass()).build();
		orderContext.setTransactionContext(transactionContext);

		orderContext.setDocumentId(request.getDocumentId());
		orderContext.setCustomerDocumentId(request.getCustomerDocumentId());
		orderContext.setXmlOrderId(request.getDocumentId());
		orderContext.setOrderTime(ZonedDateTime.now());
		orderContext.setRequestTime(request.getTimeStamp());

		if (request.getInquiryOptions() != null) {
			orderContext.setInquiryOptions(request.getInquiryOptions());
		}
		orderContext.getInquiryOptions().setCalculateShipment(true);
		orderContext.getInquiryOptions().setPlanFulfillment(true);
		orderContext.getInquiryOptions().setIncludePrices(true);
		orderContext.setOrderType(request.getOrderType());

		if (request.getFulfillmentOptions() != null) {
			orderContext.setFulfillmentOptions(request.getFulfillmentOptions());
			orderContext.setServiceClass(request.getFulfillmentOptions().getServiceClass());
			orderContext.setDeliveryMethod(request.getFulfillmentOptions().getDeliveryMethod());
			orderContext.setPsxToBeDelivered(request.getFulfillmentOptions().getPsxToBeDelivered());
		}

		if (request.getBillingOptions() != null) {
			orderContext.setBillingOptions(request.getBillingOptions());
		}

		if (request.getAccountNumber() != null) {
			try {
				orderContext.setCustomerNumber(Long.parseLong(request.getAccountNumber()));
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			}
		}
		if (request.getShipTo() != null) {
			orderContext.setShipTo(new BusinessContext(request.getShipTo()));
		}
		if (request.getNotes() != null) {
			for (Note note : request.getNotes()) {
				orderContext.updateProcessingLog(note.getMessage());
			}
		}
		for (RequestItem lineItem : request.getLineItems()) {
			LineItemContextImpl lineItemContext = new LineItemContextImpl(orderContext, lineItem);
			lineItemContext.setHandlingOptions(lineItem.getHandlingOptions());
			// Keep the supplied value intact if possible as this is intended to be used for
			// correlation
			lineItemContext.setMustGo(lineItem.getHandlingOptions().getMustGoActive());
			if (lineItem.getLineNumber() != null) {
				lineItemContext.setLineNumber(lineItem.getLineNumber());
			} else {
				lineItemContext.setLineNumber(request.getLineItems().indexOf(lineItem));
			}
			lineItemContext.setPrice(lineItem.getPrice());
			for (NoteImpl note : lineItem.getNotes()) {
				lineItemContext.getNotes().add(SupplyChainNote.builder().withMessage(note.getMessage()).build());
			}
			orderContext.getLineItems().add(lineItemContext);
		}
		return orderContext;
	}

	@Converter
	public static MotorstatePurchaseOrder coordinatorOrderContextToMotorstatePurchaseOrder(
			CoordinatorOrderContext coordinatorOrderContext) {

		// This should just build the base Motorstate PO. Don't do any outside contact
		// here. Build what we can with what we have.
		// We have already verified the part(s) are good @ Motorstate.
		// So, we'll check the invalid property, which was updated in the
		// LookUpMotorstatePart processor. If invalid=true, skip adding the part to
		// the PO here.

		// if(orderfillcontext) Operate off the nonStockContext that was built before
		// this.
		String lineCode;
		String partNumber;
		Long sequence;

		sequence = sequenceGenerator.nextId();
		log.info("Motorstate sequence ID = " + sequence);
		String motorstateSequence = String.format("%010d", sequence);
		// Zero fill the MS sequence number for the MS PO.

		String motorstatePo = String.format("%04d", coordinatorOrderContext.getCustomerNumber());
		// Zero fill the customer number for the MS PO.
		motorstatePo += "-";

		motorstatePo += motorstateSequence;

		Integer recCt = 1;
		List<Line> lines = new ArrayList<Line>();
		// order fill detail.
		for (LineItemContext lineItem : coordinatorOrderContext.getLineItems()) {
			// Check .isValid for each part here. Don't add the part to the request
			// if Motorstate doesn't recognize it.

			// Iterate thru the order records to build the MS order request.
			if (!lineItem.getInvalid()) {
				// Only add parts to the PO that Motorstate recognizes.
				lineCode = lineItem.getCounterWorksLineCode();
				// Default to Line Code in the context.
				partNumber = lineItem.getPartNumber();
				String msPartNumber = null;
				Long mPHId = lineItem.getProductId();
				List<Origcode> moaPartOrig = null;
				if (mPHId != null) {
					moaPartOrig = lineItem.getPart().getOrigcodes();
				}
				// Get MoaPartHeader record here using the partHdrId. Get the Origcode
				// record(s) below, as we might have to use the source system part #.

				if (moaPartOrig != null) {
					// We have ORIGCODE records for this part.
					log.info("ORIGCODE record found.");
					for (Origcode tempMPO : moaPartOrig) {
						// Go thru the ORIGCODE record(s), but really, we should only ever have 1
						// for a part.
						if (tempMPO.getSourceSystemCode().equals("MTRS")) {
							lineCode = tempMPO.getSourceSystemLineCode();
							partNumber = tempMPO.getSourceSystemPartNumber();
							msPartNumber = lineCode + partNumber;
						}
					}
				} else {
					// No ORIGCODE records for this partHdrId.
					// Add this to the list of parts on the PO to send
					// to Motorstate.

					log.info("No origcode records.");
					msPartNumber = lineCode + partNumber;

				}

				Line line = new Line.Builder(recCt).withPartNumber(msPartNumber)
						.withQuantityShip(lineItem.getQuantity()).withComment(" ").build();

				lines.add(line);
				// Add this line to the Array of Lines.
			}

			// Motorstate PO object build below. This is for the JSON we'll send to MS
			// for the order.
			recCt++;
		}

		MotorstatePurchaseOrder mPO = new MotorstatePurchaseOrder.Builder(motorstatePo)
				.withRequestId(motorstateSequence).withPaymentMethodCode("OnAccount").withCreditCardLast4(" ")
				.withLines(lines).withShippingMethodCode(" ").withSignatureRequired(false).build();
		// Build the Motorstate PO.
		// Put the PO on the nonStockContext as the reference document.

		log.info("Motorstate full Purchase Order : ");
		PrettyPrint.print(mPO);

		return mPO;

	}

	@Converter
	public static CoordinatorOrderContext supplyChainDocumentToOrderContext(SupplyChainSourceDocument sourceDocument) {
		if (SourceDocumentType.PurchaseOrder.equals(sourceDocument.getSourceDocumentType())) {
			CoordinatorOrderContextImpl impl = new CoordinatorOrderContextImpl(
					QuoteContextTypeConverter.sourceDocumentToCoordinatorContext(sourceDocument));
			impl.setPurchaseOrder(sourceDocument.getCustomerDocumentId());
			impl.setOrderTime(sourceDocument.getTransactionTimeStamp());
			return impl;
		}
		if (SourceDocumentType.Quote.equals(sourceDocument.getSourceDocumentType())) {
			CoordinatorOrderContextImpl impl = new CoordinatorOrderContextImpl(
					QuoteContextTypeConverter.sourceDocumentToCoordinatorContext(sourceDocument));
			impl.setOrderType(PurchaseOrderType.SpecialOrder);
			impl.setPurchaseOrder(sourceDocument.getCustomerDocumentId());
			impl.setOrderTime(sourceDocument.getTransactionTimeStamp());
			return impl;
		}
		throw new AbortProcessingException(
				"Unable to convert " + sourceDocument.getSourceDocumentType() + " to CoordinatorOrderContext.");
	}

	@Converter
	public static DocumentContext SupplyChainSourceDocumentToDocumentContext(SupplyChainSourceDocument document) {
		DocumentContext context = new DocumentContext(document);
		context.setDocumentId(document.getDocumentId());
		context.setSourceDocument(document);
		context.setTransactionalContext(new TransactionalContext(document.getTransactionContext()));
		for (SupplyChainLine li : document.getLineItems()) {
			context.getLineItems().add(new LineItemContextImpl(li));
		}
		return context;
	}

	@Converter
	public static CoordinatorOrderContext gatewayOrderPlacedEventToOrderContext(
			GatewayOrderPlacedEvent gatewayOrderEvent) {
		GatewayOrder gatewayOrder = gatewayOrderEvent.getOrder();
		InquiryRequest inquiryRequest = new InquiryRequest();

		if (gatewayOrder.getRequest() != null) {
			inquiryRequest = gatewayOrder.getRequest();
		} else {
			inquiryRequest.getInquiryOptions().setLookupSource(gatewayOrder.getOrderSource());
			inquiryRequest.getFulfillmentOptions().setDeliveryMethod(gatewayOrder.getDeliveryMethod());
			inquiryRequest.getFulfillmentOptions().setServiceClass(gatewayOrder.getServiceClass());
			inquiryRequest.setAccountNumber(gatewayOrder.getGatewayRequest().getAutowaresAccountNumber());
			inquiryRequest.setCustomerDocumentId(gatewayOrder.getPurchaseOrder());
			inquiryRequest.setTimeStamp(DateConversion.convert(gatewayOrder.getOrderTimestamp()));
		}

		TransactionContext transactionContext = TransactionContext.builder().withRequest(inquiryRequest)
				.withTransactionScope(TransactionScope.Supplying).withDeliveryMethod(gatewayOrder.getDeliveryMethod())
				.withServiceClass(gatewayOrder.getServiceClass())
				.withTransactionReferenceId(String.valueOf(gatewayOrder.getXmlOrderId())).build();

		CoordinatorOrderContextImpl context = new CoordinatorOrderContextImpl();
		context.setTransactionContext(transactionContext);
		context.setInquiryOptions(inquiryRequest.getInquiryOptions());
		context.setCustomerNumber(Long.valueOf(gatewayOrder.getGatewayRequest().getAutowaresAccountNumber()));
		context.setDeliveryMethod(gatewayOrder.getDeliveryMethod());
		context.setServiceClass(gatewayOrder.getServiceClass());
		context.setOrderTime(DateConversion.convert(gatewayOrder.getOrderTimestamp()));
		context.setCustomerDocumentId(gatewayOrder.getPurchaseOrder());

		if ("Y".equalsIgnoreCase(gatewayOrder.getPSXToBeDelivered())) {
			context.setPsxToBeDelivered(true);
			inquiryRequest.getFulfillmentOptions().setPsxToBeDelivered(true);
		}

		if (gatewayOrder.getScOrderId() != null && gatewayOrder.getScOrderId().intValue() != 0) {
			context.setSourceOrderId(gatewayOrder.getScOrderId().longValue());
		}

		if (gatewayOrder.getXmlOrderId() != null) {
			String documentId = String.valueOf(gatewayOrder.getXmlOrderId());
			context.setXmlOrderId(documentId);
			context.setDocumentId(documentId);
		}

		for (PartLineItem gatewayPart : gatewayOrder.getGatewayRequest().getParts()) {
			LineItemContextImpl lineItem = new LineItemContextImpl(context, gatewayPart);
			RequestItem requestItem = new RequestItem(gatewayPart);
			if (gatewayPart instanceof GenericGatewayPartRequest) {
				GenericGatewayPartRequest genericGatewayPart = (GenericGatewayPartRequest) gatewayPart;
				lineItem.setMustGo(genericGatewayPart.notifyIfIncomplete());
				HandlingOptions handlingOptions = new HandlingOptions();
				handlingOptions.setMustGoActive(genericGatewayPart.notifyIfIncomplete());
				handlingOptions.setPreferredLocation(genericGatewayPart.getShippingWarehouse());
				if (gatewayOrder.getPurchaseOrder() != null && gatewayOrder.getPurchaseOrder().startsWith("^")) {
					handlingOptions.setHoldBackOrder(true);
				}
				requestItem.setHandlingOptions(handlingOptions);
				lineItem.setHandlingOptions(handlingOptions);
			}
			inquiryRequest.getLineItems().add(requestItem);
			context.getLineItems().add(lineItem);
		}

		return context;
	}

	public static InquiryResponse orderContextToOrderResponse(CoordinatorOrderContext context) {
		InquiryResponse response = new InquiryResponse();
		for (FulfillmentLocationContext fulfillment : context.getFulfillmentSequence()) {
			if (fulfillment.isBeingFilledFrom()) {
				Freight fulfillmentLocation = new Freight();
				fulfillmentLocation.setLocationName(fulfillment.getLocation());
				fulfillmentLocation.setEstimatedDeliveryDateTime(fulfillment.getArrivalDate());
				fulfillmentLocation.setEstimatedShipDateTime(fulfillment.getNextDeparture());
				for (FulfillmentContext fillDetail : fulfillment.getFulfillmentDetails()) {
					if (fillDetail.getFillQuantity() > 0) {
						LineItemContext orderDetail = fillDetail.getLineItem();
						ResponseItem responseItem = convertLineItemContext(orderDetail);
						responseItem.setPlannedFillQuantity(fillDetail.getFillQuantity());
						responseItem.setAvailableQuantity(fillDetail.getQuantityOnHand());
						responseItem.getFulfillmentPlan().add(fulfillmentLocation);
					}
				}
			}
		}
		boolean addNonfillable = false;
		FulfillmentLocation nonfillable = new FulfillmentLocation();
		nonfillable.setLocationName("Not fillable");
		for (LineItemContext orderDetail : context.getLineItems()) {
			if (orderDetail.getInvalid() || orderDetail.getFulfillmentDetails().isEmpty()) {
				addNonfillable = true;
				ResponseItem responseItem = convertLineItemContext(orderDetail);
				responseItem.setPlannedFillQuantity(0);
				response.getLineItems().add(responseItem);
			}
		}
		if (addNonfillable) {
		}
		return response;
	}

	@Converter
	public static SupplyChainSourceDocument coordinatorContextToShortage(
			CoordinatorOrderContext coordinatorOrderContext) {
		Builder<?> documentBuilder = SupplyChainSourceDocumentImpl.builder();

		documentBuilder.withDocumentId(coordinatorOrderContext.getDocumentId() + "-Unfillable")
				.withTransactionDate(coordinatorOrderContext.getOrderTime())
				.withPurchaseOrderType(coordinatorOrderContext.getOrderType())
				.withParty(SupplyChainParty.builder().fromAccount(coordinatorOrderContext.getBuyingAccount()).build())
				.withParty(SupplyChainParty.builder().fromParty(coordinatorOrderContext.getSellingParty()).build())
				.withTo(SupplyChainParty.builder().fromAccount(coordinatorOrderContext.getBuyingAccount()).build())
				.withFrom(SupplyChainParty.builder().fromParty(coordinatorOrderContext.getSellingParty()).build())
				.withTransactionContext(coordinatorOrderContext.getTransactionContext())
				.withCustomerDocumentId(coordinatorOrderContext.getPurchaseOrder())
				.withTransactionStatus(coordinatorOrderContext.getTransactionStatus())
				.withTransactionStage(coordinatorOrderContext.getTransactionStage())
				.withSourceDocumentType(SourceDocumentType.Shortage)
				.withSystemType(coordinatorOrderContext.getSystemType());

		if (coordinatorOrderContext.getShipTo() != null
				&& coordinatorOrderContext.getShipTo().getBusinessDetail().getBusEntId() != 0) {
			documentBuilder.withParty(SupplyChainParty.builder().fromAccount(coordinatorOrderContext.getShipTo())
					.withPartyType(PartyType.ShipTo).build());
		}

		for (LineItemContext lineItem : coordinatorOrderContext.getLineItems()) {
			Integer orderQuantity = lineItem.getQuantity();
			Integer totalFillQuantity = lineItem.getAvailability().stream().mapToInt(Availability::getFillQuantity)
					.sum();
			if (orderQuantity > totalFillQuantity) {
				Integer notFilled = orderQuantity - totalFillQuantity;
				GenericLine line = GenericLine.builder().fromPartLineItem(lineItem)
						.withLineType(SupplyChainLineType.Generic).withQuantity(notFilled)
						.withPurchasedQuantity(lineItem.getQuantity()).withShortageCode(lineItem.getShortageCode())
						.build();
				documentBuilder.withLineItem(line);
			}
		}

		return documentBuilder.build();
	}

	private static ResponseItem convertLineItemContext(LineItemContext lineContext) {
		ResponseItem responseItem = new ResponseItem();
		responseItem.setProductId(lineContext.getProductId());
		responseItem.setBrandAaiaId(lineContext.getBrandAaiaId());
		responseItem.setCounterworksLineCode(lineContext.getCounterWorksLineCode());
		responseItem.setLineCode(lineContext.getLineCode());
		if (lineContext.getCustomerLineNumber() != null) {
			responseItem.setLineNumber(lineContext.getCustomerLineNumber().intValue());
		} else {
			responseItem.setLineNumber(lineContext.getLineNumber());
		}
		responseItem.setPartNumber(lineContext.getPartNumber());
		responseItem.setPrice(lineContext.getPrice());
		responseItem.setQuantity(lineContext.getQuantity());
		responseItem.setVendorCodeSubCode(lineContext.getVendorCodeSubCode());
		return responseItem;
	}
}
