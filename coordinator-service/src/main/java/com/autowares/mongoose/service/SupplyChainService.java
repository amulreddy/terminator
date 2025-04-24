package com.autowares.mongoose.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderContextTypeConverter;
import com.autowares.mongoose.camel.components.OrderFillContextTypeConverter;
import com.autowares.mongoose.camel.components.QuoteContextTypeConverter;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.model.TransactionalStateManager;
import com.autowares.servicescommon.model.LineItem;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.supplychain.commands.SourceDocumentClient;
import com.autowares.supplychain.commands.SupplyChainLineItemClient;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainLine;
import com.autowares.supplychain.model.SupplyChainShipment;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;
import com.autowares.supplychain.request.SourceDocumentRequest;
import com.google.common.collect.Lists;

@Component
public class SupplyChainService implements TransactionalStateManager {

	@Value("${spring.profiles.active:local}")
	private String activeProfile;
	private static SourceDocumentClient prodSourceDocumentClient = new SourceDocumentClient();
	private static SourceDocumentClient testSourceDocumentClient = new SourceDocumentClient();
	private static SupplyChainLineItemClient lineItemClient = new SupplyChainLineItemClient();

	public SupplyChainService() {
//		prodSourceDocumentClient.withServiceDomain("consul");
//		testSourceDocumentClient.withServiceDomain("testconsul");
	}

	@Override
	public SupplyChainSourceDocumentImpl retrieve(String documentId) {
		Optional<SupplyChainSourceDocumentImpl> existing = getClient().findSourceDocumentByDocumentId(documentId);
		if (existing.isPresent()) {
			return existing.get();
		}
		return null;
	}

	@Override
	public CoordinatorContext resolveOrderContext(String documentId) {
		SupplyChainSourceDocumentImpl sourceDocument = retrieve(documentId);
		return resolveOrderContext(sourceDocument);
	}
	
	@Override
	public SupplyChainSourceDocumentImpl retrieve(Long supplyChainId) {
		Optional<SupplyChainSourceDocumentImpl> existing = getClient().findSourceDocumentBySupplyChainId(supplyChainId);
		if (existing.isPresent()) {
			return existing.get();
		}
		return null;
	}
	
	@Override
	public CoordinatorContext resolveOrderContext(Long supplyChainId) {
		SupplyChainSourceDocumentImpl sourceDocument = retrieve(supplyChainId);
		return resolveOrderContext(sourceDocument);
	}

	public CoordinatorContext resolveOrderContext(SupplyChainSourceDocumentImpl sourceDocument) {
		if (sourceDocument != null) {
			SourceDocumentType documentType = sourceDocument.getSourceDocumentType();

			switch (documentType) {
			case Invoice:
				break;
			case PackSlip:
				FulfillmentLocationContext fulFillmentLocation = OrderFillContextTypeConverter
						.orderPackslipToFulfillmentLocationContext((SupplyChainShipment) sourceDocument);
				CoordinatorContext context = resolveOrderContext(
						sourceDocument.getTransactionContext().getTransactionReferenceId());
				context.setReferenceDocument(sourceDocument);
				fulFillmentLocation.setOrder(context);
				context.getFulfillmentSequence().clear();
				context.getFulfillmentSequence().add(fulFillmentLocation);
				List<LineItemContextImpl> packSlipSpecificLineItems = new ArrayList<>();
				for (LineItemContextImpl orderLineContext : context.getLineItems()) {
					for (Availability availability : fulFillmentLocation.getLineItemAvailability()) {
						LineItemContext lineItemContext = availability.getLineItem();
						if (orderLineContext.getLineNumber().equals(lineItemContext.getLineNumber())) {
							availability.setLineItem(orderLineContext);
							orderLineContext.setAvailability(Lists.newArrayList(availability));
							packSlipSpecificLineItems.add(orderLineContext);
						}
					}
				}
				context.setLineItems(packSlipSpecificLineItems);
				return context;
			case ShippingEstimate:
			
			case PurchaseOrder:
			case Quote:
				CoordinatorContext orderContext = OrderContextTypeConverter
						.supplyChainDocumentToOrderContext(sourceDocument);
				TransactionalContext tc = orderContext.getTransactionContext();
				tc.setOrderContext(orderContext);
				for (SupplyChainSourceDocument sd : sourceDocument.getTransactionContext().getSourceDocuments()) {

					if (!sourceDocument.getDocumentId().equals(sd.getDocumentId())) {

						if (SourceDocumentType.Quote.equals(sd.getSourceDocumentType())) {
							sd = retrieve(sd.getDocumentId());
							tc.setQuote(sd);
							tc.setQuoteContext(QuoteContextTypeConverter.sourceDocumentToCoordinatorContext(sd));
						}

						if (SourceDocumentType.PackSlip.equals(sd.getSourceDocumentType())) {
							sd = retrieve(sd.getDocumentId());
							tc.getPackslips().add(sd);
						}

						if (SourceDocumentType.Shortage.equals(sd.getSourceDocumentType())) {
							sd = retrieve(sd.getDocumentId());
							tc.setTransactionalShortage(sd);
						}

						if (SourceDocumentType.Invoice.equals(sd.getSourceDocumentType())) {
							// Just maintain a reference for now
							// sd = retrieve(sd.getDocumentId());
							tc.getInvoices().add(sd);
						}
						if (SourceDocumentType.ShippingEstimate.equals(sd.getSourceDocumentType())) {
							sd = retrieve(sd.getDocumentId());
							tc.getShippingEstimates().add(sd);
						}
					}

				}

				for (ProcurementGroup pg : sourceDocument.getProcurementGroups()) {
					ProcurementGroupContext pgContext = new ProcurementGroupContext(pg);
					// Look for a document to re-inflate the transactionalContext
					Optional<SupplyChainSourceDocument> x = pg.getSourceDocuments().stream()
							.filter(i -> (SourceDocumentType.Quote.equals(i.getSourceDocumentType())
									|| SourceDocumentType.PurchaseOrder.equals(i.getSourceDocumentType())))
							.findFirst();
					TransactionalContext transactionalContext = null;
					if (x.isPresent()) {
						transactionalContext = resolveTransactionalContext(x.get().getDocumentId());
					}

					if (TransactionScope.Purchasing.equals(transactionalContext.getTransactionScope())) {
						pgContext.setSupplierContext(transactionalContext);
					}
					if (TransactionScope.Supplying.equals(transactionalContext.getTransactionScope())) {
						pgContext.setCustomerContext(transactionalContext);
					}

				}

				return orderContext;
			case Return:
				break;
			case Shortage:
				break;
			case StockJob:
				break;
			default:
				if (sourceDocument.getTransactionContext() != null
						&& sourceDocument.getTransactionContext().getTransactionReferenceId() != null) {
					return resolveOrderContext(sourceDocument.getTransactionContext().getTransactionReferenceId());
				}
			}
		}
		return null;
	}

	@Override
	public TransactionalContext resolveTransactionalContext(String documentId) {
		SupplyChainSourceDocumentImpl sourceDocument = retrieve(documentId);
		if (sourceDocument != null) {
			TransactionContext existing = sourceDocument.getTransactionContext();
			if (existing != null) {
				return resolveTransactionalContext(existing);
			}
		}
		return null;
	}

	public TransactionalContext resolveTransactionalContext(TransactionContext existing) {

		if (existing.getSourceDocuments() != null && existing.getSourceDocuments().isEmpty()) {
			SourceDocumentRequest request = null;
			if (existing.getTransactionContextId() != null) {
				request = SourceDocumentRequest.builder().withTransactionContextId(existing.getTransactionContextId())
						.build();
			} else if (existing.getTransactionReferenceId() != null) {
				request = SourceDocumentRequest.builder().withDocumentId(existing.getTransactionReferenceId()).build();
			}
			Page<SupplyChainSourceDocument> documents = getClient().findSourceDocument(request);
//			Could change to getProdClient() but need to remember to change it back or we will process production documents in test
			existing.getSourceDocuments().addAll(documents.stream().collect(Collectors.toList()));
			List<TransactionContext> transactionContexts = documents.stream().map(i -> i.getTransactionContext())
					.distinct().collect(Collectors.toList());
			if (transactionContexts.size() == 1) {
				existing = transactionContexts.get(0);
			}
		}

		TransactionalContext tc = new TransactionalContext(existing);

		for (SupplyChainSourceDocument sd : existing.getSourceDocuments()) {

			if (SourceDocumentType.PurchaseOrder.equals(sd.getSourceDocumentType())) {
				sd = retrieve(sd.getDocumentId());
				tc.setOrder(sd);
				tc.setOrderContext(OrderContextTypeConverter.supplyChainDocumentToOrderContext(sd));
				tc.getOrderContext().setTransactionContext(tc);
				tc.getOrderContext().setResolved(true);
			}

			if (SourceDocumentType.Quote.equals(sd.getSourceDocumentType())) {
				sd = retrieve(sd.getDocumentId());
				tc.setQuote(sd);
				tc.setQuoteContext(QuoteContextTypeConverter.sourceDocumentToCoordinatorContext(sd));
				tc.getQuoteContext().setTransactionContext(tc);
				tc.getQuoteContext().setResolved(true);
			}

			if (SourceDocumentType.PackSlip.equals(sd.getSourceDocumentType())) {
				String documentId = sd.getDocumentId();
				sd = retrieve(documentId);
				FulfillmentLocationContext flc = OrderFillContextTypeConverter
						.orderPackslipToFulfillmentLocationContext((SupplyChainShipment) sd);
				if (flc.getSystemType() == null) {
					flc.setSystemType(sd.getSystemType());
				}
				if (tc.getOrderContext() != null) {
					CoordinatorContext orderContext = tc.getOrderContext();
					orderContext.getFulfillmentSequence().removeIf(i -> documentId.equals(i.getFulfillmentLocationId()));
					orderContext.getFulfillmentSequence().add(flc);
					flc.setOrder(orderContext);
					List<LineItemContextImpl> packSlipSpecificLineItems = new ArrayList<>();
					for (LineItemContextImpl orderLineContext : orderContext.getLineItems()) {
						for (Availability availability : flc.getLineItemAvailability()) {
							LineItemContext lineItemContext = availability.getLineItem();
							if (orderLineContext.getLineNumber().equals(lineItemContext.getLineNumber())) {
								availability.setLineItem(orderLineContext);
								orderLineContext.setAvailability(Lists.newArrayList(availability));
								packSlipSpecificLineItems.add(orderLineContext);
							}
						}
					}
				}
				tc.getPackslips().add(sd);
			}
			if (SourceDocumentType.Shortage.equals(sd.getSourceDocumentType())) {
				sd = retrieve(sd.getDocumentId());
				tc.setTransactionalShortage(sd);
			}

			if (SourceDocumentType.Invoice.equals(sd.getSourceDocumentType())) {
				// Just maintain a reference for now
				sd = retrieve(sd.getDocumentId());
				tc.getInvoices().add(sd);
			}
			
			if (SourceDocumentType.ShippingEstimate.equals(sd.getSourceDocumentType())) {
				sd = retrieve(sd.getDocumentId());
				tc.getShippingEstimates().add(sd);
			}

		}
		tc.setResolved(true);
		return tc;
	}

	@Override
	public SupplyChainSourceDocument persist(CoordinatorContext context) {
		return persist(QuoteContextTypeConverter.coordinatorContextToSupplyChainSourceDocument(context));
	}

	@Override
	public SupplyChainSourceDocument persist(SourceDocument sourceDocument) {
		return getClient().saveSourceDocument((SupplyChainSourceDocument) sourceDocument);
	}

	@Override
	public SourceDocumentClient getClient() {
		if ("prod".equals(activeProfile)) {
			return getProdClient();
		}
		return getTestClient();
	}

	public SourceDocumentClient getProdClient() {
		return prodSourceDocumentClient;
	}

	public SourceDocumentClient getTestClient() {
		return testSourceDocumentClient;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends LineItem> T persist(T lineItem) {
		return (T) lineItemClient.saveLine((SupplyChainLine) lineItem);
	}

	@Override
	public ProcurementGroupContext resolveProcurementGroupContext(ProcurementGroup pg) {
		ProcurementGroupContext pgContext = new ProcurementGroupContext(pg);
		// Look for transactionalContexts to re-inflate

		List<TransactionContext> transactionContexts = new ArrayList<>();

		/*
		 * Make the call as pg.getSourceDocuments() may only be a subset of documents
		 * tied together via this procurementGroup
		 */
		if (pg.getProcurementGroupId() != null) {
			SourceDocumentRequest request = SourceDocumentRequest.builder()
					.withProcurementGroupId(pg.getProcurementGroupId()).build();
			Page<SupplyChainSourceDocument> documents = getClient().findSourceDocument(request);
			transactionContexts = documents.stream().filter(i -> i.getTransactionContext() != null)
					.map(i -> i.getTransactionContext()).distinct().collect(Collectors.toList());
			pgContext.getSourceDocuments().addAll(documents.stream().collect(Collectors.toList()));
		} else if (pg.getSourceDocuments() != null && !pg.getSourceDocuments().isEmpty()) {
			transactionContexts = pg.getSourceDocuments().stream().filter(i -> i.getTransactionContext() != null)
					.map(i -> i.getTransactionContext()).distinct().collect(Collectors.toList());
		}

		for (TransactionContext tc : transactionContexts) {
			TransactionalContext transactionalContext = resolveTransactionalContext(tc);
			if (transactionalContext != null) {
				if (TransactionScope.Purchasing.equals(transactionalContext.getTransactionScope())) {
					pgContext.setSupplierContext(transactionalContext);
				}
				if (TransactionScope.Supplying.equals(transactionalContext.getTransactionScope())) {
					pgContext.setCustomerContext(transactionalContext);
				}
			}
		}
		pgContext.setResolved(true);
		return pgContext;
	}

	public DocumentContext resolveDocumentContext(DocumentContext documentContext) {
		if (documentContext.getSourceDocument() == null) {
			documentContext.setSourceDocument(retrieve(documentContext.getDocumentId()));
		}
		if (documentContext.getSourceDocument() != null) {
			if (documentContext.getSourceDocument() instanceof SupplyChainSourceDocument) {
				SupplyChainSourceDocument supplyChainDocument = (SupplyChainSourceDocument) documentContext
						.getSourceDocument();
				if (supplyChainDocument.getProcurementGroups() != null
						&& !supplyChainDocument.getProcurementGroups().isEmpty()) {
					for (ProcurementGroup pg : supplyChainDocument.getProcurementGroups()) {
						ProcurementGroupContext pgContext = resolveProcurementGroupContext(pg);
						documentContext.getProcurementGroupContexts().add(pgContext);
						if (pgContext.getCustomerContext().getTransactionContextId()
								.equals(supplyChainDocument.getTransactionContext().getTransactionContextId())) {
							applyTransactionalContext(pgContext.getCustomerContext(), supplyChainDocument,
									documentContext);
							documentContext.getContext().setProcurementGroupContext(pgContext);
						}
						if (pgContext.getSupplierContext() != null && pgContext.getSupplierContext().getTransactionContextId()
								.equals(supplyChainDocument.getTransactionContext().getTransactionContextId())) {
							applyTransactionalContext(pgContext.getSupplierContext(), supplyChainDocument,
									documentContext);
						}
					}
				} else if (supplyChainDocument.getTransactionContext() != null) {
					TransactionalContext transactionalContext = resolveTransactionalContext(
							supplyChainDocument.getTransactionContext());
					applyTransactionalContext(transactionalContext, supplyChainDocument, documentContext);
				}

			}
		}
		// If we cannot resolve via source document, resolve anything by
		// transactionalContext
		if (documentContext.getTransactionalContext() != null
				&& !documentContext.getTransactionalContext().isResolved()) {
			applyTransactionalContext(resolveTransactionalContext(documentContext.getTransactionalContext()), null,
					documentContext);
		}
		documentContext.setResolved(true);
		return documentContext;
	}

	private void applyTransactionalContext(TransactionalContext transactionalContext,
			SupplyChainSourceDocument supplyChainDocument, DocumentContext documentContext) {

		documentContext.setTransactionalContext(transactionalContext);
		transactionalContext.setDocumentContext(documentContext);
		if (documentContext.getContext() == null) {
			if (transactionalContext.getOrderContext() != null) {
				documentContext.setContext(transactionalContext.getOrderContext());
			} else {
				documentContext.setContext(transactionalContext.getQuoteContext());
			}
		}
		if (documentContext.getContext() != null) {
			documentContext.getContext().setTransactionContext(transactionalContext);
			if (documentContext.getFulfillmentLocationContext() == null) {
				Optional<FulfillmentLocationContext> optionalFulfillmentLocation = documentContext.getContext()
						.getFulfillmentSequence().stream()
						.filter(i -> i.getFulfillmentLocationId().equals(documentContext.getDocumentId())).findAny();
				if (optionalFulfillmentLocation.isPresent()) {
					FulfillmentLocationContext fulfillmentLocation = optionalFulfillmentLocation.get();
					documentContext.setFulfillmentLocationContext(fulfillmentLocation);
					fulfillmentLocation.setDocumentContext(documentContext);
					fulfillmentLocation.setReferenceDocument(supplyChainDocument);
				}
			}
		}
	}
}
