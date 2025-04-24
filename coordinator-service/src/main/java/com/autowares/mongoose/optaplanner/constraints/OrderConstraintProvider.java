package com.autowares.mongoose.optaplanner.constraints;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.count;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.countDistinct;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;
import static org.optaplanner.core.api.score.stream.Joiners.equal;
import static org.optaplanner.core.api.score.stream.Joiners.filtering;

import java.util.function.Function;

import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import com.autowares.mongoose.optaplanner.domain.FulfillmentLocation;
import com.autowares.mongoose.optaplanner.domain.OrderDetail;
import com.autowares.mongoose.optaplanner.domain.OrderFillDetail;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.ServiceClass;

public class OrderConstraintProvider implements ConstraintProvider {

	@Override
	public Constraint[] defineConstraints(ConstraintFactory factory) {
		return new Constraint[] { 
				fillIfEnoughQuantity(factory), 
				fillWhatWeCan(factory),
				rewardOverStockedLocations(factory),
				consolidate(factory), 
				drainLocations(factory),
				reduceTransfers(factory),
				//rewardLogisticallyGood(factory), 
				customerDeliveryImpact(factory),
				holdBackOrder(factory),
				punishNonAWI(factory),
				dontFillFromAWIifSpecified(factory),
				dontFillFromStoreUnlessSpecified(factory),
//				restrictGaylord(factory),
				preferStoreIfSpecified(factory),
				dontFillFromSupplierUnlessSpecified(factory),
				preferSupplierIfSpecified(factory),
				dontFillUnlessAvailableToday(factory),
//				dontTransferBestEffortIfServicingStocked(factory),
				pkVendorConstraint(factory),
				rewardPreferredLocations(factory)
		};
	}

	public Constraint fillIfEnoughQuantity(ConstraintFactory factory) {
		return factory.forEach(OrderFillDetail.class)
				.groupBy(OrderFillDetail::getLocation, count())
				.filter((location, fillQuantity) -> location != null && fillQuantity > location.getQoh())
				.penalize("Cant fill more than QOH", HardMediumSoftScore.ONE_HARD,
						(location, fillQuantity) -> fillQuantity - location.getQoh());
	}

	public Constraint fillWhatWeCan(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(OrderFillDetail.class)
				.filter(i -> i.getLocation() == null)
				.penalize("Try to fill what we can", HardMediumSoftScore.ONE_MEDIUM, (i -> i.getDetail().getOrderAmount()));
	}
	
	// Explicitly reward overstocked locations
	public Constraint rewardOverStockedLocations(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(i -> i.isOverstocked())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.reward("Prefer filling from locations that are overstocked", HardMediumSoftScore.ofSoft(1));
//      To address Patrick's pipeline purchasing.		
//		.reward("Prefer filling from locations that are overstocked", HardMediumSoftScore.ofSoft(1), (i -> (int) i.getOverStockedScore()));

	}
	
	public Constraint rewardPreferredLocations(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(i -> i.getPreferredLocation())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.reward("Prefer preferred locations.", HardMediumSoftScore.ofMedium(10));
	}

	// Drain locations if possible
	public Constraint drainLocations(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((location,options) -> options.getDrainInventory()))
				.penalize("Draining Inventory", HardMediumSoftScore.ofSoft(1),
						(location -> Math.round(location.getQoh() / 5)));
	}

	// Consolidate if we can
	public Constraint consolidate(ConstraintFactory factory) {
		return factory.forEach(OrderFillDetail.class)
				.groupBy(countDistinct(OrderFillDetail::getLocation))
				.filter(count -> count > 1)
				.penalize("Consolidate if possible", HardMediumSoftScore.ofSoft(1000), count -> count);
	}

	public Constraint reduceTransfers(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getTransfers() > 0)
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> options.getReduceTransfers()))
				.penalize("Prefer a lower number of transfers", HardMediumSoftScore.ofSoft(10),
						location -> location.getTransfers());
	}

	public Constraint rewardLogisticallyGood(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getLogisticalScore() != 0)
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.impact("Prefer filling from locations that are logistically good", HardMediumSoftScore.ofSoft(1),
						(location -> Math.round(location.getLogisticalScore() / 10)));
	}

	public Constraint punishNonAWI(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> !location.getIsAwi())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> options.getPreferAWILocations()))
				.penalize("Prefer filling from AWI Locations", HardMediumSoftScore.ofSoft(900));
	}
	
	public Constraint dontFillFromAWIifSpecified(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getIsAwi())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> !options.getAllowAWILocations()))
				.penalize("Dont fill from AWI Locations if specified", HardMediumSoftScore.ONE_HARD);
	}
	
	public Constraint customerDeliveryImpact(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(i -> i.getMinFillableArrivalDate() !=0 && i.getArrivalDateAsInt() > i.getMinFillableArrivalDate())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((min, options) -> !options.getAllowDeliveryImpact()))
				.penalize("Customer Delivery Impact", HardMediumSoftScore.ONE_MEDIUM);
	}
	
	public Constraint holdBackOrder(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.groupBy(sum(FulfillmentLocation::getQoh))
				.ifExists(FulfillmentOptions.class, filtering((sum, options) -> !options.getPartiallyFill()))
				.ifExists(OrderDetail.class, filtering((sum, detail) -> sum < detail.getOrderAmount()))
				.penalize("Cant Fully Fill a HBO", HardMediumSoftScore.ONE_HARD,sum -> sum);
	}

	public Constraint dontFillFromStoreUnlessSpecified(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getIsStore())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> !options.getAllowStoreInventory()))
				.penalize("Dont fill from Store Locations unless specified", HardMediumSoftScore.ONE_HARD);
	}
	
	public Constraint restrictGaylord(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getLocationName().equals("GAY"))
//				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
//				.ifExists(FulfillmentOptions.class, filtering((location, options) -> !options.getAllowStoreInventory()))
				.penalize("Dont fill from Gaylord.  EVER!!!", HardMediumSoftScore.ONE_HARD);
	}
	
	public Constraint preferStoreIfSpecified(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getIsStore())
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> options.getPreferStoreInventory()))
				.reward("Prefer store locations if specified", HardMediumSoftScore.ofMedium(2));
	}

	public Constraint dontFillFromSupplierUnlessSpecified(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getIsSupplier())
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> !options.getAllowSupplierInventory()))
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.penalize("Dont fill from Supplier Locations unless specified", HardMediumSoftScore.ONE_HARD);
	}
	
	public Constraint preferSupplierIfSpecified(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(location -> location.getIsSupplier())
				.ifExists(FulfillmentOptions.class, filtering((location, options) -> options.getPreferSupplierInventory()))
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.reward("Prefer supplier locations if specified", HardMediumSoftScore.ofMedium(2));
	}
	
	public Constraint dontFillUnlessAvailableToday(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.filter(i -> i.availableToday())
				.groupBy(sum(FulfillmentLocation::getQoh))
				.ifExists(FulfillmentOptions.class, filtering((sum, options) -> options.getNeedToday()))
				.ifExists(FulfillmentOptions.class, filtering((sum, options) -> !options.getPartiallyFill()))
				.ifExists(OrderDetail.class, filtering((sum, detail) -> sum < detail.getOrderAmount()))
				.penalize("Cant Fully Fill Today", HardMediumSoftScore.ONE_HARD, sum -> sum);
	}
	
	public Constraint dontTransferBestEffortIfServicingStocked(ConstraintFactory factory) {
		return factory.forEach(FulfillmentLocation.class)
				.ifExists(FulfillmentOptions.class, filtering((i, options) -> ServiceClass.BestEffort.equals(options.getServiceClass())))
				.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
				.filter(i -> !i.getServicingLocation())
				.ifExists(FulfillmentLocation.class, filtering((i, location) -> location.getServicingLocation()))
				.ifNotExists(FulfillmentLocation.class, filtering((i, location) -> location.isOverstocked()))
				.penalize("Wont consider non-servicing location for fulfillment on Best Effort since the servicing location stocks the product", HardMediumSoftScore.ONE_HARD);
	}
	
	public Constraint pkVendorConstraint(ConstraintFactory factory) {
			return factory.forEach(FulfillmentLocation.class)
					.filter(i -> i.getPkLocation())
					.ifExists(FulfillmentOptions.class, filtering((i, options) -> !ServiceClass.Express.equals(options.getServiceClass()) && !DeliveryMethod.CustomerPickUp.equals(options.getDeliveryMethod())))
					.ifExists(OrderFillDetail.class, equal(Function.identity(), OrderFillDetail::getLocation))
					.penalize("Avoid PK Location", HardMediumSoftScore.ofSoft(1000));
	}

}
