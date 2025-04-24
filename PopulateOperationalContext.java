package com.autowares.mongoose.camel.processors.integration;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.events.BaseViperEvent;
import com.autowares.mongoose.model.IntegrationContext;
import com.autowares.mongoose.service.MoaOperationalStateManager;
import com.autowares.supplychain.model.OperationalContext;

@Component
public class PopulateOperationalContext implements Processor {

	@Autowired
	MoaOperationalStateManager prodMoaContextResolver;
	
//	@Autowired
//	WmsCoreOperationalStateManager prodWmsContextResolver;
	
	@Override
	public void process(Exchange exchange) throws Exception {
		IntegrationContext context = exchange.getIn().getBody(IntegrationContext.class);
		
		if ("viper".equals(context.getOriginatingSystem())) {
			if (context.getEvent() instanceof BaseViperEvent) {
				BaseViperEvent event = (BaseViperEvent) context.getEvent();
				@SuppressWarnings("unused")
				OperationalContext viperContext = prodMoaContextResolver.getOperationalContext(event.getOrderID());
//				OperationalContext wmsCoreContext = prodWmsContextResolver.getOperationalContext(event.getOrderID());
//				assert viperContext.getItems().size() == wmsCoreContext.getItems().size();
//				assert viperContext.getOrderedCount() == wmsCoreContext.getOrderedCount();
//				assert viperContext.getPulledCount() == wmsCoreContext.getPulledCount();
//				assert viperContext.getPackedCount() == wmsCoreContext.getPackedCount();
			}
		}
		
	}

}
