package com.autowares.mongoose.camel.processors.integration;

import java.io.StringWriter;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.EDITypeConverter;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.yooz.DataContainer;
import com.autowares.mongoose.model.yooz.YoozDocument;
import com.autowares.mongoose.model.yooz.YoozDocumentMapper;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.google.common.collect.Lists;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

@Component
public class YoozDocumentToCsv implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Object object = exchange.getIn().getBody();
		List<YoozDocument> yoozDocument = Lists.newArrayList();
		if (object instanceof DocumentContext) {
			yoozDocument = EDITypeConverter.documentContextToYoozDocument((DocumentContext) object);
		}
		if (object instanceof SupplyChainSourceDocument) {
//			yoozDocument = EDITypeConverter.supplyChainSourceDocumentToYoozDocument((SupplyChainSourceDocument) object);
		}
		if (!yoozDocument.isEmpty()) {
			StringWriter writer = new StringWriter();
			StatefulBeanToCsv<YoozDocument> beanToCsv = new StatefulBeanToCsvBuilder<YoozDocument>(writer)
					.withSeparator('\t').build();
			for (YoozDocument line : yoozDocument) {
				beanToCsv.write(line);
			}
			exchange.getIn().setBody(writer.toString());
		} else {
			throw new AbortProcessingException("Empty document. Failed to convert");
		}
	}

}
