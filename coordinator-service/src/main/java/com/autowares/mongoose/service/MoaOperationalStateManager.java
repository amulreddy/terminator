package com.autowares.mongoose.service;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.IntegrationContextTypeConverter;
import com.autowares.mongoose.config.ObjectMapperConfig;
import com.autowares.mongoose.exception.UnimplementedException;
import com.autowares.mongoose.model.OperationalStateManager;
import com.autowares.orders.clients.OrderClient;
import com.autowares.orders.model.Item;
import com.autowares.partyconfiguration.model.Environment;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.OperationalItem;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Qualifier("ViperSystem")
public class MoaOperationalStateManager implements OperationalStateManager {

	@Autowired
	private ViperToWmsCoreUtils viperToWmsCoreUtils = new ViperToWmsCoreUtils();
	
	@Autowired
	private ObjectMapper objectMapper;

	private OrderClient orderClient;
	
	@PostConstruct
	public void init() {
		orderClient  = viperToWmsCoreUtils.getClient();
	}
	
	public MoaOperationalStateManager withEnvironment(Environment environment) {
		if (Environment.prod.equals(environment)) {
			orderClient = viperToWmsCoreUtils.getOrderProd();
		} else {
			orderClient = viperToWmsCoreUtils.getOrderTest();
		}
		return this;
	}

	@Override
	public OperationalContext getOperationalContext(Long itemId) {
		Item item = viperToWmsCoreUtils.lookupOrderByDetails(itemId, orderClient);
		return IntegrationContextTypeConverter.moaItemToOperationalContext(item);
	}

	@Override
	public OperationalContext getOperationalContext(String xmlOrderId, String building, Integer lineNumber) {
		Item item = viperToWmsCoreUtils.lookupOrderByXmlOrderIdLineNumberBuilding(xmlOrderId, lineNumber, building, orderClient);
		return IntegrationContextTypeConverter.moaItemToOperationalContext(item);
	}

	@Override
	public OperationalContext getOperationalContext(String packSlipId, Integer lineNumber) {
		Item item = viperToWmsCoreUtils.lookupOrderByPackSlipIdLineNumber(packSlipId, lineNumber, orderClient);
		if (item !=null) {
			return IntegrationContextTypeConverter.moaItemToOperationalContext(item);
		}
		return null;
	}

	@Override
	public void mergeContexts(OperationalContext sourceContext, OperationalContext targetContext) {
		OperationalItem targetItem = targetContext.getItems().get(0);
		try {
			if (objectMapper == null) {
				objectMapper = ObjectMapperConfig.objectMapper();
			}
			OperationalContext deepCopy = objectMapper.readValue(objectMapper.writeValueAsString(sourceContext),
					OperationalContext.class);
			for (OperationalItem item : deepCopy.getItems()) {
				item.setSource(targetItem.getSource());
			}
			persist(deepCopy);
		} catch (Exception e) {
			e.printStackTrace();
		} 

	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends com.autowares.servicescommon.model.OperationalItem> T persist(T operationalItem) {
		if (operationalItem instanceof Item) {
			Optional<Item> optionalItem = orderClient.saveItem((Item) operationalItem);
			if (optionalItem.isPresent()) {
				return (T) optionalItem.get();
			}
		}
		if (operationalItem instanceof com.autowares.supplychain.model.OperationalItem) {
			com.autowares.supplychain.model.OperationalItem s = (com.autowares.supplychain.model.OperationalItem) operationalItem;
			Optional<Item> optionalItem = orderClient.saveItem((Item) s.getSource());
			if (optionalItem.isPresent()) {
				return (T) optionalItem.get();
			}
		}
		throw new UnimplementedException("Didn't get a valid item.");
	}

	@Override
	public OperationalContext persist(OperationalContext context) {
		Item item = IntegrationContextTypeConverter.OperationalContexttoMoaItem(context);
		item = orderClient.saveItem(item).get();
		return IntegrationContextTypeConverter.moaItemToOperationalContext(item);
	}
}
