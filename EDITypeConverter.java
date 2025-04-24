package com.autowares.mongoose.camel.components;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.springframework.stereotype.Component;

import com.autowares.logistix.model.Shipment;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.TransactionalContext;
import com.autowares.mongoose.model.yooz.YoozDocument;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.PurchaseLine;
import com.autowares.supplychain.model.Quantity;
import com.autowares.supplychain.model.QuantityType;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainVendor;
import com.autowares.xmlgateway.edi.EdiLine;
import com.autowares.xmlgateway.edi.EdiSourceDocument;
import com.autowares.xmlgateway.edi.base.EdiDocument;

@Component
@Converter(generateLoader = true)
public class EDITypeConverter implements TypeConverters {

	@Converter
	public static EdiDocument stringToEdiDocument(String file) {
		return new EdiDocument(file, null, null);
	}

	@Converter
	public static List<EdiSourceDocument> ediDocumentToEdiSourceDocument(EdiDocument edi) {
		return EdiSourceDocument.builder().fromEdiDocument(edi).buildSourceDocuments();
	}

	@Converter
	public static DocumentContext ediSourceDocumentToDocumentContext(EdiSourceDocument edi) {
		DocumentContext context = new DocumentContext(edi);
		context.setDocumentId(edi.getDocumentId());
		context.setSourceDocument(edi);
		context.setTransactionalContext(new TransactionalContext(edi.getTransactionContext()));
		for (EdiLine li : edi.getLineItems()) {
			context.getLineItems().add(new LineItemContextImpl(li));
		}
		return context;
	}

//	@Converter
//	public static List<YoozDocument> supplyChainSourceDocumentToYoozDocument(SupplyChainSourceDocument supplyChainDocument) {
//		DocumentContext documentContext = null;
//		supplyChainDocument.getProcurementGroups().get(0).getSourceDocuments().get(0).get
//		return documentContextToYoozDocument();
//	}

	@Converter
	public static List<YoozDocument> documentContextToYoozDocument(DocumentContext documentContext) {
		List<YoozDocument> yoozDocuments = new ArrayList<YoozDocument>();

		CoordinatorContext coordinatorContext = documentContext.getContext();
		String greatPlainsVendorId = null;
		String businessName = null;
		if (coordinatorContext.getSupplier() != null && coordinatorContext.getSupplier().getBusinessDetail() != null) {
			if (coordinatorContext.getSupplier().getBusinessDetail().getGreatPlainsVendorId() != null) {
				greatPlainsVendorId = coordinatorContext.getSupplier().getBusinessDetail().getGreatPlainsVendorId();
				businessName = coordinatorContext.getSupplier().getBusinessDetail().getBusinessName();
			}
		}
		if (greatPlainsVendorId == null && documentContext.getProcurementGroupContexts() != null) {
			SupplyChainVendor member = (SupplyChainVendor) documentContext.getProcurementGroupContexts().get(0)
					.getSupplierContext().getOrder().getFrom().getMember();
			greatPlainsVendorId = member.getGreatPlainsVendorId();
			businessName = member.getBusinessName();
		}

		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
		ZonedDateTime date = documentContext.getContext().getRequestTime();
		if (date == null) {
			date = ZonedDateTime.now();
		}

		// TODO For invoices need to use context.getSouceDocument.getLineItems()
 		if (SourceDocumentType.Invoice.equals(documentContext.getSourceDocument().getSourceDocumentType())) {
			@SuppressWarnings("unchecked")
			Collection<GenericLine> items = (Collection<GenericLine>) documentContext.getSourceDocument().getLineItems();
			List<LineItemContextImpl> lines = coordinatorContext.getLineItems();
			for (GenericLine item : items) {
				
				YoozDocument yoozDocument = new YoozDocument();
				yoozDocument = staticValues(yoozDocument);

				yoozDocument.setVendorCode(greatPlainsVendorId);
				yoozDocument.setVendorName(businessName);

				if (SourceDocumentType.Invoice.equals(documentContext.getSourceDocument().getSourceDocumentType())
						&& documentContext.getTransactionalContext() != null
						&& documentContext.getTransactionalContext().getOrder() != null) {
					yoozDocument.setInvoiceNumber(documentContext.getDocumentId().substring(15));
				}
				if (documentContext.getDocumentId() != null) {
					yoozDocument.setOrderNumber(documentContext.getDocumentId());
				}

				SupplyChainSourceDocument ediDocument = (SupplyChainSourceDocument) documentContext.getSourceDocument();
				if (ediDocument.getTerms() != null && ediDocument.getTerms().getNetDueDate() != null) {
					yoozDocument.setDueDate(dateFormat.format(ediDocument.getTerms().getNetDueDate()));
				}

				if (date != null) {
					yoozDocument.setDocumentTimeStamp(dateFormat.format(date));
					yoozDocument.setStartDate(dateFormat.format(date));

				}
				Number transactionAmount = item.getPrice();
				if (transactionAmount != null) {
					yoozDocument.setDocumentTaxedAmount(new BigDecimal(transactionAmount.toString()));
					yoozDocument.setDocumentUntaxedAmount(new BigDecimal(transactionAmount.toString()));
				}
				yoozDocument.setLineNumber(item.getLineNumber().toString());
				yoozDocument.setProductCode(item.getProductId());
				yoozDocument.setItemDescription(item.getVendorCodeSubCode() + " " + item.getPartNumber());
				yoozDocument.setProductUnitPrice(item.getPrice());

				yoozDocument.setQuantityInvoices(item.getQuantity());

				yoozDocument.setAmount(item.getPrice());
				yoozDocument.setSupplierProduct(item.getProductId());
				yoozDocument.setReceptionDate(dateFormat.format(ZonedDateTime.now()));
				Shipment shipment = null;
				if (documentContext.getFulfillmentLocationContext() != null) {
					shipment = documentContext.getFulfillmentLocationContext().getShipment();
				}
				if (shipment != null) {
					String deliveryDate = dateFormat.format(shipment.getArrivalDate().toInstant());
					yoozDocument.setPlannedDeliveryDate(deliveryDate);
				}

				String endDate = dateFormat.format(ZonedDateTime.now());
				yoozDocument.setEndDate(endDate);
				
				Optional<LineItemContextImpl> matchingLine = 
					    lines.stream()
					         .filter(i -> i.getPart().getPartNumber().equals(item.getPartNumber()))
					         .findAny();
				if (matchingLine.isPresent()) {
					yoozDocument.setUnitOfMeasure(matchingLine.get().getPart().getUnitOfMeasure());
					if (yoozDocument.getProductCode() == null) {
						yoozDocument.setProductCode(matchingLine.get().getLineItem().getProductId());
					}
					if (matchingLine.get().getPart().getProductBuyer() != null) {
						yoozDocument.setBuyerCode(matchingLine.get().getPart().getProductBuyer().getInitials());
					}
				}
				if (yoozDocument.getUnitOfMeasure() == null) {
					yoozDocument.setUnitOfMeasure("EA");
				} else {
					PrettyPrint.print(" ");
				}
				
				yoozDocuments.add(yoozDocument);

			}
		} else {

			//

			for (LineItemContextImpl line : coordinatorContext.getLineItems()) {
				YoozDocument yoozDocument = new YoozDocument();
				yoozDocument = staticValues(yoozDocument);

				yoozDocument.setVendorCode(greatPlainsVendorId);
				yoozDocument.setVendorName(businessName);

				if (SourceDocumentType.Invoice.equals(documentContext.getSourceDocument().getSourceDocumentType())
						&& documentContext.getTransactionalContext() != null
						&& documentContext.getTransactionalContext().getOrder() != null) {
					yoozDocument.setInvoiceNumber(documentContext.getDocumentId().substring(15));
				}
				if (documentContext.getDocumentId() != null) {
					yoozDocument.setOrderNumber(documentContext.getDocumentId());
				}

				SupplyChainSourceDocument ediDocument = (SupplyChainSourceDocument) documentContext.getSourceDocument();
				if (ediDocument.getTerms() != null && ediDocument.getTerms().getNetDueDate() != null) {
					yoozDocument.setDueDate(dateFormat.format(ediDocument.getTerms().getNetDueDate()));
				}

				if (date != null) {
					yoozDocument.setDocumentTimeStamp(dateFormat.format(date));
					yoozDocument.setStartDate(dateFormat.format(date));

				}
				Number transactionAmount = documentContext.getTransactionalContext().getOrder().getTransactionAmount();
				if (transactionAmount != null) {
					yoozDocument.setDocumentTaxedAmount(new BigDecimal(transactionAmount.toString()));
					yoozDocument.setDocumentUntaxedAmount(new BigDecimal(transactionAmount.toString()));
				}
//			yoozDocument.setOrderCreator("shellyh@autowares.com"); // Email of the PO creator. This user must exist in
				// the Yooz users list. If it is not the case, the
				// system user will be assigned to the PO.
//			yoozDocument.setOrderApprovers("shellyh@autowares.com");
//			yoozDocument.setOrderStatus(documentContext.getContext().getTransactionStage().toString()); // Per Emerson Hoovey leave blank
				yoozDocument.setLineNumber(line.getLineNumber().toString());
				yoozDocument.setProductCode(line.getProductId());
				yoozDocument.setItemDescription(line.getVendorCodeSubCode() + " " + line.getPartNumber());
				yoozDocument.setProductUnitPrice(line.getPrice());

				PurchaseLine purchaseLine = (PurchaseLine) line.getLineItem();
				Optional<Quantity> purchasedQuantity = purchaseLine.getQuantities().stream()
						.filter(i -> i.getQuantityType().equals(QuantityType.Purchased)).findAny();
				if (purchasedQuantity.isPresent()) {
					yoozDocument.setQuantityOrdered(purchasedQuantity.get().getQuantity());
				}
				Optional<Quantity> stockedQuantity = purchaseLine.getQuantities().stream()
						.filter(i -> i.getQuantityType().equals(QuantityType.Stocked)).findAny();
				if (stockedQuantity.isPresent()) {
					yoozDocument.setQuantityReceived(stockedQuantity.get().getQuantity());
				}
				Optional<Quantity> invoicedQuantity = purchaseLine.getQuantities().stream()
						.filter(i -> i.getQuantityType().equals(QuantityType.Invoiced)).findAny();
				if (invoicedQuantity.isPresent()) {
//				yoozDocument.setQuantityInvoices(invoicedQuantity.get().getQuantity()); // Per Emerson Hoovey leave blank
				}
				
				yoozDocument.setAmount(line.getPrice());
//			yoozDocument.setGlAccount(" ");
//			yoozDocument.setCostCenterChartsCodes(" ");
//			yoozDocument.setSubsidiary(" ");
				yoozDocument.setSupplierProduct(line.getProductId());
//			yoozDocument.setHeader(" ");
//			yoozDocument.setLines(" ");
//			yoozDocument.setReceptionComment(" ");
				yoozDocument.setReceptionDate(dateFormat.format(ZonedDateTime.now()));
				Shipment shipment = null;
				if (documentContext.getFulfillmentLocationContext() != null) {
					shipment = documentContext.getFulfillmentLocationContext().getShipment();
				}
				if (shipment != null) {
					String deliveryDate = dateFormat.format(shipment.getArrivalDate().toInstant());
					yoozDocument.setPlannedDeliveryDate(deliveryDate);
				}

//          Per Emerson Hoovey:  let get rid of the addresses in columns 35 and 36.  They’re not needed.
//			if (documentContext.getContext().getBusinessContext().getBusinessDetail() != null) {
//				BusinessDetail shipTo = documentContext.getContext().getBusinessContext().getBusinessDetail();
//				yoozDocument.setDeliveryAddress(
//						shipTo.getAddress() + ", " + shipTo.getCity() + ", " + shipTo.getPostalCode());
//			}
//			if (documentContext.getContext().getSupplier() != null && documentContext.getContext().getSupplier().getBusinessDetail() != null) {
//				BusinessDetail shipFrom = documentContext.getContext().getSupplier().getBusinessDetail();
//				yoozDocument.setInvoicingAddress(
//						shipFrom.getAddress() + ", " + shipFrom.getCity() + ", " + shipFrom.getPostalCode());
//			}
//			yoozDocument.setYoozNumber(" ");
//			yoozDocument.setSublinesManagement(" ");
//			yoozDocument.setBudgetPeriodCode(" ");
//			yoozDocument.setBudgetCode(" ");
				String endDate = dateFormat.format(ZonedDateTime.now());
				yoozDocument.setEndDate(endDate);
//			yoozDocument.setLabel(" ");
//			yoozDocument.setVendorItemCode(" ");
				yoozDocuments.add(yoozDocument);
			}
		}
		return yoozDocuments;
	}

	private static YoozDocument staticValues(YoozDocument yoozDoc) {
		yoozDoc.setAction("CREATE");
		yoozDoc.setCurrency("USD");
		yoozDoc.setPaymentMethod("YZ_MAN");
		yoozDoc.setDiscountedAmout(BigDecimal.ZERO);
		yoozDoc.setTaxProfileCode("Exempt");
		yoozDoc.setTaxAmount(BigDecimal.ZERO);
		yoozDoc.setToBeRecevied("1");
		yoozDoc.setTypeOfPO("STD");
		yoozDoc.setTypeOfLine("PO");
		return yoozDoc;
	}
}
