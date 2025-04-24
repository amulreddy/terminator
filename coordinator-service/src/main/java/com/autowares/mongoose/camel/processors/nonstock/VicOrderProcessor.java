package com.autowares.mongoose.camel.processors.nonstock;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.autocare.ipo3.addquote.AdditionalChargeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.model.Origcode;
import com.autowares.ipov3.common.BusinessAddress;
import com.autowares.ipov3.model.proxy.FreightTerm;
import com.autowares.ipov3.po.PurchaseOrderImpl;
import com.autowares.ipov3.po.PurchaseOrderImpl.Builder;
import com.autowares.ipov3.po.PurchaseOrderLine;
import com.autowares.ipov3.po.PurchaseOrderResponseImpl;
import com.autowares.ipov3.po.PurchaseOrderResponseLineItem;
import com.autowares.mongoose.command.VicIpoServiceClient;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.ChargesBuilder;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.service.SupplyChainService;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.ConfiguredAccount;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.BusinessLocationAccountImpl;
import com.autowares.servicescommon.model.BusinessLocationImpl;
import com.autowares.servicescommon.model.ChargeType;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainParty;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainVendor;

@Component
public class VicOrderProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(VicOrderProcessor.class);

	private static VicIpoServiceClient vicIpoServiceClient = new VicIpoServiceClient();

	@Autowired
	private SupplyChainService supplyChainService;

	@Override
	public void process(Exchange exchange) throws Exception {

		String senderEdiId = "6162432125";
		String supplierEdiId = null;
		BusinessAddress billTo = new BusinessAddress();
		BusinessAddress shipTo = new BusinessAddress();
		BusinessAddress shipFrom = null;

		List<FreightTerm> freightTerms = null;
		String documentId = null;
		String customerQuoteDocumentId = null;
		String supplierQuoteDocumentId = null;
		String customerPoDocumentId = null;

		List<PurchaseOrderLine> lineItems = new ArrayList<PurchaseOrderLine>();
		String note = null;

		DocumentContext documentContext = exchange.getIn().getBody(DocumentContext.class);
		System.out.println("Place Order.");
		TransactionalContext transactionalContext = documentContext.getTransactionalContext();
		SupplyChain supplyChain = transactionalContext.getSupplyChain();
		Configuration configuration = supplyChain.getConfiguration();
		List<ConfiguredAccount> accounts = configuration.getAccounts();
		List<SupplyChainSourceDocument> sourceDocuments = transactionalContext.getSourceDocuments();

		for (SupplyChainSourceDocument document : sourceDocuments) {
			if (SourceDocumentType.Quote.equals(document.getSourceDocumentType())) {
				customerQuoteDocumentId = document.getDocumentId();
				supplierQuoteDocumentId = document.getDocumentId();
			}
			if (SourceDocumentType.PurchaseOrder.equals(document.getSourceDocumentType())) {
				customerPoDocumentId = document.getDocumentId();
			}
			if (SourceDocumentType.ShippingEstimate.equals(document.getSourceDocumentType())) {

			}
			if (SourceDocumentType.PackSlip.equals(document.getSourceDocumentType())) {
				documentId = document.getDocumentId();
			}
		}

		ProcurementGroupContext procurementGroup = documentContext.getProcurementGroupContexts().get(0);
		CoordinatorContext supplierQuote = procurementGroup.getSupplierContext().getQuoteContext();
		supplierQuoteDocumentId = supplierQuote.getSupplierDocumentId();
		customerQuoteDocumentId = supplierQuote.getCustomerDocumentId();

		CoordinatorContext customerOrder = procurementGroup.getCustomerContext().getOrderContext();
		CoordinatorContext supplierOrder = procurementGroup.getSupplierContext().getOrderContext();

		Builder builder = new PurchaseOrderImpl.Builder();
		for (ConfiguredAccount account : accounts) {
			if (PartyType.ShipTo.equals(account.getPartyType())) {
				shipTo.setAccountNumber(account.getAccountNumber());
			}
			if (PartyType.BillTo.equals(account.getPartyType())) {
				billTo.setAccountNumber(account.getAccountNumber());
			}
			if (PartyType.Buying.equals(account.getPartyType())) {
				senderEdiId = account.getAccountNumber();
			}
			if (PartyType.Selling.equals(account.getPartyType())) {
				supplierEdiId = account.getAccountNumber();
			}
		}

		SupplyChainSourceDocument packslip = transactionalContext.getPackslips().get(0);
		if (packslip.getFreightOptions() != null && packslip.getFreightOptions().getSelectedFreight() != null) {
			freightTerms = new ArrayList<FreightTerm>();
			Freight selectedFreight = packslip.getFreightOptions().getSelectedFreight();
			FreightTerm freightTerm = new FreightTerm();

			if (selectedFreight != null) {
				freightTerm.setCarrier(selectedFreight.getCarrier());
				freightTerm.setCarrierCode(selectedFreight.getCarrierCode());
				freightTerm.setShippingMethod(selectedFreight.getShippingMethod());
				if (selectedFreight.getCost() != null) {
					freightTerm.setShippingCharge(selectedFreight.getCost().doubleValue());
				}
				freightTerm.setShippingCode(selectedFreight.getShippingCode());
				freightTerms.add(freightTerm);
			}
		}

		SupplyChainParty to = transactionalContext.getOrder().getTo();
		SupplyChainVendor billToMember = (SupplyChainVendor) to.getMember();

		billTo.setAddressLine1(billToMember.getAddress());
		billTo.setAddressLine2(billToMember.getAddress2());
		billTo.setBusinessName(billToMember.getBusinessName());
		billTo.setCity(billToMember.getCity());
		billTo.setStateCode(billToMember.getStateProv());
		billTo.setPostalCode(billToMember.getPostalCode());
		String countryCode = "US";

		if (billToMember.getCountryCode() != null) {
			countryCode = billToMember.getCountryCode();
		}

		billTo.setCountryCode(countryCode);

		SupplyChainParty to2 = transactionalContext.getShippingEstimates().get(0).getTo();
		SupplyChainVendor shipToMember = (SupplyChainVendor) to2.getMember();

		shipTo.setAddressLine1(shipToMember.getAddress());
		shipTo.setAddressLine2(shipToMember.getAddress2());
		shipTo.setBusinessName(shipToMember.getBusinessName());
		shipTo.setCity(shipToMember.getCity());
		shipTo.setStateCode(shipToMember.getStateProv());
		shipTo.setPostalCode(shipToMember.getPostalCode());
		shipTo.setCountryCode(shipToMember.getCountryCode());
		countryCode = "US";
		if (shipToMember.getCountryCode() != null) {
			countryCode = shipToMember.getCountryCode();
		}
		shipTo.setCountryCode(countryCode);
		
		
		if(transactionalContext != null && transactionalContext.getRequest() != null && transactionalContext.getRequest().getShipFrom() != null) {
			BusinessLocationAccountImpl shipFromAccount = transactionalContext.getRequest().getShipFrom();
			BusinessLocationImpl shipFromMember = shipFromAccount.getMember();
			shipFrom = new BusinessAddress();
			
			shipFrom.setAddressLine1(shipFromMember.getAddress());
			shipFrom.setAddressLine2(shipFromMember.getAddress2());
			shipFrom.setBusinessName(shipFromMember.getName());
			shipFrom.setCity(shipFromMember.getCity());
			shipFrom.setStateCode(shipFromMember.getStateProv());
			shipFrom.setPostalCode(shipFromMember.getPostalCode());
			shipFrom.setCountryCode(shipFromMember.getCountryCode());
			shipFrom.setAccountNumber(shipFromAccount.getAccountNumber());
			
			countryCode = "US";
			
			if (shipFromMember.getCountryCode() != null) {
				countryCode = shipFromMember.getCountryCode();
			}
			
			shipFrom.setCountryCode(countryCode);
		}

		for (LineItemContextImpl lineItem : documentContext.getContext().getLineItems()) {
			PurchaseOrderLine purchaseLine = new PurchaseOrderLine();
			purchaseLine.setBrandAAIAId(lineItem.getPart().getBrandAaiaId());
			purchaseLine.setFreightTerms(freightTerms);
			purchaseLine.setLineCode(lineItem.getPart().getBrandAaiaId());
			purchaseLine.setLineNumber(BigInteger.valueOf(lineItem.getLineNumber().longValue()));
			String partNumber = lineItem.getPartNumber();
			if (lineItem.getPart() != null && lineItem.getPart().getOrigcodes() != null) {
				Optional<Origcode> foundPart = lineItem.getPart().getOrigcodes().stream()
						.filter(p -> "AAIA".equals(p.getSourceSystemCode())).findAny();
				if (foundPart.isPresent()) {
					if (foundPart.get().getSourceSystemPartNumber() != null) {
						partNumber = foundPart.get().getSourceSystemPartNumber();
					}
				}
			}
			purchaseLine.setPartNumber(partNumber);
			purchaseLine.setQuantity(lineItem.getQuantity());
			purchaseLine.setNote(note);
			purchaseLine.setShipFrom(shipFrom);
			lineItems.add(purchaseLine);
		}

		builder.withSenderEdiId(senderEdiId);
		builder.withSupplierEdiId(supplierEdiId);
		builder.withDocumentId(documentId);
		builder.withCustomerQuoteDocumentId(customerQuoteDocumentId);
		builder.withSupplierQuoteDocumentId(supplierQuoteDocumentId);
		builder.withCustomerPoDocumentId(supplierOrder.getDocumentId());
		builder.withBillTo(billTo);
		builder.withShipTo(shipTo);
		builder.withShipFrom(shipFrom);
		builder.withFreightTerms(freightTerms);
		builder.withLineItems(lineItems);
		builder.withNote(note);

		try {
			PurchaseOrderImpl purchaseOrder = builder.build();
			PrettyPrint.print(purchaseOrder);
			PurchaseOrderResponseImpl response = vicIpoServiceClient.purchaseOrder(purchaseOrder);
			PrettyPrint.print(response);

			if (response == null || response.getLineItems() == null) {
				documentContext.getFulfillmentLocationContext().setTransactionStatus(TransactionStatus.Error);
				documentContext.getFulfillmentLocationContext().setTransactionStage(TransactionStage.Done);
//				documentContext.getContext().getTransactionStage().setTran
				throw new AbortProcessingException("VIC Order Failure. No Line items returned.");
			}

			ArrayList<ChargesImpl> charges = new ArrayList<>();

			for (PurchaseOrderResponseLineItem li : response.getLineItems()) {
				if (li.getPromisedShipDate() != null) {
					ZonedDateTime estimatedShipDate = li.getPromisedShipDate().toGregorianCalendar().toZonedDateTime();
					customerOrder.getFreightOptions().getSelectedFreight().setEstimatedShipDateTime(estimatedShipDate);
				}
				if (li.getItemStatus() != null) {
					customerOrder.getNotes().add(SupplyChainNote.builder()
							.withMessage(li.getItemStatus() + " " + li.getItemStatusMessage()).build());
				}

			}

			if (response.getAdditionalCharges() != null) {
				if (response.getAdditionalCharges().getAdditionalCharge() != null) {
					for (AdditionalChargeType charge : response.getAdditionalCharges().getAdditionalCharge()) {
						ChargesImpl c = ChargesBuilder.builder().withCharge(charge.getTotal())
								.withChargeType(ChargeType.Charge).withDescription(charge.getDescription()).build();
						charges.add(c);
					}
				}
				if (response.getAdditionalCharges().getDiscount() != null) {
					for (AdditionalChargeType charge : response.getAdditionalCharges().getDiscount()) {
						ChargesImpl c = ChargesBuilder.builder().withCharge(charge.getTotal())
								.withChargeType(ChargeType.Discount).withDescription(charge.getDescription()).build();
						charges.add(c);
					}
				}
				if (response.getAdditionalCharges().getRebate() != null) {
					for (AdditionalChargeType charge : response.getAdditionalCharges().getRebate()) {
						ChargesImpl c = ChargesBuilder.builder().withCharge(charge.getTotal())
								.withChargeType(ChargeType.Rebate).withDescription(charge.getDescription()).build();
						charges.add(c);
					}
				}
			}

			if (customerOrder instanceof CoordinatorContextImpl) {
				((CoordinatorContextImpl) customerOrder).setChargesAndDiscounts(charges);
			}

			if (supplierOrder instanceof CoordinatorContextImpl) {
				((CoordinatorContextImpl) supplierOrder).setChargesAndDiscounts(charges);
			}

			if (customerOrder instanceof CoordinatorContextImpl) {
				((CoordinatorContextImpl) customerOrder).setChargesAndDiscounts(charges);
				if (charges != null) {
					for (ChargesImpl charge : charges) {
						String message = "Charge Applied: " + charge.getChargeType().name() + " "
								+ charge.getDescription() + " $" + charge.getCharge();
						SupplyChainNote chargeNote = SupplyChainNote.builder().withMessage(message).build();
						customerOrder.getNotes().add(chargeNote);
						supplierOrder.getNotes().add(chargeNote);
					}
				}
			}

			supplyChainService.persist(customerOrder);
		} catch (Exception e) {
			documentContext.getFulfillmentLocationContext().setTransactionStatus(TransactionStatus.Error);
			documentContext.getFulfillmentLocationContext().setTransactionStage(TransactionStage.Done);
			log.error("Failed to send VIC order", e);
			throw new AbortProcessingException(e.getMessage());
		}
	}

}
