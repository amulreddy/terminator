package com.autowares.mongoose.camel.processors.integration;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.command.YoozClient;
import com.autowares.mongoose.config.YoozConfig;
import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class YoozSendFiles implements Processor {
	
	@Autowired
	private YoozConfig yoozConfig;
	
	private YoozClient yoozClient = new YoozClient();
	
	private static Logger log = LoggerFactory.getLogger(YoozSendFiles.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		try {
			Object object = exchange.getIn().getBody();
			String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
			@SuppressWarnings("rawtypes")
			Map response = yoozClient.importFile(object, fileName);
			PrettyPrint.print(response);
		} catch (Exception e) {
			log.error("Failure sending files to Yooz ", e);
		}

	}
	
	@PostConstruct
	public void initialize() {
		yoozClient.withYoozConfig(yoozConfig);
	}

}
