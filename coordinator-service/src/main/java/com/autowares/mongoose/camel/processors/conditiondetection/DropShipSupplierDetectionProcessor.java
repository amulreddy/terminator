package com.autowares.mongoose.camel.processors.conditiondetection;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.servicescommon.model.PurchaseOrderType;

@Component
public class DropShipSupplierDetectionProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(DropShipSupplierDetectionProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		DocumentContext document = exchange.getIn().getBody(DocumentContext.class);
		if (document.getProcurementGroupContexts() != null && document.getProcurementGroupContexts().size() == 1) {
			ProcurementGroupContext procurementGroupContext = document.getProcurementGroupContexts().get(0);
			TransactionalContext context = procurementGroupContext.getSupplierContext();
			if (context != null) {
				CoordinatorContext orderContext = context.getOrderContext();
				if (orderContext != null && PurchaseOrderType.SpecialOrder == orderContext.getOrderType()) {
					exchange.setProperty("isDropShipSupplier", true);
					log.info("DropShip supplier detected for xmlOrderId: " + document.getDocumentId());
				}
			}
		}
	}
}
