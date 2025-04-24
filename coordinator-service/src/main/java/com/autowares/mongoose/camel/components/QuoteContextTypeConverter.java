package com.autowares.mongoose.camel.components;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.model.gateway.FreightBuilder;
import com.autowares.mongoose.model.gateway.InquiryResponseBuilder;
import com.autowares.mongoose.model.gateway.SpecialOrderResponseItemBuilder;
import com.autowares.servicescommon.model.ChargeType;
import com.autowares.servicescommon.model.Charges;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.Note;
import com.autowares.servicescommon.model.NoteImpl;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.PriceLevel;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.SupplyChainBusiness;
import com.autowares.supplychain.model.SupplyChainLine;
import com.autowares.supplychain.model.SupplyChainLineType;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainParty;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.RequestItem;
import com.autowares.xmlgateway.model.ResponseItemBuilder;

@Component
@Converter(generateLoader = true)
public class QuoteContextTypeConverter implements TypeConverters {

	@SuppressWarnings("unchecked")
	@Converter
	public static CoordinatorContext sourceDocumentToCoordinatorContext(SupplyChainSourceDocument sourceDocument) {
		CoordinatorContextImpl context = new CoordinatorContextImpl();
		context.setReferenceDocument(sourceDocument);
		context.setSystemType(sourceDocument.getSystemType());
		if (sourceDocument.getSourceDocumentType() != null) {
			context.setSourceDocumentType(sourceDocument.getSourceDocumentType());
		}

		InquiryRequest request = sourceDocumentToInquiryRequest(sourceDocument);
		TransactionContext transactionContext = sourceDocument.getTransactionContext();
		if (transactionContext != null) {
			context.setTransactionContext(transactionContext);
			context.setDeliveryMethod(transactionContext.getDeliveryMethod());
			context.setServiceClass(transactionContext.getServiceClass());
			if (SourceDocumentType.Quote.equals(sourceDocument.getSourceDocumentType())) {
				context.getTransactionContext().setQuoteContext(context);
				context.getTransactionContext().setQuote(sourceDocument);
			}
			if (SourceDocumentType.PurchaseOrder.equals(sourceDocument.getSourceDocumentType())) {
				context.getTransactionContext().setOrderContext(context);
				context.getTransactionContext().setOrder(sourceDocument);
			}
			if (transactionContext.getSupplyChain() != null) {
				context.setConfiguration(transactionContext.getSupplyChain().getConfiguration());
			}
			context.setDeliveryMethod(transactionContext.getDeliveryMethod());
			context.setServiceClass(transactionContext.getServiceClass());
		} else {
			context.setTransactionContext(new TransactionalContext());
		}

		FulfillmentOptions fulfillmentOptions = request.getFulfillmentOptions();
		if (fulfillmentOptions != null) {
			context.setFulfillmentOptions(request.getFulfillmentOptions());
		}
		context.setBillingOptions(request.getBillingOptions());
		context.setInquiryOptions(request.getInquiryOptions());
		context.setRequestTime(sourceDocument.getTransactionTimeStamp());
		context.setProcurementGroups(sourceDocument.getProcurementGroups());
		
		if(sourceDocument.getChargesAndDiscounts() != null) {
			context.setChargesAndDiscounts((Collection<ChargesImpl>) sourceDocument.getChargesAndDiscounts());
		}
		
//		if (sourceDocument.getProcurementGroups() != null) {
//			Optional<ProcurementGroup> optionalProcurementGroup = sourceDocument.getProcurementGroups().stream()
//					.findAny();
//			if (optionalProcurementGroup.isPresent()) {
//				ProcurementGroupContext procurementContext = ProcurementGroupContextConverters
//						.procurementGroupToContext(optionalProcurementGroup.get());
//				if (transactionContext != null) {
//					if (TransactionScope.Purchasing.equals(transactionContext.getTransactionScope())) {
//						procurementContext.setSupplierContext(context.getTransactionContext());
//					}
//					if (TransactionScope.Supplying.equals(transactionContext.getTransactionScope())) {
//						procurementContext.setCustomerContext(context.getTransactionContext());
//					}
//				}
//				context.setProcurementGroupContext(procurementContext);
//			}
//		}
		context.setFreightOptions(sourceDocument.getFreightOptions());
		if (sourceDocument.getSystemType() != null) {
			context.setSystemType(sourceDocument.getSystemType());
		}

		if (sourceDocument.getTo() != null && sourceDocument.getTo().getAccountNumber() != null) {
			context.setCustomerNumber(Long.valueOf(sourceDocument.getTo().getAccountNumber()));
		}

		SupplyChainParty from = sourceDocument.getFrom();
		if (from != null && from.getMember() instanceof SupplyChainBusiness) {
			BusinessContext bc = new BusinessContext((SupplyChainBusiness) from.getMember());
			context.setSupplier(bc);
		}

		SupplyChainParty to = sourceDocument.getTo();
		if (to != null && to.getMember() instanceof SupplyChainBusiness) {
			BusinessContext bc = new BusinessContext((SupplyChainBusiness) to.getMember());
			context.setBusinessContext(bc);
		}

		context.setDocumentId(sourceDocument.getDocumentId());
		context.setCustomerDocumentId(sourceDocument.getCustomerDocumentId());
		context.setSupplierDocumentId(sourceDocument.getVendorDocumentId());
		context.setTransactionStage(sourceDocument.getTransactionStage());
		context.setTransactionStatus(sourceDocument.getTransactionStatus());
		context.setOrderType(sourceDocument.getPurchaseOrderType());
		context.setNotes(sourceDocument.getNotes());

		for (SupplyChainLine supplyChainLine : sourceDocument.getLineItems()) {

			GenericLine lineItem = (GenericLine) supplyChainLine;

			LineItemContextImpl lineItemContextImpl = new LineItemContextImpl(context, lineItem);
			lineItemContextImpl.setId(lineItem.getLineId());
			lineItemContextImpl.setPrice(lineItem.getPrice());
//			lineItemContextImpl.getPriceLevels().add(new PriceLevelBuilder<>().withPriceFieldCode("WD_COST")
//					.withCurrentPrice(lineItem.getCost()).build());
//			lineItemContextImpl.getPriceLevels().add(new PriceLevelBuilder<>().withPriceFieldCode("WD_TRUE_COST")
//					.withCurrentPrice(lineItem.getTrueCost()).build());
//			lineItemContextImpl.getPriceLevels().add(new PriceLevelBuilder<>().withPriceFieldCode("WD_INV_COST")
//					.withCurrentPrice(lineItem.getInvoiceCost()).build());
//			lineItemContextImpl.getPriceLevels().add(new PriceLevelBuilder<>().withPriceFieldCode("WD_INV_CORE")
//					.withCurrentPrice(lineItem.getInvoiceCore()).build());

			if (lineItem.getLineNumber() != null) {
				lineItemContextImpl.setLineNumber(lineItem.getLineNumber());
			} else {
				lineItemContextImpl.setLineNumber(sourceDocument.getLineItems().indexOf(lineItem));
			}

			if (request != null) {
				Optional<RequestItem> optionalItem = request.getLineItems().stream()
						.filter(i -> i.getLineNumber() != null)
						.filter(i -> i.getLineNumber().equals(supplyChainLine.getLineNumber())).findAny();
				if (optionalItem.isPresent()) {
					HandlingOptions handlingOptions = optionalItem.get().getHandlingOptions();
					if (handlingOptions != null) {
						lineItemContextImpl.setHandlingOptions(handlingOptions);
						lineItemContextImpl.setMustGo(handlingOptions.getMustGoActive());
					}
				}
			}
			lineItemContextImpl.setNotes(lineItem.getNotes());
			context.getLineItems().add(lineItemContextImpl);
			List<Freight> fulfillmentLocations = lineItem.getFulfillmentLocations();
			for (Freight fulfillmentLocation : fulfillmentLocations) {
				FulfillmentLocationContext fulfillmentLocationContext = null;
				Optional<FulfillmentLocationContext> optionalFillContext = Optional.empty();
				if (context.getFulfillmentSequence() != null && fulfillmentLocation.getLocationName() != null) {
					optionalFillContext = context.getFulfillmentSequence().stream()
							.filter(i -> i.getLocation() !=null)
							.filter(i -> fulfillmentLocation.getLocationName().equals(i.getLocation())).findAny();
				}
				if (optionalFillContext.isPresent()) {
					fulfillmentLocationContext = optionalFillContext.get();
				} else {
					fulfillmentLocationContext = OrderFillContextTypeConverter
							.freightToFulfillmentLocationContext(fulfillmentLocation);

					context.getFulfillmentSequence().add(fulfillmentLocationContext);
					fulfillmentLocationContext.setOrder(context);
					fulfillmentLocationContext.getNonStockContext().setSystemType(fulfillmentLocation.getSystemType());
					fulfillmentLocationContext.setTransfers(sourceDocument.getNumberOfTransfers());
				}

				Availability availability = new Availability(lineItemContextImpl, fulfillmentLocationContext);
				if (fulfillmentLocation.getPlannedFillQuantity() != null) {
					availability.setFillQuantity(fulfillmentLocation.getPlannedFillQuantity());
				}
				if (fulfillmentLocation.getAvailableQuantity() != null) {
					availability.setQuantityOnHand(fulfillmentLocation.getAvailableQuantity());
				}
				PartAvailability partAvailability = new PartAvailability();
				partAvailability.setBuildingCode(fulfillmentLocation.getLocationName());
				if (fulfillmentLocation.getAvailableQuantity() != null) {
					partAvailability.setQuantityOnHand(fulfillmentLocation.getAvailableQuantity());
				}
				availability.setPartAvailability(partAvailability);
			}
		}
		return context;
	}

	@Converter
	public static InquiryRequest sourceDocumentToInquiryRequest(SupplyChainSourceDocument supplyChainDocument) {
		TransactionContext transactionContext = supplyChainDocument.getTransactionContext();
		InquiryRequest request = new InquiryRequest();
		if (transactionContext != null) {
			if (transactionContext.getRequest() != null) {
				return transactionContext.getRequest();
			}
			transactionContext.setRequest(request);
		}

		SupplyChainParty to = supplyChainDocument.getTo();
		if (to != null) {
			request.setAccountNumber(to.getAccountNumber());
		}
		for (SupplyChainLine lineItem : supplyChainDocument.getLineItems()) {
			RequestItem item = new RequestItem(lineItem);
			request.getLineItems().add(item);
		}
		return request;
	}

	@Converter
	public static SupplyChainSourceDocument coordinatorContextToSupplyChainSourceDocument(
			CoordinatorContext coordinatorContext) {

		com.autowares.supplychain.model.SupplyChainSourceDocumentImpl.Builder<?> sourceDocumentBuilder = SupplyChainSourceDocumentImpl
				.builder();

		TransactionScope transactionScope = TransactionScope.Supplying;
		if (coordinatorContext.getTransactionContext() != null) {
			coordinatorContext.getTransactionContext().setDeliveryMethod(coordinatorContext.getDeliveryMethod());
			coordinatorContext.getTransactionContext().setServiceClass(coordinatorContext.getServiceClass());
			
			if (coordinatorContext.getTransactionContext().getTransactionScope() != null) {
				transactionScope = coordinatorContext.getTransactionContext().getTransactionScope();
			}
		}

		sourceDocumentBuilder.withDocumentId(coordinatorContext.getDocumentId())
				.withCustomerDocumentId(coordinatorContext.getCustomerDocumentId())
				.withVendorDocumentId(coordinatorContext.getSupplierDocumentId())
				.withSourceDocumentType(coordinatorContext.getSourceDocumentType())
				.withTransactionContext(coordinatorContext.getTransactionContext())
				.withTransactionStage(coordinatorContext.getTransactionStage())
				.withTransactionStatus(coordinatorContext.getTransactionStatus())
				.withFreightOptions(coordinatorContext.getFreightOptions())
				.withSystemType(coordinatorContext.getSystemType())
				.withPurchaseOrderType(coordinatorContext.getOrderType());

		
		BigDecimal totalChargesAndDiscounts = BigDecimal.ZERO;
		
		if(coordinatorContext.getChargesAndDiscounts() != null) {
			
			Collection<ChargesImpl> charges = (Collection<ChargesImpl>) coordinatorContext.getChargesAndDiscounts();
			
			sourceDocumentBuilder.withChargesAndDiscounts(charges);
			
			for(ChargesImpl charge : charges) {
				if(ChargeType.Charge.equals(charge.getChargeType())) {
					totalChargesAndDiscounts = totalChargesAndDiscounts.add(charge.getCharge());
				} else {
					totalChargesAndDiscounts = totalChargesAndDiscounts.subtract(charge.getCharge());
				}
			}
			
		}
		
		SupplyChainParty account = SupplyChainParty.builder().fromAccount(coordinatorContext.getBusinessContext())
				.withPartyType(PartyType.Buying).build();
		
		SupplyChainParty supplier = SupplyChainParty.builder().fromParty(coordinatorContext.getSellingParty())
				.withPartyType(PartyType.Selling).build();
		
		if (coordinatorContext.getShipTo() != null
				&& coordinatorContext.getShipTo().getBusinessDetail().getBusEntId() != 0) {
			sourceDocumentBuilder.withParty(SupplyChainParty.builder().fromAccount(coordinatorContext.getShipTo())
					.withPartyType(PartyType.ShipTo).build());
		}
		sourceDocumentBuilder.withParty(account).withParty(supplier).withFrom(supplier).withTo(account);

		if (coordinatorContext instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext coordinatorOrderContext = (CoordinatorOrderContext) coordinatorContext;
			List<FulfillmentLocationContext> vendorContexts = coordinatorOrderContext.getFulfillmentSequence().stream()
					.filter(i -> LocationType.Vendor.equals(i.getLocationType())).collect(Collectors.toList());

			for (FulfillmentLocationContext fillContext : vendorContexts) {
				CoordinatorContext nonStockContext = fillContext.getNonStockContext();
				if (fillContext.isBeingFilledFrom()) {
					sourceDocumentBuilder.withProcurementGroup(nonStockContext.getProcurementGroupContext());
				}
				for (SupplyChainNote note : nonStockContext.getNotes()) {
					sourceDocumentBuilder.withNote(note);
				}
			}
		} else {
			sourceDocumentBuilder.withProcurementGroups(coordinatorContext.getProcurementGroups());
		}

		for (LineItemContext lineItem : coordinatorContext.getLineItems()) {
			com.autowares.supplychain.model.GenericLine.Builder<?> lineItemBuilder = GenericLine.builder()
					.fromPartLineItem(lineItem).withLineId(lineItem.getId()).withDemandModel(lineItem.getDemand());
			if (TransactionScope.Purchasing.equals(transactionScope)) {
				lineItemBuilder.withCustomerLineNumber(lineItem.getLineNumber());
			}
			if (TransactionScope.Supplying.equals(transactionScope) && lineItem.getCustomerLineNumber() != null) {
				lineItemBuilder.withCustomerLineNumber(lineItem.getCustomerLineNumber().intValue());
			}
			if (SourceDocumentType.PurchaseOrder.equals(coordinatorContext.getSourceDocumentType())) {
				lineItemBuilder.withLineType(SupplyChainLineType.PurchaseLine);
			}
			if (SourceDocumentType.Quote.equals(coordinatorContext.getSourceDocumentType())) {
				lineItemBuilder.withLineType(SupplyChainLineType.QuoteLine);
			}
			if (SourceDocumentType.PackSlip.equals(coordinatorContext.getSourceDocumentType())) {
				lineItemBuilder.withLineType(SupplyChainLineType.ShipmentLine);
				lineItemBuilder.withPurchasedQuantity(lineItem.getQuantity());
			}

			Part part = lineItem.getPart();
			lineItemBuilder.withPrice(lineItem.getPrice());
			if (part != null) {
				lineItemBuilder.withCoreCharge(part.getCorePrice());
				lineItemBuilder.withBrandAaiaId(part.getBrandAaiaId());
				lineItemBuilder.withVendorCode(part.getVendorCodeSubCode());
				BigDecimal itemPrice = lineItem.getPrice();
				if (itemPrice != null) {
					if (part.getCorePrice() != null) {
						itemPrice = itemPrice.add(part.getCorePrice());
					}
				}

				if (part.getPriceLevels() != null) {
					Optional<PriceLevel> wdCost = part.getPriceLevels().stream()
							.filter(p -> "WD Price".equals(p.getPriceFieldCode())).findAny();
					// For Mark : True Cost.
					Optional<PriceLevel> wdTrueCost = part.getPriceLevels().stream()
							.filter(p -> "Invoice WD".equals(p.getPriceFieldCode())).findAny();
					// For Mark : WD Invoice Cost.
					Optional<PriceLevel> wdInvCost = part.getPriceLevels().stream()
							.filter(p -> "Net WD".equals(p.getPriceFieldCode())).findAny();
					// wdInvCore is not a defined price field. mmey_owner.price_fields.
					Optional<PriceLevel> wdInvCore = part.getPriceLevels().stream()
							.filter(p -> "Net WD CORE".equals(p.getPriceFieldCode())).findAny();

					if (wdCost.isPresent()) {
						lineItemBuilder.withCost(wdCost.get().getCurrentPrice());
					}
					if (wdTrueCost.isPresent()) {
						lineItemBuilder.withTrueCost(wdTrueCost.get().getCurrentPrice());
					}
					if (wdInvCost.isPresent()) {
						lineItemBuilder.withInvoiceCost(wdInvCost.get().getCurrentPrice());
					}
					if (wdInvCore.isPresent()) {
						lineItemBuilder.withInvoiceCore(wdInvCore.get().getCurrentPrice());
					}
				}
			}
			lineItemBuilder.withNotes(lineItem.getNotes());
			sourceDocumentBuilder.withLineItem(lineItemBuilder.build());
		}

		sourceDocumentBuilder.withNotes(coordinatorContext.getNotes());

		// Needed to copy data on the original document through the context as the
		// context does not model everything defined in supplychain
		if (coordinatorContext.getReferenceDocument() instanceof SupplyChainSourceDocument) {
			SupplyChainSourceDocument originalDocument = (SupplyChainSourceDocument) coordinatorContext
					.getReferenceDocument();
			sourceDocumentBuilder.withVendorDocumentId(originalDocument.getVendorDocumentId());
			sourceDocumentBuilder.withSupplyChainId(originalDocument.getSupplyChainId());
			sourceDocumentBuilder.withCreationDate(originalDocument.getCreationDate());
			sourceDocumentBuilder.withCompletionDate(originalDocument.getCompletionDate());
			sourceDocumentBuilder.withFreightCharge(originalDocument.getFreightCharge());
			sourceDocumentBuilder.withTotalDiscountsAndCharges(originalDocument.getTotalDiscountsAndCharges());
			sourceDocumentBuilder.withNetTerms(originalDocument.getNetTerms());
			sourceDocumentBuilder.withBatchDate(originalDocument.getBatchDate());
		}
		
		if(totalChargesAndDiscounts != null && !totalChargesAndDiscounts.equals(BigDecimal.ZERO)) {
			sourceDocumentBuilder.withTotalDiscountsAndCharges(totalChargesAndDiscounts);
		}

		return sourceDocumentBuilder.build();
	}

	@Converter
	public static CoordinatorContext inquiryRequestToQuoteContext(InquiryRequest inquiry) {
		CoordinatorContextImpl coordinatorContextImpl = new CoordinatorContextImpl();
		String documentId = UUID.randomUUID().toString();
		coordinatorContextImpl.setDocumentId(documentId);

		TransactionContext transactionContext = TransactionContext.builder()
				.withTransactionScope(TransactionScope.Supplying).withRequest(inquiry)
				.withTransactionReferenceId(documentId).build();
		coordinatorContextImpl.setTransactionContext(transactionContext);
		if (inquiry.getInquiryOptions() != null) {
			coordinatorContextImpl.setInquiryOptions(inquiry.getInquiryOptions());
		}
		if (inquiry.getFulfillmentOptions() != null) {
			coordinatorContextImpl.setFulfillmentOptions(inquiry.getFulfillmentOptions());
			coordinatorContextImpl.setServiceClass(inquiry.getFulfillmentOptions().getServiceClass());
			coordinatorContextImpl.setDeliveryMethod(inquiry.getFulfillmentOptions().getDeliveryMethod());
		}
		if (inquiry.getAccountNumber() != null) {
			try {
				coordinatorContextImpl.setCustomerNumber(Long.parseLong(inquiry.getAccountNumber()));
			} catch (NumberFormatException e) {
				throw new RuntimeException(e);
			}
		}
		if (inquiry.getShipTo() != null) {
			coordinatorContextImpl.setShipTo(new BusinessContext(inquiry.getShipTo()));
		}
		if (inquiry.getNotes() != null) {
			for (Note note : inquiry.getNotes()) {
				coordinatorContextImpl.updateProcessingLog(note.getMessage());
			}
		}
		for (RequestItem lineItem : inquiry.getLineItems()) {
			LineItemContextImpl lineItemContext = new LineItemContextImpl(coordinatorContextImpl, lineItem);
			// Keep the supplied value intact if possible as this is intended to be used for
			// correlation
			if (lineItem.getLineNumber() != null) {
				lineItemContext.setLineNumber(lineItem.getLineNumber());
			} else {
				lineItemContext.setLineNumber(inquiry.getLineItems().indexOf(lineItem));
			}
			lineItemContext.setPrice(lineItem.getPrice());
			lineItemContext.setHandlingOptions(lineItem.getHandlingOptions());
			for (NoteImpl note : lineItem.getNotes()) {
				lineItemContext.getNotes().add(SupplyChainNote.builder().withMessage(note.getMessage()).build());
			}
			coordinatorContextImpl.getLineItems().add(lineItemContext);
		}

		// Save state as ready for processing
		coordinatorContextImpl.setTransactionStage(TransactionStage.New);
		coordinatorContextImpl.setTransactionStatus(TransactionStatus.Processing);

		return coordinatorContextImpl;
	}

	@Converter
	public static InquiryResponse coordinatorContextToInquiryResponse(CoordinatorContext context) {
		InquiryResponseBuilder responseBuilder = new InquiryResponseBuilder();
		responseBuilder.withDocumentReferenceId(context.getDocumentId());
		if (context.getShipTo() != null) {
			responseBuilder.withShipTo(context.getShipTo());
		}

		for (LineItemContext lineItem : context.getLineItems()) {
			ResponseItemBuilder itemBuilder = convertLineItemContext(lineItem);
			

			if (lineItem.getInvalid() || lineItem.getAvailability().size() == 0
					|| 0 == lineItem.getAvailability().stream().mapToInt(i -> i.getQuantityOnHand()).sum()) {
				FreightBuilder locationBuilder = new FreightBuilder().withLocationName("Not fillable");
				itemBuilder.withPlannedFillQuantity(0);
				itemBuilder.withFulfillmentLocation(locationBuilder.build());
			}

			for (Availability availability : lineItem.getAvailability()) {
				// TODO if we want we can get to non stock availability through
				// availability.getMatchingLineItem();
				// TODO we should clean this up to populate the availability on the main context
				if (availability.getMatchingLineItem() != null) {
					try {
						availability = availability.getMatchingLineItem().getAvailability().get(0);
					} catch (Exception e) {
					}
				}
				
				if(availability.getFulfillmentLocation() != null) {
					if(availability.getFulfillmentLocation().getNonStockContext() != null) {
						CoordinatorContext nonStockContext = availability.getFulfillmentLocation().getNonStockContext();
						if(PurchaseOrderType.SpecialOrder.equals(nonStockContext.getOrderType())) {
							if(itemBuilder instanceof SpecialOrderResponseItemBuilder) {
								SpecialOrderResponseItemBuilder sb = (SpecialOrderResponseItemBuilder)itemBuilder;
								sb.withSpecialOrderSystem(nonStockContext.getSystemType());
							}
						}
					}
				}

				if (context.getInquiryOptions().getPlanFulfillment()) {
					if (0 < availability.getFillQuantity()) {
						Freight location = convertAvailability(availability);
						if (location != null) {
							itemBuilder.addAvailableQuantity(availability.getQuantityOnHand());
							itemBuilder.addPlannedFillQuantity(availability.getFillQuantity());
							itemBuilder.withFulfillmentLocation(location);
						}
					}
				} else {
					if (availability.getQuantityOnHand() > 0
							|| SystemType.AwiWarehouse.equals(availability.getFulfillmentLocation().getSystemType())) {
						Freight location = convertAvailability(availability);
						if (location != null) {
							itemBuilder.addAvailableQuantity(availability.getQuantityOnHand());
							itemBuilder.addPlannedFillQuantity(availability.getFillQuantity());
							itemBuilder.withFulfillmentLocation(location);
						}
						FulfillmentLocationContext fulfillmentLocationContext = availability.getFulfillmentLocation();
						SupplyChainSourceDocument referenceDocument = fulfillmentLocationContext.getReferenceDocument();
						if (referenceDocument != null) {
							if (SourceDocumentType.Quote.equals(referenceDocument.getSourceDocumentType())) {
								for (Freight freightOption : referenceDocument.getFreightOptions()
										.getAvailableFreight()) {
									freightOption.setLocationName(location.getLocationName());
									freightOption.setLocationType(location.getLocationType());
									freightOption.setSystemType(location.getSystemType());
									freightOption.setDocumentReferenceId(location.getDocumentReferenceId());
									itemBuilder.withFulfillmentLocation(freightOption);
								}
							}
						}

					}
				}

			}

			responseBuilder.withResponseItem(itemBuilder.build());
		}
		return responseBuilder.build();
	}

	@Converter
	public static CoordinatorContext lineItemContextToCoordinatorContext(LineItemContext lineItem) {
		return lineItem.getContext();
	}

	@Converter
	public static CoordinatorContext documentContextToContext(DocumentContext documentContext) {
		return documentContext.getContext();
	}

	private static Freight convertAvailability(Availability availability) {
		FulfillmentLocationContext fulfillmentLocation = availability.getFulfillmentLocation();
		if (fulfillmentLocation == null) {
			return null;
		}
		Freight freight = OrderFillContextTypeConverter.orderFillContextToFreight(fulfillmentLocation);
		freight.setSequence(getFulfillmentOrder(fulfillmentLocation));
		freight.setAvailableQuantity(availability.getQuantityOnHand());
		freight.setPlannedFillQuantity(availability.getFillQuantity());
		freight.setPrice(availability.getProcurementCost());
		if (availability.getPartAvailability() != null) {
			// Use part availability as fulfillmentLocation is Building specific, not
			// warehouse number
			freight.setWarehouseNumber(Integer.valueOf(availability.getPartAvailability().getWarehouseNumber()));
		}
		return freight;
	}

	private static ResponseItemBuilder convertLineItemContext(LineItemContext orderDetail) {
		ResponseItemBuilder itemBuilder = new SpecialOrderResponseItemBuilder().fromPartLineItem(orderDetail);
		if (orderDetail.getPart() != null) {
			itemBuilder.withPart(orderDetail.getPart());
		}
		if (orderDetail.getShortageCode() != null) {
			itemBuilder.withShortageCode(orderDetail.getShortageCode().toString());
		}
		return itemBuilder;
	}

	private static Integer getFulfillmentOrder(FulfillmentLocationContext fillContext) {

		Comparator<ZonedDateTime> nullSafeDateComparator = Comparator.nullsLast(ZonedDateTime::compareTo);
		Comparator<Integer> nullSafeIntegerComparator = Comparator.nullsLast(Integer::compareTo);
		Comparator<FulfillmentLocationContext> transferComparator = Comparator
				.comparing(FulfillmentLocationContext::getTransfers, nullSafeIntegerComparator);
		Comparator<FulfillmentLocationContext> arrivalComparator = Comparator
				.comparing(FulfillmentLocationContext::getArrivalDate, nullSafeDateComparator);
		Comparator<FulfillmentLocationContext> finalComparator = arrivalComparator.thenComparing(transferComparator);

		CoordinatorContext context = fillContext.getOrder();
		int sequence = 0;

		context.getFulfillmentSequence().stream().sorted(finalComparator);
		for (Iterator<FulfillmentLocationContext> i = context.getFulfillmentSequence().iterator(); i.hasNext();) {
			if (i.next().equals(fillContext)) {
				return sequence;
			} else {
				sequence++;
			}
		}
		return sequence;
	}

}
