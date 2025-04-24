package com.autowares.mongoose.camel.processors.integration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.events.ViperOrderUpdateEvent;
import com.autowares.mongoose.model.IntegrationContext;
import com.autowares.mongoose.model.OperationalEventContext;
import com.autowares.mongoose.model.OperationalEventType;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.mongoose.service.ViperToWmsCoreUtils;
import com.autowares.orders.model.Item;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.supplychain.commands.SourceDocumentClient;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.ShipmentLine;
import com.autowares.supplychain.model.SupplyChainLocation;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainShipment;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.google.common.collect.Lists;

@Component
public class UpdatePackslipShipQuantity implements Processor {

	@Autowired
	private ViperToWmsCoreUtils viperToWmsCoreUtils;

	@Autowired
	private SupplyChainService supplyChainService;

	private static Logger log = LoggerFactory.getLogger(UpdatePackslipShipQuantity.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		SourceDocumentClient sourceDocumentClient = supplyChainService.getClient();
		try {
			IntegrationContext integrationContext = exchange.getIn().getBody(IntegrationContext.class);

			List<OperationalEventContext> pullOrPackEvents = Lists.newArrayList();

			pullOrPackEvents.addAll(integrationContext.getOperationalEvents().stream()
					.filter(i -> i.getOperationalEventType() == OperationalEventType.Pulled)
					.collect(Collectors.toList()));
			pullOrPackEvents.addAll(integrationContext.getOperationalEvents().stream()
					.filter(i -> i.getOperationalEventType() == OperationalEventType.Packed)
					.collect(Collectors.toList()));

			for (OperationalEventContext pullContext : pullOrPackEvents) {
				Integer quantity = pullContext.getQuantity();
				if (quantity != null && quantity > 0) {
					String buildingCode = integrationContext.getLocation().getName();
					Long moaOrderId = pullContext.getMoaOrderDetailId();
					String xmlOrderId = pullContext.getLineItemContext().getContext().getDocumentId();
					Integer lineNumber = pullContext.getLineItemContext().getLineNumber();

					Item moaOrderDetail = null;
					moaOrderDetail = viperToWmsCoreUtils.lookupProdOrderByDetails(moaOrderId);
					moaOrderDetail = viperToWmsCoreUtils.resolveOrderId(moaOrderDetail);

					Optional<SupplyChainSourceDocumentImpl> purchaseOrderResponse = sourceDocumentClient
							.findSourceDocumentByDocumentId(xmlOrderId, SourceDocumentType.PurchaseOrder);
					if (!purchaseOrderResponse.isEmpty()) {
						SupplyChainSourceDocumentImpl purchaseOrder = purchaseOrderResponse.get();
						Optional<GenericLine> optionalItem = purchaseOrder.getLineItems().stream()
								.filter(i -> i.getLineNumber() != null && i.getLineNumber().equals(lineNumber))
								.findAny();
						Optional<Freight> optionalFulfillmentLocation = purchaseOrder.getLineItems().stream()
								.map(i -> i.getFulfillmentLocations()).flatMap(List::stream)
								.filter(i -> i.getLocationName().equals(buildingCode)).findAny();
						if (optionalItem.isPresent()) {
							SupplyChainShipment packSlip = null;
							GenericLine purchaseLine = optionalItem.get();
							GenericLine shipmentLine = null;
							if (optionalFulfillmentLocation.isPresent()) {
								log.info("Found fulfillment location: referenceDocumentId = "
										+ optionalFulfillmentLocation.get().getDocumentReferenceIds().get(0));
								Optional<SupplyChainShipment> existingPackSlip = sourceDocumentClient
										.findSourceDocumentByDocumentId(
												optionalFulfillmentLocation.get().getDocumentReferenceIds().get(0),
												SourceDocumentType.PackSlip);
								if (existingPackSlip.isPresent()) {
									packSlip = existingPackSlip.get();
									Optional<GenericLine> optionalPackSlipItem = packSlip.getLineItems().stream()
											.filter(i -> i.getLineNumber().equals(lineNumber)).findAny();
									if (optionalPackSlipItem.isPresent()) {
										shipmentLine = optionalPackSlipItem.get();
									} else {
										// create newPackslipLine
										shipmentLine = ShipmentLine.builder().fromPartLineItem(purchaseLine).build();
										packSlip.getLineItems().add(shipmentLine);
										purchaseLine.getNotes().add(SupplyChainNote.builder()
												.withMessage("Viper shipping from discrepancy with Mongoose").build());
										sourceDocumentClient.saveSourceDocument(purchaseOrder);
									}
								}
							} else {
								FreightOptions freightOptions = new FreightOptions();
								Freight freight = new Freight();
								freight.setCarrier("Autowares");
								freight.setLocationName(buildingCode);
								freight.setLocationType(LocationType.Warehouse);
								freight.setSystemType(SystemType.AwiWarehouse);
								freight.setDescription("Viper/Mongoose mismatch - " + buildingCode);
								freightOptions.setSelectedFreight(freight);
								shipmentLine = ShipmentLine.builder().fromPartLineItem(purchaseLine).build();
								SupplyChainShipment newPackSlip = SupplyChainShipment.builder()
										.withPrimaryProcurementGroup(purchaseOrder.getPrimaryProcurementGroup())
										.withProcurementGroup(purchaseOrder.getPrimaryProcurementGroup())
										.withTransactionContext(purchaseOrder.getTransactionContext())
										.withLocation(SupplyChainLocation.builder()
												.withName(buildingCode).build())
										.withFreightOptions(freightOptions).withLineItem(shipmentLine).build();
								packSlip = newPackSlip;
							}

							if (integrationContext.getEvent() instanceof ViperOrderUpdateEvent) {
								// Behaves different for wms-core?
							}

							Integer shipQuantity = quantity;
							if (OperationalEventType.Pulled.equals(pullContext.getOperationalEventType())) {
								Integer alreadyShippedQuantity = shipmentLine.getShippedQuantity() != null ? shipmentLine.getShippedQuantity():0;
								shipQuantity = alreadyShippedQuantity + shipQuantity;
							}

							shipmentLine.setShippedQuantity(shipQuantity);
							shipmentLine.setQuantity(shipQuantity);
							sourceDocumentClient.saveSourceDocument(packSlip);

						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
