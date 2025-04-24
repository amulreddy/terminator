package com.autowares.mongoose.coordinator.optaplanner;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.optaplanner.benchmark.api.PlannerBenchmark;
import org.optaplanner.benchmark.api.PlannerBenchmarkFactory;

import com.autowares.mongoose.optaplanner.domain.FulfillmentLocation;
import com.autowares.mongoose.optaplanner.domain.OrderDetail;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.google.common.collect.Lists;

@Disabled
public class Benchmark {

	PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory
			.createFromXmlResource("optaplannerBenchmark.xml");

	@Test
	public void largeOrderAmount() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(150);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(200);
		cwt.setTransfers(2);
		cwt.setLogisticalScore(11);
		cwt.setArrivalDateAsInt(74585);
		cwt.setArrivalDate(ZonedDateTime.of(2021, 8, 19, 0, 0, 0, 0, ZoneId.systemDefault()));

		FulfillmentLocation chi = new FulfillmentLocation();
		chi.setLocationName("CHI");
		chi.setQoh(140);
		chi.setTransfers(3);
		chi.setLogisticalScore(302);
		chi.setArrivalDateAsInt(74585);
		chi.setArrivalDate(ZonedDateTime.of(2021, 8, 19, 0, 0, 0, 0, ZoneId.systemDefault()));

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(130);
		wrn.setTransfers(1);
		wrn.setLogisticalScore(352);
		wrn.setArrivalDateAsInt(74585);
		wrn.setArrivalDate(ZonedDateTime.of(2021, 8, 19, 0, 0, 0, 0, ZoneId.systemDefault()));


		FulfillmentOptions options = new FulfillmentOptions();
		options.setReduceTransfers(true);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail,
				Lists.newArrayList(cwt, chi, wrn), options);

		PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(f);
		benchmark.benchmarkAndShowReportInBrowser();

	}
	
	@Test
	public void shouldBeFastButIsnt() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(25);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(27);
		cwt.setLogisticalScore(11);

		FulfillmentLocation chi = new FulfillmentLocation();
		chi.setLocationName("CHI");
		chi.setQoh(20);
		chi.setLogisticalScore(302);

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(22);
		wrn.setLogisticalScore(352);

		FulfillmentOptions options = new FulfillmentOptions();

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail,
				Lists.newArrayList(cwt, chi, wrn), options);

		PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(f);
		benchmark.benchmarkAndShowReportInBrowser();

	}


}
