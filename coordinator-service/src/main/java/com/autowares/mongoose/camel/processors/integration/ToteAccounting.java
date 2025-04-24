package com.autowares.mongoose.camel.processors.integration;

import java.time.ZonedDateTime;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.orders.clients.ToteClient;

@Component
public class ToteAccounting implements Processor {

	private ToteClient toteClient = new ToteClient();

	@Override
	public void process(Exchange exchange) throws Exception {
		try {
			@SuppressWarnings("rawtypes")
			Map event = exchange.getIn().getBody(Map.class);
			toteClient.returnTote((String) event.get("barcode"), (Integer) event.get("stopNumber"), ZonedDateTime.now());
		} catch (Exception e) {
		//	e.printStackTrace();
		}
	}

}
