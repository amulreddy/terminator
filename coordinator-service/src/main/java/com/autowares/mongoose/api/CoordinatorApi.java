package com.autowares.mongoose.api;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.autowares.mongoose.camel.components.QuoteContextTypeConverter;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.service.LockedDocuments;
import com.autowares.servicescommon.model.DocumentModification;
import com.autowares.servicescommon.model.LineItemModification;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.xmlgateway.model.BasicInquiryRequest;
import com.autowares.xmlgateway.model.InquiryRequest;
import com.autowares.xmlgateway.model.InquiryResponse;
import com.autowares.xmlgateway.model.OrderRequest;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(value = { "/coordinator"} )
public class CoordinatorApi {

	@Produce("direct:api")
	private ProducerTemplate template;

	@Produce(value = "direct:unprocessedOrders")
	private ProducerTemplate unprocessedTemplate;

	@Produce(value = "direct:inquiry")
	private ProducerTemplate inquiryTemplate;

	@Produce(value = "direct:saveInitialQuote")
	private ProducerTemplate quoteTemplate;

	@Produce(value = "direct:processQuote")
	private ProducerTemplate processQuoteTemplate;

	@Produce(value = "direct:cancel")
	private ProducerTemplate cancelTemplate;

	@Produce(value = "direct:generate-invoice")
	private ProducerTemplate generateInvoiceTemplate;

	@Produce(value = "direct:ediInput")
	private ProducerTemplate ediInput;

	@Autowired
	private ProducerTemplate producerTemplate;

	@PostMapping(path = "/inquireSimple")
	@Operation(summary = "Makes a basic inquiry request")
	public InquiryResponse inquireSimple(@RequestBody BasicInquiryRequest request) {
		CoordinatorContext context = (CoordinatorContext) inquiryTemplate.requestBody(inquiryTemplate.getDefaultEndpoint(), request);
		return QuoteContextTypeConverter.coordinatorContextToInquiryResponse(context);
	}

	@PostMapping(path = "/inquire")
	@Operation(summary = "Makes a full inquiry request")
	public InquiryResponse inquire(@RequestBody InquiryRequest request) {
		CoordinatorContext context = (CoordinatorContext) inquiryTemplate.requestBody(inquiryTemplate.getDefaultEndpoint(), request);
		return QuoteContextTypeConverter.coordinatorContextToInquiryResponse(context);
	}

	@PostMapping(path = "/processOrder")
	@Operation(summary = "Places an order")
	public Object processOrder(@RequestBody OrderRequest request)
			throws InterruptedException, ExecutionException, IOException {
		CompletableFuture<Object> f = template.asyncSendBody(template.getDefaultEndpoint(), request);
		CoordinatorContext context = (CoordinatorContext) f.get();
		return context.getReferenceDocument();
	}

	@PostMapping(path = "/processEdiOrder")
	@Operation(summary = "Places an EDI order")
	public String processEdiOrder(@RequestBody OrderRequest request)
			throws InterruptedException, ExecutionException, IOException {
		CompletableFuture<Object> f = ediInput.asyncSendBody(ediInput.getDefaultEndpoint(), request);
		String string = (String) f.get();
		return string;
	}

	@GetMapping(path = "/processUnprocessedOrders")
	@Operation(summary = "Processes all unprocessed orders, or requests for a specific order to be processed")
	public String processUnProcessedOrder(@RequestParam(name = "xmlOrderId", required = false) Long xmlOrderId) {
		producerTemplate.start();
		String context = producerTemplate.requestBody("direct:unprocessedOrders", xmlOrderId, String.class);
		producerTemplate.stop();
		return context;
	}

	@PostMapping(path = "/quote")
	@Operation(summary = "Requests a quote")
	public InquiryResponse quote(@RequestBody InquiryRequest request)
			throws InterruptedException, ExecutionException, IOException {
		CompletableFuture<Object> f = quoteTemplate.asyncSendBody(quoteTemplate.getDefaultEndpoint(), request);
		CoordinatorContext context = (CoordinatorContext) f.get();
		return QuoteContextTypeConverter.coordinatorContextToInquiryResponse(context);
	}

	@GetMapping(path = "/processDocument")
	@Operation(summary = "Requests the reprocessing of a specific document (with the additional option of treating the document as if it was being processed at a specific time)")
	public String processDocument(
			@RequestParam(name = "documentId", required = true) String documentId,
			@RequestParam(name = "asOf", required = false) @DateTimeFormat(iso=ISO.DATE_TIME) ZonedDateTime asOf) {
		producerTemplate.start();
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("processAsOf", asOf);
		map.put("forcedProcessing", true);
		String context = producerTemplate.requestBodyAndHeaders("direct:unprocessedDocuments", documentId, map , String.class);
		producerTemplate.stop();
		return context;
	}

	@GetMapping(path = "/currentDocuments")
	@Operation(summary = "Requests a list of all documents that are currently processing")
	public Set<String> currentDocuments() {
		return LockedDocuments.getDocuments().stream().map(
				i -> i.getAssetId() + " processing: " + Duration.between(i.getLockTimestamp(), ZonedDateTime.now()))
				.collect(Collectors.toSet());
	}

	@PostMapping(path = "/processQuote")
	@Operation(summary = "Requests a quote for an Inquiry")
	public SupplyChainSourceDocument processQuote(@RequestBody InquiryRequest request)
			throws InterruptedException, ExecutionException, IOException {
		request.getInquiryOptions().getLocationTypes().add(LocationType.Vendor);
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("forcedProcessing", true);
		CoordinatorContext context = processQuoteTemplate.requestBodyAndHeaders(processQuoteTemplate.getDefaultEndpoint(),
				request, map, CoordinatorContext.class);
		return (SupplyChainSourceDocument) context.getReferenceDocument();
	}

	@PostMapping(path = "/processQuoteDocument")
	@Operation(summary = "Requests a quote for a Document")
	public SupplyChainSourceDocument processQuote(@RequestBody SourceDocument request)
			throws InterruptedException, ExecutionException, IOException {
		CompletableFuture<Object> f = processQuoteTemplate.asyncSendBody(processQuoteTemplate.getDefaultEndpoint(),
				request);
		CoordinatorContext context = (CoordinatorContext) f.get();
		return (SupplyChainSourceDocument) context.getReferenceDocument();
	}

	@PostMapping(path = "/cancel")
	@Operation(summary = "Requests the cancellation of particular line items in the given pack slip document")
	public DocumentModification cancel(@RequestBody DocumentModification request) throws InterruptedException, ExecutionException {
		DocumentContext context = new DocumentContext(request);
		context = cancelTemplate.requestBody(cancelTemplate.getDefaultEndpoint(), context, DocumentContext.class);
		DocumentModification response = new DocumentModification();
		response.setDocumentId(context.getDocumentId());
		response.setAction(context.getAction());
		for (LineItemContextImpl l : context.getLineItems()) {
			LineItemModification line= new LineItemModification(l);
			line.setSuccess(true);
			response.getLineItems().add(line);
		}
		return response;
	}

	@PostMapping(path = "/generateInvoice")
	@Operation(summary = "Requests an invoice to be generated")
	public void generateInvoice(@RequestBody DocumentModification request) throws InterruptedException, ExecutionException {
		DocumentContext context = new DocumentContext(request);
		CompletableFuture<Object> f = generateInvoiceTemplate.asyncSendBody(generateInvoiceTemplate.getDefaultEndpoint(),
				context);
		f.get();
	}
}
