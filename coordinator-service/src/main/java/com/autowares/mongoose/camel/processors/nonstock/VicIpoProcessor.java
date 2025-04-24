package com.autowares.mongoose.camel.processors.nonstock;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.autocare.ipo3.addquote.AdditionalChargeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.partservice.model.Origcode;
import com.autowares.ipov3.common.BusinessAddress;
import com.autowares.ipov3.model.proxy.FreightTerm;
import com.autowares.ipov3.quote.AddQuoteResponseImpl;
import com.autowares.ipov3.quote.RequestForQuoteImpl;
import com.autowares.ipov3.quote.RequestLine;
import com.autowares.ipov3.quote.ResponseLine;
import com.autowares.mongoose.command.VicIpoServiceClient;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.BusinessContext;
import com.autowares.mongoose.model.ChargesBuilder;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorContextImpl;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.model.vic.BusinessAddressBuilder;
import com.autowares.partyconfiguration.model.ConfiguredAccount;
import com.autowares.partyconfiguration.model.SupplyChain;
import com.autowares.servicescommon.model.BusinessLocationAccountImpl;
import com.autowares.servicescommon.model.BusinessLocationImpl;
import com.autowares.servicescommon.model.ChargeType;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.servicescommon.util.SequenceGenerator;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.xmlgateway.model.InquiryRequest;

@Component
public class VicIpoProcessor implements Processor {

	private static Logger log = LoggerFactory.getLogger(VicIpoProcessor.class);
	private static SequenceGenerator sequenceGenerator = new SequenceGenerator();

	private static VicIpoServiceClient vicIpoServiceClient = new VicIpoServiceClient();

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fulfillmentLocationContext = exchange.getIn()
				.getBody(FulfillmentLocationContext.class);

		if (fulfillmentLocationContext.getNonStockContext() == null) {
			throw new AbortProcessingException("No non stock context found.");
		}
		CoordinatorContext nonStockContext = fulfillmentLocationContext.getNonStockContext();
		SupplyChain nonStockSupplyChain = nonStockContext.getTransactionContext().getSupplyChain();
		CoordinatorContext coordinatorContext = nonStockContext.getProcurementGroupContext().getCustomerContext()
				.getQuoteContext();

		BusinessContext supplier = nonStockContext.getSupplier();
		if (supplier == null) {
			throw new AbortProcessingException("Supplier not found.");
		}

		coordinatorContext.setTransactionStage(TransactionStage.Open);
		coordinatorContext.setTransactionStatus(TransactionStatus.Pending);
		coordinatorContext.setOrderType(PurchaseOrderType.SpecialOrder);
		
		if (coordinatorContext.getInquiryOptions() != null) {
			coordinatorContext.getInquiryOptions().setUseFlatRateShipping(true);
		}
		if (coordinatorContext.getTransactionContext() != null && coordinatorContext.getTransactionContext().getRequest() != null
				&& coordinatorContext.getTransactionContext().getRequest().getInquiryOptions() != null) {
			coordinatorContext.getTransactionContext().getRequest().getInquiryOptions().setUseFlatRateShipping(true);
		}

		FreightOptions freightOptions = new FreightOptions();

		String senderEdiId = "6162432125";
		String supplierEdiId = null;
		String billToAccountNumber = null;
		String shipToAccountNumber = null;

		for (ConfiguredAccount account : nonStockSupplyChain.getConfiguration().getAccounts()) {
			if (PartyType.ShipTo.equals(account.getPartyType())) {
				shipToAccountNumber = account.getAccountNumber();
			}
			if (PartyType.BillTo.equals(account.getPartyType())) {
				billToAccountNumber = account.getAccountNumber();
			}
			if (PartyType.Buying.equals(account.getPartyType())) {
				senderEdiId = account.getAccountNumber();
			}
			if (PartyType.Selling.equals(account.getPartyType())) {
				supplierEdiId = account.getAccountNumber();
			}
		}

		RequestForQuoteImpl requestForQuoteImpl = new RequestForQuoteImpl();
		requestForQuoteImpl.setDocumentId(coordinatorContext.getDocumentId());
		requestForQuoteImpl.setCustomerQuoteDocumentId(String.valueOf(sequenceGenerator.nextId()));
		requestForQuoteImpl.setSenderEdiId(senderEdiId);

		// requestForQuoteImpl.setSupplierEdiId("9723168100");
		requestForQuoteImpl.setSupplierEdiId(supplierEdiId);

		// The Supplier from the coordinator context should be AUTOWARES
		BusinessDetail billToBusinessDetail = coordinatorContext.getSupplier().getBusinessDetail();

		BusinessAddress billTo = BusinessAddressBuilder.builder().withBusinessAddress(billToBusinessDetail)
				.withAccountNumber(billToAccountNumber).build();

		requestForQuoteImpl.setBillTo(billTo);

		BusinessDetail shiptoBusinessDetail = coordinatorContext.getBusinessContext().getBusinessDetail();

		BusinessAddress shipTo = BusinessAddressBuilder.builder().withBusinessAddress(shiptoBusinessDetail)
				.withAccountNumber(shipToAccountNumber).build();

		requestForQuoteImpl.setShipTo(shipTo);

		requestForQuoteImpl.setLineItems(new ArrayList<>());

		for (LineItemContextImpl li : coordinatorContext.getLineItems()) {
			RequestLine requestLine = new RequestLine();
			requestLine.setBrandAAIAId(li.getBrandAaiaId());
			requestLine.setLineCode(li.getBrandAaiaId());
			requestLine.setLineNumber(new BigInteger("" + li.getLineNumber()));
			String partNumber = li.getPartNumber();
			if (li.getPart() != null && li.getPart().getOrigcodes() != null) {
				Optional<Origcode> foundPart = li.getPart().getOrigcodes().stream()
						.filter(p -> "AAIA".equals(p.getSourceSystemCode())).findAny();
				if (foundPart.isPresent()) {
					if (foundPart.get().getSourceSystemPartNumber() != null) {
						partNumber = foundPart.get().getSourceSystemPartNumber();
					}
				}
			}
			requestLine.setPartNumber(partNumber);
			requestLine.setQuantity(li.getQuantity());
			requestForQuoteImpl.getLineItems().add(requestLine);
		}

		PrettyPrint.print(requestForQuoteImpl);

		try {
			AddQuoteResponseImpl response = vicIpoServiceClient.quote(requestForQuoteImpl);

			PrettyPrint.print(response);

			if (!"ACCEPTED".equals(response.getStatusCode())) {
				throw new AbortProcessingException(
						response.getStatusCode() + " " + response.getReasonCode() + " " + response.getStatusMessage());
			}

			if (response.getCustomerQuoteDocumentId() != null) {
				CoordinatorContext quoteContext = coordinatorContext.getProcurementGroupContext().getSupplierContext()
						.getQuoteContext();
				quoteContext.setCustomerDocumentId(response.getCustomerQuoteDocumentId());
			}

			if (response.getSupplierQuoteDocumentId() != null) {
				CoordinatorContext quoteContext = coordinatorContext.getProcurementGroupContext().getSupplierContext()
						.getQuoteContext();
				quoteContext.setSupplierDocumentId(response.getSupplierQuoteDocumentId());
			}

			if (response.getFreightTerms() != null) {
				for (FreightTerm freight : response.getFreightTerms()) {
					Freight availableFreight = new Freight();
					availableFreight.setCarrier(freight.getCarrier());
					availableFreight.setCarrierCode(freight.getCarrierCode());
					availableFreight.setShippingMethod(freight.getShippingMethod());
					availableFreight.setShippingCode(freight.getShippingCode());
					if (freight.getShippingCharge() != null) {
						availableFreight.setCost(BigDecimal.valueOf(freight.getShippingCharge()));
					}
					availableFreight.setDescription(freight.getShippingMethod());
					freightOptions.getAvailableFreight().add(availableFreight);
				}
			}

			if (response.getShipFrom() != null) {
				addShipFrom(fulfillmentLocationContext, response.getShipFrom());
			}

			ArrayList<ChargesImpl> charges = new ArrayList<>();

			List<ResponseLine> lineItems = response.getLineItems();
			for (ResponseLine li : lineItems) {
				PrettyPrint.print(li.getLineCode() + " " + li.getPartNumber());
				Optional<LineItemContextImpl> matchedLine = coordinatorContext.getLineItems().stream()
						.filter(i -> BigInteger.valueOf(i.getLineNumber().longValue()).equals(li.getLineNumber()))
						.findAny();
				if (li.getQuoteStatus() != null) {

					if (matchedLine.isPresent()) {
						matchedLine.get().getNotes().add(
								SupplyChainNote.builder().withMessage("Quote status : " + li.getQuoteStatus()).build());
						matchedLine.get().getNotes()
								.add(SupplyChainNote.builder().withMessage(li.getStatusMessage()).build());
					}

					if (!"OK".equalsIgnoreCase(li.getQuoteStatus())) {
						throw new AbortProcessingException(
								"Quote status : " + li.getQuoteStatus() + " " + li.getStatusMessage());
					}
				}
				if (li.getFreightTerms() != null) {
					for (FreightTerm freight : li.getFreightTerms()) {
						Freight availableFreight = new Freight();
						availableFreight.setCarrier(freight.getCarrier());
						availableFreight.setCarrierCode(freight.getCarrierCode());
						availableFreight.setShippingMethod(freight.getShippingMethod());
						availableFreight.setShippingCode(freight.getShippingCode());
						if (freight.getShippingCharge() != null) {
							availableFreight.setCost(BigDecimal.valueOf(freight.getShippingCharge()));
						}
						availableFreight.setDescription(freight.getShippingMethod());
						freightOptions.getAvailableFreight().add(availableFreight);

						PrettyPrint.print(" - " + freight.getCarrier() + "  " + freight.getCarrierCode());
						PrettyPrint.print(" - " + freight.getShippingMethod() + "  " + freight.getShippingCode());
						PrettyPrint.print(" - " + freight.getShippingCharge());
						PrettyPrint.print(" ");
					}
				}

				if (li.getPromisedShipDate() != null) {
					for (Freight freight : freightOptions.getAvailableFreight()) {
						freight.setEstimatedShipDateTime(
								li.getPromisedShipDate().toGregorianCalendar().toZonedDateTime());
					}
				}

				if (li.getNote() != null) {
					coordinatorContext.getNotes().add(SupplyChainNote.builder().withMessage(li.getNote()).build());
					nonStockContext.getNotes().add(SupplyChainNote.builder().withMessage(li.getNote()).build());
				}

				if (li.getShipFrom() != null) {
					addShipFrom(fulfillmentLocationContext, li.getShipFrom());
				}

				if (li.getAdditionalCharges() != null) {
					if (li.getAdditionalCharges().getAdditionalCharge() != null) {
						for (AdditionalChargeType charge : li.getAdditionalCharges().getAdditionalCharge()) {
							ChargesImpl c = ChargesBuilder.builder().withCharge(charge.getTotal())
									.withChargeType(ChargeType.Charge).withDescription(charge.getDescription()).build();
							charges.add(c);
						}
					}
					if (li.getAdditionalCharges().getDiscount() != null) {
						for (AdditionalChargeType charge : li.getAdditionalCharges().getDiscount()) {
							ChargesImpl c = ChargesBuilder.builder().withCharge(charge.getTotal())
									.withChargeType(ChargeType.Discount).withDescription(charge.getDescription())
									.build();
							charges.add(c);
						}
					}
					if (li.getAdditionalCharges().getRebate() != null) {
						for (AdditionalChargeType charge : li.getAdditionalCharges().getRebate()) {
							ChargesImpl c = ChargesBuilder.builder().withCharge(charge.getTotal())
									.withChargeType(ChargeType.Rebate).withDescription(charge.getDescription()).build();
							charges.add(c);
						}
					}
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

//			fulfillmentLocationContext.setChargesAndDiscounts(charges);
//
//			if (coordinatorContext instanceof CoordinatorContextImpl) {
//				((CoordinatorContextImpl) coordinatorContext).setChargesAndDiscounts(charges);
//				if (charges != null) {
//					for (ChargesImpl charge : charges) {
//						String message = "Charge Applied: " + charge.getChargeType().name() + " "
//								+ charge.getDescription() + " $" + charge.getCharge();
//						SupplyChainNote note = SupplyChainNote.builder().withMessage(message).build();
//						coordinatorContext.getNotes().add(note);
//					}
//				}
//			}

//			if (nonStockContext instanceof CoordinatorContextImpl) {
//				((CoordinatorContextImpl) nonStockContext).setChargesAndDiscounts(charges);
//			}

		} catch (Exception e) {
			coordinatorContext.setNotes(new ArrayList<>());
			coordinatorContext.getNotes().add(SupplyChainNote.builder().withMessage("SPS Integration ERROR").build());
			coordinatorContext.getNotes().add(SupplyChainNote.builder().withMessage(e.getMessage()).build());
			coordinatorContext.getNotes().add(SupplyChainNote.builder().withMessage(e.getLocalizedMessage()).build());
			e.printStackTrace();

			coordinatorContext.setTransactionStatus(TransactionStatus.Error);

			if (e instanceof AbortProcessingException) {
				throw e;
			}

		}

		fulfillmentLocationContext.getNonStockContext().setFreightOptions(freightOptions);

		for (LineItemContextImpl lI : coordinatorContext.getLineItems()) {
			Availability availability = new Availability(lI, fulfillmentLocationContext);
			availability.setFillQuantity(lI.getQuantity());

		}

	}

	private void addShipFrom(FulfillmentLocationContext fulfillmentLocationContext, BusinessAddress shipFromAddress) {
		TransactionalContext transactionContext = fulfillmentLocationContext.getOrder().getTransactionContext();
		if (transactionContext.getRequest() == null) {
			transactionContext.setRequest(new InquiryRequest());
		}

		InquiryRequest inquiryRequest = transactionContext.getRequest();

		BusinessLocationImpl member = new BusinessLocationImpl();
		BusinessLocationAccountImpl shipFrom = new BusinessLocationAccountImpl();

		shipFrom.setPartyType(PartyType.ShipFrom);
		shipFrom.setSystemType(SystemType.VIC);
		shipFrom.setMember(member);
		shipFrom.setAccountNumber(shipFromAddress.getAccountNumber());
		member.setAddress(shipFromAddress.getAddressLine1());
		member.setAddress2(shipFromAddress.getAddressLine2());
		member.setCity(shipFromAddress.getCity());
		member.setStateProv(shipFromAddress.getStateCode());
		member.setCountryCode(shipFromAddress.getCountryCode());
		member.setPostalCode(shipFromAddress.getPostalCode());
		member.setName(shipFromAddress.getBusinessName());

		inquiryRequest.setShipFrom(shipFrom);
	}

}