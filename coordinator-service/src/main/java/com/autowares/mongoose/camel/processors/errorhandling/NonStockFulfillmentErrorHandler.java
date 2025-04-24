package com.autowares.mongoose.camel.processors.errorhandling;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.UnimplementedException;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class NonStockFulfillmentErrorHandler implements Processor {

	private static Logger log = LoggerFactory.getLogger(NonStockFulfillmentErrorHandler.class);

	@Autowired
	private SupplyChainService supplyChainService;

	@Override
	public void process(Exchange exchange) throws Exception {

		Object context = exchange.getIn().getBody();
		FulfillmentLocationContext fulfillmentLocationContext = null;
		ProcurementGroupContext procurementGroupContext = null;

		if (context instanceof FulfillmentLocationContext) {
			fulfillmentLocationContext = (FulfillmentLocationContext) context;
			if (fulfillmentLocationContext.getOrder() != null) {
				procurementGroupContext = fulfillmentLocationContext.getOrder().getProcurementGroupContext();
			}
		}

		if (context instanceof DocumentContext) {
			DocumentContext documentContext = (DocumentContext) context;
			if (documentContext.getFulfillmentLocationContext() != null) {
				fulfillmentLocationContext = documentContext.getFulfillmentLocationContext();
				if (documentContext.getProcurementGroupContexts() != null
						&& !documentContext.getProcurementGroupContexts().isEmpty()) {
					procurementGroupContext = documentContext.getProcurementGroupContexts().stream().findFirst().get();
				}
			}
		}

		if (procurementGroupContext != null) {
			if (PurchaseOrderType.SpecialOrder
					.equals(procurementGroupContext.getCustomerContext().getOrderContext().getOrderType())) {
				log.info("Update special order error status.");
				SupplyChainSourceDocument customerOrder = procurementGroupContext.getCustomerContext().getOrder();
				customerOrder.setTransactionStatus(TransactionStatus.Error);
				customerOrder.setTransactionStage(TransactionStage.Done);

				Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
				if (caused != null) {
					if (!(caused instanceof UnimplementedException)) {
						String message = ExceptionUtils.getStackTrace(caused);
						customerOrder.getNotes().add(SupplyChainNote.builder().withMessage(message).build());
					}
				}
				supplyChainService.persist(customerOrder);
			}

		}
	}
}
