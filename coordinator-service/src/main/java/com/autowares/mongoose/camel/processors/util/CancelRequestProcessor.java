package com.autowares.mongoose.camel.processors.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderContextTypeConverter;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.service.MoaOperationalStateManager;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.mongoose.service.ViperInventoryStateManager;
import com.autowares.servicescommon.model.LineItem;
import com.autowares.servicescommon.model.LineItemModification;
import com.autowares.servicescommon.model.OperationalStage;
import com.autowares.servicescommon.model.ShortageCode;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.Operation;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainLineType;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;

@Component
public class CancelRequestProcessor implements Processor {

	@Autowired
	SupplyChainService transactionalStateManager;

	@Autowired
	MoaOperationalStateManager operationalStateManager;

	@Autowired
	ViperInventoryStateManager inventoryStateManager;

	@Override
	public void process(Exchange exchange) throws Exception {
		DocumentContext document = exchange.getIn().getBody(DocumentContext.class);
		if (document.getFulfillmentLocationContext() != null) {

			SupplyChainSourceDocumentImpl shortage = (SupplyChainSourceDocumentImpl) document.getContext()
					.getTransactionContext().getTransactionalShortage();

			for (LineItem lineItem : document.getLineItems()) {
				for (Availability availability : document.getFulfillmentLocationContext().getLineItemAvailability()) {
					if (availability.getLineItem().getLineNumber() == lineItem.getLineNumber()) {

						LineItemContext contextLineItem = availability.getLineItem();
						OperationalContext operationalContext = availability.getOperationalContext();

						if (operationalContext != null) {
							OperationalStage stage = operationalContext.getCurrentOperationalStage();
							if (OperationalStage.ordered.equals(stage)) {
								for (OperationalItem operationalItem : operationalContext.getItems()) {
									Operation operation = new Operation(operationalItem, operationalContext,
											OperationalStage.canceled);
									operationalItem.setCurrentOperation(operation);
								}
								operationalContext = operationalStateManager.persist(operationalContext);
								inventoryStateManager.unallocate(contextLineItem);
							} else {
								throw new AbortProcessingException(
										"Unable to cancel: the order is already in stage: " + stage);
							}

						}

						// We have no operational context, just cancel the order
						document.getFulfillmentLocationContext().getLineItems().remove(contextLineItem);
						document.getSourceDocument().getLineItems()
								.removeIf(i -> i.getLineNumber().equals(contextLineItem.getLineNumber()));

						if (shortage != null) {
							// We are going to update the existing shortage document with the line items being cancelled
							GenericLine shortageLine = GenericLine.builder().fromPartLineItem(contextLineItem)
									.withLineType(SupplyChainLineType.Generic)
									.withQuantity(contextLineItem.getQuantity())
									.withPurchasedQuantity(lineItem.getQuantity())
									.withShortageCode(ShortageCode.Cancelled).build();
							shortage.getLineItems().add(shortageLine);

						} else {
							// We are going to generate a new shortage document off of the order context
							contextLineItem.setShortageCode(ShortageCode.Cancelled);
							availability.setFillQuantity(0);
						}
						document.getContext()
								.updateProcessingLog("Cancelled Line Item " + contextLineItem.getLineNumber() + " "
										+ contextLineItem.getVendorCode() + " " + contextLineItem.getPartNumber());

						if (lineItem instanceof LineItemModification) {
							LineItemModification lineItemModification = (LineItemModification) lineItem;
							lineItemModification.setSuccess(true);
						}

					}

				}
			}
			// Persist Packslip
			transactionalStateManager.persist(document.getSourceDocument());
			// Persist Shortage
			if (shortage == null) {
				shortage = (SupplyChainSourceDocumentImpl) OrderContextTypeConverter
						.coordinatorContextToShortage((CoordinatorOrderContext) document.getContext());
			}
			transactionalStateManager.persist(shortage);
			System.out.println();
		}
	}

}
