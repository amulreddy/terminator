package com.autowares.mongoose.optaplanner.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;

import com.autowares.servicescommon.model.FulfillmentOptions;

@PlanningSolution
public class OrderDetailFulfillment implements Serializable {

	private static final long serialVersionUID = 1L;
	@ProblemFactProperty
	private OrderDetail detail;
	@ProblemFactProperty
	private FulfillmentOptions fulfillmentOptions;
	@ValueRangeProvider(id = "locationRange")
	@ProblemFactCollectionProperty
	private Set<FulfillmentLocation> stockingLocations = new HashSet<>();
	private HardMediumSoftScore score;
	@PlanningEntityCollectionProperty
	public List<OrderFillDetail> fulfillments = new ArrayList<>();
	private String explanation;

	public OrderDetailFulfillment(OrderDetail detail, List<FulfillmentLocation> locations, FulfillmentOptions fulfillmentOptions) {
		super();
		this.stockingLocations = locations.stream().collect(Collectors.toSet());
		this.detail = detail;
		this.fulfillmentOptions = fulfillmentOptions;
		for (int i = 0; i < detail.getOrderAmount(); i++) {
			OrderFillDetail fulfillment = new OrderFillDetail(null, detail, i);
			fulfillments.add(fulfillment);
		}
		Optional<Entry<Integer, Integer>> arrivalDateMap = locations
				.stream()
				.collect(Collectors.groupingBy(location -> location.getArrivalDateAsInt(), Collectors.summingInt(location -> location.getQoh())))
				.entrySet()
					.stream()
						.sorted(Comparator.comparing(i -> i.getKey()))
						.filter(entry -> entry.getValue() >= detail.getOrderAmount())
					.findFirst();
			if(arrivalDateMap.isPresent()) {
				this.stockingLocations.stream().forEach(i -> i.setMinFillableArrivalDate(arrivalDateMap.get().getKey()));
			}
	}

	public OrderDetailFulfillment() {
		super();
	}

	@PlanningScore
	public HardMediumSoftScore getScore() {
		return score;
	}

	public void setScore(HardMediumSoftScore score) {
		this.score = score;
	}

	public FulfillmentOptions getFulfillmentOptions() {
		return fulfillmentOptions;
	}

	public void setFulfillmentOptions(FulfillmentOptions fulfillmentOptions) {
		this.fulfillmentOptions = fulfillmentOptions;
	}

	public Set<FulfillmentLocation> getStockingLocations() {
		return stockingLocations;
	}

	public void setStockingLocations(Set<FulfillmentLocation> locations) {
		this.stockingLocations = locations;
	}

	public void setFulfillments(List<OrderFillDetail> fulfillments) {
		this.fulfillments = fulfillments;
	}

	public List<OrderFillDetail> getFulfillments() {
		return fulfillments;
	}

	public Map<String, Long> getFulfillmentMap() {
		return this.fulfillments.stream().filter(i -> i.getLocation() != null)
				.map(i -> i.getLocation().getLocationName())
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	public String getExplanation() {
		return explanation;
	}

}
