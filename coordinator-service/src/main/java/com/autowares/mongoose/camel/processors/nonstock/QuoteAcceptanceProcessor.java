package com.autowares.mongoose.camel.processors.nonstock;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class QuoteAcceptanceProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(QuoteAcceptanceProcessor.class);

	@Autowired
	SupplyChainService transactionalStateManager;

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorOrderContext orderContext = exchange.getIn().getBody(CoordinatorOrderContext.class);

		log.info("Updating quote status and stage.");

		if (orderContext.getProcurementGroupContext() != null) {
			ProcurementGroupContext procurementGroup = orderContext.getProcurementGroupContext();
			TransactionalContext customerTransaction = procurementGroup.getCustomerContext();
			TransactionalContext supplierTransaction = procurementGroup.getSupplierContext();

			if (customerTransaction != null && customerTransaction.getQuoteContext() != null) {
				CoordinatorContext customerQuoteContext = customerTransaction.getQuoteContext();
				updateContext(customerQuoteContext);
			}

			if (supplierTransaction != null && supplierTransaction.getQuoteContext() != null) {
				CoordinatorContext supplierQuoteContext = supplierTransaction.getQuoteContext();

				updateContext(supplierQuoteContext);

				List<SupplyChainSourceDocument> shippingEstimates = supplierQuoteContext.getTransactionContext()
						.getShippingEstimates();
				
				if (shippingEstimates != null) {
					for (SupplyChainSourceDocument estimate : shippingEstimates) {
						estimate.setTransactionStage(TransactionStage.Complete);
						estimate.setTransactionStatus(TransactionStatus.Accepted);
						transactionalStateManager.persist(estimate);
					}
				}
			}
		}
	}

	public void updateContext(CoordinatorContext coordinatorContext) {
		coordinatorContext.setTransactionStage(TransactionStage.Complete);
		coordinatorContext.setTransactionStatus(TransactionStatus.Accepted);
		transactionalStateManager.persist(coordinatorContext);
	}

}