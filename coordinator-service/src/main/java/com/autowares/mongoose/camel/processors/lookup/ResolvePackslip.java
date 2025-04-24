package com.autowares.mongoose.camel.processors.lookup;

import java.util.Collection;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.partyconfiguration.client.PartyConfigurationClient;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class ResolvePackslip implements Processor {

	@Autowired
	private SupplyChainService transactionalStateManager;

	PartyConfigurationClient partyClient = new PartyConfigurationClient();

	@Override
	public void process(Exchange exchange) throws Exception {

		Object object = exchange.getIn().getBody();
		DocumentContext document = new DocumentContext();
		DocumentContext originalDocumentContext = null;

		if (object instanceof DocumentContext) {
			originalDocumentContext = (DocumentContext) object;
//			SupplyChainSourceDocument packslip = originalDocumentContext.getTransactionalContext().getPackslips()
//					.get(0);
			document.setSourceDocument(originalDocumentContext.getSourceDocument());
			if (document.getSourceDocument() instanceof SupplyChainSourceDocument) {
				SupplyChainSourceDocument supplyChainDocument = (SupplyChainSourceDocument) document
						.getSourceDocument();
				supplyChainDocument.getProcurementGroups().addAll(
						originalDocumentContext.getTransactionalContext().getOrderContext().getProcurementGroups());
				supplyChainDocument.setTransactionContext(originalDocumentContext.getTransactionalContext());
			}

			document.setLineItems(originalDocumentContext.getLineItems());
			document.setDocumentId(originalDocumentContext.getDocumentId());
			document.setTransactionalContext(originalDocumentContext.getTransactionalContext());
			document.setFulfillmentLocationContext(originalDocumentContext.getFulfillmentLocationContext());
			document.setAction(originalDocumentContext.getAction());
			document.setContext(originalDocumentContext.getContext());
		}

		document = transactionalStateManager.resolveDocumentContext(document);
		if (document != null && document.getProcurementGroupContexts() != null
				&& document.getProcurementGroupContexts().size() == 1) {
			Long supplier = 4656L;
			Long customerBusinessEntityId = document.getProcurementGroupContexts().get(0).getCustomerContext()
					.getOrderContext().getBusinessContext().getBusinessDetail().getBusEntId();
			Collection<SupplyChain> supplyChains = partyClient.findSupplyChains(Set.of(customerBusinessEntityId),
					Set.of(supplier), null);
			for (SupplyChain supplyChain : supplyChains) {
				Configuration configuration = supplyChain.getConfiguration();
				if (configuration != null) {
					if (configuration.getSettings() != null) {
						exchange.getProperties().putAll(configuration.getSettings());
					}
				}
			}
		}
		exchange.getIn().setBody(document);
	}

}