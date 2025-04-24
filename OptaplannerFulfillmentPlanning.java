package com.autowares.mongoose.camel.processors.fulfillmentplanning;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.camel.processors.postfulfillment.PickupWarehouseVendorIntegration;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.criteria.PkCriteria;
import com.autowares.mongoose.optaplanner.domain.FulfillmentLocation;
import com.autowares.mongoose.optaplanner.domain.OrderDetail;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.ShortageCode;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.supplychain.model.DemandModel;
import com.autowares.supplychain.model.DemandModel.Builder;
import com.autowares.supplychain.model.optaplanner.OptaplannerFulfillmentLocation;
import com.google.common.collect.Lists;

@Component
public class OptaplannerFulfillmentPlanning implements Processor {

	SolverFactory<OrderDetailFulfillment> solverFactory = SolverFactory
			.createFromXmlResource("OrderFulfillmentSolverConfig.xml");
	Logger log = LoggerFactory.getLogger(OptaplannerFulfillmentPlanning.class);

	@Autowired
	PickupWarehouseVendorIntegration pickupWarehouseVendorIntegration;

	@Override
	public void process(Exchange exchange) throws Exception {
		LineItemContext lineItemContext = exchange.getIn().getBody(LineItemContext.class);

		Optional<Availability> fillable = lineItemContext.getAvailability().stream()
				.filter(i -> i.getQuantityOnHand() > 0).findAny();
		if (fillable.isPresent()) {
			ScoreManager<OrderDetailFulfillment, HardMediumSoftScore> manager = ScoreManager.create(solverFactory);
			OrderDetailFulfillment problem = convertLineItemToProblem(lineItemContext);
			OrderDetailFulfillment solution = solve(problem, manager);
			lineItemContext.setPlannedFulfillment(solution);
			if (solution.getScore().isFeasible()) {
				lineItemContext = convertFulfillment(solution, lineItemContext);
			}
		} else {
			lineItemContext.setFulfillmentDetails(new ArrayList<FulfillmentContext>());
		}
		lineItemContext.setShortageCode(getNotFillableCode(lineItemContext));
		DemandModel demand = convertDetailToDemand(lineItemContext);
		lineItemContext.setDemand(demand);
		exchange.getIn().setHeaders(exchange.getIn().getHeaders());
		exchange.getIn().setBody(lineItemContext);
	}

	private LineItemContext convertFulfillment(OrderDetailFulfillment solution, LineItemContext lineItem) {

		// EntrySet is locationID, fillQuantity
		for (Entry<String, Long> entrySet : solution.getFulfillmentMap().entrySet()) {
			Integer fillQuantity = entrySet.getValue().intValue();
			for (Availability availability : lineItem.getAvailability()) {
				if (availability.getFulfillmentLocation().getLocation().equals(entrySet.getKey())) {
					availability.setFillQuantity(fillQuantity);
					if (fillQuantity > 0) {
						// lineItem.getContext().getProcurementGroups().addAll(availability.getFulfillmentLocation().getNonStockContext().getProcurementGroups());
						for (int i = 0; i < fillQuantity; i++) {
							Optional<FulfillmentContext> optionalItem = lineItem.getFulfillmentDetails().stream()
									.filter(item -> item.getLocation() == null).findAny();
							if (optionalItem.isPresent()) {
								FulfillmentContext item = optionalItem.get();
								item.setFillQuantity(1);
								item.setFulfillmentLocation(availability.getFulfillmentLocation());
								item.setAvailability(availability);
								availability.getFulfillments().add(item);
							}
						}
					}
				}
			}
		}

		return lineItem;

	}

	private ShortageCode getNotFillableCode(LineItemContext lineItemContext) {
		// Previous processors may set this like purchase restrictions, we shouldnt get
		// here but in case we do just return it;
		if (lineItemContext.getShortageCode() != null) {
			return lineItemContext.getShortageCode();
		}
		OrderDetailFulfillment solution = lineItemContext.getPlannedFulfillment();
		Part part = lineItemContext.getPart();
		if (part == null) {
			return ShortageCode.NonResolvableProduct;
		}

		if (part != null && part.getAvailability() != null) {
			if (part.getAvailability().isEmpty()) {
				return ShortageCode.NonStockedProduct;
			}
			int totalAvailable = part.getAvailability().stream().mapToInt(PartAvailability::getQuantityOnHand).sum();
			if (totalAvailable < lineItemContext.getQuantity()) {
				return ShortageCode.InsufficientQuantityOnHand;
			}
		}

		if (solution != null) {
			if (!solution.getScore().isFeasible()) {
				log.warn("Unable to fulfill according to the requirements: " + solution.getExplanation());
				return (ShortageCode.ConstraintViolation);
			}
		}
		return null;
	}

	private OrderDetailFulfillment solve(OrderDetailFulfillment problem,
			ScoreManager<OrderDetailFulfillment, HardMediumSoftScore> manager) {
		Solver<OrderDetailFulfillment> solver = solverFactory.buildSolver();
		OrderDetailFulfillment solution = solver.solve(problem);
		solution.setExplanation(manager.explainScore(solution).getSummary());
		return solution;
	}

	private OrderDetailFulfillment convertLineItemToProblem(LineItemContext lineItem) {
		List<FulfillmentLocation> possibleLocations = Lists.newArrayList();
		CoordinatorContext context = lineItem.getContext();
		FulfillmentOptions fulfillmentOptions = new FulfillmentOptions();
		if (context.getFulfillmentOptions() != null) {
			fulfillmentOptions = new FulfillmentOptions(context.getFulfillmentOptions());
		}

		// Set these off the context as they can be modified from the original request
		fulfillmentOptions.setDeliveryMethod(context.getDeliveryMethod());
		fulfillmentOptions.setServiceClass(context.getServiceClass());

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(lineItem.getLineNumber());
		detail.setOrderAmount(lineItem.getQuantity());

		for (Availability availability : lineItem.getAvailability()) {
			FulfillmentLocationContext locationContext = availability.getFulfillmentLocation();
			PartAvailability partAvailability = availability.getPartAvailability();
			if (locationContext != null && partAvailability != null && !excluded(locationContext, lineItem, fulfillmentOptions)) {
				FulfillmentLocation planningLocation = new FulfillmentLocation();
				planningLocation.setLocationName(locationContext.getLocation());

				if (partAvailability.getQuantityOnHand() == null) {
					partAvailability.setQuantityOnHand(0);
				}
				planningLocation.setQoh(partAvailability.getQuantityOnHand());
				if (partAvailability.getQuantityOnOrder() == null) {
					partAvailability.setQuantityOnOrder(0);
				}
				planningLocation.setQuantityOnOrder(partAvailability.getQuantityOnOrder());
				if (partAvailability.getQuantityOnBackorder() == null) {
					partAvailability.setQuantityOnBackorder(0);
				}
				planningLocation.setQuantityOnBackOrder(partAvailability.getQuantityOnBackorder());
				if (partAvailability.getPreMark() == null) {
					partAvailability.setPreMark(0);
				}
				planningLocation.setPreMark(partAvailability.getPreMark());

				if (partAvailability.getQuantityOnBackorder() + partAvailability.getQuantityOnHand()
						+ partAvailability.getQuantityOnOrder() + partAvailability.getPreMark() != 0) {
					// There are parts at this location or it is stocked here ( premark > 0 ).

					planningLocation.setServicingLocation(locationContext.getServicingLocation());

					if (LocationType.Store.equals(locationContext.getLocationType())) {
						planningLocation.setIsStore(true);
					}

					if (LocationType.Vendor.equals(locationContext.getLocationType())) {
						planningLocation.setIsAwi(false);
						planningLocation.setIsSupplier(true);
					}

					if (SystemType.MotorState.equals(locationContext.getSystemType())) {
						fulfillmentOptions.setPartiallyFill(false);
						fulfillmentOptions.setAllowSupplierInventory(true);
						lineItem.updateOrderLog("Motorstate stocking location.  Setting partial fill to false.");
					}

					Optional<PkCriteria> pkResults = pickupWarehouseVendorIntegration.getPkCriteriaList().stream()
							.filter(i -> i.getBuildingCode().equals(planningLocation.getLocationName()))
							.filter(i -> i.getVendorCode().equals(lineItem.getPart().getVendorCodeSubCode())).findAny();
					if (pkResults.isPresent()) {
						planningLocation.setPkLocation(true);
					}

					planningLocation.setArrivalDate(locationContext.getArrivalDate());
					if (locationContext.getNextDeparture() != null) {
						planningLocation.setTimeToDepart(
								Duration.between(ZonedDateTime.now(), locationContext.getNextDeparture()));
					}

					if (locationContext.getTransfers() != null) {
						planningLocation.setTransfers(locationContext.getTransfers());
					}

					for (String preferredLocation : fulfillmentOptions.getPreferredLocations()) {
						if (locationContext.getLocation().equals(preferredLocation)) {
							planningLocation.setPreferredLocation(true);
						}
					}

					if (DeliveryMethod.CustomerPickUp.equals(context.getDeliveryMethod())) {
						HandlingOptions handlingOptions = lineItem.getHandlingOptions();
						if (locationContext.getLocation().equals(handlingOptions.getPreferredLocation())) {
							planningLocation.setPreferredLocation(true);
						}
						if (locationContext.getWarehouseMaster() != null) {
							WarehouseMaster warehouseMaster = locationContext.getWarehouseMaster();
							if (String.valueOf(warehouseMaster.getWarehouseNumber())
									.equals(handlingOptions.getPreferredLocation())) {
								planningLocation.setPreferredLocation(true);
							}
						}
					}

					possibleLocations.add(planningLocation);
				}
			}
		}

		OrderDetailFulfillment problem = new OrderDetailFulfillment(detail, possibleLocations, fulfillmentOptions);
		return problem;
	}

	private boolean excluded(FulfillmentLocationContext locationContext, LineItemContext lineItem, FulfillmentOptions options) {
		String currentLocation = locationContext.getLocation();
		List<String> excludedLocations = Lists
				.newArrayList(options.getExcludedLocations());
		boolean excluded = excludedLocations.stream().anyMatch(i -> currentLocation.equals(i));
		if (!excluded) {
			excluded = currentLocation.equals(lineItem.getHandlingOptions().getExcludeLocation());
		}
		return excluded;
	}

	private DemandModel convertDetailToDemand(LineItemContext orderDetailContext) {
		DemandModel demand = orderDetailContext.getDemand();
		Builder<?> builder = DemandModel.builder();
		if (demand != null) {
			builder.fromDemand(demand);
		}
		CoordinatorContext orderContext = orderDetailContext.getContext();
		if (orderContext.getBusinessContext() != null
				&& orderContext.getBusinessContext().getBusinessDetail().getServicingWarehouse() != null) {
			builder.withIdealFulfillmentLocation(orderContext.getBusinessContext().getBusinessDetail()
					.getServicingWarehouse().getBuildingMnemonic());
		}

		if (orderDetailContext.getPlannedFulfillment() != null) {
			OrderDetailFulfillment f = orderDetailContext.getPlannedFulfillment();
			List<OptaplannerFulfillmentLocation> availability = f.getStockingLocations().stream()
					.map(i -> (OptaplannerFulfillmentLocation) i).collect(Collectors.toList());
			builder.withAvailability(availability);
		}
		return builder.build();
	}

}
