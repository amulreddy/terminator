package com.autowares.mongoose.camel.components;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.events.BaseViperEvent;
import com.autowares.mongoose.events.ViperOrderUpdateEvent;
import com.autowares.mongoose.events.ViperProductivityEvent;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.IntegrationContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.OperationalEventContext;
import com.autowares.mongoose.model.OperationalEventType;
import com.autowares.orders.model.Item;
import com.autowares.orders.model.ProcessStage;
import com.autowares.orders.model.ViperTote;
import com.autowares.servicescommon.model.Entity;
import com.autowares.servicescommon.model.EntityType;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.LocationImpl;
import com.autowares.servicescommon.model.OperationalStage;
import com.autowares.servicescommon.model.PartLineItemImpl;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.WarehouseEmployee;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.Operation;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainPerson;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Component
@Converter(generateLoader = true)
public class IntegrationContextTypeConverter implements TypeConverters {

//	@Converter
//	public IntegrationContext wmsCoreJobUpdateEventToIntegrationContext(WmsCoreJobUpdateEvent jobUpdateEvent) {
//
//		/** Create integration context */
//		IntegrationContext integrationContext = new IntegrationContext();
//		WarehouseEmployee employee = new WarehouseEmployee();
//
//		employee.setEntityType(EntityType.Person);
//		employee.setName(jobUpdateEvent.getEmployeeName());
//		employee.setId(jobUpdateEvent.getEmployeeId());
//		employee.setEmployeeNumber(jobUpdateEvent.getEmployeeNumber());
//		integrationContext.setActor(employee);
//
//		LocationImpl location = new LocationImpl();
//		location.setName(jobUpdateEvent.getBuilding());
//		integrationContext.setLocation(location);
//		integrationContext.setEvent(jobUpdateEvent);
//		integrationContext.setOriginatingSystem("mongoose");
//		integrationContext.setActionTimestamp(
//				ZonedDateTime.ofInstant(jobUpdateEvent.getEventDate().toInstant(), ZoneId.systemDefault()));
//
//		/** Create coordinator contexts */
//		List<LineItemContextImpl> lineItems = new ArrayList<>();
//		List<CoordinatorContextImpl> orderContexts = new ArrayList<>();
//		List<FulfillmentLocationContext> fulfillmentLocationContexts = new ArrayList<>();
//
//		for (WmsCoreJobUpdateEventItem item : jobUpdateEvent.getItems()) {
//			if (item.getItemId() == null) {
//				continue;
//			}
//			LineItemContextImpl lineItemContext = null;
//			CoordinatorContextImpl coordinatorContext = null;
//			FulfillmentLocationContext fulfillmentLocationContext = null;
//
//			Optional<CoordinatorContextImpl> orderContext = orderContexts.stream()
//					.filter(c -> item.getXmlOrderId().equals(c.getDocumentId())).findFirst();
//
//			if (orderContext.isPresent()) {
//				coordinatorContext = orderContext.get();
//				Optional<FulfillmentLocationContext> optionalFulfillmentLocationContext = coordinatorContext
//						.getFulfillmentSequence().stream().filter(i -> i.getLocation().equals(location.getName()))
//						.findFirst();
//				if (optionalFulfillmentLocationContext.isPresent()) {
//					fulfillmentLocationContext = optionalFulfillmentLocationContext.get();
//				}
//			} else {
//				coordinatorContext = new CoordinatorContextImpl();
//				coordinatorContext.setCustomerNumber(Long.parseLong(item.getCustomerNumber()));
//				coordinatorContext.setDocumentId(item.getXmlOrderId());
//				fulfillmentLocationContext = new FulfillmentLocationContext(coordinatorContext, location.getName());
//				orderContexts.add(coordinatorContext);
//				fulfillmentLocationContexts.add(fulfillmentLocationContext);
//				integrationContext.getCoordinatorContexts().add(coordinatorContext);
//			}
//
//			Optional<LineItemContextImpl> lineItem = coordinatorContext.getLineItems().stream()
//					.filter(l -> l.getLineNumber() != null).filter(l -> l.getLineNumber().equals(item.getLineNumber()))
//					.findFirst();
//			if (lineItem.isPresent()) {
//				lineItemContext = lineItem.get();
//				lineItemContext.setQuantity(lineItemContext.getQuantity() + 1);
//				lineItemContext.setOriginalQuantity(lineItemContext.getOriginalQuantity() + 1);
//			} else {
//				lineItemContext = new LineItemContextImpl(coordinatorContext, item);
//				lineItems.add(lineItemContext);
//				coordinatorContext.getLineItems().add(lineItemContext);
//			}
//
//			FulfillmentContext fulfillmentContext = new FulfillmentContext(lineItemContext);
//			fulfillmentContext.setFulfillmentLocation(fulfillmentLocationContext);
//		}
//
//		/** Create Operational Events */
//		for (LineItemContext lineItem : lineItems) {
//
//			List<WmsCoreJobUpdateEventItem> orderItems = jobUpdateEvent.getItems().stream()
//					.filter(i -> i.getXmlOrderId().equals(lineItem.getContext().getDocumentId()))
//					.filter(i -> i.getLineNumber().equals(lineItem.getLineNumber())).collect(Collectors.toList());
//
//			Map<String, List<WmsCoreJobUpdateEventItem>> itemsByStatus = orderItems.stream()
//					.collect(Collectors.groupingBy(WmsCoreJobUpdateEventItem::getStatus));
//
//			OperationalEventContext operationalEvent = new OperationalEventContext();
//			operationalEvent.setIntegrationContext(integrationContext);
//			operationalEvent.setQuantity(orderItems.size());
//			operationalEvent.setMoaOrderDetailId(orderItems.get(0).getOrderId());
//			operationalEvent.setLineItemContext(lineItem);
//
//			switch (jobUpdateEvent.getJobType()) {
//			case pull:
//				switch (jobUpdateEvent.getStatus()) {
//				case complete:
//					if (itemsByStatus.containsKey("pulled")) {
//						operationalEvent = new OperationalEventContext(operationalEvent);
//						operationalEvent.setQuantity(itemsByStatus.get("pulled").size());
//						operationalEvent.setOperationalEventType(OperationalEventType.Pulled);
//						integrationContext.getOperationalEvents().add(operationalEvent);
//					}
//					if (itemsByStatus.containsKey("zeroed")) {
//						operationalEvent = new OperationalEventContext(operationalEvent);
//						operationalEvent.setQuantity(itemsByStatus.get("zeroed").size());
//						operationalEvent.setOperationalEventType(OperationalEventType.Zeroed);
//						integrationContext.getOperationalEvents().add(operationalEvent);
//					}
//					break;
//				case assigned:
//					operationalEvent = new OperationalEventContext(operationalEvent);
//					operationalEvent.setOperationalEventType(OperationalEventType.Assigned);
//					integrationContext.getOperationalEvents().add(operationalEvent);
//					break;
//				case abandoned:
//					operationalEvent = new OperationalEventContext(operationalEvent);
//					operationalEvent.setOperationalEventType(OperationalEventType.Unassigned);
//					integrationContext.getOperationalEvents().add(operationalEvent);
//					break;
//				default:
//					break;
//				}
//				break;
//			case pack:
//				switch (jobUpdateEvent.getStatus()) {
//				case complete:
//					operationalEvent = new OperationalEventContext(operationalEvent);
//					operationalEvent.setQuantity(orderItems.size());
//					operationalEvent.setOperationalEventType(OperationalEventType.Packed);
//					integrationContext.getOperationalEvents().add(operationalEvent);
//					break;
//				case assigned:
//					break;
//				case abandoned:
//				default:
//					break;
//				}
//				break;
//			case stock:
//				switch (jobUpdateEvent.getStatus()) {
//				case complete:
//					break;
//				case assigned:
//					break;
//				case abandoned:
//				default:
//					break;
//				}
//				break;
//			case load:
//				switch (jobUpdateEvent.getStatus()) {
//				case complete:
//					break;
//				case assigned:
//					break;
//				default:
//					break;
//				}
//				break;
//			case returnProcessing:
//				switch (jobUpdateEvent.getStatus()) {
//				case complete:
//					break;
//				default:
//					break;
//				}
//				break;
//			default:
//				break;
//
//			}
//		}
//
//		return integrationContext;
//	}

	@Converter
	public IntegrationContext productivityEventToIntegrationContext(Map<String, ?> event) {
		if (event.get("eventType").equals("productivityEvent")) {
			return productivityEventToIntegrationContext(
					new ObjectMapper().convertValue(event, ViperProductivityEvent.class));
		} else if (event.get("eventType").equals("OrderUpdate")) {
			return orderUpdateEventIntegrationContext(
					new ObjectMapper().convertValue(event, ViperOrderUpdateEvent.class));
		} else {
			throw new RuntimeException(
					"Map to IntegrationContext Exception, could not map event type: " + event.get("eventType"));
		}
	}

	@Converter
	public IntegrationContext orderUpdateEventIntegrationContext(ViperOrderUpdateEvent event) {
		return baseViperEventToIntegrationContext(event);
	}

	@Converter
	public IntegrationContext productivityEventToIntegrationContext(ViperProductivityEvent event) {
		return baseViperEventToIntegrationContext(event);
	}

	@Converter
	public IntegrationContext baseViperEventToIntegrationContext(BaseViperEvent event) {
		ViperOrderUpdateEvent orderUpdateEvent = null;
		if (event instanceof ViperOrderUpdateEvent) {
			orderUpdateEvent = (ViperOrderUpdateEvent) event;
		}
		IntegrationContext integrationContext = new IntegrationContext();
		WarehouseEmployee warehouseEmployee = new WarehouseEmployee();

		warehouseEmployee.setName(event.getEmployeeName());
		warehouseEmployee.setEmployeeNumber(event.getEmployeeNumber());
		warehouseEmployee.setEntityType(EntityType.Person);
		integrationContext.setActor(warehouseEmployee);
		integrationContext.setEvent(event);
		integrationContext.setActionTimestamp(ZonedDateTime.now());

		LocationImpl location = new LocationImpl();
		location.setName(event.getBuildingCode());
		integrationContext.setLocation(location);
		integrationContext.setOriginatingSystem("viper");

		CoordinatorContextImpl coordinatorContext = new CoordinatorContextImpl();
		coordinatorContext.setCustomerNumber(Long.parseLong(event.getCustomerNumber()));
		coordinatorContext.setDocumentId(event.getXmlOrderID());

		FulfillmentLocationContext fulfillmentLocationContext = new FulfillmentLocationContext(coordinatorContext,
				location.getName());
		PartLineItemImpl item = new PartLineItemImpl();
		// Not correct on orderUpdate
		item.setCustomerLineNumber(Integer.parseInt(event.getCustomerNumber()));
		item.setLineCode(event.getVendor());
		// orderCustomerRec on orderUpdate
		if (orderUpdateEvent != null) {
			item.setLineNumber(orderUpdateEvent.getOrderCustomerRec());
		} else {
			item.setLineNumber(Integer.parseInt(event.getCustomerNumber()));
		}
		item.setPartNumber(event.getPartNumber());
		item.setPrice(BigDecimal.valueOf(event.getBillPrice()));
		item.setProductId(event.getMoaPartHeaderId());
		// shipQuantity on orderUpdate
		if (orderUpdateEvent != null) {
			item.setQuantity(orderUpdateEvent.getShipQuantity());
		} else {
			item.setQuantity(event.getQuantity());
		}
		item.setVendorCodeSubCode(event.getVendor());
		LineItemContextImpl lineItemContext = new LineItemContextImpl(coordinatorContext, item);

		for (int i = 0; i < item.getQuantity(); i++) {
			FulfillmentContext fulfillmentContext = new FulfillmentContext(lineItemContext);
			fulfillmentContext.setFulfillmentLocation(fulfillmentLocationContext);
		}

		integrationContext.setCoordinatorContexts(Lists.newArrayList(coordinatorContext));
		integrationContext.setLineItems(Lists.newArrayList(lineItemContext));

		OperationalEventContext operationalEvent = new OperationalEventContext();
		operationalEvent.setIntegrationContext(integrationContext);
		operationalEvent.setQuantity(item.getQuantity());
		operationalEvent.setMoaOrderDetailId(event.getOrderID());
		operationalEvent.setLineItemContext(lineItemContext);

		if (orderUpdateEvent != null) {
			if ("Pulled".equals(orderUpdateEvent.getStatus())) {
				operationalEvent.setOperationalEventType(OperationalEventType.Pulled);
			}
		}

		if (orderUpdateEvent != null) {
			if ("Packed".equals(orderUpdateEvent.getStatus())) {
				operationalEvent.setOperationalEventType(OperationalEventType.Packed);
			}
		}

		if ("Pulling".equals(event.getProgSource())) {
			if ("Scan Part".equals(event.getAction())) {
				operationalEvent.setOperationalEventType(OperationalEventType.Pulled);
			}
		}

		if ("Putaway".equals(event.getProgSource())) {
			if ("Putaway".equals(event.getAction())) {
				operationalEvent.setOperationalEventType(OperationalEventType.Stocked);
			}
		}

		integrationContext.setOperationalEvents(Lists.newArrayList(operationalEvent));

		return integrationContext;
	}

	@Converter
	public static Item OperationalContexttoMoaItem(OperationalContext operationalContext) {
		OperationalItem operationalItem = operationalContext.getItems().get(0);
		Object source = operationalItem.getSource();
		if (source instanceof Item) {
			Item moaOrderItem = (Item) source;
			moaOrderItem.setPulledQuantity(operationalContext.getPulledCount());
			Optional<Operation> pullOperation = operationalContext.getLastOperationByStage(OperationalStage.pulled);
			if (pullOperation.isPresent()) {
				moaOrderItem.setPulledTime(pullOperation.get().getTimeStamp().toLocalDateTime());
				moaOrderItem.setPullerId(getEmployeeId(pullOperation.get().getActor()));
				Map<String, Long> itemsPerTote = operationalContext.getOperationsByStage(OperationalStage.pulled)
						.stream().filter(i -> i.getContainerId() != null)
						.collect(Collectors.groupingBy(i -> i.getContainerId(), Collectors.counting()));
				for (Entry<String, Long> entry : itemsPerTote.entrySet()) {
					ViperTote viperTote = new ViperTote();
					viperTote.setToteSerial(entry.getKey());
					viperTote.setPullQuantity(entry.getValue().intValue());
					viperTote.setBuilding(moaOrderItem.getBuilding());
					viperTote.setOrderId(moaOrderItem.getOrderItemId().longValue());
					moaOrderItem.getTotes().add(viperTote);
				}
			}
			moaOrderItem.setPackQuantity(operationalContext.getPackedCount());
			Optional<Operation> packOperation = operationalContext.getLastOperationByStage(OperationalStage.packed);
			if (packOperation.isPresent()) {
				moaOrderItem.setPackedTime(packOperation.get().getTimeStamp().toLocalDateTime());
				moaOrderItem.setPackerId(getEmployeeId(packOperation.get().getActor()));
				Map<String, Long> itemsPerTote = operationalContext.getOperationsByStage(OperationalStage.packed)
						.stream().filter(i -> i.getContainerId() != null)
						.collect(Collectors.groupingBy(i -> i.getContainerId(), Collectors.counting()));
				for (Entry<String, Long> entry : itemsPerTote.entrySet()) {
					ViperTote viperTote = new ViperTote();
					viperTote.setToteSerial(entry.getKey());
					viperTote.setPackQuantity(entry.getValue().intValue());
					viperTote.setBuilding(moaOrderItem.getBuilding());
					viperTote.setOrderId(moaOrderItem.getOrderItemId().longValue());
					moaOrderItem.getTotes().add(viperTote);
				}
			}
			Operation currentOperation = operationalItem.getCurrentOperation();
			OperationalStage currentStage = currentOperation.getOperationalStage();
			//TODO any other stages need to zero out the ship quantity?
			if (OperationalStage.canceled.equals(currentStage)) {
				moaOrderItem.setShipQuantity(0);
			}
			moaOrderItem.setProcessStage(ProcessStage.find(currentStage));
			return moaOrderItem;
		}
		throw new RuntimeException("Unsupported source item type");
	}

	private static Integer getEmployeeId(Entity e) {
		if (e instanceof SupplyChainPerson) {
			SupplyChainPerson employee = (SupplyChainPerson) e;
			if (employee.getEmployeeId() != null) {
				return employee.getEmployeeId().intValue();
			}
		}
		return null;
	}

	@Converter
	public static OperationalContext moaItemToOperationalContext(Item item) {
		OperationalContext operationalContext = new OperationalContext();
		if (item == null) {
			return operationalContext;
		}
		operationalContext.setCorrelation(item.getPackslipId());
		operationalContext.setDocumentType(SourceDocumentType.PackSlip);
		operationalContext.setCreationTimeStamp(item.getCreateTimestamp());
		if (item.getLastUpdateTimestamp() != null) {
			operationalContext.setModificationTimeStamp(item.getLastUpdateTimestamp().atZone(ZoneId.systemDefault()));
		}

		Integer plannedFillQuantity = (item.getPlannedFillQuantity() != null) ? item.getPlannedFillQuantity() : 0;
		for (int i = 0; i < plannedFillQuantity; i++) {
			OperationalItem operationalItem = moaItemToOperationalItem(item);
			operationalContext.getItems().add(operationalItem);
			updateMoaOperationalItem(OperationalStage.ordered, operationalContext, item);
		}

		Integer pulledQuantity = (item.getPulledQuantity() != null) ? item.getPulledQuantity() : 0;
		Integer packQuantity = (item.getPackQuantity() != null) ? item.getPackQuantity() : 0;
		Integer invoicedQuantity = (item.getInvoiceQuantity() != null) ? item.getInvoiceQuantity() : 0;
		if (invoicedQuantity > plannedFillQuantity) {
			int difference = invoicedQuantity - plannedFillQuantity;
			for (int i = 0; i < difference; i++) {
				OperationalItem operationalItem = moaItemToOperationalItem(item);
				operationalContext.getItems().add(operationalItem);
			}
		}

		switch (item.getProcessStage()) {
		case Allocated:
			break;
		case Canceled:
			for (int i = 0; i < plannedFillQuantity; i++) {
				updateMoaOperationalItem(OperationalStage.canceled, operationalContext, item);
			};
			break;
		case ErrorCorrectionFlag:
			break;
		case NewOrder:
			break;
		case Packed:
			break;
		case Picked:
			break;
		case Picking:
			for (int i = 0; i < plannedFillQuantity; i++) {
				updateMoaOperationalItem(OperationalStage.pulling, operationalContext, item);
			}
			break;
		case PickupOrderPacked:
			break;
		case RecheckFlag:
			break;
		case UnfinishedOrder:
			break;
		default:
			break;
		}

		Integer totePulledQuantity = 0;
		Integer totePackedQuantity = 0;
		for (ViperTote tote : item.getTotes()) {
			totePulledQuantity += tote.getPullQuantity();
			totePackedQuantity += tote.getPackQuantity();
			for (int i = 0; i < tote.getPullQuantity(); i++) {
				OperationalItem operationalItem = updateMoaOperationalItem(OperationalStage.pulled, operationalContext,
						item);
				Operation operation = operationalItem.getCurrentOperation();
				operation.setContainerId(tote.getToteSerial());
				operation.setTimeStamp(ZonedDateTime.of(item.getPulledTime(), ZoneId.systemDefault()));
				if (item.getPullerId() != null) {
					operation.setActor(
							SupplyChainPerson.builder().withEmployeeId(item.getPullerId().longValue()).build());
				}
			}
			for (int i = 0; i < tote.getPackQuantity(); i++) {
				OperationalItem operationalItem = updateMoaOperationalItem(OperationalStage.packed, operationalContext,
						item);
				Operation operation = operationalItem.getCurrentOperation();
				operation.setContainerId(tote.getToteSerial());
				operation.setTimeStamp(ZonedDateTime.of(item.getPackedTime(), ZoneId.systemDefault()));
				if (tote.getPackerId() != null) {
					operation.setActor(
							SupplyChainPerson.builder().withEmployeeId(tote.getPackerId().longValue()).build());
				}
			}
		}

		if (totePulledQuantity == 0 && pulledQuantity > 0) {
			// Bulk item
			for (int i = 0; i < pulledQuantity; i++) {
				OperationalItem operationalItem = updateMoaOperationalItem(OperationalStage.pulled, operationalContext,
						item);
				Operation operation = operationalItem.getCurrentOperation();
				if (item.getPulledTime() != null) {
					operation.setTimeStamp(ZonedDateTime.of(item.getPulledTime(), ZoneId.systemDefault()));
				}
				if (item.getPullerId() != null) {
					operation.setActor(
							SupplyChainPerson.builder().withEmployeeId(item.getPullerId().longValue()).build());
				}
			}
		} else {
			if (pulledQuantity != totePulledQuantity) {
				// We lost data -> Then what do we do
			}
		}
		if (packQuantity != totePackedQuantity && packQuantity > 0) {
			for (int i = 0; i < packQuantity; i++) {
				OperationalItem operationalItem = updateMoaOperationalItem(OperationalStage.packed, operationalContext,
						item);
				Operation operation = operationalItem.getCurrentOperation();
				if (item.getPackedTime() != null) {
					operation.setTimeStamp(ZonedDateTime.of(item.getPackedTime(), ZoneId.systemDefault()));
				}
				if (item.getPackerId() != null) {
					operation.setActor(
							SupplyChainPerson.builder().withEmployeeId(item.getPackerId().longValue()).build());
				}
			}
		}

		for (int i = 0; i < item.getToRecheckQuantity(); i++) {
			OperationalItem operationalItem = updateMoaOperationalItem(OperationalStage.recheck, operationalContext,
					item);
			Operation operation = operationalItem.getCurrentOperation();
			operation.setTimeStamp(ZonedDateTime.of(item.getPulledTime(), ZoneId.systemDefault()));
			if (item.getPullerId() != null) {
				operation.setActor(SupplyChainPerson.builder().withEmployeeId(item.getPullerId().longValue()).build());
			}
		}
		for (int i = 0; i < invoicedQuantity; i++) {
			updateMoaOperationalItem(OperationalStage.shipped, operationalContext, item);
		}
		for (int i = 0; i < invoicedQuantity; i++) {
			updateMoaOperationalItem(OperationalStage.invoiced, operationalContext, item);
		}

		for (int i = 0; i < item.getErrorQuantity(); i++) {
			updateMoaOperationalItem(OperationalStage.errorCorrection, operationalContext, item);
		}

		return operationalContext;
	}

	private static OperationalItem updateMoaOperationalItem(OperationalStage stage,
			OperationalContext operationalContext, Item item) {
		Predicate<OperationalItem> predicate = i -> !i.hasBeenThroughStage(stage);
		if (OperationalStage.packed.equals(stage)) {
			predicate = predicate.and(i -> i.hasBeenThroughStage(OperationalStage.pulled));
		}
		if (OperationalStage.recheck.equals(stage)) {
			predicate = predicate.and(i -> !i.hasBeenThroughStage(OperationalStage.pulled));
		}
		if (OperationalStage.errorCorrection.equals(stage)) {
			predicate = predicate.and(i -> !i.hasBeenThroughStage(OperationalStage.pulled));
		}
		Optional<OperationalItem> optionalOperational = operationalContext.getItems().stream().filter(predicate)
				.findAny();
		OperationalItem operationalItem = null;
		if (optionalOperational.isPresent()) {
			operationalItem = optionalOperational.get();
		} else {
			operationalItem = moaItemToOperationalItem(item);
		}
		Operation operation = new Operation(operationalItem, operationalContext, stage);
		if (OperationalStage.ordered.equals(stage)) {
			operation.setTimeStamp(item.getOrder().getOrderTime());
		} else {
			if(item.getLastUpdateTimestamp() != null) {
				operation.setTimeStamp(ZonedDateTime.of(item.getLastUpdateTimestamp(),ZoneId.systemDefault()));
			}
		}
		
		operationalItem.setCurrentOperation(operation);
		return operationalItem;
	}

	@Converter
	public static OperationalItem moaItemToOperationalItem(Item item) {
		OperationalItem operationalItem = new OperationalItem();
		operationalItem.setSource(item);
		operationalItem.setPartNumber(item.getPartNumber());
		operationalItem.setVendorCodeSubCode(item.getVendorCode());
		if (item.getPartHeaderId() != null) {
			operationalItem.setProductId(item.getPartHeaderId().longValue());
		}
		return operationalItem;
	}

//	@Converter
//	public static OperationalContext wmsCoreWarehouseRequestToOperationalContext(
//			WmsCoreWarehouseProcessingRequest request) {
//		OperationalContext operationalContext = new OperationalContext();
//		operationalContext.setCorrelation(request.getFulfillmentLocation());
//		operationalContext.setDocumentType(SourceDocumentType.PackSlip);
//		operationalContext.setCreationTimeStamp(request.getRequestDate());
//
//		for (Containable c : request.getItems()) {
//			if (c instanceof WmsCorePhysicalPart) {
//				WmsCorePhysicalPart part = (WmsCorePhysicalPart) c;
//				OperationalItem operationalItem = wmsCorePhysicalPartToOperationalItem(part);
//				operationalContext.getItems().add(operationalItem);
//				if (part.getItemHistory() != null) {
//					Comparator<ZonedDateTime> nullsafe = Comparator.nullsFirst(ZonedDateTime::compareTo);
//					part.getItemHistory().sort(Comparator.comparing(WmsCoreItemHistory::getTimestamp, nullsafe));
//					for (WmsCoreItemHistory history : part.getItemHistory()) {
//						OperationalStage stage = convertCoreStatusToStage(history.getAction());
//						WmsCoreEmployee wmsEmployee = history.getEmployee();
//						ZonedDateTime time = history.getTimestamp();
//						operationalItem = updateWmsCoreOperationalItem(stage, operationalContext, operationalItem,
//								part);
//						Operation operation = operationalItem.getCurrentOperation();
//						WarehouseEmployee employee = new WarehouseEmployee();
//						employee.setEmployeeNumber(wmsEmployee.getEmployeeNumber());
//						employee.setName(wmsEmployee.getFirstName() + " " + wmsEmployee.getLastName());
//						if (operation != null) {
//							operation.setActor(employee);
//							operation.setTimeStamp(time);
//						}
//					}
//				}
//
//				OperationalStage stage = operationalItem.getOperationalStage();
//				if (stage == null) {
//					stage = OperationalStage.ordered;
//				}
//				try {
//					stage = OperationalStage.valueOf(part.getStatus());
//				} catch (Exception e) {
//				}
//				switch (stage) {
//				case errorCorrection:
//					break;
//				case invoiced:
//					updateWmsCoreOperationalItem(OperationalStage.shipped, operationalContext, operationalItem, part);
//					updateWmsCoreOperationalItem(OperationalStage.invoiced, operationalContext, operationalItem, part);
//					break;
//				case labeled:
//					break;
//				case ordered:
//					updateWmsCoreOperationalItem(OperationalStage.ordered, operationalContext, operationalItem, part);
//					break;
//				case packed:
//					updateWmsCoreOperationalItem(OperationalStage.packed, operationalContext, operationalItem, part);
//					break;
//				case pulled:
//					updateWmsCoreOperationalItem(OperationalStage.pulled, operationalContext, operationalItem, part);
//					break;
//				case pulling:
//					updateWmsCoreOperationalItem(OperationalStage.pulling, operationalContext, operationalItem, part);
//					break;
//				case putaway:
//					break;
//				case received:
//					break;
//				case recheck:
//					break;
//				case shipped:
//					updateWmsCoreOperationalItem(OperationalStage.shipped, operationalContext, operationalItem, part);
//					break;
//				case stocking:
//					break;
//				default:
//					break;
//
//				}
//			}
//		}
//		return operationalContext;
//	}

//	private static OperationalStage convertCoreStatusToStage(String status) {
//		OperationalStage stage = OperationalStage.ordered;
//		try {
//			stage = OperationalStage.valueOf(status);
//		} catch (Exception e) {
//		}
//		if ("assigned".equals(status)) {
//			stage = OperationalStage.pulling;
//		}
//		if ("abandoned".equals(status)) {
//			stage = OperationalStage.ordered;
//		}
//		return stage;
//	}

//	@Converter
//	public static OperationalItem wmsCorePhysicalPartToOperationalItem(WmsCorePhysicalPart part) {
//		// why? OperationalItem operationalItem = new OperationalItem((Part) part);
//		OperationalItem operationalItem = new OperationalItem();
//		operationalItem.setSource(part);
//		operationalItem.setPartNumber(part.getPartNumber());
//		operationalItem.setLineCode(part.getLineCode());
//		operationalItem.setProductId(part.getProductId());
//		operationalItem.setLineNumber(part.getLineNumber());
//		operationalItem.setVendorCodeSubCode(part.getVendorCode());
//		return operationalItem;
//	}

	// Since the history duplicates what the current state on the item is, we want
	// to re-use that operation if we
	// created it off the history
//	private static OperationalItem updateWmsCoreOperationalItem(OperationalStage stage,
//			OperationalContext operationalContext, OperationalItem operationalItem, WmsCorePhysicalPart item) {
//		Optional<Operation> optionalOperation = operationalItem.getOperationsByStage(stage).stream().findAny();
//		if (optionalOperation.isPresent()) {
//			return operationalItem;
//		}
//
//		Operation operation = new Operation(operationalItem, operationalContext, stage);
//		operationalItem.setCurrentOperation(operation);
//		if (item.getParent() instanceof WmsCoreContainerTote) {
//			WmsCoreContainerTote tote = (WmsCoreContainerTote) item.getParent();
//			Optional<Packaging> packaging = tote.getPackaging().stream().filter(i -> i.getBarcode() != null).findAny();
//			if (packaging.isPresent()) {
//				operation.setContainerId(packaging.get().getBarcode());
//			}
//		}
//		return operationalItem;
//	}

	public static OperationalContext supplyChainPackSlipToIntegrationContext(SupplyChainSourceDocumentImpl packSlip,
			Integer lineNumber) {
		OperationalContext operationalContext = new OperationalContext();
		operationalContext.setCorrelation(packSlip.getDocumentId());
		operationalContext.setDocumentType(SourceDocumentType.PackSlip);
		operationalContext.setModificationTimeStamp(packSlip.getModificationTimeStamp());
		operationalContext.setCreationTimeStamp(packSlip.getCreationDate());

		for (GenericLine lineItem : packSlip.getLineItems()) {
			if (lineItem.getLineNumber().equals(lineNumber)) {
				for (Freight location : lineItem.getFulfillmentLocations()) {
					if (packSlip.getDocumentId().equals(location.getDocumentReferenceId())) {
						for (int i = 0; i < lineItem.getPurchasedQuantity(); i++) {
							OperationalItem operationalItem = new OperationalItem(lineItem);
							new Operation(operationalItem, operationalContext, OperationalStage.ordered);
							operationalContext.getItems().add(operationalItem);
						}
						for (int i = 0; i < lineItem.getShippedQuantity(); i++) {
							updateSupplyChainOperationalItem(OperationalStage.pulled, operationalContext, lineItem);
							updateSupplyChainOperationalItem(OperationalStage.packed, operationalContext, lineItem);
							updateSupplyChainOperationalItem(OperationalStage.shipped, operationalContext, lineItem);
						}
					}
				}
			}
		}
		return operationalContext;
	}

	private static void updateSupplyChainOperationalItem(OperationalStage stage, OperationalContext operationalContext,
			GenericLine lineItem) {
		Optional<OperationalItem> optionalOperational = operationalContext.getItems().stream()
				.filter(s -> !stage.equals(s.getOperationalStage())).findAny();
		OperationalItem operationalItem = null;
		if (optionalOperational.isPresent()) {
			operationalItem = optionalOperational.get();
		} else {
			operationalItem = new OperationalItem(lineItem);
		}
		Operation operation = new Operation(operationalItem, operationalContext, stage);
		operationalItem.setCurrentOperation(operation);
	}

}
