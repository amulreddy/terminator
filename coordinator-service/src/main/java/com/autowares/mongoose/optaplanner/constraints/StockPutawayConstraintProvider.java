package com.autowares.mongoose.optaplanner.constraints;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.count;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.countDistinct;
import static org.optaplanner.core.api.score.stream.Joiners.equal;
import static org.optaplanner.core.api.score.stream.Joiners.filtering;

import java.util.function.Function;

import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import com.autowares.mongoose.optaplanner.domain.StockDetail;
import com.autowares.mongoose.optaplanner.domain.StockPutawayDetail;
import com.autowares.mongoose.optaplanner.domain.StockingLocation;

@SuppressWarnings("removal")
public class StockPutawayConstraintProvider implements ConstraintProvider {

	@Override
	public Constraint[] defineConstraints(ConstraintFactory factory) {
		return new Constraint[] {
				putawayWhatWeCan(factory),
				putawayIfSpace(factory),
				penalizeExistingInventory(factory)
				//consolidate(factory)
		};
	}
	
	public Constraint putawayWhatWeCan(ConstraintFactory factory) {
		return factory.fromUnfiltered(StockPutawayDetail.class)
				.filter(i -> i.getLocation() == null)
				.penalize("Try to putaway what we can", HardMediumSoftScore.ONE_MEDIUM, (i -> i.getDetail().getQuantity()));
	}

	public Constraint putawayIfSpace(ConstraintFactory factory) {
		return factory.from(StockPutawayDetail.class)
				.groupBy(StockPutawayDetail::getLocation, count())
				.filter((location, preMark) -> location != null && location.getPreMark() != null && preMark > location.getPreMark())
				.penalize("Dont putaway more than premark", HardMediumSoftScore.ONE_MEDIUM,
						(location, preMark) -> preMark - location.getPreMark());
	}
	
	public Constraint penalizeExistingInventory(ConstraintFactory factory) {
		return factory.from(StockingLocation.class)
				.ifExists(StockPutawayDetail.class, equal(Function.identity(), StockPutawayDetail::getLocation))
				//.ifExists(FulfillmentOptions.class, filtering((location,options) -> options.getDrainInventory()))
				.join(StockDetail.class, filtering((location,detail) -> getStockPenalty(location,detail) > 1))
				.penalize("Penalize High Inventory", HardMediumSoftScore.ofSoft(10),
						((location, detail) -> getStockPenalty(location, detail)));
	}
	
	// Consolidate if we can
	public Constraint consolidate(ConstraintFactory factory) {
		return factory.from(StockPutawayDetail.class)
				.groupBy(countDistinct(StockPutawayDetail::getLocation))
				.filter(count -> count > 1)
				.penalize("Consolidate if possible", HardMediumSoftScore.ofSoft(10), count -> count);
	}
	
	// 1-5
	private Integer getStockPenalty(StockingLocation location, StockDetail detail) {
		return Math.min(5, (Math.max(1, location.getQoh()/detail.getQuantity())));
	}



	
}
