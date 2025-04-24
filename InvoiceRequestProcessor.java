package com.autowares.mongoose.camel.processors.invoicing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.OrderFillContextTypeConverter;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.service.SequenceGeneratorService;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.mongoose.utils.MatchingUtils;
import com.autowares.servicescommon.model.ChargeType;
import com.autowares.servicescommon.model.Charges;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainParty;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl.Builder;
import com.autowares.xmlgateway.edi.EdiSourceDocument;

@Component
public class InvoiceRequestProcessor implements Processor {

	@Autowired
	SupplyChainService transactionalStateManager;

	@Autowired
	SequenceGeneratorService sequenceGeneratorService;

	private static Logger log = LoggerFactory.getLogger(InvoiceRequestProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		// TODO Move routing logic back into the route
		DocumentContext document = exchange.getIn().getBody(DocumentContext.class);
		if ("Process EDI ASN".equals(document.getAction())) {
			buildDocument(null, document.getTransactionalContext(), document, false, SourceDocumentType.PackSlip, true);
		}
		if ("Invoice Packslip".equals(document.getAction()) || "Process EDI Invoice".equals(document.getAction())) {
			if (document.getProcurementGroupContexts() != null && document.getProcurementGroupContexts().size() == 1) {
				ProcurementGroupContext procurementGroupContext = document.getProcurementGroupContexts().get(0);
				TransactionalContext supplierTransactionContext = procurementGroupContext.getSupplierContext();
				buildDocument(procurementGroupContext, supplierTransactionContext, document, false,
						SourceDocumentType.Invoice, false);
				TransactionalContext customerTransactionContext = procurementGroupContext.getCustomerContext();
				buildDocument(procurementGroupContext, customerTransactionContext, document, true,
						SourceDocumentType.Invoice, true);
			} else {
				buildDocument(null, document.getTransactionalContext(), document, false, SourceDocumentType.Invoice,
						false);
			}
		}
	}

	private void buildDocument(ProcurementGroupContext procurementGroupContext,
			TransactionalContext transactionalContext, DocumentContext document, boolean invoiceFromOrder,
			SourceDocumentType documentType, boolean generateDocumentId) {
		List<SupplyChainSourceDocument> invoices = transactionalContext.getInvoices();

		CoordinatorContext orderContext = transactionalContext.getOrderContext();
		SupplyChainParty to = SupplyChainParty.builder().fromAccount(orderContext.getBusinessContext())
				.withPartyType(PartyType.Buying).build();
		SupplyChainParty from = SupplyChainParty.builder().fromParty(orderContext.getSellingParty())
				.withPartyType(PartyType.Selling).build();
		Builder<?> documentBuilder = SupplyChainSourceDocumentImpl.builder().withSourceDocumentType(documentType)
				.withDocumentId(document.getDocumentId()).withFrom(from).withTo(to)
				.withTransactionStatus(TransactionStatus.Accepted).withTransactionContext(transactionalContext)
				.withProcurementGroup(procurementGroupContext).withChargesAndDiscounts(
						(Collection<ChargesImpl>) document.getSourceDocument().getChargesAndDiscounts());

		if ("Process EDI ASN".equals(document.getAction())) {
			documentBuilder.withTransactionStage(TransactionStage.Complete);
		}
		if (document.getDocumentId() == null || "Invoice Packslip".equals(document.getAction())) {
			documentBuilder.withDocumentId(sequenceGeneratorService.getNextSequence());
		}
		if (generateDocumentId) {
			documentBuilder.withDocumentId(sequenceGeneratorService.getNextSequence());
		}
		if (document.getSourceDocument() != null) {
			SourceDocument sourceDocument = document.getSourceDocument();
			if (sourceDocument.getSystemType() != null) {
				documentBuilder.withSystemType(sourceDocument.getSystemType());
			}
			boolean useFlatRateShipping = false;
			
			if (sourceDocument instanceof SupplyChainSourceDocument) {
				SupplyChainSourceDocument supplyChainDocument = (SupplyChainSourceDocument) sourceDocument;
				if (supplyChainDocument.getFreightOptions() != null) {
					documentBuilder.withFreightOptions(supplyChainDocument.getFreightOptions());
					documentBuilder.setFreightCharge(supplyChainDocument.getFreightCharge());
				}
				
				/** If we we are working with an invoice that has flat rate shipping,  we need to pull in the shipping from the order */
				if (SourceDocumentType.Invoice.equals(documentType)) {
					if(transactionalContext.getRequest() != null && transactionalContext.getRequest().getInquiryOptions() != null &&  transactionalContext.getRequest().getInquiryOptions().getUseFlatRateShipping() != null && transactionalContext.getRequest().getInquiryOptions().getUseFlatRateShipping()) {
						useFlatRateShipping=true;
						SupplyChainSourceDocument orderDocument = transactionalContext.getOrder();
						if (orderDocument.getFreightOptions() != null) {
							Freight selectedFreight = OrderFillContextTypeConverter.copyFreight(orderDocument.getFreightOptions().getSelectedFreight());
							FreightOptions freightOptions = new FreightOptions();
							freightOptions.setSelectedFreight(selectedFreight);
							documentBuilder.withFreightOptions(freightOptions);
							documentBuilder.setFreightCharge(orderDocument.getFreightCharge());
						}
					}
				}
			}

			BigDecimal totalChargesAndDiscounts = BigDecimal.ZERO;

			if (document.getSourceDocument().getChargesAndDiscounts() != null && !useFlatRateShipping) {

				Collection<? extends Charges> charges = document.getSourceDocument()
						.getChargesAndDiscounts();

				for (Charges charge : charges) {
					if (ChargeType.Charge.equals(charge.getChargeType())) {
						totalChargesAndDiscounts = totalChargesAndDiscounts.add((BigDecimal) charge.getCharge());
					} else {
						totalChargesAndDiscounts = totalChargesAndDiscounts.subtract((BigDecimal) charge.getCharge());
					}
				}
				documentBuilder.withTotalDiscountsAndCharges(totalChargesAndDiscounts);
			}
		}

		Integer invoiceTotalSum = 0;
		for (LineItemContextImpl documentLineItem : document.getLineItems()) {
			Integer lineItemQuantity = documentLineItem.getQuantity();
			if (SourceDocumentType.Invoice.equals(documentType)) {
				Integer invoiceSum = 0;
				for (SupplyChainSourceDocument invoice : invoices) {
					Optional<GenericLine> invoiceLine = MatchingUtils.matchByLineNumber(documentLineItem,
							invoice.getLineItems());
					if (invoiceLine.isPresent()) {
						invoiceSum += invoiceLine.get().getQuantity();
						invoiceTotalSum += invoiceSum;
					} 
				}
				if (invoiceSum == documentLineItem.getQuantity()) {
					continue;
				}
				if (invoiceSum < documentLineItem.getQuantity()) {
					lineItemQuantity = documentLineItem.getQuantity() - invoiceSum;
				}
			}
			com.autowares.supplychain.model.GenericLine.Builder<?> lineBuilder = GenericLine.builder();
			if (invoiceFromOrder) {
				Optional<LineItemContextImpl> optionalOrderLine = MatchingUtils.matchByProduct(documentLineItem,
						orderContext.getLineItems());
				if (optionalOrderLine.isPresent()) {
					lineBuilder.fromPartLineItem(optionalOrderLine.get());
				} else {
					documentBuilder.withTransactionStatus(TransactionStatus.Error);
					documentBuilder.withNote(SupplyChainNote.builder().withMessage("Could not match.").build());
				}
			} else {
				lineBuilder.fromPartLineItem(documentLineItem);
			}
			lineBuilder.withQuantity(lineItemQuantity);
			if (SourceDocumentType.PackSlip.equals(documentType)) {
				Optional<LineItemContextImpl> optionalOrderLine = MatchingUtils.matchByProduct(documentLineItem,
						orderContext.getLineItems());
				if (optionalOrderLine.isPresent()) {
					lineBuilder.withPurchasedQuantity(optionalOrderLine.get().getQuantity());
				}
			}
			documentBuilder.withLineItem(lineBuilder.build());
		}
		if (SourceDocumentType.Invoice.equals(documentType) && document.getLineItems().stream()
				.mapToInt(LineItemContextImpl::getQuantity).sum() > invoiceTotalSum) {
			SupplyChainSourceDocument invoice = documentBuilder.build();
			transactionalStateManager.persist(invoice);
			log.info("Saving invoice " + invoice.getDocumentId());
			transactionalContext.getInvoices().add(invoice);
		}
		if (SourceDocumentType.PackSlip.equals(documentType)) {
			if (document.getSourceDocument() instanceof EdiSourceDocument) {
				EdiSourceDocument ediSourceDocument = (EdiSourceDocument) document.getSourceDocument();
//				ediSourceDocument.getFreightOptions().getDefaultFreight().setDescription("Change it here");
				documentBuilder.withFreightOptions(ediSourceDocument.getFreightOptions());
			}
			List<ProcurementGroup> pr = document.getTransactionalContext().getOrder().getProcurementGroups();
			if (pr != null && !pr.isEmpty()) {
				documentBuilder.withProcurementGroups(pr);
			}
			SupplyChainSourceDocument packslip = documentBuilder.build();
			transactionalStateManager.persist(packslip);
			log.info("Saving packslip " + packslip.getDocumentId());
			transactionalContext.getPackslips().add(packslip);
		}
	}

}
