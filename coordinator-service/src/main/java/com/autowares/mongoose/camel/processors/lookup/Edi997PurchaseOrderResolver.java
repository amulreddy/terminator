package com.autowares.mongoose.camel.processors.lookup;

import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.TransactionalStateManager;
import com.autowares.xmlgateway.edi.base.EdiDocument;
import com.autowares.xmlgateway.edi.base.EdiRecord;

@Component
public class Edi997PurchaseOrderResolver implements Processor{
	
	@Autowired
	TransactionalStateManager tsm;

	@Override
	public void process(Exchange exchange) throws Exception {
		
		EdiDocument ediDocument = exchange.getIn().getBody(EdiDocument.class);
		List<EdiRecord> irrList = ediDocument.getRecords("IRR");
		Optional<EdiRecord> optionalIrr = irrList.stream().findAny();
		if (optionalIrr.isPresent()) {
			EdiRecord ediRecord = optionalIrr.get();
			Long supplyChainId = ediRecord.getField("IRR05").getValueAsLong();
			exchange.getIn().setBody(tsm.resolveOrderContext(supplyChainId));
		}
	}

}
