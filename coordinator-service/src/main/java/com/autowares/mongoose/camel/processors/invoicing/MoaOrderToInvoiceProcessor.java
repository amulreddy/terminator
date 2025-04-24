package com.autowares.mongoose.camel.processors.invoicing;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.partservice.PartBase;
import com.autowares.invoicesservice.client.InvoicesClient;
import com.autowares.invoicesservice.model.MoaInvoice;
import com.autowares.invoicesservice.model.MoaInvoiceLineItem;
import com.autowares.mongoose.camel.components.OrderFillContextTypeConverter;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.model.TransactionalStateManager;
import com.autowares.mongoose.utils.MatchingUtils;
import com.autowares.servicescommon.model.ChargeType;
import com.autowares.servicescommon.model.Charges;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.PriceLevel;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SourceDocumentImpl;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.util.DateConversion;
import com.autowares.supplychain.model.SupplyChainLine;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;

@Component
public class MoaOrderToInvoiceProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(MoaOrderToInvoiceProcessor.class);

	InvoicesClient invoicesClient = new InvoicesClient();

	@Autowired
	private TransactionalStateManager transactionalStateManager;

	@Override
	public void process(Exchange exchange) throws Exception {

		Freight selectedFreight = null;
		Collection<? extends Charges> chargesAndDiscounts = null;
		int transType=1;
		Boolean isSpecialOrder = false;

		if (exchange.getIn().getBody() instanceof MoaInvoice) {
			MoaInvoice invoice = exchange.getIn().getBody(MoaInvoice.class);
			saveMoaInvoice(invoice);
		}

		if (exchange.getIn().getBody() instanceof FulfillmentLocationContext) {
			MoaInvoice invoice = OrderFillContextTypeConverter
					.fulfillmentLocationContextToMoaInvoice(exchange.getIn().getBody(FulfillmentLocationContext.class));
			saveMoaInvoice(invoice);
		}

		if (exchange.getIn().getBody() instanceof DocumentContext) {
			DocumentContext documentContext = exchange.getIn().getBody(DocumentContext.class);
			List<SupplyChainSourceDocument> customerInvoiceList = new ArrayList<>();
			CoordinatorContext customerOrderContext = null;
			// TODO null checks and type checks.
			SupplyChainSourceDocument sourceDocument = (SupplyChainSourceDocument) documentContext.getSourceDocument();
			if (sourceDocument.getFreightOptions() != null
					&& sourceDocument.getFreightOptions().getSelectedFreight() != null) {
				selectedFreight = sourceDocument.getFreightOptions().getSelectedFreight();
			}
			chargesAndDiscounts = sourceDocument.getChargesAndDiscounts();
			if (documentContext.getProcurementGroupContexts() != null) {
				for (ProcurementGroupContext procurementGroupContext : documentContext.getProcurementGroupContexts()) {
					if (procurementGroupContext.getCustomerContext() != null) {
						for (SupplyChainSourceDocument invoice : procurementGroupContext.getCustomerContext()
								.getInvoices()) {
							customerOrderContext = procurementGroupContext.getCustomerContext().getOrderContext();
							addInvoice(invoice, customerInvoiceList);
						}
					}
				}
			}
			if (documentContext.getTransactionalContext() != null && TransactionScope.Supplying
					.equals(documentContext.getTransactionalContext().getTransactionScope())) {
				for (SupplyChainSourceDocument invoice : documentContext.getTransactionalContext().getInvoices()) {
					addInvoice(invoice, customerInvoiceList);
				}
				customerOrderContext = documentContext.getTransactionalContext().getOrderContext();
			}
			if (customerOrderContext != null) {
				if(PurchaseOrderType.DropShip.equals(customerOrderContext.getOrderType())) {
					transType=5;
				}
				if(PurchaseOrderType.SpecialOrder.equals(customerOrderContext.getOrderType())) {
					isSpecialOrder=true;
				}
			}
			for (SupplyChainSourceDocument invoiceDocument : customerInvoiceList) {

				MoaInvoice invoice = new MoaInvoice();

				if (customerOrderContext.getCustomerNumber() != null) {
					invoice.setCustomerNumber(customerOrderContext.getCustomerNumber().intValue());
				}

				BusinessContext businessContext = customerOrderContext.getBusinessContext();
				BusinessDetail businessDetail = null;

				if (businessContext != null) {
					businessDetail = businessContext.getBusinessDetail();
					invoice.setCwStno(businessDetail.getCwStoreNo());
				}

				invoice.setInvoiceTimeStamp(ZonedDateTime.now());
				TransactionalContext transactionContext = customerOrderContext.getTransactionContext();
				if (transactionContext != null) {
					if (transactionContext.getRequest() != null) {
						invoice.setOrderTimestamp(transactionContext.getRequest().getTimeStamp());
						invoice.setPurchaseorder(transactionContext.getRequest().getCustomerDocumentId());
					}
					String xmlOrderId = null;
					if (isSpecialOrder) {
						SupplyChainSourceDocumentImpl doc = (SupplyChainSourceDocumentImpl) customerOrderContext.getReferenceDocument();
						xmlOrderId = doc.getDocumentId();
					} else {
						xmlOrderId = transactionContext.getTransactionReferenceId();
					}
					invoice.setXmlOrderId(xmlOrderId);
					invoice.setDocumentId(xmlOrderId);
				}
				
				
				
				for (SupplyChainLine invoiceLine : invoiceDocument.getLineItems()) {
					Optional<LineItemContextImpl> lineItemContextImpl = MatchingUtils.matchByLineNumber(invoiceLine,
							customerOrderContext.getLineItems());
					if (lineItemContextImpl.isPresent()) {
						LineItemContextImpl lineItemContext = lineItemContextImpl.get();
						MoaInvoiceLineItem invoiceLineItem = new MoaInvoiceLineItem(lineItemContext);
						// TODO Enumerate the source prog flag. ( Invoice source )
						invoiceLineItem.setSourceprogflag("o");

						invoiceLineItem.setShipqty(invoiceLine.getQuantity());
						invoiceLineItem.setTranstype(transType);

						PartBase part = null;

						if (lineItemContext.getPart() != null) {
							part = lineItemContext.getPart();
							String partDescription = part.getShortDescription();
							if(isSpecialOrder) {
								partDescription="**SO**";
							}
							invoiceLineItem.setPartDescription(partDescription);

							invoiceLineItem.setCorevalue(part.getCorePrice());
							invoiceLineItem.setUnitOfMeasure(part.getMinWarehouseSellUom());
							invoiceLineItem.setPartClass(String.valueOf(part.getMovementClass()));
							invoiceLineItem.setPackageqty(part.getPackageQuantity());
							invoiceLineItem.setPreprice(BigDecimal.ZERO);
							invoiceLineItem.setWeight(part.getWeight());
							invoiceLineItem.setPriceChangeDate(
									DateConversion.convert(lineItemContext.getPart().getPriceChangeDate()));
							invoiceLineItem.setNetinvoiceflag(part.getNetInvoicePart());

							// TODO Fix this.
//						invoiceLineItem.setHazmatptr(part.geth);

							Optional<PriceLevel> wdCost = part.getPriceLevels().stream()
									.filter(p -> "WD Price".equals(p.getPriceFieldCode())).findAny();
							// For Mark : True Cost.
							Optional<PriceLevel> wdTrueCost = part.getPriceLevels().stream()
									.filter(p -> "Invoice WD".equals(p.getPriceFieldCode())).findAny();
							// For Mark : WD Invoice Cost.
							Optional<PriceLevel> wdInvCost = part.getPriceLevels().stream()
									.filter(p -> "Net WD".equals(p.getPriceFieldCode())).findAny();
							// wdInvCore is not a defined price field. mmey_owner.price_fields.
							Optional<PriceLevel> wdInvCore = part.getPriceLevels().stream()
									.filter(p -> "Net WD CORE".equals(p.getPriceFieldCode())).findAny();

							if (wdCost.isPresent()) {
								invoiceLineItem.setWdCost(wdCost.get().getCurrentPrice());
							} else {
								invoiceLineItem.setWdCost(BigDecimal.ZERO);
							}
							if (wdTrueCost.isPresent()) {
								invoiceLineItem.setWdTrueCost(wdTrueCost.get().getCurrentPrice());
							} else {
								invoiceLineItem.setWdTrueCost(BigDecimal.ZERO);
							}
							if (wdInvCost.isPresent()) {
								invoiceLineItem.setWdInvCost(wdInvCost.get().getCurrentPrice());
							} else {
								invoiceLineItem.setWdInvCost(BigDecimal.ZERO);
							}
							if (wdInvCore.isPresent()) {
								invoiceLineItem.setWdInvCore(wdInvCore.get().getCurrentPrice());
							} else {
								invoiceLineItem.setWdInvCore(BigDecimal.ZERO);
							}
						}
						if (lineItemContext.getQuantity() > 0) {
							invoiceLineItem.setOrdertypeflag("D"); // Debit
						} else if (lineItemContext.getQuantity() < 0) {
							invoiceLineItem.setOrdertypeflag("C"); // Credit
						}

						// Drop Ship.
						invoiceLineItem.setTranstype(transType);
						invoiceLineItem.setOverrideSendAsnYn('Y');
						invoice.getLineItems().add(invoiceLineItem);
					}
				}
				if (!TransactionStage.Complete.equals(invoiceDocument.getTransactionStage())) {
					if (selectedFreight != null && selectedFreight.getCost() != null) {
						MoaInvoiceLineItem invoiceLineItem = new MoaInvoiceLineItem();
						invoiceLineItem.setOrdertypeflag("D");
						invoiceLineItem.setOrdqty(1);
						invoiceLineItem.setTranstype(transType);
						invoiceLineItem.setVendorCodeSubCode("TC09");
						invoiceLineItem.setVendor("TC");
						invoiceLineItem.setPartNumber("FREIGHT");
						invoiceLineItem.setBillprice(selectedFreight.getCost());
						// OrderSourceFlag doesn't appear to be set on normal line items.
						invoiceLineItem.setCorevalue(BigDecimal.ZERO);
						invoiceLineItem.setCreatedOnTimestamp(ZonedDateTime.now());
						invoiceLineItem.setCustomerNumber(invoice.getCustomerNumber());
						// Doesn't work in testing saved as 39, which was an already used value.
						invoiceLineItem.setCustrec(0);
						invoiceLineItem.setLineNumber(invoiceLineItem.getCustrec());
						invoiceLineItem.setDc("D");
						// TODO validate Gdate. In our test, was set in the future.
						// Do we need to populate the cost field for FREIGHT? If so, with what value?
						// Should net invoice flag be set?
						invoiceLineItem.setGdate(null);
						invoiceLineItem.setJobberprice(invoiceLineItem.getBillprice());
						invoiceLineItem.setOrderTimestamp(ZonedDateTime.now());
						invoiceLineItem.setJulian(null);
						invoiceLineItem.setOverrideSendAsnYn('Y');
						invoiceLineItem.setPartDescription("*SPEC.CHG*S.O.S*");
						invoiceLineItem.setPrice(invoiceLineItem.getBillprice());
						invoiceLineItem.setPurchaseorder(invoice.getPurchaseorder());
						invoiceLineItem.setShipqty(1);
						invoiceLineItem.setSourceprogflag("o");
						invoiceLineItem.setPackageqty(1);
						invoiceLineItem.setPreprice(BigDecimal.ZERO);

						// ?????
						// No Truck Run pointer in any of the other invoice records.
//						invoiceLineItem.setTruckRunPtr(documentContext.getFulfillmentLocationContext().getViperTruckRunId().intValue());
						invoiceLineItem.setXmlOrderId(documentContext.getSourceDocument().getDocumentId());

						invoiceLineItem.setUnitOfMeasure("EA");

						invoice.getLineItems().add(invoiceLineItem);
					}

					if (chargesAndDiscounts != null) {
						for (Charges charge : chargesAndDiscounts) {
							MoaInvoiceLineItem invoiceLineItem = new MoaInvoiceLineItem();
							invoiceLineItem.setOrdertypeflag("D");
							invoiceLineItem.setOrdqty(1);
							invoiceLineItem.setTranstype(transType);
							invoiceLineItem.setVendorCodeSubCode("TC09");
							invoiceLineItem.setVendor("TC");
							invoiceLineItem.setPartNumber("MISCCHARGE");
							invoiceLineItem.setBillprice((BigDecimal) charge.getCharge());
							invoiceLineItem.setCorevalue(BigDecimal.ZERO);
							invoiceLineItem.setCreatedOnTimestamp(ZonedDateTime.now());
							invoiceLineItem.setCustomerNumber(invoice.getCustomerNumber());
							invoiceLineItem.setCustrec(0);
							invoiceLineItem.setLineNumber(invoiceLineItem.getCustrec());
							if (ChargeType.Charge.equals(charge.getChargeType())) {
								invoiceLineItem.setDc("D");
							} else {
								invoiceLineItem.setDc("C");
							}
							invoiceLineItem.setGdate(null);
							invoiceLineItem.setJobberprice(invoiceLineItem.getBillprice());
							invoiceLineItem.setOrderTimestamp(ZonedDateTime.now());
							invoiceLineItem.setJulian(null);
							invoiceLineItem.setOverrideSendAsnYn('Y');
							invoiceLineItem.setPartDescription("*SPEC.CHG*S.O.S*");
							invoiceLineItem.setPrice(invoiceLineItem.getBillprice());
							invoiceLineItem.setPurchaseorder(invoice.getPurchaseorder());
							invoiceLineItem.setShipqty(1);
							invoiceLineItem.setSourceprogflag("o");
							invoiceLineItem.setPackageqty(1);
							invoiceLineItem.setPreprice(BigDecimal.ZERO);

//						invoiceLineItem.setTruckRunPtr(documentContext.getFulfillmentLocationContext().getViperTruckRunId().intValue());
							invoiceLineItem.setXmlOrderId(documentContext.getSourceDocument().getDocumentId());

							invoiceLineItem.setUnitOfMeasure("EA");

							invoice.getLineItems().add(invoiceLineItem);
						}
					}

					invoiceDocument.setTransactionStage(TransactionStage.Complete);
					if (TransactionStatus.Error != invoiceDocument.getTransactionStatus()) {
						invoiceDocument.setTransactionStatus(TransactionStatus.Accepted);
						saveMoaInvoice(invoice);
					}
					transactionalStateManager.persist(invoiceDocument);
					SupplyChainSourceDocument customerOrder = transactionContext.getOrder();
					customerOrder.setTransactionStage(TransactionStage.Complete);
					transactionalStateManager.persist(customerOrder);
				}
			}
		}
	}

	private MoaInvoice saveMoaInvoice(MoaInvoice invoice) {
		log.info("invoice processing " + invoice.getDocumentId());
		return invoicesClient.createInvoiceRecord(invoice);
	}

	private void addInvoice(SupplyChainSourceDocument sourceDocument, List<SupplyChainSourceDocument> invoices) {
		if (!invoices.stream().map(i -> i.getDocumentId()).anyMatch(i -> i.equals(sourceDocument.getDocumentId()))) {
			invoices.add(sourceDocument);
		}
	}
}
