package com.autowares.mongoose.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;

import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.model.PartLineItem;
import com.autowares.servicescommon.model.ProductLineItem;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.SupplyChainLine;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

public class MatchingUtils {

	public static Boolean isModified(GenericLine lineA, GenericLine lineB) {
		if (lineA.getQuantity() != null && !lineA.getQuantity().equals(lineB.getQuantity())) {
			return true;
		}
		if (lineA.getShippedQuantity() != null && !lineA.getShippedQuantity().equals(lineB.getShippedQuantity())) {
			return true;
		}
		if (lineA.getReceivedQuantity() != null && !lineA.getReceivedQuantity().equals(lineB.getReceivedQuantity())) {
			return true;
		}
		if (lineA.getStockedQuantity() != null && !lineA.getStockedQuantity().equals(lineB.getStockedQuantity())) {
			return true;
		}
		if (lineA.getPrice() != null && !lineA.getPrice().equals(lineB.getPrice())) {
			return true;
		}

		if (lineA.getCoreCharge() != null && !lineA.getCoreCharge().equals(lineB.getCoreCharge())) {
			return true;
		}
		return false;
	}

	public static void mergeDocument(SupplyChainSourceDocument existing, SupplyChainSourceDocument updated) {
		Long supplyChainId = updated.getSupplyChainId();
		if (existing.getSupplyChainId() != null) {
			supplyChainId = existing.getSupplyChainId();
		}
		// We may want to update the transactionContext in the future, currently doesnt
		// touch it
		BeanUtils.copyProperties(updated, existing, "lineItems", "transactionContext", "primaryProcurementGroup","transactionStage", "transactionStatus");
		updated.setSupplyChainId(supplyChainId);
		existing.setSupplyChainId(supplyChainId);
		mergeLineItems(existing, updated);
	}

	public static void mergeLineItems(SupplyChainSourceDocument existing, SupplyChainSourceDocument updated) {
		for (SupplyChainLine line : updated.getLineItems()) {
			GenericLine updatedLineItem = (GenericLine) line;
			Optional<? extends SupplyChainLine> oexistingline = matchByLineNumber(line, existing.getLineItems());
			if (!oexistingline.isPresent()) {
				//Fall back to productId
				 oexistingline = matchByProduct(line, existing.getLineItems());
			}
			if (!oexistingline.isPresent()) {
				oexistingline = matchByPartNumber(line, existing.getLineItems());
			}
			if (oexistingline.isPresent()) {
				GenericLine existingLine = (GenericLine) oexistingline.get();
				Long lineId = updatedLineItem.getLineId();
				Integer lineNumber = updatedLineItem.getLineNumber();
				if (existingLine.getLineId() != null) {
					lineId = existingLine.getLineId();
				}
				if (existingLine.getLineNumber() != null) {
					lineNumber = existingLine.getLineNumber();
				}
				BeanUtils.copyProperties(updatedLineItem, existingLine);
				updatedLineItem.setLineId(lineId);
				existingLine.setLineId(lineId);
				updatedLineItem.setLineNumber(lineNumber);
				existingLine.setLineNumber(lineNumber);
			} else {
				@SuppressWarnings("unchecked")
				List<GenericLine> impl = (List<GenericLine>) existing.getLineItems();
				impl.add(updatedLineItem);
			}

		}
	}

	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> matchByProduct(PartLineItem lineToMatch, Collection<? extends ProductLineItem> lines) {
		return (Optional<T>) lines.stream().filter(i -> i.getProductId() != null)
				.filter(i -> i.getProductId().equals(lineToMatch.getProductId())).findFirst();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> matchByPartNumber(PartLineItem lineToMatch, Collection<? extends PartLineItem> lines) {
		return (Optional<T>) lines.stream().filter(i -> i.getPartNumber() != null)
				.filter(i -> i.getPartNumber().equals(lineToMatch.getPartNumber())).findFirst();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> matchByLineNumber(PartLineItem lineToMatch, Collection<? extends PartLineItem> lines) {
		return (Optional<T>) lines.stream().filter(i -> i.getLineNumber() != null)
				.filter(i -> i.getLineNumber().equals(lineToMatch.getLineNumber())).findFirst();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> matchByPartNumberManufacturerLineCode(LineItemContext lineToMatch, Collection<? extends LineItemContext> lines) {
		return (Optional<T>) lines.stream().filter(i -> i.getPartNumber() != null)
				.filter(i -> i.getManufacturerLineCode() != null)
				.filter(i -> i.getPartNumber().equals(lineToMatch.getPartNumber()))
				.filter(i -> i.getManufacturerLineCode().equals(lineToMatch.getManufacturerLineCode()))
				.findFirst();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> matchByOrigCodePartNumber(PartLineItem lineToMatch, Collection<? extends LineItemContext> lines) {
		return (Optional<T>) lines.stream().filter(i -> i.getPart() != null)
				.filter(i -> i.getPart().getOrigcodes() != null)
				.filter(i -> i.getPart().getOrigcodes()
				.stream().filter(p -> p.getSourceSystemPartNumber()
				.equals(lineToMatch.getPartNumber())).findFirst().isPresent()).findFirst();
	}
	
//	public static void markOperational(WmsEventLineItemContext lineContext, IntegrationContext context,
//			OperationalStage stage, String documentId) {
//		Map<String, OperationalContext> opMap = context.getOperationalMap();
//
//		Optional<LineItemContext> optionalPurchaseLine = MatchingUtils.matchByProduct(lineContext,
//				context.getPurchaseLines());
//
//		if (optionalPurchaseLine.isPresent()) {
//			LineItemContext purchaseLine = optionalPurchaseLine.get();
//			for (int i = 0; i < lineContext.getQuantity(); i++) {
//				Optional<OperationalItem> optionalOperational = purchaseLine.getOperationalItems().stream()
//						.filter(s -> !stage.equals(s.getOperationalStage())).findAny();
//
//				OperationalItem operationalItem = null;
//				if (optionalOperational.isPresent()) {
//					operationalItem = optionalOperational.get();
//				} else {
//					// Not expecting this item
//					log.info("Got extra item for lineNumber: " + purchaseLine.getLineNumber() +" in stage: " + stage);
//					operationalItem = new OperationalItem(purchaseLine);
//					purchaseLine.getOperationalItems().add(operationalItem);
//				}
//
//				OperationalContext opJob = opMap.get(documentId);
//				if (opJob == null) {
//					opJob = new OperationalContext();
//					opJob.setCorrelation(documentId);
//					if (OperationalStage.shipped.equals(stage)) {
//						opJob.setDocumentType(SourceDocumentType.Shipment);
//					}
//					if (OperationalStage.received.equals(stage)) {
//						opJob.setDocumentType(SourceDocumentType.PackSlip);
//					}
//					if (OperationalStage.stocking.equals(stage)) {
//						opJob.setDocumentType(SourceDocumentType.StockJob);
//					}
//					if (OperationalStage.putaway.equals(stage)) {
//						opJob.setDocumentType(SourceDocumentType.StockJob);
//					}
//
//					opMap.put(documentId, opJob);
//				}
//
//				Operation operation = new Operation(operationalItem, opJob);
//				operation.setOperationalStage(stage);
//				operation.setTimeStamp(DateConversion.convert(lineContext.getEvent().getTimeStamp()));
//				operationalItem.setCurrentOperation(operation);
//				
//				opJob.getItems().add(operationalItem);
//			}
//		} else {
//			// Can't resolve this line item
//			log.info("Operating on product not defined in the purchase VC: " + lineContext.getVendorCodeSubCode()
//					+ " PN: " + lineContext.getPartNumber() + " in stage: " + stage);
//		}
//
//	}


}
