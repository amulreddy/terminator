package com.autowares.mongoose.coordinator.optaplanner;

import org.junit.jupiter.api.Test;

import org.optaplanner.test.api.score.stream.ConstraintVerifier;
import com.autowares.mongoose.optaplanner.constraints.OrderConstraintProvider;
import com.autowares.mongoose.optaplanner.domain.FulfillmentLocation;
import com.autowares.mongoose.optaplanner.domain.OrderDetail;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.mongoose.optaplanner.domain.OrderFillDetail;
import com.autowares.servicescommon.model.FulfillmentOptions;

public class ConstraintStreamTests {

	private ConstraintVerifier<OrderConstraintProvider, OrderDetailFulfillment> constraintVerifier = ConstraintVerifier
			.build(new OrderConstraintProvider(), OrderDetailFulfillment.class, OrderFillDetail.class);

	@Test
	public void fillIfEnoughQuantity() {
		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(0);
		OrderDetail detail = new OrderDetail();
		OrderFillDetail fill = new OrderFillDetail(flt, detail, 0);
		OrderFillDetail fill1 = new OrderFillDetail(flt, detail, 1);
		constraintVerifier.verifyThat(OrderConstraintProvider::fillIfEnoughQuantity).given(fill, fill1).penalizesBy(2);
	}

	@Test
	public void notFillingPenalizes1() {

		OrderDetail detail = new OrderDetail();
		detail.setOrderAmount(1);
		detail.setDetailId(1);
		OrderFillDetail fill = new OrderFillDetail(null, detail, 0);
		constraintVerifier.verifyThat(OrderConstraintProvider::fillWhatWeCan).given(fill).penalizesBy(1);
	}

	@Test
	public void customerDeliveryImpact() {
		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setArrivalDateAsInt(3);
		flt.setMinFillableArrivalDate(1);
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setArrivalDateAsInt(2);
		grr.setMinFillableArrivalDate(2);
		OrderDetail detail = new OrderDetail();
		OrderFillDetail fill = new OrderFillDetail(flt, detail, 0);
		OrderFillDetail fill1 = new OrderFillDetail(flt, detail, 1);
		FulfillmentOptions options = new FulfillmentOptions();
		constraintVerifier.verifyThat(OrderConstraintProvider::customerDeliveryImpact)
				.given(grr, flt, fill, fill1, options).penalizesBy(1);
	}

	@Test
	public void testHBO() {
		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(1);
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		flt.setQoh(1);
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(100);
		OrderFillDetail fill = new OrderFillDetail(flt, detail, 0);
		OrderFillDetail fill1 = new OrderFillDetail(flt, detail, 1);
		FulfillmentOptions options = new FulfillmentOptions();
		options.setPartiallyFill(false);

		constraintVerifier.verifyThat(OrderConstraintProvider::holdBackOrder)
				.given(flt, grr, detail, fill, fill1, options).penalizesBy(1);
	}

	@Test
	public void customerDeliveryImpact2() {
		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
//		flt.setArrivalDateAsInt(3);
		flt.setMinFillableArrivalDate(1000000);
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setArrivalDateAsInt(2);
		grr.setMinFillableArrivalDate(2);
		OrderDetail detail = new OrderDetail();
		OrderFillDetail fill = new OrderFillDetail(flt, detail, 0);
		OrderFillDetail fill1 = new OrderFillDetail(flt, detail, 1);
		FulfillmentOptions options = new FulfillmentOptions();
		constraintVerifier.verifyThat(OrderConstraintProvider::customerDeliveryImpact)
				.given(grr, flt, fill, fill1, options).penalizesBy(1);
	}

}
