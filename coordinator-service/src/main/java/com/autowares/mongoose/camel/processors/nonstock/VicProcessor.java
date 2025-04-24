package com.autowares.mongoose.camel.processors.nonstock;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.PartAvailability;
import com.autowares.ipov3.IPOV3Request;
import com.autowares.ipov3.RequestClient;
import com.autowares.ipov3.model.IPOExchangeDocumentBuilder;
import com.autowares.ipov3.model.IPOExchangetransactionBuilder;
import com.autowares.ipov3.model.proxy.AcknowledgePurchaseOrder;
import com.autowares.ipov3.model.proxy.AddQuote;
import com.autowares.ipov3.model.proxy.ChangeToEnum;
import com.autowares.ipov3.model.proxy.FreightTermType;
import com.autowares.ipov3.model.proxy.IPOExchangeTransaction;
import com.autowares.ipov3.model.proxy.LineType;
import com.autowares.ipov3.model.proxy.ProxyModel;
import com.autowares.ipov3.model.proxy.PurchaseOrder;
import com.autowares.ipov3.model.proxy.RequestForQuote;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.partyconfiguration.model.ConfiguredAccount;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.AccountImpl;
import com.autowares.servicescommon.model.BusinessLocationImpl;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.servicescommon.util.SequenceGenerator;

@Component
public class VicProcessor implements Processor {

	RequestClient client = new RequestClient();
	SequenceGenerator sequenceGenerator = new SequenceGenerator();

	private static Logger log = LoggerFactory.getLogger(VicProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		FulfillmentLocationContext orderFillContext = exchange.getIn().getBody(FulfillmentLocationContext.class);

		CoordinatorContext nonStockContext = orderFillContext.getNonStockContext();
		SupplyChain supplyChain = nonStockContext.getTransactionContext().getSupplyChain();
		CoordinatorContext mainContext = orderFillContext.getOrder();
		exchange.getIn().setHeaders(exchange.getIn().getHeaders());

		IPOExchangeDocumentBuilder<?, ?> documentExchangeBuilder = null;
		IPOExchangetransactionBuilder<?> transactionBuilder = null;

		if (mainContext instanceof CoordinatorOrderContext) {
			documentExchangeBuilder = IPOExchangeDocumentBuilder.newProcessPurchaseOrder();
			transactionBuilder = documentExchangeBuilder.newProcessPurchaseBuilder();
			transactionBuilder.withPurchaseOrder();
			String documentId = mainContext.getDocumentId();
			if (documentId == null) {
				documentId = String.valueOf(sequenceGenerator.nextId());
				mainContext.setDocumentId(documentId);
			}
			transactionBuilder.withCustomerPurchaseOrder(documentId);
		} else {
			documentExchangeBuilder = IPOExchangeDocumentBuilder.newRequestForQuote();
			transactionBuilder = documentExchangeBuilder.newRequestForQuoteBuilder();
			transactionBuilder.withRequestForQuote();
		}

		transactionBuilder.withCustomerDocumentId(mainContext.getDocumentId());

		Optional<ConfiguredAccount> account = supplyChain.getConfiguration().getAccounts().stream()
				.filter(a -> SystemType.VIC.equals(a.getSystemType()))
				.filter(a -> PartyType.Buying.equals(a.getPartyType())).findAny();
		if (account.isPresent()) {
			documentExchangeBuilder.withDocumentGuid(UUID.randomUUID().toString()).withDocumentTimestamp()
					.withSenderEdi(account.get().getAccountNumber());
		} else {
			// TODO: Add notes to context.
		}

		buildIPOTransaction(orderFillContext, transactionBuilder);

		try {

			if (mainContext instanceof CoordinatorOrderContext) {
				order(orderFillContext, documentExchangeBuilder, nonStockContext, transactionBuilder, supplyChain);
			} else {
				quote(orderFillContext, documentExchangeBuilder, nonStockContext, transactionBuilder, supplyChain);
			}

		} catch (Exception e) {
			log.error("Error calling IPO.");
			log.error(e.getMessage());
			e.printStackTrace();

			mainContext.setTransactionStage(TransactionStage.Open);
			mainContext.setTransactionStatus(TransactionStatus.Error);

			nonStockContext.updateProcessingLog(e.getMessage());
		}

		exchange.getIn().setHeaders(exchange.getIn().getHeaders());
	}

	private void order(FulfillmentLocationContext context, IPOExchangeDocumentBuilder<?, ?> documentExchangeBuilder,
			CoordinatorContext nonStockContext, IPOExchangetransactionBuilder<?> ipoExchangetransactionBuilder,
			SupplyChain supplyChain) throws JAXBException {

		Map<Integer, LineItemContext> correlationMap = new HashMap<>();
		Integer lineNumber = 1;
		for (LineItemContext lineItemContext : nonStockContext.getLineItems()) {
			correlationMap.put(lineNumber, lineItemContext);
			ipoExchangetransactionBuilder.withPart(lineItemContext, lineNumber);
			lineNumber++;
		}

//		for (FulfillmentContext fulfillmentContext : context.getFulfillmentDetails()) {
//			correlationMap.put(lineNumber, fulfillmentContext.getLineItem());
//			ipoExchangetransactionBuilder.withPart(fulfillmentContext.getLineItem(), lineNumber);
//			lineNumber++;
//		}

//		IPOExchangeTransaction processPurchaseOrder = ipoExchangetransactionBuilder.build();
//		PrettyPrint.print(processPurchaseOrder);
		ipoExchangetransactionBuilder.withSupplierQuoteDocumentReference(nonStockContext.getSupplierDocumentId()); // TODO:
																													// Validate
		documentExchangeBuilder
				.withPurchaseOrderBuilder((IPOExchangetransactionBuilder<PurchaseOrder>) ipoExchangetransactionBuilder);
		org.autocare.ipo3.processpurchaseorder.ProcessPurchaseOrder message = (org.autocare.ipo3.processpurchaseorder.ProcessPurchaseOrder) documentExchangeBuilder
				.buildNativeObject();

		PrettyPrint.print(message);

		IPOV3Request<org.autocare.ipo3.processpurchaseorder.ProcessPurchaseOrder, ?> soapRequest = new IPOV3Request()
				.withMessageLogging().withRequest(message).withUrl(supplyChain.getProcuringSystem().getUrl()).withLogin(
						supplyChain.getProcuringSystem().getUserName(), supplyChain.getProcuringSystem().getPassword());

		org.autocare.ipo3.acknowledgepurchaseorder.AcknowledgePurchaseOrder response = client
				.processPurchaseOrder(soapRequest);

		AcknowledgePurchaseOrder acknowledgePurchaseOrder = ProxyModel.newInstance(AcknowledgePurchaseOrder.class,
				response);

		for (IPOExchangeTransaction exchangeTransaction : acknowledgePurchaseOrder.getDataArea().getPurchaseOrder()) {
			populateContext(context, correlationMap, nonStockContext, exchangeTransaction);
		}
	}

	private void quote(FulfillmentLocationContext fulfillmentLocationContext,
			IPOExchangeDocumentBuilder<?, ?> documentExchangeBuilder, CoordinatorContext nonStockContext,
			IPOExchangetransactionBuilder<?> ipoExchangetransactionBuilder, SupplyChain supplyChain)
			throws JAXBException {

		Map<Integer, LineItemContext> correlationMap = new HashMap<>();
		Integer lineNumber = 1;
		for (LineItemContext lineItemContext : nonStockContext.getLineItems()) {
			correlationMap.put(lineNumber, lineItemContext);
			ipoExchangetransactionBuilder.withPart(lineItemContext, lineNumber);
			lineNumber++;
		}

		documentExchangeBuilder.withRequestForQuoteBuilder(
				(IPOExchangetransactionBuilder<RequestForQuote>) ipoExchangetransactionBuilder);
		org.autocare.ipo3.addrequestquote.AddRequestForQuote message = (org.autocare.ipo3.addrequestquote.AddRequestForQuote) documentExchangeBuilder
				.buildNativeObject();

		PrettyPrint.print(message);
		org.autocare.ipo3.addquote.AddQuote addQuoteResponse = null;
		IPOV3Request<org.autocare.ipo3.addrequestquote.AddRequestForQuote, ?> soapRequest = new IPOV3Request()
				.withMessageLogging().withRequest(message).withUrl(supplyChain.getProcuringSystem().getUrl()).withLogin(
						supplyChain.getProcuringSystem().getUserName(), supplyChain.getProcuringSystem().getPassword());
		addQuoteResponse = client.addQuote(soapRequest);
		AddQuote addQuote = ProxyModel.newInstance(AddQuote.class, addQuoteResponse);

		for (IPOExchangeTransaction quote : addQuote.getDataArea().getQuote()) {
			populateContext(fulfillmentLocationContext, correlationMap, nonStockContext, quote);
		}
	}

	private void populateContext(FulfillmentLocationContext fulfillmentLocation,
			Map<Integer, LineItemContext> correlationMap, CoordinatorContext nonStockContext,
			IPOExchangeTransaction document) {

		CoordinatorContext mainContext = fulfillmentLocation.getOrder();
		try {
			if (document.getHeader().getDocumentIds() != null
					&& document.getHeader().getDocumentIds().getSupplierQuoteDocumentId() != null) {
				nonStockContext.setSupplierDocumentId(
						document.getHeader().getDocumentIds().getSupplierQuoteDocumentId().getSupplierDocumentId());
			}

			if (document.getHeader().getFreightTerms() != null
					&& document.getHeader().getFreightTerms().getFreightTerm() != null) {
				for (FreightTermType freightTerm : document.getHeader().getFreightTerms().getFreightTerm()) {
					Freight freight = new Freight();
					freight.setCarrier(freightTerm.getCommonCarrier().getValue());
					freight.setCarrierCode(freightTerm.getShippingMethod().getFreightTermCode());
					freight.setDescription(freightTerm.getShippingMethod().getValue());
					freight.setCost(freightTerm.getShippingCharge());
//					nonStockContext.getFreightOptions().getAvailableFreight().add(freight);
					// TODO Figure out freight. Do we ever get this back?
				}
			}

			if (document.getLines() != null) {
				for (LineType line : document.getLines().getLine()) {
					Optional<LineItemContext> optionalItem = Optional
							.of(correlationMap.get(line.getLineNumber().intValue()));
					if (optionalItem.isPresent()) {
						LineItemContext item = optionalItem.get();
						Optional<Availability> optionalAvailability = item.getAvailability().stream().filter(
								i -> i.getFulfillmentLocation().getLocation().equals(fulfillmentLocation.getLocation()))
								.findAny();

						Availability availability = new Availability(item, fulfillmentLocation);

						if (optionalAvailability.isPresent()) {
							availability = optionalAvailability.get();

						} else {
							optionalAvailability = fulfillmentLocation.getLineItemAvailability().stream()
									.filter(i -> i.getLineItem().equals(item)).findAny();
							if (optionalAvailability.isPresent()) {
								availability = optionalAvailability.get();
							}

						}
						if (mainContext instanceof CoordinatorOrderContext) {
							if (line.getOrderQuantity() != null && line.getOrderQuantity().getValue() != null) {
								availability.setFillQuantity(line.getOrderQuantity().getValue().intValue());
							}
						}
						Integer quantityOnHand = item.getOriginalQuantity();
						availability.setQuantityOnHand(quantityOnHand);
						PartAvailability partAvailability = new PartAvailability();
						partAvailability.setQuantityOnHand(quantityOnHand);
						availability.setPartAvailability(partAvailability);
						if (line.getExtendedPrice() != null) {
							BigDecimal itemCost = line.getExtendedPrice().divide(BigDecimal.valueOf(quantityOnHand));
							availability.setProcurementCost(itemCost);
							item.setPrice(itemCost);
						}

						if (line.getItemStatusChanges() != null
								&& line.getItemStatusChanges().getItemStatusChange() != null
								&& !line.getItemStatusChanges().getItemStatusChange().get(0).getTo()
										.equals(ChangeToEnum.OK)) {
							String status = line.getItemStatusChanges().getItemStatusChange().stream()
									.map(s -> s.getTo() + " " + s.getDescription()).collect(Collectors.joining(", "));
							throw new ContinueProcessingException(status);
						}
						if (line.getShipFromParty() != null) {
							fulfillmentLocation.setDescription(line.getShipFromParty().getName());
						}

						if (line.getFreightTerms() != null) {
							for (FreightTermType freightTerm : line.getFreightTerms().getFreightTerm()) {
								Freight freight = new Freight();
								freight.setCarrier(freightTerm.getCommonCarrier().getValue());
								freight.setCarrierCode(freightTerm.getShippingMethod().getFreightTermCode());
								freight.setDescription(freightTerm.getShippingMethod().getValue());
								freight.setCost(freightTerm.getShippingCharge());
								item.getFreightOptions().getAvailableFreight().add(freight);
							}
						}
					}
				}

			}
			nonStockContext.updateProcessingLog("Success");
			mainContext.setTransactionStage(TransactionStage.Open);

			if (mainContext instanceof CoordinatorOrderContext) {
				mainContext.setTransactionStatus(TransactionStatus.Accepted);
			} else {
				mainContext.setTransactionStatus(TransactionStatus.Pending);
			}

			if (!(mainContext instanceof CoordinatorOrderContext)) {
				fulfillmentLocation.setQuoted(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			if (e.getMessage() == null) {
				nonStockContext.updateProcessingLog("Failed to get Remote Rfq Document Id");
			} else {
				nonStockContext.updateProcessingLog(e.getMessage());
				;
			}
			mainContext.setTransactionStage(TransactionStage.Open);
			mainContext.setTransactionStatus(TransactionStatus.Error);
		}
	}

	private void buildIPOTransaction(FulfillmentLocationContext orderFillContext,
			IPOExchangetransactionBuilder<?> transactionBldr) {

		SupplyChain supplyChain = null;
		try {
			supplyChain = orderFillContext.getNonStockContext().getTransactionContext().getSupplyChain();
		} catch (Exception e) {
			throw new AbortProcessingException("No Supply Chain or Procurement Group on NonStockContext");
		}
		Optional<ConfiguredAccount> billTo = supplyChain.getBillTo(SystemType.VIC);
		Optional<ConfiguredAccount> buyingAccount = supplyChain.getBuyingAccount(SystemType.VIC);
		Optional<ConfiguredAccount> shipTo = supplyChain.getShipTo(SystemType.VIC);
		CoordinatorContext orderContext = orderFillContext.getOrder();
		Optional<ConfiguredAccount> sellingAccount = supplyChain.getSellingAccount(SystemType.VIC);

		String documentId = orderContext.getDocumentId();

		BusinessLocationImpl member = new BusinessLocationImpl();
		member.setName("Autowares");
		member.setAddress("PO Box ShipItHere");
		member.setCity("Grand Rapids");

		if (billTo.isPresent()) {
			AccountImpl account = new AccountImpl();
			account.setMember(member);
			account.setAccountNumber(billTo.get().getAccountNumber());
			transactionBldr.withBillTo(account);
		} else {
			AccountImpl account = new AccountImpl();
			account.setMember(member);
			account.setAccountNumber(buyingAccount.get().getAccountNumber());
			transactionBldr.withBillTo(account);
		}

		// Ship to is required from testing.
		if (shipTo.isPresent()) {
			AccountImpl account = new AccountImpl();
			account.setMember(member);
			account.setAccountNumber(shipTo.get().getAccountNumber());
			transactionBldr.withShipTo(account);
		} else {
			AccountImpl account = new AccountImpl();
			account.setMember(member);
			account.setAccountNumber(billTo.get().getAccountNumber());
			transactionBldr.withShipTo(account);
		}

		if (sellingAccount.isPresent()) {
			transactionBldr.withHostParty(sellingAccount.get().getAccountNumber());
		} else {
			throw new AbortProcessingException("No VIC selling account configured for moa business id: "
					+ supplyChain.getProcuringPartnership().getSupplier().getMember().getId());
		}

		transactionBldr.withDocumentTimeNow().withDefaultFreightTerms().withDocumentId(documentId);

	}

}