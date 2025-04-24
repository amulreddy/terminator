package com.autowares.mongoose.camel.components;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.ids.model.VendorMaster;
import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.apis.partservice.PartBase;
import com.autowares.invoicesservice.model.MoaInvoice;
import com.autowares.invoicesservice.model.MoaInvoiceLineItem;
import com.autowares.logistix.model.Shipment;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.CoordinatorOrderContextImpl;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.orders.model.Item;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.PartyConfigurationParty;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.FulfillmentLocation;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.PriceLevel;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.util.DateConversion;
import com.autowares.stockcheck.OrderPartRequestItem;
import com.autowares.stockcheck.OrderRequest;
import com.autowares.supplychain.model.DocumentSummary;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.PurchaseLine;
import com.autowares.supplychain.model.ShipmentLine;
import com.autowares.supplychain.model.SupplyChainBusiness;
import com.autowares.supplychain.model.SupplyChainBusinessImpl;
import com.autowares.supplychain.model.SupplyChainLine;
import com.autowares.supplychain.model.SupplyChainLocation;
import com.autowares.supplychain.model.SupplyChainParty;
import com.autowares.supplychain.model.SupplyChainShipment;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.xmlgateway.edi.base.EdiDocument;
import com.autowares.xmlgateway.edi.base.EdiRecord;
import com.autowares.xmlgateway.edi.records.BaseItemPO1;
import com.autowares.xmlgateway.edi.records.BeginningSegmentForPurchaseOrderBEG;
import com.autowares.xmlgateway.edi.records.DateTimeReferenceDTM;
import com.autowares.xmlgateway.edi.records.FunctionalGroupHeaderGS;
import com.autowares.xmlgateway.edi.records.FunctionalGroupTrailerGE;
import com.autowares.xmlgateway.edi.records.InterchangeControlHeaderISA;
import com.autowares.xmlgateway.edi.records.InterchangeControlTrailerIEA;
import com.autowares.xmlgateway.edi.records.NameN1;
import com.autowares.xmlgateway.edi.records.NameN3;
import com.autowares.xmlgateway.edi.records.NameN4;
import com.autowares.xmlgateway.edi.records.ReferenceIdentificationREF;
import com.autowares.xmlgateway.edi.records.TransactionSetHeaderST;
import com.autowares.xmlgateway.edi.records.TransactionSetTrailerSE;
import com.autowares.xmlgateway.edi.records.TransactionTotalsCTT;
import com.autowares.xmlgateway.edi.types.DTMCode;
import com.autowares.xmlgateway.edi.types.PurchaseOrderTypeCode;
import com.autowares.xmlgateway.edi.types.REFCode;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.RequestItem;

@Component
@Converter(generateLoader = true)
public class OrderFillContextTypeConverter implements TypeConverters {

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	@Converter
	public static SupplyChainSourceDocument orderFillContextToSupplyChainPurchaseOrder(
			FulfillmentLocationContext fulfillmentLocation) {
		com.autowares.supplychain.model.SupplyChainSourceDocumentImpl.Builder<?> orderBuilder = SupplyChainSourceDocumentImpl
				.builder();
		CoordinatorContext nonStockContext = fulfillmentLocation.getNonStockContext();
		SystemType systemType = fulfillmentLocation.getSystemType();
		orderBuilder// .withFreightOptions(nonStockContext.getFreightOptions())
				.withVendorDocumentId(nonStockContext.getSupplierDocumentId())
				.withTransactionStage(TransactionStage.Open).withSystemType(systemType)
				.withTransactionContext(nonStockContext.getTransactionContext())
				.withProcurementGroups(nonStockContext.getProcurementGroups());
		if (nonStockContext.getTransactionContext() != null
				&& nonStockContext.getTransactionContext().getSupplyChain() != null) {
			buildProcuringSupplyChainParties(orderBuilder, nonStockContext.getTransactionContext().getSupplyChain());
		}

		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			PurchaseLine lineItem = PurchaseLine.builder().fromPartLineItem(availability.getLineItem())
					.withVendorCode(availability.getLineItem().getVendorCode())
					.withQuantity(availability.getFillQuantity()).build();
			orderBuilder.withLineItem(lineItem);
		}
		orderBuilder.withNotes(nonStockContext.getNotes());

		return orderBuilder.build();
	}

	@Converter
	public static Freight orderFillContextToFreight(FulfillmentLocationContext orderFillContext) {
		Freight freight = new Freight();

		Shipment shipment = orderFillContext.getShipment();
		if (shipment != null) {
			if (shipment.getArrivalRun() != null) {
				freight.setDeliveryRunName(shipment.getArrivalRun().getRunName());
			}
		}
		ZonedDateTime deliveryDate = orderFillContext.getArrivalDate();
		BusinessDetail supplier = null;
		String description = orderFillContext.getDescription();

		CoordinatorContext nonStockContext = orderFillContext.getNonStockContext();
		if (nonStockContext.getSupplier() != null) {

			if (nonStockContext.getSupplier() != null && nonStockContext.getSupplier().getBusinessDetail() != null) {
				supplier = nonStockContext.getSupplier().getBusinessDetail();
				description = supplier.getBusinessName();
			}
			if (deliveryDate == null) {
				if (supplier != null && supplier.getVendorMaster() != null) {
					description = description + " - " + supplier.getVendorMaster().getSoleadtime();
				} else {
					deliveryDate = ZonedDateTime.now().plusWeeks(3);
					description = description + " - 3 to 5 Weeks";
				}
			}
		}

		if (SystemType.AwiWarehouse.equals(orderFillContext.getSystemType())
				|| SystemType.MotorState.equals(orderFillContext.getSystemType())) {
			freight.setCarrier("Autowares");
			if (orderFillContext.getWarehouseMaster() != null) {
				freight.setWarehouseNumber(orderFillContext.getWarehouseMaster().getWarehouseNumber());
				if (orderFillContext.getWarehouseMaster().getWarehouseName() != null) {
					description = orderFillContext.getWarehouseMaster().getWarehouseName();
				}
			}
		}

		freight.setEstimatedDeliveryDateTime(deliveryDate);
		freight.setDescription(description);
		freight.setDeliveryRunType(orderFillContext.getDeliveryRunType());
		freight.setEstimatedShipDateTime(orderFillContext.getNextDeparture());
		freight.setLocationType(orderFillContext.getLocationType());
		freight.setLocationName(orderFillContext.getLocation());
		freight.setSystemType(orderFillContext.getSystemType());
		freight.setTrackingNumber(orderFillContext.getTrackingNumber());
		freight.setDeliveryMethod(orderFillContext.getDeliveryMethod());
		if (orderFillContext.getShippingMethod() != null) {
			freight.setShippingMethod(orderFillContext.getShippingMethod().name());
		}
		freight.setServiceClass(orderFillContext.getServiceClass());
		freight.setExpireTime(orderFillContext.getExpireTime());
		return freight;
	}

	@Converter
	public static SupplyChainShipment orderFillContextToSupplyChainPackslip(
			FulfillmentLocationContext fulfillmentLocation) {
		com.autowares.supplychain.model.SupplyChainShipment.Builder<?> shipmentBuilder = SupplyChainShipment.builder();
		CoordinatorContext orderContext = fulfillmentLocation.getOrder();

		SupplyChainParty shipTo = SupplyChainParty.builder().fromAccount(orderContext.getBusinessContext())
				.withPartyType(PartyType.ShipTo).build();

		if (orderContext.getShipTo() != null
				&& orderContext.getBusinessContext().getBusinessDetail().getBusEntId() != 0) {
			shipTo = SupplyChainParty.builder().fromAccount(orderContext.getShipTo()).withPartyType(PartyType.ShipTo)
					.build();
		}

		SupplyChainParty shipFrom = SupplyChainParty.builder().fromParty(orderContext.getSellingParty())
				.withPartyType(PartyType.ShipFrom).build();

		FreightOptions freightOptions = new FreightOptions();
		if (fulfillmentLocation.getOrder().getFreightOptions() != null
				&& fulfillmentLocation.getOrder().getFreightOptions().getSelectedFreight() != null) {
			Freight selectedFreight = copyFreight(fulfillmentLocation.getOrder().getFreightOptions().getSelectedFreight());
			freightOptions.setSelectedFreight(selectedFreight);
		} else {
			Freight selectedFreight = orderFillContextToFreight(fulfillmentLocation);
			selectedFreight
					.setDeliveryRunType(RunType.find(orderContext.getDeliveryMethod(), orderContext.getServiceClass()));
			freightOptions.setSelectedFreight(selectedFreight);
		}

		TransactionalContext transactionContext = orderContext.getTransactionContext();
		transactionContext.setDeliveryMethod(orderContext.getDeliveryMethod());
		transactionContext.setServiceClass(orderContext.getServiceClass());

		shipmentBuilder.withTransactionStage(fulfillmentLocation.getTransactionStage())
				.withDocumentId(fulfillmentLocation.getFulfillmentLocationId().toString())
				.withVendorDocumentId(fulfillmentLocation.getTrackingNumber())
				.withSystemType(fulfillmentLocation.getSystemType())
				.withLocation(SupplyChainLocation.builder().withName(fulfillmentLocation.getLocation()).build())
				.withProcurementGroups(fulfillmentLocation.getNonStockContext().getProcurementGroups())
				.withTransactionContext(transactionContext)
				.withTransactionStatus(fulfillmentLocation.getTransactionStatus()).withFreightOptions(freightOptions)
				.withParty(shipTo).withTo(shipTo).withFrom(shipFrom)
				.withNumberOfTransfers(fulfillmentLocation.getTransfers());

		if (orderContext instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext coordinatorOrderContext = (CoordinatorOrderContext) orderContext;
			shipmentBuilder.withPurchaseOrderType(coordinatorOrderContext.getOrderType());
		}

		WarehouseMaster wm = fulfillmentLocation.getWarehouseMaster();
		if (wm != null && wm.getBusinessBase() != null) {
			SupplyChainBusiness warehouse = SupplyChainBusinessImpl.builder()
					.withMoaBusinessId(wm.getBusinessBase().getBusEntId())
					.withBusinessName(wm.getBusinessBase().getBusinessName()).build();
			SupplyChainParty sourceParty = SupplyChainParty.builder().withMember(warehouse)
					.withPartyType(PartyType.ShipFrom).build();
			shipmentBuilder.withParty(sourceParty);
			shipmentBuilder.withFrom(sourceParty);
		}

		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			if (availability.getFillQuantity() > 0) {
				ShipmentLine lineItem = ShipmentLine.builder().fromPartLineItem(availability.getLineItem())
						.withVendorCode(availability.getLineItem().getVendorCode())
						// Quantity/ShipQuantity should be 0 as we are just planning at this point
						.withQuantity(0).withShippedQuantity(0).withPurchasedQuantity(availability.getFillQuantity())
						.build();
				shipmentBuilder.withLineItem(lineItem);
			}
		}

		return shipmentBuilder.build();
	}

	// TODO: Merge with the Speical Orders freight copy...
	public static Freight copyFreight(Freight freightObject) {
		Freight freightCopy = new Freight();
//		freightCopy.setFulfillmentLocationId(freightObject.getFulfillmentLocationId());
		freightCopy.setCarrier(freightObject.getCarrier());
		freightCopy.setCarrierCode(freightObject.getCarrierCode());
		freightCopy.setCost(freightObject.getCost());
		freightCopy.setDescription(freightObject.getDescription());
//		freightCopy.setFreightId(freightObject.getFreightId());
		freightCopy.setShippingCode(freightObject.getShippingCode());
		freightCopy.setShippingMethod(freightObject.getShippingMethod());
		freightCopy.setLocationName(freightObject.getLocationName());
		freightCopy.setLocationType(freightObject.getLocationType());
		freightCopy.setSystemType(freightObject.getSystemType());
		freightCopy.setEstimatedShipDateTime(freightObject.getEstimatedShipDateTime());
		freightCopy.setEstimatedDeliveryDateTime(freightObject.getEstimatedDeliveryDateTime());
		if (freightObject.getDeliveryMethod() != null && freightObject.getServiceClass() != null) {
			freightCopy.setDeliveryRunType(
					RunType.find(freightObject.getDeliveryMethod(), freightObject.getServiceClass()));
		}
		freightCopy.setDeliveryMethod(freightObject.getDeliveryMethod());
		freightCopy.setServiceClass(freightObject.getServiceClass());
		freightCopy.setWarehouseNumber(freightObject.getWarehouseNumber());
		freightCopy.setSequence(freightObject.getSequence());
		freightCopy.setDescription(freightObject.getDescription());
		freightCopy.setDeliveryRunName(freightObject.getDeliveryRunName());
		freightCopy.setTrackingNumber(freightObject.getTrackingNumber());
		return freightCopy;
	}

	@Converter
	public static FulfillmentLocationContext freightToFulfillmentLocationContext(Freight freight) {
		FulfillmentLocationContext fulfillmentLocationContext = fulfillmentLocationToFulfillLocationContext(freight);
		if (freight.getShippingMethod() != null) {
			try {
				fulfillmentLocationContext.setShippingMethod(DeliveryMethod.valueOf(freight.getShippingMethod()));
			} catch (Exception e) {
				fulfillmentLocationContext.setShippingMethod(DeliveryMethod.Carrier);
			}
		}
		return fulfillmentLocationContext;
	}

	@Converter
	public static FulfillmentLocationContext fulfillmentLocationToFulfillLocationContext(
			FulfillmentLocation fulfillmentLocation) {
		FulfillmentLocationContext fulfillmentLocationContext = new FulfillmentLocationContext();
		fulfillmentLocationContext.setLocation(fulfillmentLocation.getLocationName());
		fulfillmentLocationContext.setLocationType(fulfillmentLocation.getLocationType());
		fulfillmentLocationContext.setDescription(fulfillmentLocation.getDescription());
		fulfillmentLocationContext.setSystemType(fulfillmentLocation.getSystemType());
		fulfillmentLocationContext.setTrackingNumber(fulfillmentLocation.getTrackingNumber());
		fulfillmentLocationContext.setProcurementGroupId(fulfillmentLocation.getProcurementGroupId());
		fulfillmentLocationContext.setArrivalDate(fulfillmentLocation.getEstimatedDeliveryDateTime());
		fulfillmentLocationContext.setDeliveryMethod(fulfillmentLocation.getDeliveryMethod());
		fulfillmentLocationContext.setServiceClass(fulfillmentLocation.getServiceClass());
		if (fulfillmentLocation.getDocumentReferenceId() != null) {
			fulfillmentLocationContext.setDocumentId(fulfillmentLocation.getDocumentReferenceId());
			fulfillmentLocationContext.setFulfillmentLocationId(fulfillmentLocation.getDocumentReferenceId());
			fulfillmentLocationContext.getNonStockContext().setDocumentId(fulfillmentLocation.getDocumentReferenceId());
		}
		if (fulfillmentLocation.getWarehouseNumber() != null) {
			WarehouseMaster wm = new WarehouseMaster();
			wm.setWarehouseNumber(fulfillmentLocation.getWarehouseNumber());
			fulfillmentLocationContext.setWarehouseMaster(wm);
		}
		return fulfillmentLocationContext;
	}

	@Converter
	public static CoordinatorContext coordinatorContext(FulfillmentLocationContext orderFillContext) {
		return orderFillContext.getOrder();
	}

	@Converter
	public static FulfillmentLocationContext orderPackslipToFulfillmentLocationContext(SupplyChainShipment shipment) {
		// Find the FulfillmentLocation object that represents this shipment.
		Optional<Freight> location = shipment.getLineItems().stream().map(i -> i.getFulfillmentLocations())
				.flatMap(List::stream).filter(f -> f.getDocumentReferenceId().equals(shipment.getDocumentId()))
				.findAny();
		FulfillmentLocationContext fulfillmentLocationContext = null;
		if (location.isPresent()) {
			Freight fulfillmentLocation = location.get();
			fulfillmentLocationContext = freightToFulfillmentLocationContext(fulfillmentLocation);
			fulfillmentLocationContext.setReferenceDocument(shipment);
			fulfillmentLocationContext.setFulfillmentLocationId(shipment.getDocumentId());
			fulfillmentLocationContext.setTransactionStage(shipment.getTransactionStage());
			fulfillmentLocationContext.setTransactionStatus(shipment.getTransactionStatus());
			fulfillmentLocationContext.setTransfers(shipment.getNumberOfTransfers());
			for (GenericLine shipmentLine : shipment.getLineItems()) {
				LineItemContext lineItemContext = new LineItemContextImpl(shipmentLine);
				Availability availability = new Availability(lineItemContext, fulfillmentLocationContext);
				availability.setFillQuantity(shipmentLine.getPurchasedQuantity());
			}
			CoordinatorOrderContextImpl orderContext = new CoordinatorOrderContextImpl();
			Optional<DocumentSummary> optionalSummary = shipment.getRelatedDocuments().stream()
					.filter(d -> SourceDocumentType.PurchaseOrder.equals(d.getSourceDocumentType())).findAny();
			if (optionalSummary.isPresent()) {
				DocumentSummary summary = optionalSummary.get();
				orderContext.setDocumentId(summary.getDocumentId());
				fulfillmentLocationContext.setOrder(orderContext);
				return fulfillmentLocationContext;
			}
			throw new AbortProcessingException("No document id found.");
		}
		throw new AbortProcessingException("No fulfullment location found.");
	}
	
	public static FulfillmentLocationContext shippingEstimateToFulfillmentLocationContext(SupplyChainSourceDocument shipment, CoordinatorContext context) {
		// Find the FulfillmentLocation object that represents this shipment.
//		Optional<Freight> location = shipment.getLineItems().stream().map(i -> i.getFulfillmentLocations())
//				.flatMap(List::stream).filter(f -> f.getDocumentReferenceId().equals(shipment.getDocumentId()))
//				.findAny();
		Optional<Freight> location = shipment.getFreightOptions().getAvailableFreight().stream().findAny();
		FulfillmentLocationContext fulfillmentLocationContext = null;
		if (location.isPresent()) {
			Freight fulfillmentLocation = location.get();
			fulfillmentLocationContext = freightToFulfillmentLocationContext(fulfillmentLocation);
			fulfillmentLocationContext.setReferenceDocument(shipment);
			fulfillmentLocationContext.setFulfillmentLocationId(shipment.getDocumentId());
			fulfillmentLocationContext.setTransactionStage(shipment.getTransactionStage());
			fulfillmentLocationContext.setTransactionStatus(shipment.getTransactionStatus());
			fulfillmentLocationContext.setTransfers(shipment.getNumberOfTransfers());
			for (SupplyChainLine shipmentLine : shipment.getLineItems()) {
				LineItemContext lineItemContext = new LineItemContextImpl(shipmentLine);
				Availability availability = new Availability(lineItemContext, fulfillmentLocationContext);
				availability.setFillQuantity(shipmentLine.getQuantity());
			}
//			CoordinatorContextImpl orderContext = new CoordinatorContextImpl();
//			Optional<DocumentSummary> optionalSummary = shipment.getRelatedDocuments().stream()
//					.filter(d -> SourceDocumentType.Quote.equals(d.getSourceDocumentType())).findAny();
//			if (optionalSummary.isPresent()) {
//				DocumentSummary summary = optionalSummary.get();
//				orderContext.setDocumentId(summary.getDocumentId());
				fulfillmentLocationContext.setOrder(context);
				return fulfillmentLocationContext;
//			}
//			throw new AbortProcessingException("No document id found.");
		}
		throw new AbortProcessingException("No fulfullment location found.");
	}

	@Converter
	public static MoaInvoice fulfillmentLocationContextToMoaInvoice(FulfillmentLocationContext fulfillmentLocation) {
		CoordinatorOrderContext context = (CoordinatorOrderContext) fulfillmentLocation.getOrder();

		MoaInvoice invoice = new MoaInvoice();

		if (context.getCustomerNumber() != null) {
			invoice.setCustomerNumber(context.getCustomerNumber().intValue());
		}

		BusinessContext businessContext = context.getBusinessContext();
		BusinessDetail businessDetail = null;

		if (businessContext != null) {
			businessDetail = businessContext.getBusinessDetail();
			invoice.setCwStno(businessDetail.getCwStoreNo());
		}

		invoice.setInvoiceTimeStamp(ZonedDateTime.now());
		TransactionContext transactionContext = context.getTransactionContext();
		if (transactionContext != null) {
			if (transactionContext.getRequest() != null) {
				invoice.setOrderTimestamp(transactionContext.getRequest().getTimeStamp());
				invoice.setPurchaseorder(transactionContext.getRequest().getCustomerDocumentId());
			}
			String xmlOrderId = transactionContext.getTransactionReferenceId();
			invoice.setXmlOrderId(xmlOrderId);
			invoice.setDocumentId(xmlOrderId);
		}
		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			LineItemContext lineItemContext = availability.getLineItem();
			MoaInvoiceLineItem invoiceLineItem = new MoaInvoiceLineItem(lineItemContext);
			// TODO Enumerate the source prog flag. ( Invoice source )
			invoiceLineItem.setSourceprogflag("o");
//					if (context.getOrderSource() != null) {
//						invoiceLineItem.setOrdersourceflag(context.getOrderSource().getViperSourceFlag());
//					}

			int shipQuantity = 0;
			if (availability.getOperationalContext() != null) {
				shipQuantity = availability.getOperationalContext().getShippedCount();
			}
			if (!(SystemType.AwiWarehouse.equals(fulfillmentLocation.getSystemType()))) {
				shipQuantity = availability.getFillQuantity();
			}
			invoiceLineItem.setShipqty(shipQuantity);

			invoiceLineItem.setTranstype(1);

			PartBase part = null;

			if (lineItemContext.getPart() != null) {
				part = lineItemContext.getPart();
				invoiceLineItem.setPartDescription(part.getShortDescription());

				invoiceLineItem.setCorevalue(part.getCorePrice());
				invoiceLineItem.setUnitOfMeasure(part.getMinWarehouseSellUom());
				invoiceLineItem.setPartClass(String.valueOf(part.getMovementClass()));
				invoiceLineItem.setPackageqty(part.getPackageQuantity());
				invoiceLineItem.setPreprice(BigDecimal.ZERO);
				invoiceLineItem.setWeight(part.getWeight());
				invoiceLineItem
						.setPriceChangeDate(DateConversion.convert(lineItemContext.getPart().getPriceChangeDate()));
				invoiceLineItem.setNetinvoiceflag(part.getNetInvoicePart());

				if (shipQuantity == 0 && part.getAvailability() != null) {
					for (PartAvailability partAvailability : part.getAvailability()) {
						if (partAvailability.getQuantityOnBackorder() != null
								&& partAvailability.getQuantityOnBackorder() > 0) {
							invoiceLineItem.setOnFactoryBackorder(true);
						}
					}
				}

				// TODO Fix this.
//				invoiceLineItem.setHazmatptr(part.geth);
			} else {
				String vendorCodeSubCode = "^^^^";
				invoiceLineItem.setPartDescription("NO FILE??");
				if (lineItemContext.getLineCode() != null) {
					vendorCodeSubCode = lineItemContext.getLineCode();
				}
				if (context.getTransactionContext() != null) {
					InquiryRequest request = context.getTransactionContext().getRequest();
					if (request != null) {
						Optional<RequestItem> requestItem = request.getLineItems().stream()
								.filter(i -> i.getLineNumber().equals(lineItemContext.getLineNumber())).findAny();
						if (requestItem.isPresent()) {
							vendorCodeSubCode = requestItem.get().getLineCode();
						}
					}
				}
				invoiceLineItem.setVendorCodeSubCode(StringUtils.substring(vendorCodeSubCode, 0, 2) + "^^");
			}

			if (availability.getMoaOrderDetail() != null) {

				Item item = availability.getMoaOrderDetail();
				if (item.getOrder() != null) {
					if (invoice.getOrderTimestamp() == null) {
						invoice.setOrderTimestamp(item.getOrder().getOrderTime());
					}
					if (invoice.getPurchaseorder() == null) {
						invoice.setPurchaseorder(item.getOrder().getPurchaseOrderNumber());
					}
					if (invoice.getXmlOrderId() == null) {
						invoice.setXmlOrderId(item.getOrder().getXmlOrderId());
					}
					if (invoice.getDocumentId() == null) {
						invoice.setDocumentId(item.getOrder().getXmlOrderId());
					}
				}

				if (item.getOrderItemId() != null) {
					invoiceLineItem.setOrderId(item.getOrderItemId().longValue());
				}

				if (lineItemContext.getMustGo() != null && lineItemContext.getMustGo()) {
					invoiceLineItem.setMustGo("Y");
				} else {
					invoiceLineItem.setMustGo("N");
				}

				invoiceLineItem.setCoreBank("N"); // TODO: Verify with Greg.

				/**
				 * We don't know what preprice is but this is how viper calculates it.
				 */
				if (item.getBillprice() != null) {
					String divisor = "0.6";
					if ("PER".equals(item.getBuilding())) {
						divisor = "0.7";
					}
					BigDecimal prePrice = item.getBillprice().divide(new BigDecimal(divisor), 2, RoundingMode.HALF_UP);
					invoiceLineItem.setPreprice(prePrice.setScale(2, RoundingMode.HALF_UP));
				}

				if (part != null && part.getPriceLevels() != null) {
					Optional<PriceLevel> wdCost = part.getPriceLevels().stream()
							.filter(p -> "WD Price".equals(p.getPriceFieldCode())).findAny();
					Optional<PriceLevel> wdTrueCost = part.getPriceLevels().stream()
							.filter(p -> "Invoice WD".equals(p.getPriceFieldCode())).findAny();
					Optional<PriceLevel> wdInvCost = part.getPriceLevels().stream()
							.filter(p -> "Net WD".equals(p.getPriceFieldCode())).findAny();
					Optional<PriceLevel> wdInvCore = part.getPriceLevels().stream()
							.filter(p -> "Net WD CORE".equals(p.getPriceFieldCode())).findAny();

					if (wdCost.isPresent()) {
						invoiceLineItem.setWdCost(wdCost.get().getCurrentPrice());
					} else {
						invoiceLineItem.setWdCost(BigDecimal.ZERO);
					}
					if (wdTrueCost.isPresent()) {
						invoiceLineItem.setWdTrueCost(wdTrueCost.get().getCurrentPrice());
					} else {
						invoiceLineItem.setWdTrueCost(BigDecimal.ZERO);
					}
					if (wdInvCost.isPresent()) {
						invoiceLineItem.setWdInvCost(wdInvCost.get().getCurrentPrice());
					} else {
						invoiceLineItem.setWdInvCost(BigDecimal.ZERO);
					}
					if (wdInvCore.isPresent()) {
						invoiceLineItem.setWdInvCore(wdInvCore.get().getCurrentPrice());
					} else {
						invoiceLineItem.setWdInvCore(BigDecimal.ZERO);
					}
				}

				if (lineItemContext.getQuantity() > 0) {
					invoiceLineItem.setOrdertypeflag("D"); // Debit
				} else if (lineItemContext.getQuantity() < 0) {
					invoiceLineItem.setOrdertypeflag("C"); // Credit
				}

				if (item.getZone() != null) {
					invoiceLineItem.setZone(String.valueOf(item.getZone()));
				}

				if (item.getPullerId() != null) {
					invoiceLineItem.setPullerId(item.getPullerId());
				}

				if (item.getHazmatId() == null) {
					invoiceLineItem.setHazmatId(0);
				} else {
					invoiceLineItem.setHazmatId(item.getHazmatId().intValue());
				}

				if (item.getTruckRunId() != null) {
					invoiceLineItem.setTruckRunPtr(item.getTruckRunId());
				}

				String shippingMethod = "";
				if (item.getShippingMethod() != null) {
					shippingMethod = item.getShippingMethod().toString();
				}

				switch (shippingMethod) {
				case "2": // PSX/Express Delivery
					invoiceLineItem.setTranstype(3);
					break;
				case "P": // Pickup
					invoiceLineItem.setTranstype(4);
					break;
				case "U": // UPS / Carrier
					invoiceLineItem.setTranstype(2);
					break;
				default: // Night
					invoiceLineItem.setTranstype(1);
					break;
				}

				invoiceLineItem.setDeliveryTimestamp(item.getDeliveryTime());
				invoiceLineItem.setWarehouse(item.getWarehouse());
				invoiceLineItem.setShipper(item.getShipperNumber());
//				invoiceLineItem.setWdTrueCost(); Not sure where to get this
//				invoiceLineItem.setWdInvCost(); Not sure where to get this either.

				if (item.getSpecialFlag() != null) {
					invoiceLineItem.setSpecialhandlingflag(item.getSpecialFlag());
				}

			}
			invoice.getLineItems().add(invoiceLineItem);
		}
		return invoice;
	}

	@Converter
	public static OrderRequest fulfillmentLocationContextToCounterWorksOrderRequest(
			FulfillmentLocationContext fulfillmentLocation) {
		OrderRequest request = new OrderRequest();
		CoordinatorContext context = fulfillmentLocation.getOrder();
		if (context instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext orderContext = (CoordinatorOrderContext) context;
			request.setPoNumber(orderContext.getPurchaseOrder());
			if (orderContext.getBusinessContext().getShop() != null) {
				request.setCustnum(orderContext.getBusinessContext().getShop().getStoreAccountNumber());
			}
			// TODO DeliveryMethod
		}

		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			request.getItems().add(new OrderPartRequestItem(availability.getLineItem()));
		}
		return request;
	}

	@Converter
	public static FulfillmentLocationContext documentContextToFulfillmentLocationContext(
			DocumentContext documentContext) {
		return documentContext.getFulfillmentLocationContext();
	}

	@Converter
	public EdiDocument fulfillmentLocationContextToEdiDocument(FulfillmentLocationContext fulfillmentLocation) {
		return coordinatorContextToEdiDocument(fulfillmentLocation.getDocumentContext());
	}

	@Converter
	public EdiDocument coordinatorContextToEdiDocument(DocumentContext documentContext) {
		CoordinatorContext supplierOrderContext = null;
		CoordinatorContext customerOrderContext = null;

		Optional<ProcurementGroupContext> optionalProcurement = documentContext.getProcurementGroupContexts().stream()
				.findAny();

		if (optionalProcurement.isPresent()) {
			supplierOrderContext = optionalProcurement.get().getSupplierContext().getOrderContext();
			customerOrderContext = optionalProcurement.get().getCustomerContext().getOrderContext();
		}

		String interchangeControlNumber = StringUtils.right(String.valueOf(System.currentTimeMillis()), 9);
		String transactionSetControlNumber = "22222";
		String groupControlNumber = "3333";
		if (supplierOrderContext != null && supplierOrderContext.getTransactionContext() != null
				&& supplierOrderContext.getTransactionContext().getOrder() != null) {
			transactionSetControlNumber = String
					.valueOf(supplierOrderContext.getTransactionContext().getOrder().getSupplyChainId());
			groupControlNumber = String
					.valueOf(supplierOrderContext.getTransactionContext().getOrder().getSupplyChainId());
		}
		ZonedDateTime transactionTime = supplierOrderContext.getTransactionTimeStamp();

		if (transactionTime == null && supplierOrderContext instanceof CoordinatorOrderContextImpl) {
			CoordinatorOrderContextImpl impl = (CoordinatorOrderContextImpl) supplierOrderContext;
			impl.setTransactionTime(ZonedDateTime.now());
		}
		BusinessContext billTo = supplierOrderContext.getBusinessContext();
		BusinessContext shipTo = supplierOrderContext.getBusinessContext();
		@SuppressWarnings("unused")
		Configuration customerConfig = null;
		Configuration supplierConfig = null;
		PurchaseOrderType orderType = PurchaseOrderType.PurchaseOrder;

		if (customerOrderContext != null) {
			orderType = customerOrderContext.getOrderType();
			if (PurchaseOrderType.DropShip.equals(orderType)) {
				shipTo = customerOrderContext.getBusinessContext();
				customerConfig = customerOrderContext.getConfiguration();
			}
		}
		if (supplierOrderContext != null) {
			supplierConfig = supplierOrderContext.getConfiguration();
		}
		String gcomReceiverId = null;
		String gcomQualifier = null;
		VendorMaster vendorMaster = null;
		if (supplierOrderContext.getSupplier() != null
				&& supplierOrderContext.getSupplier().getBusinessDetail() != null) {
			vendorMaster = supplierOrderContext.getSupplier().getBusinessDetail().getVendorMaster();
			if (vendorMaster != null) {
				gcomReceiverId = vendorMaster.getGcomreceiverid();
				gcomQualifier = vendorMaster.getGcomreceiverqualifier();
			}
		}
		if (supplierConfig != null && !"prod".equals(activeProfile)) {
			if (supplierConfig.getSettings().get("testGcomReceiverId") != null) {
				gcomReceiverId = (String) supplierConfig.getSettings().get("testGcomReceiverId");
			}
		}

		EdiDocument doc = new EdiDocument();
		List<EdiRecord> records = new ArrayList<>();
		doc.setRecords(records);
		InterchangeControlHeaderISA isa = new InterchangeControlHeaderISA();
		isa.setInterchangeReceiverId(gcomReceiverId);
		isa.setInterchangeIdQualifierReceiver(gcomQualifier);
		isa.setInterchangeControlNumber(interchangeControlNumber);
		isa.setInterchangeDate(supplierOrderContext.getTransactionDate());
		isa.setInterchangeTime(supplierOrderContext.getTransactionTime());
		if (!"prod".equals(activeProfile)) {
			isa.setTestOrProduction("T");
		}
		// TODO Not returned
		isa.setInterchangeSenderId("6162432125");
		// TODO Not returned - VCSC/Prodline?
//		isa.setInterchangeReceiverId(member.getGcommerceId());

		records.add(isa.toEdiRecord());

		FunctionalGroupHeaderGS gs = new FunctionalGroupHeaderGS();
		gs.setDate(supplierOrderContext.getTransactionDate());
		gs.setTime(supplierOrderContext.getTransactionTime());
		gs.setGroupControlNumber(groupControlNumber);
		gs.setFunctionalIdentifierCode("PO");
		gs.setApplicationSenderCode("6162432125");
		if (gcomReceiverId != null) {
			gs.setApplicationReceiverCode(gcomReceiverId);
		}
		records.add(gs.toEdiRecord());

		TransactionSetHeaderST st = new TransactionSetHeaderST();
		st.setTransactionSetControlNumber(transactionSetControlNumber);
		st.setTransactionSetIdentifierCode("850");
		records.add(st.toEdiRecord());

		BeginningSegmentForPurchaseOrderBEG beg = new BeginningSegmentForPurchaseOrderBEG();
		beg.setDate(supplierOrderContext.getTransactionDate());
		beg.setPurchaseOrderNumber(supplierOrderContext.getDocumentId());
		if (PurchaseOrderType.DropShip.equals(orderType)) {
			beg.setPurchaseOrderTypeCode(PurchaseOrderTypeCode.DropShip);
		}
		records.add(beg.toEdiRecord());

		if (vendorMaster != null) {
			ReferenceIdentificationREF ref = new ReferenceIdentificationREF();
			ref.setReferenceIdentificationQualifier(REFCode.ProductGroup);
			ref.setReferenceIdentification(vendorMaster.getCwLineCd());
			records.add(ref.toEdiRecord());
		}

		if (customerOrderContext != null && customerOrderContext.getCustomerDocumentId() != null) {
			ReferenceIdentificationREF ref = new ReferenceIdentificationREF();
			ref.setReferenceIdentificationQualifier(REFCode.EndUserPurchaseOrderNumber);
			ref.setReferenceIdentification(customerOrderContext.getCustomerDocumentId());
			records.add(ref.toEdiRecord());
		}

		DateTimeReferenceDTM dtm = new DateTimeReferenceDTM();
		dtm.setDate(supplierOrderContext.getTransactionDate());
		dtm.setTime(supplierOrderContext.getTransactionTime());
		dtm.setDateTimeQualifier(DTMCode.RequestedShip);
		records.add(dtm.toEdiRecord());

		@SuppressWarnings("unused")
		com.autowares.xmlgateway.edi.types.EntityType entityType = com.autowares.xmlgateway.edi.types.EntityType.BillToParty;
		// TODO Handle N1-N4 Bill To Location.

		NameN1 btN1 = new NameN1();
		btN1.setEntityIdentifierCode("BT");
		btN1.setIdentiferCodeQualifier("91");
		// TODO - Replace with Buyer account - check Viper code for logic
		btN1.setIdentificationCode("01");
		btN1.setName(billTo.getBusinessDetail().getBusinessName());
		records.add(btN1.toEdiRecord());
		addBasicBusinessData(billTo.getBusinessDetail(), records);

		NameN1 stN1 = new NameN1();
		stN1.setEntityIdentifierCode("ST");
		stN1.setIdentiferCodeQualifier("92");
		stN1.setIdentificationCode(shipTo.getAccountNumber());
		stN1.setName(shipTo.getBusinessDetail().getBusinessName());
		records.add(stN1.toEdiRecord());
		addBasicBusinessData(shipTo.getBusinessDetail(), records);

		int hashTotal = 0;
		for (LineItemContext line : supplierOrderContext.getLineItems()) {

			hashTotal += line.getQuantity();
			BaseItemPO1 baseItem = new BaseItemPO1();
			baseItem.setLineNumber(line.getLineNumber());
			baseItem.setBuyerPartNumber(line.getPartNumber());
			baseItem.setVendorPartNumber(line.getPartNumber());
			baseItem.setQuantity(line.getQuantity());
			if (line.getPart() != null) {
				Optional<PriceLevel> wdCost = line.getPart().getPriceLevels().stream()
						.filter(i -> "WD Price".equals(i.getPriceFieldCode())).findAny();

				if (wdCost.isPresent()) {
					baseItem.setPrice(wdCost.get().getCurrentPrice());
				}
			}
			baseItem.setUnitOfMeasure("EA");
			baseItem.setUnitPriceCode("EA");
			if (line.getManufacturerLineCode() != null) {
				baseItem.setManufactuerCodeQualifier("MF");
				baseItem.setManufacturerCode(line.getManufacturerLineCode());
			}
			records.add(baseItem.toEdiRecord());
		}

		TransactionTotalsCTT ctt = new TransactionTotalsCTT();
		ctt.setNumberofLineItems(supplierOrderContext.getLineItems().size());
		ctt.setHashTotal(String.valueOf(hashTotal));
		records.add(ctt.toEdiRecord());

		TransactionSetTrailerSE se = new TransactionSetTrailerSE();
		se.setTransactionControlNumber(transactionSetControlNumber);
		se.setNumberOfSegments(9);
		records.add(se.toEdiRecord());

		FunctionalGroupTrailerGE ge = new FunctionalGroupTrailerGE();
		ge.setGroupControlNumber(groupControlNumber);
		ge.setNumberOfTransactionSets(1);
		records.add(ge.toEdiRecord());

		InterchangeControlTrailerIEA iea = new InterchangeControlTrailerIEA();
		iea.setNumberOfFunctionalGroups(1);
		iea.setInterchangeControlNumber(interchangeControlNumber);
		records.add(iea.toEdiRecord());
		return doc;
	}

	private static void addBasicBusinessData(BusinessDetail businessDetail, List<EdiRecord> records) {
		NameN3 n3 = new NameN3();
		n3.setAddressInformation1(businessDetail.getAddress());
		n3.setAddressInformation2(businessDetail.getAddress2());
		records.add(n3.toEdiRecord());

		NameN4 n4 = new NameN4();
		n4.setCityName(businessDetail.getCity());
		n4.setStateProv(businessDetail.getStateProv());
		n4.setPostalCode(businessDetail.getPostalCode());
		n4.setCountryCode("USA");
		records.add(n4.toEdiRecord());
	}

	private static SupplyChainSourceDocumentImpl.Builder<?> buildProcuringSupplyChainParties(
			SupplyChainSourceDocumentImpl.Builder<?> documentBuilder, SupplyChain supplyChain) {
		PartyConfigurationParty account = supplyChain.getProcuringPartnership().getAccount();
		SupplyChainParty buyingSupplyChainParty = SupplyChainParty.builder().fromParty(account).build();
		PartyConfigurationParty supplier = supplyChain.getProcuringPartnership().getSupplier();
		SupplyChainParty supplierSupplyChainParty = SupplyChainParty.builder().fromParty(supplier).build();
		documentBuilder.withParty(supplierSupplyChainParty).withParty(buyingSupplyChainParty);
		documentBuilder.withFrom(supplierSupplyChainParty);
		documentBuilder.withTo(buyingSupplyChainParty);
		if (supplyChain.getProcuringSystem() != null) {
			SystemType systemType = supplyChain.getProcuringSystem().getSystemType();
			documentBuilder.withSystemType(systemType);
		}
		return documentBuilder;
	}

	public static SupplyChainSourceDocument orderFillContextToShipmentEstimate(
			FulfillmentLocationContext fulfillmentLocation) {
		SupplyChainSourceDocumentImpl.Builder<?> shipmentBuilder = SupplyChainSourceDocumentImpl.builder();
		CoordinatorContext orderContext = fulfillmentLocation.getOrder();

		SupplyChainParty shipTo = SupplyChainParty.builder().fromAccount(orderContext.getBusinessContext())
				.withPartyType(PartyType.ShipTo).build();

		if (orderContext.getShipTo() != null
				&& orderContext.getBusinessContext().getBusinessDetail().getBusEntId() != 0) {
			shipTo = SupplyChainParty.builder().fromAccount(orderContext.getShipTo()).withPartyType(PartyType.ShipTo)
					.build();
		}

		SupplyChainParty shipFrom = SupplyChainParty.builder().fromParty(orderContext.getSellingParty())
				.withPartyType(PartyType.ShipFrom).build();
		FreightOptions freightOptions = new FreightOptions();
		if (fulfillmentLocation.getNonStockContext() != null
				&& fulfillmentLocation.getNonStockContext().getFreightOptions() != null) {
			freightOptions=fulfillmentLocation.getNonStockContext().getFreightOptions();
		} else {

			Freight selectedFreight = orderFillContextToFreight(fulfillmentLocation);
			selectedFreight
					.setDeliveryRunType(RunType.find(orderContext.getDeliveryMethod(), orderContext.getServiceClass()));
			freightOptions.setSelectedFreight(selectedFreight);
		}

		TransactionalContext transactionContext = orderContext.getTransactionContext();
		transactionContext.setDeliveryMethod(orderContext.getDeliveryMethod());
		transactionContext.setServiceClass(orderContext.getServiceClass());

		shipmentBuilder.withSourceDocumentType(SourceDocumentType.ShippingEstimate)
				.withTransactionStage(fulfillmentLocation.getTransactionStage())
				.withDocumentId(fulfillmentLocation.getFulfillmentLocationId().toString())
				.withVendorDocumentId(fulfillmentLocation.getTrackingNumber())
				.withSystemType(fulfillmentLocation.getSystemType())
				.withLocation(SupplyChainLocation.builder().withName(fulfillmentLocation.getLocation()).build())
				.withProcurementGroups(fulfillmentLocation.getNonStockContext().getProcurementGroups())
				.withTransactionContext(transactionContext)
				.withTransactionStatus(fulfillmentLocation.getTransactionStatus()).withFreightOptions(freightOptions)
				.withParty(shipTo).withTo(shipTo).withFrom(shipFrom)
				.withNumberOfTransfers(fulfillmentLocation.getTransfers());

		if (orderContext instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext coordinatorOrderContext = (CoordinatorOrderContext) orderContext;
			shipmentBuilder.withPurchaseOrderType(coordinatorOrderContext.getOrderType());
		}

		WarehouseMaster wm = fulfillmentLocation.getWarehouseMaster();
		if (wm != null && wm.getBusinessBase() != null) {
			SupplyChainBusiness warehouse = SupplyChainBusinessImpl.builder()
					.withMoaBusinessId(wm.getBusinessBase().getBusEntId())
					.withBusinessName(wm.getBusinessBase().getBusinessName()).build();
			SupplyChainParty sourceParty = SupplyChainParty.builder().withMember(warehouse)
					.withPartyType(PartyType.ShipFrom).build();
			shipmentBuilder.withParty(sourceParty);
			shipmentBuilder.withFrom(sourceParty);
		}

		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			if (availability.getFillQuantity() > 0) {
				ShipmentLine lineItem = ShipmentLine.builder().fromPartLineItem(availability.getLineItem())
						.withVendorCode(availability.getLineItem().getVendorCode())
						// Quantity/ShipQuantity should be 0 as we are just planning at this point
						.withQuantity(0).withShippedQuantity(0).withPurchasedQuantity(availability.getFillQuantity())
						.build();
				shipmentBuilder.withLineItem(lineItem);
			}
		}

		return shipmentBuilder.build();
	}

}