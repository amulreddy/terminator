package com.autowares.mongoose.coordinator.optaplanner;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import com.autowares.mongoose.optaplanner.domain.FulfillmentLocation;
import com.autowares.mongoose.optaplanner.domain.OrderDetail;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.util.PrettyPrint;
import com.google.common.collect.Lists;

public class FulfillmentTest {
	
	SolverFactory<OrderDetailFulfillment> solverFactory = SolverFactory
			.createFromXmlResource("OrderFulfillmentSolverConfig.xml");
	ScoreManager<OrderDetailFulfillment, HardMediumSoftScore> manager = ScoreManager.create(solverFactory);
	
	private OrderDetailFulfillment solve(OrderDetailFulfillment problem) {
		Solver<OrderDetailFulfillment> solver = solverFactory.buildSolver();
		OrderDetailFulfillment x = solver.solve(problem);
		manager = ScoreManager.create(solverFactory);
		System.out.println(manager.explainScore(x));
		return x;
	}

	@Test
	public void availabilityTest() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(9);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(3);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(3);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(5);
		
		FulfillmentLocation gay = new FulfillmentLocation();
		gay.setLocationName("GAY");
		gay.setQoh(6);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(flt, grr, cwt, gay), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(1, fulfillment.get("CWT").intValue());
		assertEquals(3, fulfillment.get("GRR").intValue());
		assertEquals(5, fulfillment.get("FLT").intValue());
		assertEquals(0, fulfillment.get("GAY").intValue());
	}

	@Test
	public void partialAvailabilityTest() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(3);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(3);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(0);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr, flt), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(3, fulfillment.get("CWT").intValue());
		assertEquals(3, fulfillment.get("GRR").intValue());
		assertNull(fulfillment.get("FLT"));
	}

	@Test
	public void moreAvailabilityInOneLocationTest() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(40);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(3);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(3);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(100);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt,grr,flt), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(f.getScore());
		PrettyPrint.print(fulfillment);
	
		assertTrue(f.getScore().isFeasible());
		assertNull(fulfillment.get("CWT"));
		assertNull(fulfillment.get("GRR"));
		assertEquals(40, fulfillment.get("FLT").intValue());
	}

	@Test
	@Disabled
	public void basicLogisticalTest() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setLogisticalScore(16);
		cwt.setQoh(66);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(22);
		grr.setLogisticalScore(10);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(10);
		flt.setLogisticalScore(40);
		//40 is the break point as its evenly divisible by 5

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr, flt), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(f.getFulfillmentMap());
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("FLT").intValue());
	}

	//TODO this should put them all at FLT
	@Test
	public void logisticalShouldntOverrideConsolidation() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(4);
		cwt.setLogisticalScore(441);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(4);
		grr.setLogisticalScore(441);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(11);
		flt.setLogisticalScore(99);
		ArrayList<FulfillmentLocation> options = Lists.newArrayList(flt, cwt, grr);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, options, new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(f.getFulfillmentMap());
		Map<Object, Indictment<HardMediumSoftScore>> imap = manager.explainScore(f).getIndictmentMap();
		for (Entry<Object, Indictment<HardMediumSoftScore>> entry : imap.entrySet()) {
			Indictment<HardMediumSoftScore> indictment = entry.getValue();
			 if (indictment == null) {
				    System.out.println("Null Indictment");
			        continue;
			 }
			  // The score impact of that planning entity
			    HardMediumSoftScore totalScore = indictment.getScore();
			    System.out.println("TotalScore " + totalScore);

			    for (ConstraintMatch<HardMediumSoftScore> constraintMatch : indictment.getConstraintMatchSet()) {
			        String constraintName = constraintMatch.getConstraintName();
			        HardMediumSoftScore score = constraintMatch.getScore();
			        System.out.println(constraintName + " " + score);
			    }
			 
		}
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("FLT").intValue());
	}

	@Test
	public void logisticalTestWithOneBigBadLogisticalLocation() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(2);
		cwt.setLogisticalScore(32);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(1);
		grr.setLogisticalScore(32);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(11);
		flt.setLogisticalScore(2);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr, flt), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(f.getFulfillmentMap());
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("FLT").intValue());
	}

	@Test
	public void logisticalTestWithDifferentArrivalDates() {

		// Even with a large number of product and higher logistical score (impossible?
		// as arrival is the majority of that score)
		// Fulfilling at FLT breaks the customer impact constraint so it only uses FLT
		// since a null fulfillment is a hard constraint
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(5);
		cwt.setArrivalDateAsInt(2);
		cwt.setLogisticalScore(10);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(5);
		grr.setArrivalDateAsInt(2);
		grr.setLogisticalScore(10);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(41);
		flt.setArrivalDateAsInt(3);
		flt.setLogisticalScore(100);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr, flt), new FulfillmentOptions());
		f = solve(f);
		PrettyPrint.print(f);
		
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(5, fulfillment.get("GRR").intValue());
		assertEquals(5, fulfillment.get("CWT").intValue());

	}

	@Test
	public void testWithDifferentArrivalDates() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(2);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(2);
		cwt.setArrivalDateAsInt(4);
		cwt.setMinFillableArrivalDate(3);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(2);
		grr.setArrivalDateAsInt(3);
		grr.setMinFillableArrivalDate(3);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(1);
		flt.setArrivalDateAsInt(2);
		flt.setMinFillableArrivalDate(3);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr, flt), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(2, fulfillment.get("GRR").intValue());

	}
	
	@Test
	public void testWithDifferentArrivalDates2() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(2);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(2);
//		cwt.setArrivalDateAsInt(4);
//		cwt.setMinFillableArrivalDate(3);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(2);
		grr.setArrivalDateAsInt(3);
		grr.setMinFillableArrivalDate(3);
		grr.setTransfers(1);

		FulfillmentLocation flt = new FulfillmentLocation();
		flt.setLocationName("FLT");
		flt.setQoh(1);
		flt.setArrivalDateAsInt(2);
		flt.setMinFillableArrivalDate(3);
		flt.setTransfers(2);

		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr, flt), new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(2, fulfillment.get("GRR").intValue());

	}
	
	@Test
	public void testHoldBack() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);
		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(6);

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(3);
		
		FulfillmentOptions options = new FulfillmentOptions();
		options.setPartiallyFill(false);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, grr), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertFalse(f.getScore().isFeasible());
	}
	
	@Test
	public void testAllowStoreLocation() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(6);

		FulfillmentLocation store = new FulfillmentLocation();
		store.setLocationName("323");
		store.setIsStore(true);
		store.setQoh(60);

		FulfillmentOptions options = new FulfillmentOptions();
		options.setPreferStoreInventory(false);
		options.setAllowStoreInventory(false);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(store, cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertNull(fulfillment.get("323"));

	}
	
	@Test
	public void testPreferStoreLocation() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(60);

		FulfillmentLocation store = new FulfillmentLocation();
		store.setLocationName("323");
		store.setIsStore(true);
		store.setQoh(60);

		FulfillmentOptions options = new FulfillmentOptions();
		options.setPreferStoreInventory(true);
		options.setAllowStoreInventory(true);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(store, cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("323").intValue());
	}
	
	@Test
	public void testPreferredLocation() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(10);
		cwt.setPreferredLocation(true);
		cwt.setMinFillableArrivalDate(1);
		cwt.setArrivalDateAsInt(2);

		FulfillmentLocation store = new FulfillmentLocation();
		store.setLocationName("GRR");
//		store.setIsStore(true);
		store.setQoh(60);
		store.setMinFillableArrivalDate(1);
		store.setArrivalDateAsInt(1);

		FulfillmentOptions options = new FulfillmentOptions();
//		options.setPreferStoreInventory(true);
//		options.setAllowStoreInventory(true);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(store, cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("CWT").intValue());
	}
	
	
	
	@Test
	public void testPreferAWILocation() {
		ZonedDateTime arrivalDate = ZonedDateTime.now().plusDays(1);
		
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setArrivalDate(arrivalDate);;
		cwt.setQoh(4);

		FulfillmentLocation store = new FulfillmentLocation();
		store.setLocationName("323");
		store.setIsAwi(false);
		store.setArrivalDate(arrivalDate);
		store.setQoh(99);

		FulfillmentOptions options = new FulfillmentOptions();
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(store, cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(f);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("323").intValue());
	}
	
	@Test
	public void testAvailableToday() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(60);
		cwt.setArrivalDate(ZonedDateTime.now().plusDays(1));

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(6);
		wrn.setArrivalDate(ZonedDateTime.now());

		FulfillmentOptions options = new FulfillmentOptions();
		options.setNeedToday(true);
		options.setPartiallyFill(false);
		
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(wrn, cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertFalse(f.getScore().isFeasible());
	}
	
	@Test
	public void testDrainLocations() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(60);

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(10);

		FulfillmentOptions options = new FulfillmentOptions();
		options.setDrainInventory(true);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, wrn), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("WRN").intValue());
	}
	
	
	@Test
	public void testTransfers() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(60);
		cwt.setTransfers(3);

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(60);
		wrn.setTransfers(1);

		FulfillmentOptions options = new FulfillmentOptions();
		options.setReduceTransfers(true);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt, wrn), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("WRN").intValue());
	}
	
	@Test
	public void testTransfersScoresLessThanConsolidation() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(9);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(6);
		cwt.setTransfers(1);
		
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(8);
		grr.setTransfers(1);

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(9);
		wrn.setTransfers(3);

		FulfillmentOptions options = new FulfillmentOptions();
		options.setReduceTransfers(false);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(grr, cwt, wrn), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(9, fulfillment.get("WRN").intValue());
	}
	
	@Test
	public void testFulfillmentOneLocation() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(60);
		cwt.setTransfers(5);

		FulfillmentOptions options = new FulfillmentOptions();
		options.setReduceTransfers(true);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("CWT").intValue());
	}
	
	@Test
	public void testPk() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(9);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(6);
		//cwt.setArrivalDateAsInt(20);
		
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(8);
		//grr.setArrivalDateAsInt(20);

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(900);
		wrn.setPkLocation(true);
		//wrn.setArrivalDateAsInt(10);

		FulfillmentOptions options = new FulfillmentOptions();
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(grr, cwt, wrn), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(1, fulfillment.get("CWT").intValue());
		assertEquals(8, fulfillment.get("GRR").intValue());
	}
	
	@Test
	public void testPk2() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(1);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(6);
		cwt.setTransfers(2);
		//cwt.setArrivalDateAsInt(20);
		
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(8);
		grr.setTransfers(2);
		//grr.setArrivalDateAsInt(20);

		FulfillmentLocation wrn = new FulfillmentLocation();
		wrn.setLocationName("WRN");
		wrn.setQoh(900);
		wrn.setPkLocation(true);
		wrn.setTransfers(1);
		//wrn.setArrivalDateAsInt(10);

		FulfillmentOptions options = new FulfillmentOptions();
		
		options.setServiceClass(ServiceClass.Express);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(grr, cwt, wrn), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(1, fulfillment.get("WRN").intValue());
	}
	
	
	@Test
	public void testSplitCase() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(2);

		FulfillmentLocation chi = new FulfillmentLocation();
		chi.setLocationName("CHI");
		chi.setQoh(1);
		chi.setTransfers(1);
		chi.setArrivalDate(ZonedDateTime.now().plusHours(1));
		chi.setArrivalDateAsInt(21041);
		
		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setLocationName("GRR");
		grr.setQoh(9);
		grr.setArrivalDate(ZonedDateTime.now().plusDays(1));
		grr.setTransfers(2);
		grr.setArrivalDateAsInt(107441);

		FulfillmentOptions options = new FulfillmentOptions();
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(chi,grr), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(2, fulfillment.get("GRR").intValue());
	}
	
	@Test
	public void testPartialNonAwi() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(3);

		FulfillmentLocation per = new FulfillmentLocation();
		per.setLocationName("PER");
		per.setQoh(1);
		
		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(1);
		
		FulfillmentOptions options = new FulfillmentOptions();
		options.setPartiallyFill(false);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(per,cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertFalse(f.getScore().isFeasible());
//		assertEquals(2, fulfillment.get("GRR").intValue());
	}
	
	@Test
	public void testPartialFillToday() {
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(5);

		FulfillmentLocation per = new FulfillmentLocation();
		per.setLocationName("PER");
		per.setArrivalDate(ZonedDateTime.now());
		per.setQoh(1);
		
		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setArrivalDate(ZonedDateTime.now().plusDays(2));
		cwt.setQoh(6);
		
		FulfillmentOptions options = new FulfillmentOptions();
		options.setPartiallyFill(true);
		options.setNeedToday(true);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(per,cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
//		assertEquals(2, fulfillment.get("GRR").intValue());
	}

	
//	@Test
//	public void testDontTransferBestEffortIfServicingStocked() {
//		OrderDetail detail = new OrderDetail();
//		detail.setDetailId(1);
//		detail.setOrderAmount(5);
//
//		FulfillmentLocation grr = new FulfillmentLocation();
//		grr.setLocationName("GRR");
//		grr.setArrivalDate(ZonedDateTime.now());
//		grr.setQoh(0);
//		grr.setServicingLocation(true);
//		
//		FulfillmentLocation cwt = new FulfillmentLocation();
//		cwt.setLocationName("CWT");
//		cwt.setArrivalDate(ZonedDateTime.now());
//		cwt.setPreMark(88);
//		cwt.setQoh(60);
//		
//		FulfillmentOptions options = new FulfillmentOptions();
//		options.setServiceClass(ServiceClass.BestEffort);
//		
//		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(grr,cwt), options);
//		f = solve(f);
//		Map<String, Long> fulfillment = f.getFulfillmentMap();
//		PrettyPrint.print(fulfillment);
//		assertEquals(-25, f.getScore().getMediumScore());
//	}
	
	@Test
	public void testOverStock() {
		
		PrettyPrint.print( ZonedDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS));
		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(5);
		ZonedDateTime arrival = ZonedDateTime.now();
		
		FulfillmentLocation per = new FulfillmentLocation();
		per.setLocationName("FLT");
		per.setArrivalDate(arrival);
		per.setQoh(15);
		per.setPreMark(1);
		per.setTransfers(1);
		
		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setArrivalDate(arrival);
		cwt.setQoh(60);
		cwt.setPreMark(100);
		cwt.setTransfers(1);
		
		FulfillmentOptions options = new FulfillmentOptions();
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(per,cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(per);
		PrettyPrint.print(cwt);
		PrettyPrint.print(fulfillment);
		assertEquals(5, fulfillment.get("FLT").intValue());
	}
	
	@Test
	public void testScore() {

		FulfillmentLocation grr = new FulfillmentLocation();
		grr.setQoh(100);
		grr.setPreMark(200);
		
		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setQoh(19);
		cwt.setPreMark(4);
		
		PrettyPrint.print(grr);
		PrettyPrint.print(cwt);
	}
	
	@Test
	public void testSupplierLocation() {

		OrderDetail detail = new OrderDetail();
		detail.setDetailId(1);
		detail.setOrderAmount(10);

		FulfillmentLocation cwt = new FulfillmentLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(60);
		cwt.setTransfers(0);
		cwt.setArrivalDate(ZonedDateTime.now().plusDays(1));

		FulfillmentLocation supplier = new FulfillmentLocation();
		supplier.setLocationName("WIX");
		supplier.setIsSupplier(true);
		supplier.setIsAwi(false);
		supplier.setQoh(10);
		supplier.setTransfers(10);
		supplier.setArrivalDate(ZonedDateTime.now().plusDays(10));

		FulfillmentOptions options = new FulfillmentOptions();
		options.setPreferSupplierInventory(true);
		options.setAllowSupplierInventory(true);
		options.setPreferAWILocations(false);
		
		OrderDetailFulfillment f = new OrderDetailFulfillment(detail, Lists.newArrayList(supplier, cwt), options);
		f = solve(f);
		Map<String, Long> fulfillment = f.getFulfillmentMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(10, fulfillment.get("WIX").intValue());
	}


}
