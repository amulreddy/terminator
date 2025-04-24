package com.autowares.mongoose.coordinator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autowares.apis.ids.command.LocationLookupClient;
import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.ids.model.LocationLookupRequest;
import com.autowares.apis.partservice.MultiPartRequest;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.command.PartClient;
import com.autowares.assetmanagement.client.AssetClient;
import com.autowares.assetmanagement.model.AssetType;
import com.autowares.ipov3.common.BusinessAddress;
import com.autowares.ipov3.model.proxy.FreightTerm;
import com.autowares.ipov3.quote.AddQuoteResponseImpl;
import com.autowares.ipov3.quote.RequestForQuoteImpl;
import com.autowares.ipov3.quote.RequestLine;
import com.autowares.ipov3.quote.ResponseLine;
import com.autowares.mongoose.client.CoordinatorClient;
import com.autowares.mongoose.command.VicIpoServiceClient;
import com.autowares.partyconfiguration.model.PartnershipType;
import com.autowares.servicescommon.model.AccountImpl;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.supplychain.commands.SourceDocumentClient;
import com.autowares.supplychain.model.DocumentSummary;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainBusinessImpl;
import com.autowares.supplychain.model.SupplyChainParty;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;
import com.autowares.supplychain.model.TransactionContext;
import com.autowares.supplychain.model.TransactionScope;
import com.autowares.xmlgateway.edi.base.EdiDocument;
import com.autowares.xmlgateway.edi.base.EdiRecord;
import com.autowares.xmlgateway.edi.records.BIGHeader;
import com.autowares.xmlgateway.edi.records.BaseItemIT1;
import com.autowares.xmlgateway.edi.records.CarrierDetailCAD;
import com.autowares.xmlgateway.edi.records.CarrierDetailTD5;
import com.autowares.xmlgateway.edi.records.DateTimeReferenceDTM;
import com.autowares.xmlgateway.edi.records.HierarchicalLevelHL;
import com.autowares.xmlgateway.edi.records.InterchangeControlHeaderISA;
import com.autowares.xmlgateway.edi.records.InvoiceTermsDescriptionITD;
import com.autowares.xmlgateway.edi.records.ItemDescriptionPID;
import com.autowares.xmlgateway.edi.records.ItemDetail;
import com.autowares.xmlgateway.edi.records.ItemIdentification;
import com.autowares.xmlgateway.edi.records.NameN1;
import com.autowares.xmlgateway.edi.records.NameN3;
import com.autowares.xmlgateway.edi.records.NameN4;
import com.autowares.xmlgateway.edi.records.PurchaseOrderReference;
import com.autowares.xmlgateway.edi.records.ReferenceIdentificationREF;
import com.autowares.xmlgateway.edi.records.ServicePromotionAllowanceSAC;
import com.autowares.xmlgateway.edi.records.TransactionTotalsCTT;
import com.autowares.xmlgateway.edi.records.TransportationInstructionsFOB;
import com.autowares.xmlgateway.edi.types.AllowanceChargeCode;
import com.autowares.xmlgateway.edi.types.DTMCode;
import com.autowares.xmlgateway.edi.types.DescriptionCode;
import com.autowares.xmlgateway.edi.types.HLLevelCode;
import com.autowares.xmlgateway.edi.types.ItemStatusCode;
import com.autowares.xmlgateway.edi.types.REFCode;
import com.autowares.xmlgateway.edi.types.ShipmentMethodOfPayment;
import com.autowares.xmlgateway.edi.types.TermsBasisDateCode;
import com.autowares.xmlgateway.edi.types.TermsTypeCode;
import com.autowares.xmlgateway.edi.types.TransportationTypeCode;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.OrderRequest;
import com.autowares.xmlgateway.model.RequestItem;
import com.google.common.collect.Lists;

//@Disabled
public class IpoTestManual {

	private static Logger log = LoggerFactory.getLogger(IpoTestManual.class);

	private static final String QUOTE_DOCUMENT_ID_FILE_NAME = "tmp_test_id.txt";
	private static final String TEST_PART_NUMBER = "89013";
	private static final String TEST_PART_LINE = "FOU";
	
//	private static final String TEST_PART_NUMBER = "CU13301";
//	private static final String TEST_PART_LINE = "RAD";
	private static final Integer TEST_PART_QTY = 1;
	
	
	// VIC Error - Brand not Authorized
//	item.setPartNumber("HP4155");
//	item.setLineCode("STD");
	
	// vendor not configured - Manual
//	item.setPartNumber("109685X");
//	item.setLineCode("BEP");
	
	
//	private static final String TEST_PART_NUMBER = "18-9420-2";
//	private static final String TEST_PART_LINE = "PRI";

	@Test
	public void quoteTest() {

		RequestForQuoteImpl requestForQuoteImpl = new RequestForQuoteImpl();
		requestForQuoteImpl.setDocumentId("1306683467748335616");
		requestForQuoteImpl.setCustomerQuoteDocumentId("499331");
		requestForQuoteImpl.setSenderEdiId("6162432125");
		requestForQuoteImpl.setSupplierEdiId("9723168100");

		BusinessAddress billTo = new BusinessAddress();
		billTo.setAccountNumber("01");
		billTo.setBusinessName("SAI KIRAN");
		billTo.setAddressLine1("1115 SE Olson Dr");
		billTo.setAddressLine2("109");
		billTo.setCity("Waukee");
		billTo.setStateCode("IA");
		billTo.setPostalCode("50263");
		billTo.setCountryCode("US");
		requestForQuoteImpl.setBillTo(billTo);

		BusinessAddress shipTo = new BusinessAddress();
		shipTo.setAccountNumber("01");
		shipTo.setBusinessName("SAI KIRAN");
		shipTo.setAddressLine1("1115 SE Olson Dr");
		shipTo.setAddressLine2("109");
		shipTo.setCity("Waukee");
		shipTo.setStateCode("IA");
		shipTo.setPostalCode("50263");
		shipTo.setCountryCode("US");
		requestForQuoteImpl.setShipTo(shipTo);

		requestForQuoteImpl.setLineItems(new ArrayList<>());
		RequestLine lineItem = new RequestLine();
		lineItem.setLineNumber(BigInteger.valueOf(25));
		lineItem.setPartNumber("47655");
		lineItem.setLineCode("BBRN");
		lineItem.setBrandAAIAId("BBRN");
		lineItem.setQuantity(1);

		requestForQuoteImpl.getLineItems().add(lineItem);
		PrettyPrint.print("Request");
		PrettyPrint.print(requestForQuoteImpl);

		VicIpoServiceClient vicIpoServiceClient = new VicIpoServiceClient();
		vicIpoServiceClient.withServiceDomain("testconsul");
		AddQuoteResponseImpl response = vicIpoServiceClient.quote(requestForQuoteImpl);

		PrettyPrint.print(" ");
		PrettyPrint.print("Response");
		PrettyPrint.print(response);

		List<ResponseLine> lineItems = response.getLineItems();
		for (ResponseLine li : lineItems) {
			PrettyPrint.print(" ");
			PrettyPrint.print("Freight");
			PrettyPrint.print(li.getLineCode() + " " + li.getPartNumber());
			for (FreightTerm freight : li.getFreightTerms()) {
				PrettyPrint.print(" - " + freight.getCarrier() + "  " + freight.getCarrierCode());
				PrettyPrint.print(" - " + freight.getShippingMethod() + "  " + freight.getShippingCode());
				PrettyPrint.print(" - " + freight.getShippingCharge());
				PrettyPrint.print(" ");
			}
		}

	}

	// STEP: ZERO - create a quote document to work with.
	@Test
	public void requestQuote() {
		CoordinatorClient coordinatorClient = new CoordinatorClient();
		coordinatorClient.withPort(8282).withLocalService();
		InquiryRequest quoteRequest = new InquiryRequest();
		quoteRequest.setAccountNumber("750");
		List<RequestItem> lineItems = new ArrayList<>();
		RequestItem item = new RequestItem();
		
		// VIC Error - Brand not Authorized
//		item.setPartNumber("HP4155");
//		item.setLineCode("STD");
		
		item.setPartNumber(TEST_PART_NUMBER);
		item.setLineCode(TEST_PART_LINE);
		item.setQuantity(TEST_PART_QTY);
		
		
//		item.setPartNumber("109685X");
//		item.setLineCode("BEP");
		item.setLineNumber(1);
		lineItems.add(item);
		quoteRequest.setLineItems(lineItems);
		InquiryResponse response = coordinatorClient.quote(quoteRequest);
		log.info(response.getDocumentReferenceId());
		checkStatus(response.getDocumentReferenceId());
		saveQuoteDocumentId(response.getDocumentReferenceId());
	}

	// STEP: ONE - clean up all the related documents and run a quote through.
	//
	// 1) Clean out related documents
	// 2) Reset the purchase order type on the quote
	// 3) Process the quote
	// 4) check the status
	@Test
	public void cleanupAndRunQuote() {
		String documentId = getQuoteDocumentId();

		SourceDocumentClient sc = new SourceDocumentClient();
		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");
		CoordinatorClient coordinatorClient = new CoordinatorClient();
		coordinatorClient.withPort(8282).withLocalService();

		log.info("Running documentId: {}", documentId);
		this.clean(documentId, false);

		Optional<SupplyChainSourceDocumentImpl> doc = sc.findSourceDocumentByDocumentId(documentId);

		if (doc.isPresent()) {
			// TODO: Don't save the purchase order type on the quote in coordinator
			// processing
			doc.get().setPurchaseOrderType(null);
			sc.saveSourceDocument(doc.get());
		}

		try {
			coordinatorClient.processDocument(documentId);
		} catch (Exception e) {
			log.error(
					"Sometimes this errors because of serialization issues in the response.  We should fix this at some point.");
			log.error("Caught error: " + e.getMessage());
		}

		checkStatus(documentId);
	}

	// STEP: TWO - Check the status of the quote (optional, processing the quote
	// should leave it in a correct state)
	//
	// 1) Print out the current status of the quote document and related documents.
	@Test
	public void checkStatusTest() {
		checkStatus(getQuoteDocumentId());
	}

	// STEP: THREE - select the freight
	//
	// 1) Looks up the customer quote and sets the selected freight on the
	// ShippingEstimate
	@Test
	public void selectShipping() {
		String selectFreightOption = "UPS Ground";
		String documentId = getQuoteDocumentId();
		SourceDocumentClient sc = new SourceDocumentClient();
		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");

		Optional<SupplyChainSourceDocumentImpl> doc = sc.findSourceDocumentByDocumentId(documentId);
		Optional<SupplyChainSourceDocument> shippingEstimate = Optional.empty();

		if (doc.isPresent()) {
			if (doc.get().getProcurementGroups() != null && !doc.get().getProcurementGroups().isEmpty()) {
				Optional<DocumentSummary> docSummary = doc.get().getRelatedDocuments().stream()
						.filter(r -> r.getSourceDocumentType() == SourceDocumentType.ShippingEstimate).findAny();
				if (docSummary.isPresent()) {
					shippingEstimate = sc.findSourceDocumentByDocumentId(docSummary.get().getDocumentId());
				}
			}
		}

		if (shippingEstimate.isPresent()) {
			shippingEstimate.get().getFreightOptions().getAvailableFreight().stream().forEach(i -> {
				log.info(i.getDescription());
			});

			Optional<Freight> freight = shippingEstimate.get().getFreightOptions().getAvailableFreight().stream()
					.filter(i -> i.getDescription().equals(selectFreightOption)).findAny();
			if (freight.isPresent()) {
				shippingEstimate.get().getFreightOptions().getAvailableFreight().remove(freight.get());
				shippingEstimate.get().getFreightOptions().setSelectedFreight(freight.get());
			}

			sc.saveSourceDocument(shippingEstimate.get());
		}
	}

	// STEP: FOUR - Place a purchase order
	//
	// 1) Places an order for the quoted document.
	@Test
	public void requestOrder() {
		String orderReferenceId = getQuoteDocumentId();
		CoordinatorClient coordinatorClient = new CoordinatorClient();
		coordinatorClient.withPort(8282).withLocalService();
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setAccountNumber("750");
		orderRequest.setCustomerDocumentId("750-123456789");
		List<RequestItem> lineItems = new ArrayList<>();
		RequestItem item = new RequestItem();
		item.setPartNumber(TEST_PART_NUMBER);
		item.setLineCode(TEST_PART_LINE);
		item.setQuantity(TEST_PART_QTY);
		item.setLineNumber(1);
		item.setDocumentReferenceIds(Lists.newArrayList(orderReferenceId));
		lineItems.add(item);
		orderRequest.setLineItems(lineItems);
		Object response = coordinatorClient.processOrder(orderRequest);
		PrettyPrint.print(response);
	}

	@Test
	public void inquiryOrder() {
		String quoteReferenceId = getQuoteDocumentId();
		CoordinatorClient coordinatorClient = new CoordinatorClient();
		coordinatorClient.withLocalService();
		InquiryRequest quoteRequest = new InquiryRequest();
		quoteRequest.setAccountNumber("750");
		List<RequestItem> lineItems = new ArrayList<>();
		RequestItem item = new RequestItem();

//		item.setPartNumber("51515");
//		item.setLineCode("WIX");

//		item.setPartNumber("109685X");
//		item.setLineCode("BEP");

		item.setPartNumber(TEST_PART_NUMBER);
		item.setLineCode(TEST_PART_LINE);
		item.setQuantity(TEST_PART_QTY);
		item.setLineNumber(1);
		item.setDocumentReferenceIds(Lists.newArrayList(quoteReferenceId));
		lineItems.add(item);
		quoteRequest.setLineItems(lineItems);
//		quoteRequest.getInquiryOptions().setCalculateShipment(true);
//		quoteRequest.getInquiryOptions().setPlanFulfillment(true);
//		quoteRequest.getFulfillmentOptions().setAllowStoreInventory(true);

		InquiryResponse response = coordinatorClient.inquire(quoteRequest);
		PrettyPrint.print(response);
	}

	// STEP: FIVE - Cleanup (Optional)
	//
	// 1) remove all related documents
	// 2) leaves customer quote only
	@Test
	public void cleanup() {
		clean(getQuoteDocumentId(), false);
	}

	@Test
	public void cleanupOrder() {
		clean(getQuoteDocumentId(), true);
	}

	// STEP: SIX - Remove (Optional)
	//
	// 1) remove all related documents.
	// 2) delete customer quote.
	@Test
	public void remove() {
		String documentId = getQuoteDocumentId();
		SourceDocumentClient sc = new SourceDocumentClient();
		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");
		clean(documentId, false);
		deleteDoc(sc, documentId);
		File file = new File(QUOTE_DOCUMENT_ID_FILE_NAME);
		file.delete();
	}

	// UTILITY
	public void clean(String documentId, Boolean purchaseOrderOnly) {
		SourceDocumentClient sc = new SourceDocumentClient();
		AssetClient assetClient = new AssetClient();

		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");

		assetClient.withServiceDomain("testconsul");

		// Delete asset
		try {
			log.info("Deleting locks");
			assetClient.deleteAsset(documentId, AssetType.Document);
			log.info("Locks cleaned up");
		} catch (Exception e) {
			log.info("Nothing to unlock.");
		}
		// look up document
		Optional<SupplyChainSourceDocument> doc = sc.findSourceDocumentByDocumentId(documentId);

		if (!doc.isPresent()) {
			log.info("Nothing to clean");
			return;
		}

		// delete all the related documents

		if (doc.get().getProcurementGroups() != null) {
			for (ProcurementGroup pg : doc.get().getProcurementGroups()) {
				if (pg.getSourceDocuments() != null) {
					for (SupplyChainSourceDocument sd : pg.getSourceDocuments()) {
						if (purchaseOrderOnly) {
							if (!SourceDocumentType.Invoice.equals(sd.getSourceDocumentType())
									&& !SourceDocumentType.PackSlip.equals(sd.getSourceDocumentType())) {
								continue;
							}
						}
						if (!sd.getDocumentId().equals(doc.get().getDocumentId())) {
							sd = (SupplyChainSourceDocument) sc.findSourceDocumentByDocumentId(sd.getDocumentId())
									.get();
							log.info("(pg) Deleting {} {}", sd.getDocumentId(), sd.getSourceDocumentType().name(),
									sd.getSupplyChainId());
							sc.delete(sd.getSupplyChainId());
							log.info("Deleted");
						}
					}
				}
			}
		}

		if (doc.get().getTransactionContext() != null
				&& doc.get().getTransactionContext().getSourceDocuments() != null) {

			for (SupplyChainSourceDocument sd : doc.get().getTransactionContext().getSourceDocuments()) {
				if (!sd.getDocumentId().equals(doc.get().getDocumentId())) {
					if (purchaseOrderOnly) {
						if (!SourceDocumentType.Invoice.equals(sd.getSourceDocumentType())
								&& !SourceDocumentType.PackSlip.equals(sd.getSourceDocumentType())) {
							continue;
						}
					}
					sd = (SupplyChainSourceDocument) sc.findSourceDocumentByDocumentId(sd.getDocumentId()).get();
					log.info("(tc) Deleting {} {}", sd.getDocumentId(), sd.getSourceDocumentType().name(),
							sd.getSupplyChainId());
					sc.delete(sd.getSupplyChainId());
					log.info("Deleted");
				}
			}
		}

		doc.get().setTransactionStatus(TransactionStatus.Processing);
		sc.saveSourceDocument(doc.get());

	}

	// UTILITY
	private void checkStatus(String documentId) {
		SourceDocumentClient sc = new SourceDocumentClient();
		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");

		Optional<SupplyChainSourceDocumentImpl> doc = sc.findSourceDocumentByDocumentId(documentId);
		Optional<SupplyChainSourceDocument> shippingEstimate = Optional.empty();

		Boolean hasQuoteDocument = doc.isPresent();
		Boolean hasShippingEstimate = false;
		Boolean hasFreightOptions = false;
		Boolean hasSelectedFreight = false;
		Boolean hasPackSlip = false;
		Boolean hasPurchaseOrder = false;
		Boolean hasSupplierQuote = false;
		Boolean hasSupplierPurchaseOrder = false;
		Boolean hasInvoice = false;
		Boolean hasSupplierInvoice = false;

		if (doc.isPresent()) {
			TransactionStage stage = doc.get().getTransactionStage();
			TransactionStatus status = doc.get().getTransactionStatus();
			log.info(documentId + " Stage: " + stage.name() + " Status: " + status.name());

			log.info("Related Documents: ");
			doc.get().getRelatedDocuments().forEach(i -> {
				log.info("\t" + i.getDocumentId() + " " + " " + i.getSourceDocumentType());
			});

			if (doc.get().getProcurementGroups() != null && !doc.get().getProcurementGroups().isEmpty()) {

				for (ProcurementGroup pg : doc.get().getProcurementGroups()) {
					log.info("Procurement Group: " + pg.getProcurementGroupName());
					pg.getSourceDocuments().stream().forEach(sd -> {
						log.info("\t\t(pg) " + pg.getProcurementGroupName() + " " + sd.getDocumentId() + " " + " "
								+ sd.getSourceDocumentType());
					});

					Optional<SupplyChainSourceDocument> supplierQuoteSummary = pg.getSourceDocuments().stream()
							.filter(r -> r.getSourceDocumentType() == SourceDocumentType.Quote).findAny();

					hasSupplierQuote = supplierQuoteSummary.isPresent();

					Optional<SupplyChainSourceDocument> supplierPoSummary = pg.getSourceDocuments().stream()
							.filter(r -> r.getSourceDocumentType() == SourceDocumentType.PurchaseOrder).findAny();

					hasSupplierPurchaseOrder = supplierPoSummary.isPresent();

					Optional<SupplyChainSourceDocument> supplierInvoiceSummary = pg.getSourceDocuments().stream()
							.filter(r -> r.getSourceDocumentType() == SourceDocumentType.Invoice).findAny();

					hasSupplierInvoice = supplierInvoiceSummary.isPresent();

					if (supplierQuoteSummary.isPresent()) {
						Optional<SupplyChainSourceDocumentImpl> supplierQuote = sc
								.findSourceDocumentByDocumentId(supplierQuoteSummary.get().getDocumentId());
						log.info("Customer documentId: {}", supplierQuote.get().getCustomerDocumentId());
						log.info("Supplier documentId: {}", supplierQuote.get().getVendorDocumentId());
					}
				}
			}

			Optional<DocumentSummary> docSummary = doc.get().getRelatedDocuments().stream()
					.filter(r -> r.getSourceDocumentType() == SourceDocumentType.ShippingEstimate).findAny();

			if (docSummary.isPresent()) {
				shippingEstimate = sc.findSourceDocumentByDocumentId(docSummary.get().getDocumentId());
				hasShippingEstimate = true;
			}

			Optional<DocumentSummary> packSlipSummary = doc.get().getRelatedDocuments().stream()
					.filter(r -> r.getSourceDocumentType() == SourceDocumentType.PackSlip).findAny();

			hasPackSlip = packSlipSummary.isPresent();

			Optional<DocumentSummary> poSummary = doc.get().getRelatedDocuments().stream()
					.filter(r -> r.getSourceDocumentType() == SourceDocumentType.PurchaseOrder).findAny();

			hasPurchaseOrder = poSummary.isPresent();

			Optional<DocumentSummary> invoiceSummary = doc.get().getRelatedDocuments().stream()
					.filter(r -> r.getSourceDocumentType() == SourceDocumentType.Invoice).findAny();

			hasInvoice = invoiceSummary.isPresent();

			try {
				if (shippingEstimate.isPresent()) {
					log.info("Available Freight:");
					hasFreightOptions = !shippingEstimate.get().getFreightOptions().getAvailableFreight().isEmpty();
					shippingEstimate.get().getFreightOptions().getAvailableFreight().stream().forEach(i -> {
						log.info("\tAvailable Freight: {}", i.getDescription());
					});

					Freight selectedFreight = shippingEstimate.get().getFreightOptions().getSelectedFreight();

					if (selectedFreight != null) {
						log.info("Selected Freight:");
						log.info("\tSelected Freight: {}", selectedFreight.getDescription());
						hasSelectedFreight = true;
					} else {
						log.warn("No selected freight was found.");
					}
				} else {
					log.error("No Shipping Estimate was found.");
				}
			} catch (Exception e) {
				log.error("Failed to evaluate shippingEstimate");
				e.printStackTrace();
			}
		}

		log.info("Summary:");
		log.info("\thasQuoteDocument \t\t{}", hasQuoteDocument);
		log.info("\thasSupplierQuote \t\t{}", hasSupplierQuote);
		log.info("\thasShippingEstimate \t\t{}", hasShippingEstimate);
		log.info("\thasFreightOptions \t\t{}", hasFreightOptions);
		log.info("\thasSelectedFreight \t\t{}", hasSelectedFreight);
		log.info("\thasPackSlip \t\t\t{}", hasPackSlip);
		log.info("\thasPurchaseOrder \t\t{}", hasPurchaseOrder);
		log.info("\thasSupplierPurchaseOrder \t{}", hasSupplierPurchaseOrder);
		log.info("\thasInvoice \t\t\t{}", hasInvoice);
		log.info("\thasSupplierInvoice \t\t{}", hasSupplierInvoice);
	}

	@Test
	public void testSupplyChain() {
		SourceDocumentClient sc = new SourceDocumentClient();
		LocationLookupClient idsClient = new LocationLookupClient();
		PartClient pc = new PartClient();

		idsClient.withServiceDomain("testconsul");
		sc.withServiceDomain("testconsul");
		pc.withServiceDomain("testconsul");

		deleteDoc(sc, "unit_testing_customer_quote");
		deleteDoc(sc, "unit_testing_supplier_quote");
		deleteDoc(sc, "unit_testing_estimate");

		deleteDoc(sc, "unit_testing_customer_order");
		deleteDoc(sc, "unit_testing_supplier_order");
		deleteDoc(sc, "unit_testing_packslip");

		deleteDoc(sc, "unit_testing_supplier_invoice");
		deleteDoc(sc, "unit_testing_customer_invoice");

		LocationLookupRequest request = new LocationLookupRequest();
		request.setAwiAccountNo("1000");
		BusinessDetail awi = idsClient.findBusinessLocation(request);

		request.setAwiAccountNo("750");
		BusinessDetail testAccount = idsClient.findBusinessLocation(request);

		request.setBusEntId("165959");
		request.setAwiAccountNo(null);
		BusinessDetail supplier = idsClient.findBusinessLocation(request);

		MultiPartRequest partRequest = new MultiPartRequest();
		partRequest.setParts(new ArrayList<>());
		Part part = new Part();
		part.setPartNumber("AC2");
		part.setLineNumber(0);
		part.setCwLineCode("STD");
		partRequest.getParts().add(part);
		MultiPartRequest partResponse = pc.lookupParts(partRequest);
		part = partResponse.getParts().get(0);

		assertNotNull(awi);
		assertNotNull(testAccount);
		assertNotNull(supplier);
		assertNotNull(part);

		GenericLine lineItem = GenericLine.builder().withProductId(part.getProductId())
				.withPartNumber(part.getPartNumber()).withLineCode(part.getLineCode()).withLineNumber(1).build();

		ProcurementGroup pg1 = ProcurementGroup.builder().withPartnershipType(PartnershipType.Procuring)
				.withProcurementGroupName("pg1").build();

		TransactionContext tc1 = TransactionContext.builder().withTransactionScope(TransactionScope.Supplying)
				.withTransactionContextId(UUID.randomUUID());
		TransactionContext tc2 = TransactionContext.builder().withTransactionScope(TransactionScope.Purchasing)
				.withTransactionContextId(UUID.randomUUID());

		lineItem.setQuantity(1);
		lineItem.setShippedQuantity(1);

		SupplyChainSourceDocument clientQuote = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_customer_quote").withSourceDocumentType(SourceDocumentType.Quote)
				.withFrom(businessEntityToParty(awi, PartyType.Selling))
				.withTo(businessEntityToParty(testAccount, PartyType.Buying)).withProcurementGroup(pg1)
				.withLineItem(lineItem).withTransactionContext(tc1).build();

		SupplyChainSourceDocument supplierQuote = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_supplier_quote").withSourceDocumentType(SourceDocumentType.Quote)
				.withFrom(businessEntityToParty(supplier, PartyType.Selling))
				.withTo(businessEntityToParty(awi, PartyType.Buying)).withLineItem(lineItem).withProcurementGroup(pg1)
				.withTransactionContext(tc2).build();

		FreightOptions freightOptions = new FreightOptions();
		Freight freightOption1 = new Freight();
		freightOption1.setCarrier("UPS");
		freightOption1.setDescription("DESC 1");
		freightOption1.setCost(new BigDecimal("2.50"));
		freightOptions.getAvailableFreight().add(freightOption1);

		Freight freightOption2 = new Freight();
		freightOption2.setCarrier("UPS");
		freightOption2.setDescription("DESC 2");
		freightOption2.setCost(new BigDecimal("7.50"));
		freightOptions.getAvailableFreight().add(freightOption2);

		Freight freightOption3 = new Freight();
		freightOption3.setCarrier("UPS");
		freightOption3.setDescription("DESC 3");
		freightOption3.setCost(new BigDecimal("10.50"));
		freightOptions.setSelectedFreight(freightOption3);

		SupplyChainSourceDocument shippingEstimate = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_estimate").withSourceDocumentType(SourceDocumentType.ShippingEstimate)
				.withFrom(businessEntityToParty(supplier, PartyType.ShipFrom))
				.withTo(businessEntityToParty(testAccount, PartyType.ShipTo)).withLineItem(lineItem)
				.withProcurementGroup(pg1).withFreightOptions(freightOptions).withTransactionContext(tc2).build();

		PrettyPrint.print(clientQuote);
		clientQuote = sc.saveSourceDocument(clientQuote);
		supplierQuote = sc.saveSourceDocument(supplierQuote);
		shippingEstimate = sc.saveSourceDocument(shippingEstimate);

		FreightOptions orderFreight = new FreightOptions();
		orderFreight.setSelectedFreight(freightOption3);

		SupplyChainSourceDocument clientOrder = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_customer_order").withSourceDocumentType(SourceDocumentType.PurchaseOrder)
				.withFrom(businessEntityToParty(awi, PartyType.Selling))
				.withTo(businessEntityToParty(testAccount, PartyType.Buying)).withFreightOptions(orderFreight)
				.withProcurementGroup(pg1).withLineItem(lineItem).withTransactionContext(tc1).build();

		SupplyChainSourceDocument supplierOrder = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_supplier_order").withSourceDocumentType(SourceDocumentType.PurchaseOrder)
				.withFrom(businessEntityToParty(supplier, PartyType.Selling))
				.withTo(businessEntityToParty(awi, PartyType.Buying)).withLineItem(lineItem).withProcurementGroup(pg1)
				.withTransactionContext(tc2).build();

		SupplyChainSourceDocument packSlip = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_packslip").withSourceDocumentType(SourceDocumentType.PackSlip)
				.withFrom(businessEntityToParty(supplier, PartyType.ShipFrom))
				.withTo(businessEntityToParty(testAccount, PartyType.ShipTo)).withLineItem(lineItem)
				.withProcurementGroup(pg1).withTransactionContext(tc2).build();

		clientOrder = sc.saveSourceDocument(clientOrder);
		supplierOrder = sc.saveSourceDocument(supplierOrder);
		packSlip = sc.saveSourceDocument(packSlip);

		SupplyChainSourceDocument supplierInvoice = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_supplier_invoice").withSourceDocumentType(SourceDocumentType.Invoice)
				.withFrom(businessEntityToParty(supplier, PartyType.Selling))
				.withTo(businessEntityToParty(awi, PartyType.Buying)).withLineItem(lineItem).withProcurementGroup(pg1)
				.withTransactionContext(tc2).build();

		SupplyChainSourceDocument clientInvoice = SupplyChainSourceDocumentImpl.builder()
				.withDocumentId("unit_testing_customer_invoice").withSourceDocumentType(SourceDocumentType.Invoice)
				.withFrom(businessEntityToParty(awi, PartyType.Selling))
				.withTo(businessEntityToParty(testAccount, PartyType.Buying)).withProcurementGroup(pg1)
				.withLineItem(lineItem).withTransactionContext(tc1).build();

		supplierInvoice = sc.saveSourceDocument(supplierInvoice);

		clientInvoice = sc.saveSourceDocument(clientInvoice);

	}

	private void deleteDoc(SourceDocumentClient sc, String docId) {
		Optional<SupplyChainSourceDocument> doc = sc.findSourceDocumentByDocumentId(docId);
		if (doc.isPresent()) {
			sc.delete(doc.get().getSupplyChainId());
		}
	}

	private SupplyChainParty businessEntityToParty(BusinessDetail business, PartyType partyType) {
		AccountImpl account = new AccountImpl();
		SupplyChainBusinessImpl entity = SupplyChainBusinessImpl.builder().withMoaBusinessId(business.getBusEntId())
				.build();
//		SupplyChainBusinessImpl entity = new SupplyChainBusinessImpl();
//		entity.setId(business.getBusEntId());
//		entity.setEntityType(EntityType.Business);
		account.setMember(entity);
		return SupplyChainParty.builder().fromAccount(account).withPartyType(partyType).build();
	}

	@Test
	public void requestHLWOrder() {
		CoordinatorClient coordinatorClient = new CoordinatorClient();
		coordinatorClient.withPort(8282).withLocalService();
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setAccountNumber("25");
		List<RequestItem> lineItems = new ArrayList<>();
		RequestItem item = new RequestItem();
		item.setPartNumber("C1");
		item.setLineCode("CCC");
		item.setQuantity(12);
		item.setLineNumber(1);
		lineItems.add(item);
		orderRequest.setLineItems(lineItems);
		orderRequest.getFulfillmentOptions().setServiceClass(ServiceClass.BestEffort);
		Object response = coordinatorClient.processOrder(orderRequest);
		PrettyPrint.print(response);
	}
	
	@Test
	public void createASN() {
		
		SourceDocumentClient sc = new SourceDocumentClient();
		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");

		Optional<SupplyChainSourceDocumentImpl> document = sc.findSourceDocumentByDocumentId(getQuoteDocumentId());
		
		String supplierDocumentId = null;
		if (document.get().getProcurementGroups() != null && !document.get().getProcurementGroups().isEmpty()) {

			for (ProcurementGroup pg : document.get().getProcurementGroups()) {
				Optional<SupplyChainSourceDocument> supplierPoSummary = pg.getSourceDocuments().stream()
						.filter(r -> r.getSourceDocumentType() == SourceDocumentType.PurchaseOrder).findAny();

				boolean hasSupplierPurchaseOrder = supplierPoSummary.isPresent();
				if (hasSupplierPurchaseOrder) {
					supplierDocumentId = supplierPoSummary.get().getDocumentId();
					document = sc.findSourceDocumentByDocumentId(supplierDocumentId);
				}

			}
		}
		
		EdiDocument doc = new EdiDocument();
		List<EdiRecord> records = new ArrayList<>();
		doc.setRecords(records);
		InterchangeControlHeaderISA isa = new InterchangeControlHeaderISA();
		doc.getRecords().add(isa.toEdiRecord());
		
//		BSNHeader bsn = new BSNHeader();
//		doc.getRecords().add(bsn.toEdiRecord());
		EdiRecord ediRecord = new EdiRecord("BSN*00*2308309*2025-01-03*1300*", "\\*");
//		bsn.fromEdiRecord(ediRecord);
		doc.getRecords().add(ediRecord);
		DateTimeReferenceDTM shipDate = new DateTimeReferenceDTM();
		shipDate.setDateTimeQualifier(DTMCode.Shipped);
		shipDate.setDate(LocalDate.now());
		shipDate.setTime(LocalTime.now());
		doc.getRecords().add(shipDate.toEdiRecord());
		DateTimeReferenceDTM estimatedDelivery = new DateTimeReferenceDTM();
		estimatedDelivery.setDateTimeQualifier(DTMCode.EstimatedDelivery);
		estimatedDelivery.setDate(LocalDate.now());
		estimatedDelivery.setTime(LocalTime.now());
		doc.getRecords().add(estimatedDelivery.toEdiRecord());
		//TODO Also test date with DTMCode.EstimatedArrivalDate 
		HierarchicalLevelHL hlShipped = new HierarchicalLevelHL();
		hlShipped.setIdNumber("1");
		hlShipped.setParentIdNumber("1");
		hlShipped.setLevelCode(HLLevelCode.Shipped);
		doc.getRecords().add(hlShipped.toEdiRecord());
		
		CarrierDetailTD5 carrierDetail = new CarrierDetailTD5();
		carrierDetail.setRoutingSequenceCode("B");
		carrierDetail.setIdentificationCodeIdentifier("2");
		carrierDetail.setIdentificationCode("UPS");
		carrierDetail.setTransportationTypeCode(TransportationTypeCode.MotorCommonCarrier);
		carrierDetail.setRouting("GROUND");
		doc.getRecords().add(carrierDetail.toEdiRecord());
		
		ReferenceIdentificationREF referenceIdentification = new ReferenceIdentificationREF();
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.TrackingNumber);
		referenceIdentification.setReferenceIdentification("38723423487");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.PackingList);
		referenceIdentification.setReferenceIdentification("592570");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.BillOfLading);
		referenceIdentification.setReferenceIdentification("592570");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.ProductGroup);
		referenceIdentification.setReferenceIdentification("GAT");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		TransportationInstructionsFOB shippingMethodOfPayment = new TransportationInstructionsFOB();
		shippingMethodOfPayment.setShippingPaymentMethod(ShipmentMethodOfPayment.PaidByBuyer);
		doc.getRecords().add(shippingMethodOfPayment.toEdiRecord());
		TransactionContext tc = document.get().getTransactionContext();
		NameN1 name = new NameN1();
		name.setEntityIdentifierCode("VN");
		name.setName("Gates Corporation");
		name.setIdentiferCodeQualifier("92");
		name.setIdentificationCode("007061617");
		doc.getRecords().add(name.toEdiRecord());
		
//		NameN2 name2 = new NameN2();
//		name2.setName("1234 Gates Drive SW");
		
		NameN3 name3 = new NameN3();
		name3.setAddressInformation1("1234 Gates Blvd.");
		doc.getRecords().add(name3.toEdiRecord());
		
		NameN4 name4 = new NameN4();
		name4.setCityName("Little Rock");
		name4.setStateProv("AR");
		name4.setPostalCode("99999");
		name4.setCountryCode("US");
		doc.getRecords().add(name4.toEdiRecord());
		
		PurchaseOrderReference prf = new PurchaseOrderReference();
		prf.setPurchaseOrderNumber(supplierDocumentId);
		prf.setDate("2025-01-07");
		doc.getRecords().add(prf.toEdiRecord());
		
		List<GenericLine> lineItems = document.get().getLineItems();
		for (GenericLine line : lineItems) {
			ItemIdentification itemIdentification = new ItemIdentification();
			itemIdentification.setLineNumber(1);
			itemIdentification.setBuyerPartNumber(line.getPartNumber());
			itemIdentification.setVendorPartNumber(line.getPartNumber());
			itemIdentification.setUpcNumber(line.getUpcNumber());
			itemIdentification.setManufacturerCode(line.getLineCode());
			doc.getRecords().add(itemIdentification.toEdiRecord());

			ItemDetail itemDetail = new ItemDetail();
			itemDetail.setItemStatusCode(ItemStatusCode.Shipped);
			itemDetail.setQuantity(line.getQuantity());
			itemDetail.setQuantityOrdered(line.getPurchasedQuantity());
			itemDetail.setQuantityOrderedUOM("EA");
			itemDetail.setQuantityShippedToDate(line.getShippedQuantity());
			itemDetail.setQuantityUOM("EA");
			doc.getRecords().add(itemDetail.toEdiRecord());

			ItemDescriptionPID pid = new ItemDescriptionPID();
			pid.setDescriptionCode(DescriptionCode.FreeForm);
			pid.setDescription("Part Description");
			doc.getRecords().add(pid.toEdiRecord());
		}
		TransactionTotalsCTT ctt = new TransactionTotalsCTT();
		ctt.setNumberofLineItems(lineItems.size());
		ctt.setHashTotal("1");
		doc.getRecords().add(ctt.toEdiRecord());
		
		try (FileOutputStream fout = new FileOutputStream("edi/myFile.ASN")) {
			fout.write(doc.toString().getBytes());
			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		System.out.println(doc.toString());
		
		
	}
	
	@Test
	public void createInvoice() {
		
		SourceDocumentClient sc = new SourceDocumentClient();
		sc.withDeleteTimoutDuration(Duration.ofSeconds(120l)).withServiceDomain("testconsul");

		Optional<SupplyChainSourceDocumentImpl> document = sc.findSourceDocumentByDocumentId(getQuoteDocumentId());
		
		String supplierDocumentId = null;
		if (document.get().getProcurementGroups() != null && !document.get().getProcurementGroups().isEmpty()) {

			for (ProcurementGroup pg : document.get().getProcurementGroups()) {
				Optional<SupplyChainSourceDocument> supplierPoSummary = pg.getSourceDocuments().stream()
						.filter(r -> r.getSourceDocumentType() == SourceDocumentType.PurchaseOrder).findAny();

				boolean hasSupplierPurchaseOrder = supplierPoSummary.isPresent();
				if (hasSupplierPurchaseOrder) {
					supplierDocumentId = supplierPoSummary.get().getDocumentId();
					document = sc.findSourceDocumentByDocumentId(supplierDocumentId);
				}

			}
		}
		
		EdiDocument doc = new EdiDocument();
		List<EdiRecord> records = new ArrayList<>();
		doc.setRecords(records);
		InterchangeControlHeaderISA isa = new InterchangeControlHeaderISA();
		doc.getRecords().add(isa.toEdiRecord());
		
		BIGHeader bigHeader = new BIGHeader();
//		bigHeader.setInvoiceDate(LocalDate.now());
//		bigHeader.setPoDate(LocalDate.now());
		bigHeader.setInvoiceId(UUID.randomUUID().toString());
		bigHeader.setPoNumber(supplierDocumentId);
		doc.getRecords().add(bigHeader.toEdiRecord());
		
		
		
		ReferenceIdentificationREF referenceIdentification = new ReferenceIdentificationREF();
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.TrackingNumber);
		referenceIdentification.setReferenceIdentification("38723423487");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.PackingList);
		referenceIdentification.setReferenceIdentification("592570");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.BillOfLading);
		referenceIdentification.setReferenceIdentification("592570");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		referenceIdentification.setReferenceIdentificationQualifier(REFCode.ProductGroup);
		referenceIdentification.setReferenceIdentification("GAT");
		doc.getRecords().add(referenceIdentification.toEdiRecord());
		
		TransportationInstructionsFOB shippingMethodOfPayment = new TransportationInstructionsFOB();
		shippingMethodOfPayment.setShippingPaymentMethod(ShipmentMethodOfPayment.PaidByBuyer);
		doc.getRecords().add(shippingMethodOfPayment.toEdiRecord());
		TransactionContext tc = document.get().getTransactionContext();
		NameN1 name = new NameN1();
		name.setEntityIdentifierCode("VN");
		name.setName("Gates Corporation");
		name.setIdentiferCodeQualifier("92");
		name.setIdentificationCode("007061617");
		doc.getRecords().add(name.toEdiRecord());
		
//		NameN2 name2 = new NameN2();
//		name2.setName("1234 Gates Drive SW");
		
		NameN3 name3 = new NameN3();
		name3.setAddressInformation1("1234 Gates Blvd.");
		doc.getRecords().add(name3.toEdiRecord());
		
		NameN4 name4 = new NameN4();
		name4.setCityName("Little Rock");
		name4.setStateProv("AR");
		name4.setPostalCode("99999");
		name4.setCountryCode("US");
		doc.getRecords().add(name4.toEdiRecord());
		
		InvoiceTermsDescriptionITD itd = new InvoiceTermsDescriptionITD();
		itd.setDescription("Terms per Alliance agreement");
		itd.setDeferredDueDate(LocalDate.now());
		itd.setDeferredPaymentDueDate(LocalDate.now());
		itd.setTermsBasisDateCode(TermsBasisDateCode.MutuallyDefined);
		itd.setDeferredInstallmentPaymentAmount(BigDecimal.ZERO);
		itd.setDeferredPaymentDueDate(LocalDate.now());
		itd.setDiscountAmount(BigDecimal.ZERO);
		itd.setDiscountDaysDue(0);
		itd.setDiscountPercent(BigDecimal.ZERO);
		itd.setNetDays(0);
		itd.setNetDueDate(LocalDate.now());
		itd.setTermsTypeCode(TermsTypeCode.Basic);
		doc.getRecords().add(itd.toEdiRecord());
		
		DateTimeReferenceDTM shipDate = new DateTimeReferenceDTM();
		shipDate.setDateTimeQualifier(DTMCode.Shipped);
		shipDate.setDate(LocalDate.now());
		shipDate.setTime(LocalTime.now());
		doc.getRecords().add(shipDate.toEdiRecord());
		
		TransportationInstructionsFOB fob = new TransportationInstructionsFOB();
		fob.setShippingPaymentMethod(ShipmentMethodOfPayment.PaidBySeller);
		doc.getRecords().add(fob.toEdiRecord());
		
		
		List<GenericLine> lineItems = document.get().getLineItems();
		for (GenericLine line : lineItems) {
			
			BaseItemIT1 it1 = new BaseItemIT1();
			it1.setLineNumber(line.getLineNumber());
			it1.setQuantity(line.getQuantity());
			it1.setBuyerPartNumber(line.getPartNumber());
			it1.setVendorPartNumber(line.getPartNumber());
			it1.setUnitOfMeasure("EA");
			it1.setPrice(BigDecimal.TEN);
			doc.getRecords().add(it1.toEdiRecord());

			ItemDescriptionPID pid = new ItemDescriptionPID();
			pid.setDescriptionCode(DescriptionCode.FreeForm);
			pid.setDescription("Part Description");
			doc.getRecords().add(pid.toEdiRecord());
		}
		
		CarrierDetailCAD cad = new CarrierDetailCAD();
		cad.setTransportMethodTypeCode("M");
		cad.setStandardCarrierCode("UPS");
		cad.setRouting("UPS Ground");
		doc.getRecords().add(cad.toEdiRecord());
		
		TransactionTotalsCTT ctt = new TransactionTotalsCTT();
		ctt.setNumberofLineItems(lineItems.size());
		ctt.setHashTotal("1");
		doc.getRecords().add(ctt.toEdiRecord());
		
		ServicePromotionAllowanceSAC sac = new ServicePromotionAllowanceSAC();
		sac.setAllowanceOrCharge(AllowanceChargeCode.Charge);
		sac.setChargeCode("D240");
		sac.setAmount("2500");
		sac.setChargeDescription("FREIGHT");
		doc.getRecords().add(sac.toEdiRecord());
		
		sac = new ServicePromotionAllowanceSAC();
		sac.setAllowanceOrCharge(AllowanceChargeCode.Charge);
		sac.setChargeCode("H000");
		sac.setAmount("500");
		sac.setChargeDescription("Shipping");
		doc.getRecords().add(sac.toEdiRecord());
		
		try (FileOutputStream fout = new FileOutputStream("edi/myFile.INV")) {
			fout.write(doc.toString().getBytes());
			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		System.out.println(doc.toString());
		
		
		
		
	}
	
	
	

	public String saveQuoteDocumentId(String quoteDocumentId) {
		try (FileOutputStream fout = new FileOutputStream(QUOTE_DOCUMENT_ID_FILE_NAME)) {
			fout.write(quoteDocumentId.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return quoteDocumentId;
	}
	
	public String getQuoteDocumentId() {
		try (FileInputStream fin = new FileInputStream(QUOTE_DOCUMENT_ID_FILE_NAME)) {
			byte[] buffer = new byte[1024];
			fin.read(buffer);
			return new String(buffer).trim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Failed to read file");
	}

}
