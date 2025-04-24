package com.autowares.mongoose.coordinator.optaplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import com.autowares.mongoose.optaplanner.domain.StockDetail;
import com.autowares.mongoose.optaplanner.domain.StockPutawayOptimization;
import com.autowares.mongoose.optaplanner.domain.StockingLocation;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.util.PrettyPrint;
import com.google.common.collect.Lists;

public class PutawayOptimizationTest {

	SolverFactory<StockPutawayOptimization> solverFactory = SolverFactory
			.createFromXmlResource("StockPutawaySolverConfig.xml");
	Solver<StockPutawayOptimization> solver = solverFactory.buildSolver();

	private StockPutawayOptimization solve(StockPutawayOptimization problem) {
		StockPutawayOptimization x = solver.solve(problem);
		ScoreManager<StockPutawayOptimization, ?> manager = ScoreManager.create(solverFactory);
		System.out.println(manager.explainScore(x));
		return x;
	}

	@Test
	public void maxTest() {
		StockDetail detail = new StockDetail();
		detail.setDetailId(1);
		detail.setQuantity(10);

		StockingLocation cwt = new StockingLocation();
		cwt.setLocationName("CWT");
		cwt.setPreMark(3);

		StockingLocation grr = new StockingLocation();
		grr.setLocationName("GRR");
		grr.setPreMark(3);

		StockingLocation flt = new StockingLocation();
		flt.setLocationName("FLT");
		flt.setPreMark(6);
		;

		StockPutawayOptimization f = new StockPutawayOptimization(detail, Lists.newArrayList(cwt, grr, flt),
				new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getStockPutawayMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());
		assertEquals(3, fulfillment.get("CWT").intValue());
		assertEquals(3, fulfillment.get("GRR").intValue());
		assertEquals(4, fulfillment.get("FLT").intValue());
	}

	@Test
	public void preferLowStock() {
		StockDetail detail = new StockDetail();
		detail.setDetailId(1);
		detail.setQuantity(10);

		StockingLocation cwt = new StockingLocation();
		cwt.setLocationName("CWT");
		cwt.setQoh(11);

		StockingLocation grr = new StockingLocation();
		grr.setLocationName("GRR");
		grr.setQoh(0);

		StockingLocation flt = new StockingLocation();
		flt.setLocationName("FLT");
		flt.setQoh(500);

		StockPutawayOptimization f = new StockPutawayOptimization(detail, Lists.newArrayList(cwt, grr, flt),
				new FulfillmentOptions());
		f = solve(f);
		Map<String, Long> fulfillment = f.getStockPutawayMap();
		PrettyPrint.print(fulfillment);
		assertTrue(f.getScore().isFeasible());

	}

}
