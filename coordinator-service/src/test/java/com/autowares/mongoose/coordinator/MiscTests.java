package com.autowares.mongoose.coordinator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import com.autowares.mongoose.camel.components.MyPlaceNotificationTypeConverters;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.IntegrationContext;
import com.autowares.mongoose.model.OperationalStateManager;
import com.autowares.mongoose.service.MoaOperationalStateManager;
import com.autowares.partyconfiguration.client.PartyConfigurationClient;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.Environment;
import com.autowares.partyconfiguration.model.Partnership;
import com.autowares.partyconfiguration.model.PartnershipType;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.CustomerFulfillmentOptions;
import com.autowares.servicescommon.model.OperationalStage;
import com.autowares.servicescommon.model.OrderSource;
import com.autowares.servicescommon.model.PartLineItem;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.Operation;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainPerson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.google.common.collect.Lists;

public class MiscTests {
	
	@Test
	public void testMethod () {
		
		String regEx = ".*([\\d]{2})$";
		Matcher testVar = Pattern.compile(regEx).matcher("VC_PK01");
		if (testVar.matches()) {
			testVar.group(1);
			System.out.println("warehouseNumber = " + testVar.group(1));
		}
	}
	
	@Test
	public void tM () {
		
		String regEx = "([A-Z0-9]{4})[A-Z0-9]";
		Matcher testVar = Pattern.compile(regEx).matcher("SD01NWX01N   \n    \n   14");
		while (testVar.find()) {
			System.out.println("VendorSubCode = " + testVar.group(1));
		}
		
	}
	
	@Test
	public void testSort() {
		SystemType lookupSystemType = OrderSource.AWIOL.getSystemType();
		SupplyChain awi = new SupplyChain();
		awi.setSupplyingPartnership(new Partnership(null, null, SystemType.AwiWarehouse, SystemType.AwiWarehouse, PartnershipType.Supplying, null));
		SupplyChain myPlace = new SupplyChain();
		myPlace.setSupplyingPartnership(new Partnership(null, null, SystemType.MyPlace, SystemType.MyPlace, PartnershipType.Supplying, null));
		SupplyChain ms = new SupplyChain();
		ms.setSupplyingPartnership(new Partnership(null, null, SystemType.MotorState, SystemType.MotorState, PartnershipType.Supplying, null));
		SupplyChain result = Lists.newArrayList(myPlace, awi, ms).stream().sorted(resultComparator(lookupSystemType)).findFirst().get();
		SupplyChain result2 = Lists.newArrayList(ms, myPlace, awi).stream().sorted(resultComparator(lookupSystemType)).findFirst().get();
		SupplyChain result3 = Lists.newArrayList(awi, ms, myPlace).stream().sorted(resultComparator(lookupSystemType)).findFirst().get();

//		PrettyPrint.print(result);
		assertEquals(lookupSystemType, result.getSupplyingPartnership().getSupplier().getSystemType());
		assertEquals(lookupSystemType, result2.getSupplyingPartnership().getSupplier().getSystemType());
		assertEquals(lookupSystemType, result3.getSupplyingPartnership().getSupplier().getSystemType());

	}
	
	private Comparator<SupplyChain> resultComparator(SystemType systemType) {

		Comparator<SupplyChain> systemTypeComparator = Comparator.comparing(SupplyChain::getSupplyingPartnership, (s1, s2) -> {
			if (s1 == s2) {
				return 0;
			}
			if (s1 != null && s1.getSupplier().getSystemType().equals(systemType)) {
				return -1;
			}
			return 0;
		});

		return systemTypeComparator;
	}
	
	@Test
	public void exchangeHeaderTest() {
		
		CamelContext cc = new DefaultCamelContext();
		DefaultExchange exchange = new DefaultExchange(cc);
//		exchange.getIn().setHeader("test", "Y");
		Object testVar = exchange.getIn().getHeader("test");
		if (testVar != null) {
			PrettyPrint.print(testVar);
		} else {
			System.out.println("testVar is null.");
		}
		
	}
	
	
	@Test
	public void modelPlay() {
		CoordinatorContext order = new CoordinatorContextImpl();
		FulfillmentLocationContext fc = new FulfillmentLocationContext(order, "GRR");
		OperationalContext oc = new OperationalContext();
		fc.setOperationalContext(oc);
		PartLineItem lineItem = GenericLine.builder().withProductId(12345l).withPartNumber("51515").withLineCode("WIX").withQuantity(15).build();
		PartLineItem lineItem2 = GenericLine.builder().withProductId(12345l).withPartNumber("STD").withLineCode("AC1").withQuantity(10).build();
		
		for (int i = 0; i < lineItem.getQuantity(); i++) {
			OperationalItem item = new OperationalItem(lineItem);
			oc.getItems().add(item);
			Operation pulled = new Operation(item, oc);
			pulled.setOperationalStage(OperationalStage.pulled);
			pulled.setActor(SupplyChainPerson.builder().withName("Brian").build());
			item.setCurrentOperation(pulled);
			Operation packOperation = new Operation(item, oc);
			packOperation.setOperationalStage(OperationalStage.packed);
			packOperation.setActor(SupplyChainPerson.builder().withName("George").build());
			item.setCurrentOperation(packOperation);
		}
		
		OperationalItem item2 = new OperationalItem(lineItem2);
		oc.getItems().add(item2);
		
		Optional<OperationalItem> optionalItem  = oc.findByProductStage(lineItem, OperationalStage.packed).stream().findAny();
		OperationalItem item1 = optionalItem.get();

		assertFalse(item1.hasBeenThroughStage(OperationalStage.shipped));
		assertTrue(item1.hasBeenThroughStage(OperationalStage.pulled));
		assertTrue(item1.hasBeenThroughStage(OperationalStage.packed));
	}
	

	
	@Test
	/*
	 * Move to Integration testing if keeping
	 */
	@Disabled
	public void syncContexts() {
		String xmlOrderId = "1152229388499832832";
		String building = "GRR";
		Integer lineNumber = 1;
		IntegrationContext integration = new IntegrationContext();
		// We got an event sourced from viper
		integration.setOriginatingSystem("viper");
		// Resolve source state
		MoaOperationalStateManager prodResolver = new MoaOperationalStateManager();
		prodResolver.withEnvironment(Environment.prod);
		OperationalContext operationalContext = prodResolver.getOperationalContext(xmlOrderId, building, lineNumber);
		// Resolve target states
		MoaOperationalStateManager testResolver = new MoaOperationalStateManager();
		testResolver.withEnvironment(Environment.test);
		OperationalContext testOperationalContext = testResolver.getOperationalContext(xmlOrderId, building, lineNumber);

		integration.setSourceContext(Pair.of(operationalContext, prodResolver));
		integration.getTargetContexts().add(Pair.of(testOperationalContext, testResolver));
		
		for (Pair<OperationalContext, OperationalStateManager> target : integration.getTargetContexts()) {
			OperationalContext sourceContext = integration.getSourceContext().getFirst();
			OperationalContext targetContext = target.getFirst();
			//if (targetContext.isOutOfSync(sourceContext)) {
				// Merge any target with the source data
				target.getSecond().mergeContexts(sourceContext, targetContext);
		//	} else {
				System.out.println("Target "+ target.getSecond().getClass().getSimpleName() +"  is in sync");
		//	}
		}
	}
	
	
	@Test
	/*
	 * Move to Integration testing if keeping
	 */
	@Disabled
	public void testConfiguration() throws JsonProcessingException {
//		Pair pair = Pair.of(" ", " ");
		PartyConfigurationClient partyConfigurationClient = new PartyConfigurationClient();
		partyConfigurationClient.withLocalService();
		ObjectMapper mapper = new ObjectMapper();
//		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		Collection<SupplyChain> supplyChains = partyConfigurationClient.findSupplyChains(Set.of(256L), Set.of(4656L), SystemType.AwiWarehouse);
//		SupplyChain supplyChain = supplyChains.stream().findFirst().get();
		mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, DefaultTyping.NON_FINAL);
		Configuration configuration = new Configuration();
		HashMap<String,Object> settings = new HashMap<String,Object>();
		settings.put("fulfillmentOptions", new CustomerFulfillmentOptions());
		configuration.setSettings(settings);
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configuration);
		System.out.println(json);
		Configuration deserialized = mapper.readValue(json, Configuration.class);
		
		
	}
	
	@Test
	public void miscTimeConversion() {
		
		Map<String, Object> map = new HashMap<>();
		String key = "testDate";
		map.put(key, "05/09/2024 6:52:09 AM");
		ZonedDateTime time = MyPlaceNotificationTypeConverters.getZonedDateTime(map, key);
		PrettyPrint.print(time);
	}
	
	
	
}
