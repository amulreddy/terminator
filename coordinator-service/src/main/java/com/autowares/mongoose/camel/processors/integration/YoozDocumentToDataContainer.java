package com.autowares.mongoose.camel.processors.integration;

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

@Component
public class YoozDocumentToDataContainer implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Object object = exchange.getIn().getBody();
		List<YoozDocument> yoozDocuments = Lists.newArrayList();
		if (object instanceof DocumentContext) {
			yoozDocuments = EDITypeConverter.documentContextToYoozDocument((DocumentContext) object);
		}
		if (object instanceof SupplyChainSourceDocument) {
//			yoozDocument = EDITypeConverter.supplyChainSourceDocumentToYoozDocument((SupplyChainSourceDocument) object);
		}
		if (!yoozDocuments.isEmpty()) {
			try {
				DataContainer dataContainer = YoozDocumentMapper.mapToDataContainer(yoozDocuments);
				PrettyPrint.print(dataContainer);
				exchange.getIn().setBody(dataContainer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			throw new AbortProcessingException("Empty document. Failed to convert");
		}

	}

}
