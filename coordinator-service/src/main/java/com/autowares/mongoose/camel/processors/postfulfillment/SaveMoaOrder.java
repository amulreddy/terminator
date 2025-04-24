package com.autowares.mongoose.camel.processors.postfulfillment;

import java.util.Comparator;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.apis.partservice.PrintNowFlag;
import com.autowares.logisticsservice.model.LogisticsCustomerRun;
import com.autowares.logistix.model.TargetType;
import com.autowares.mongoose.camel.components.IntegrationContextTypeConverter;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.Shop;
import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;
import com.autowares.orders.clients.OrderClient;
import com.autowares.orders.model.Item;
import com.autowares.orders.model.Order;
import com.autowares.orders.model.OrderProcessingLog;
import com.autowares.orders.model.ProcessStage;
import com.autowares.servicescommon.model.BillingOptions;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.HandlingPriorityType;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.supplychain.model.DemandModel;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.RequestItem;

@Component
public class SaveMoaOrder implements Processor {

	OrderClient orderClient = new OrderClient();

	private static Logger log = LoggerFactory.getLogger(SaveMoaOrder.class);

	@Override
	public void process(Exchange exchange) throws Exception {
//		orderClient.withLocalService();
		Order order = null;

		FulfillmentLocationContext fulfillmentLocationContext = null;
		CoordinatorOrderContext request = null;
		if (exchange.getIn().getBody() instanceof FulfillmentLocationContext) {
			fulfillmentLocationContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
		} else if (exchange.getIn().getBody() instanceof CoordinatorOrderContext) {
			request = exchange.getIn().getBody(CoordinatorOrderContext.class);
		} else if (exchange.getIn().getBody() instanceof DocumentContext) {
			fulfillmentLocationContext = exchange.getIn().getBody(DocumentContext.class)
					.getFulfillmentLocationContext();
		}

		if (fulfillmentLocationContext != null) {
			MotorstatePurchaseOrder mPO = exchange.getIn().getHeader("motorstatePurchaseOrder",
					MotorstatePurchaseOrder.class);
			CoordinatorOrderContext orderContext = (CoordinatorOrderContext) fulfillmentLocationContext.getOrder();
			order = startBuildingOrder(orderContext);

			for (Availability availability : fulfillmentLocationContext.getLineItemAvailability()) {
				// Normal fulfillment
				LineItemContext lineItem = availability.getLineItem();
				int locationFillQuantity = availability.getFillQuantity();
				if (locationFillQuantity > 0) {
					buildItem(order, lineItem, availability, locationFillQuantity, orderContext, mPO);
				}
			}
			log.info("Saving:  " + order.getCustomerNumber() + " " + order.getXmlOrderId() + " "
					+ order.getPurchaseOrderNumber() + " " + order.getOrderTime());
			try {
				order = orderClient.saveOrder(order).get();
			} catch (Exception e) {
				throw new RetryLaterException("Unable to save MOA Order: " + order.getXmlOrderId(), e);
			}
			log.info("Saved:  " + order.getCustomerNumber() + " " + order.getXmlOrderId() + " "
					+ order.getPurchaseOrderNumber() + " " + order.getOrderTime());
			// Set the order id for each item on the context.

			for (Availability availability : fulfillmentLocationContext.getLineItemAvailability()) {
				LineItemContext detail = availability.getLineItem();
				for (Item item : order.getItems()) {
					if (detail.getLineNumber().equals(item.getCustomerLineNumber())) {
						if (fulfillmentLocationContext.getLocation().equals(item.getBuilding())) {
							availability.setMoaOrderDetail(item);
							availability.setOperationalContext(
									IntegrationContextTypeConverter.moaItemToOperationalContext(item));
						}
					}
				}
			}
		}

		if (request != null) {
			order = startBuildingOrder(request);
			for (LineItemContext lineItem : request.getLineItems()) {

//				If unable to fill any items create fulfillment at first fulfillment in list 
//				else create fulfillment for each location with fill quantity greater than zero.

				log.debug("Saving detail: " + lineItem.getVendorCode() + " " + lineItem.getPartNumber());

				Integer totalFillQuantity = lineItem.getAvailability().stream().mapToInt(Availability::getFillQuantity)
						.sum();

				if (totalFillQuantity == 0) {
					Optional<Availability> optionalAvailability = lineItem.getAvailability().stream()
							.filter(i -> i.getNumberOfTransfers() != null)
							.sorted(Comparator.comparing(Availability::getNumberOfTransfers)).findFirst();
					if (optionalAvailability.isPresent()) {
						buildItem(order, lineItem, optionalAvailability.get(), 0, request, null);
					} else {
						Optional<Availability> warehouse10 = lineItem.getAvailability().stream()
								.filter(i -> i.getFulfillmentLocation().getWarehouseMaster() != null
										&& 10 == i.getFulfillmentLocation().getWarehouseMaster().getWarehouseNumber())
								.findAny();
						if (warehouse10.isPresent()) {
							buildItem(order, lineItem, warehouse10.get(), 0, request, null);
						} else {
							buildItem(order, lineItem, null, 0, request, null);
						}
					}
				}
				log.debug("Total Fill Quantity == " + totalFillQuantity + "  detail.getQuantity() == "
						+ lineItem.getQuantity());

			}
			log.info(order.getCustomerNumber() + " " + order.getXmlOrderId() + " " + order.getPurchaseOrderNumber()
					+ " " + order.getOrderTime());
			try {
				order = orderClient.saveOrder(order).get();
			} catch (Exception e) {
				throw new AbortProcessingException("Unable to save MOA Order: " + order.getXmlOrderId(), e);
			}
		}

		exchange.getIn().setHeader("persistedInMoa", true);

	}

	private Order startBuildingOrder(CoordinatorOrderContext request) {
		String purchaseOrder = request.getPurchaseOrder();
		Shop shop = request.getBusinessContext().getShop();
		if (shop != null) {
			purchaseOrder = shop.getStoreAccountNumber() + " " + shop.getBusinessName();
			purchaseOrder = StringUtils.truncate(purchaseOrder, 20);
		}

		Order order = new Order();
		order.setBusinessEntityId(request.getBusinessContext().getBusinessDetail().getBusEntId());
		order.setCustomerNumber(String.valueOf(request.getCustomerNumber()));
		if (request.getOrderTime() != null) {
			order.setOrderTime(request.getOrderTime());
		} else {
			order.setOrderTime(request.getRequestTime());
		}
		order.setPurchaseOrderNumber(purchaseOrder);
		order.setXmlOrderId(request.getXmlOrderId());
		order.setOrderSource(request.getOrderSource());
		order.setSourceOrderId(request.getSourceOrderId());
		order.setShipTo(request.getShipTo());
		MDC.put("customerNumber", order.getCustomerNumber());
		log.info("Saving MOA Order --- Customer == " + request.getCustomerNumber());
		return order;
	}

	private Item buildItem(Order order, LineItemContext detail, Availability availability, int shipQuantity,
			CoordinatorOrderContext orderContext, MotorstatePurchaseOrder mPO) {
		Item item = new Item();

		// Temporary
		item.setSourceFlag("o");

		InquiryRequest inquiryRequest = orderContext.getTransactionContext().getRequest();
		Optional<RequestItem> optionalRequestItem = inquiryRequest.getLineItems().stream()
				.filter(i -> i.getLineNumber().equals(detail.getLineNumber())).findAny();
		PrintNowFlag printNowFlag = null;
		if (optionalRequestItem.isPresent()) {
			RequestItem requestItem = optionalRequestItem.get();
			HandlingOptions handlingOptions = requestItem.getHandlingOptions();
			BillingOptions billingOptions = orderContext.getBillingOptions();
			item.setBillingOptions(billingOptions);
			if (billingOptions != null) {
				if (billingOptions.getForceToPaperSlip() != null && billingOptions.getForceToPaperSlip()) {
					printNowFlag = PrintNowFlag.ForcePrint;
				}
				if (billingOptions.getBillOnly() != null && billingOptions.getBillOnly()) {
					printNowFlag = PrintNowFlag.BillOnly;
				}
			}
			if (handlingOptions.getSkipASN()) {
				item.setSourceFlag("f");
			}
			if (handlingOptions.getHoldBackOrder()) {
				item.setHbo(true);
			}
		}

		PartAvailability partAvailability = null;
		HandlingPriorityType handlingPriorityType = null;
		if (availability != null) {
			FulfillmentLocationContext fulfillmentLocation = availability.getFulfillmentLocation();
			handlingPriorityType = availability.getHandlingPriorityType();
			final String location = fulfillmentLocation.getLocation();
			item.setPackslipId(fulfillmentLocation.getDocumentId());
			item.setTrackingNumber(fulfillmentLocation.getTrackingNumber());
			item.setBuilding(location);
			item.setDepartureTime(fulfillmentLocation.getNextDeparture());
			item.setDeliveryTime(fulfillmentLocation.getArrivalDate());
			LogisticsCustomerRun customerRun = fulfillmentLocation.getLogisticsCustomerRun();
			if (customerRun != null) {
				log.debug("Truck Run Assigned: " + customerRun.getTruckRun().getDeliveryMethod() + "  "
						+ customerRun.getTruckRun().getTruckRunName());
				item.setPriority(customerRun.getPriority());
				item.setTruckRunId(customerRun.getTruckRun().getTruckRunId().intValue());
				item.setCutOffSlot(customerRun.getCutOffSlot());

			} else {
				if (shipQuantity > 0 && !SystemType.MotorState.equals(fulfillmentLocation.getSystemType())) {
					throw new AbortProcessingException("Cannot save moa order without a customer run");
				}
			}
			partAvailability = availability.getPartAvailability();

			// To match what viper does, set mustgo if ordered Express and going night
			if (ServiceClass.Express.equals(orderContext.getServiceClass())
					&& RunType.nightDelivery.equals(fulfillmentLocation.getDeliveryRunType())) {
				detail.setMustGo(true);
			}

		}
		item.setOrderQuantity(detail.getQuantity());
		item.setShipQuantity(shipQuantity);
		item.setPlannedFillQuantity(shipQuantity);
		item.setProcessStage(ProcessStage.Allocated);
		item.setBillprice(detail.getPrice());
		if (detail.getLineCode() != null) {
			item.setVendorCode(StringUtils.substring(detail.getLineCode(), 0, 4));
		}
		item.setPartNumber(detail.getPartNumber());
		log.debug("Billprice = " + detail.getPrice());
		Part part = detail.getPart();

		if (part != null) {
			item.setPartHeaderId(part.getMoaPartHeaderId());
			item.setPartNumber(part.getPartNumber());
			item.setLineCode(part.getCwLineCode());
			item.setVendorCode(part.getVendorCodeSubCode());
			item.setCorevalue(part.getCorePrice());
			item.setHazmatId(part.getHazmatId());
			if (partAvailability != null) {

				log.debug("Availability = " + partAvailability.getBuildingCode() + ":" + partAvailability.getZone()
						+ ":" + partAvailability.getLocation());
				item.setZone(partAvailability.getZone());
				if (partAvailability.getLocation() != null) {
					item.setLocation(partAvailability.getLocation().intValue());
					if (printNowFlag == null) {
						printNowFlag = partAvailability.getPrintNow();
					}
				}
				item.setWarehouse((int) partAvailability.getWarehouseNumber());
				if (printNowFlag != null) {
					switch (printNowFlag) {
					case BillOnly:
						item.setForcedFlag('N');
						break;
					case ForcePrint:
						item.setForcedFlag('Y');
						break;
					}
				}
			}
		}

		item.setOrderType(getOrderType(handlingPriorityType, availability, printNowFlag));
		item.setMustGo(detail.getMustGo());

		if (handlingPriorityType != null) {
			if (handlingPriorityType.getShipType() != null) {
				item.setShippingMethod(handlingPriorityType.getShipType().charAt(0));
			} else {
				if (availability != null) {
					if (HandlingPriorityType.OrderOnHold.equals(handlingPriorityType) && DeliveryMethod.CustomerPickUp
							.equals(availability.getFulfillmentLocation().getShippingMethod())) {
						item.setShippingMethod('P');
					}
					if (HandlingPriorityType.OrderOnHold.equals(handlingPriorityType) && (RunType.expressDelivery
							.equals(availability.getFulfillmentLocation().getDepartureRunType())
							|| RunType.expressPickup
									.equals(availability.getFulfillmentLocation().getDepartureRunType()))) {
						item.setShippingMethod('2');
					}
				}
			}
		}

		if (orderContext.getPsxToBeDelivered()) {
			item.setTobedelivered('Y');
		} else {
			item.setTobedelivered('N');
		}
		item.setCustomerLineNumber(detail.getLineNumber());
		for (SupplyChainNote note : orderContext.getNotes()) {
			OrderProcessingLog entry = new OrderProcessingLog();
			entry.setMessage(note.getMessage());
			item.getProcessingLog().add(entry);
		}
		DemandModel demand = detail.getDemand();
		if (demand != null) {
			for (SupplyChainNote note : demand.getNotes()) {
				OrderProcessingLog entry = new OrderProcessingLog();
				entry.setMessage(note.getMessage());
				item.getProcessingLog().add(entry);
			}
		}
		if (mPO != null) {
			// Motorstate Purchase Order. Check for errors.
			Boolean hasErrors = false;
			String errorCode = null;
			String errorDescription = null;
			if (mPO.getHasErrors() != null) {
				hasErrors = mPO.getHasErrors();
			}
			if (hasErrors) {
				// Motorstate PO has errors. Set msErrorCode and msErrorDescription.
				Boolean hasLineErrors = false;
				try {
					hasLineErrors = mPO.getLines().get(0).getErrors().isEmpty();
				} catch (Exception e) {
					e.printStackTrace();
					hasLineErrors = true;
				}
				if (!hasLineErrors) {
					errorCode = mPO.getLines().get(0).getErrors().get(0).getCode();
					errorDescription = mPO.getLines().get(0).getErrors().get(0).getMessage();
				}
				if (errorCode == null) {
					Boolean hasHeaderErrors = mPO.getHeaderErrors().isEmpty();
					Boolean hasMainErrors = mPO.getErrors().isEmpty();
					if (!hasHeaderErrors) {
						// Check to see if the HeaderErrors object has data.
						errorCode = mPO.getHeaderErrors().get(0).getCode();
						errorDescription = mPO.getHeaderErrors().get(0).getMessage();
					} else {
						// If there is no HeaderErrors data, try the Errors object data.
						if (!hasMainErrors) {
							errorCode = mPO.getErrors().get(0).getCode();
							errorDescription = mPO.getErrors().get(0).getMessage();
						}
					}
				}
				if (errorCode != null) {
					item.setMsErrorCode(errorCode);
					item.setMsErrorCodeDescription(errorDescription);
				} else {
					// Do I want to put something here in case there is an error, but both Error
					// objects are null?
					errorCode = "???";
					errorDescription = "Problem with Validate, but no Error Code.";
				}
				item.setErrorFlag("Z");
				log.info("Error in Motorstate PO.");
			} else {
				// Motorstate PO was sent successfully.
				item.setMsOrderId(mPO.getOrderNumber());
				item.setErrorFlag("Y");
				log.info("Motorstate order sent successfully.");
			}
		}
		order.getItems().add(item);
		return item;
	}

	private Character getOrderType(HandlingPriorityType handlingPriorityType, Availability availability,
			PrintNowFlag printNow) {

		if (handlingPriorityType == null || availability == null || PrintNowFlag.BillOnly == printNow
				|| PrintNowFlag.ForcePrint == printNow) {
			return 'N';
		}

		if (handlingPriorityType.getViperOrderType() != null) {
			return handlingPriorityType.getViperOrderType().charAt(0);
		}

		if (availability.getFulfillmentLocation().getHandlingResult().getPullTarget() != null) {
			if (TargetType.Printer == availability.getFulfillmentLocation().getHandlingResult().getPullTarget()
					.getTargetType()) {
				return 'N';
			}
		}

		PartAvailability partAvailability = availability.getPartAvailability();
		Character orderType = 'M';
		if (partAvailability != null && partAvailability.getBulkItem() != null && partAvailability.getBulkItem()) {
			orderType = 'B';
		}

		return orderType;

	}

}
