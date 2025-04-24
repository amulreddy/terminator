package com.autowares.mongoose.camel.processors.util;

import javax.transaction.Transactional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class PrettyPrinter implements Processor {

	@Override
	@Transactional
	public void process(Exchange exchange) throws Exception {
		Object o = exchange.getIn().getBody();
		PrettyPrint.print(o);
	}

}
