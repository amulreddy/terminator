package com.autowares.mongoose.optaplanner.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
public class StockPutawayOptimization implements Serializable {

	private static final long serialVersionUID = 1L;
	@ProblemFactProperty
	private StockDetail detail;
	@ProblemFactProperty
	private FulfillmentOptions fulfillmentOptions;
	@ValueRangeProvider(id = "locationRange")
	@ProblemFactCollectionProperty
	private List<StockingLocation> stockingLocations = new ArrayList<>();
	private HardMediumSoftScore score;
	@PlanningEntityCollectionProperty
	public List<StockPutawayDetail> stockPutaway = new ArrayList<>();
	private String explanation;

	public StockPutawayOptimization(StockDetail detail, List<StockingLocation> locations, FulfillmentOptions fulfillmentOptions) {
		super();
		this.stockingLocations = locations;
		this.detail = detail;
		this.fulfillmentOptions = fulfillmentOptions;
		for (int i = 0; i < detail.getQuantity(); i++) {
			StockPutawayDetail stockPutawayDetail = new StockPutawayDetail();
			stockPutawayDetail.setDetail(detail);
			stockPutaway.add(stockPutawayDetail);
		}
		Optional<Entry<Integer, Integer>> arrivalDateMap = locations
				.stream()
				.collect(Collectors.groupingBy(location -> location.getArrivalDateAsInt(), Collectors.summingInt(location -> location.getQoh())))
				.entrySet()
					.stream()
						.sorted(Comparator.comparing(i -> i.getKey()))
						.filter(entry -> entry.getValue() >= detail.getQuantity())
					.findFirst();
			if(arrivalDateMap.isPresent()) {
				this.stockingLocations.stream().forEach(i -> i.setMinFillableArrivalDate(arrivalDateMap.get().getKey()));
			}
	}

	public StockPutawayOptimization() {
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

	public List<StockingLocation> getStockingLocations() {
		return stockingLocations;
	}

	public void setStockingLocations(List<StockingLocation> locations) {
		this.stockingLocations = locations;
	}

	public void setStockPutaway(List<StockPutawayDetail> stockPutaway) {
		this.stockPutaway = stockPutaway;
	}

	public List<StockPutawayDetail> getStockPutaway() {
		return stockPutaway;
	}

	public Map<String, Long> getStockPutawayMap() {
		return this.stockPutaway.stream().filter(i -> i.getLocation() != null)
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
