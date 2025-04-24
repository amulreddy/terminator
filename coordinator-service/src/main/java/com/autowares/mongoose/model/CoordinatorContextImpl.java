package com.autowares.mongoose.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.servicescommon.model.BillingOptions;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.InquiryOptions;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.TransactionContext;

public class CoordinatorContextImpl implements CoordinatorContext {

	private Long customerNumber;
	private BusinessContext businessContext = new BusinessContext();
	private List<LineItemContextImpl> lineItems = new ArrayList<>();
	private LinkedHashSet<FulfillmentLocationContext> fulfillmentSequence = new LinkedHashSet<>();
	private FulfillmentOptions fulfillmentOptions = new FulfillmentOptions();
	private BillingOptions billingOptions = new BillingOptions();
	private InquiryOptions inquiryOptions = new InquiryOptions();
	private ZonedDateTime requestTime = ZonedDateTime.now();
	private String documentId;
	private String customerDocumentId;
	private ZonedDateTime transactionTime;
	private String supplierDocumentId;
	private String supplierMessage;
	private List<ProcurementGroup> procurementGroups = new ArrayList<>();
	private TransactionStatus transactionStatus = TransactionStatus.Processing;
	private TransactionStage transactionStage = TransactionStage.New;
	private List<SupplyChainNote> notes = new ArrayList<>();
	private PurchaseOrderType orderType = PurchaseOrderType.PurchaseOrder;
	private Object referenceDocument;
	private BusinessContext supplier;
	private CoordinatorProcessingPhase processingStage = CoordinatorProcessingPhase.One;
	private BusinessContext shipTo;
	private SystemType systemType;
	private ServiceClass serviceClass;
	private DeliveryMethod deliveryMethod;
	private SourceDocumentType sourceDocumentType = SourceDocumentType.Quote;
	private Configuration configuration;
	private FreightOptions freightOptions;
	private CoordinatorContext sourceContext;
	private List<CoordinatorContext> relatedContexts = new ArrayList<>();
	private ProcurementGroupContext procurementGroupContext;
	private TransactionalContext transactionalContext;
	private boolean isResolved = false;
	private Collection<ChargesImpl> chargesAndDiscounts = new ArrayList<>();

	public BusinessContext getShipTo() {
		return shipTo;
	}

	public void setShipTo(BusinessContext shipTo) {
		this.shipTo = shipTo;
	}

	public CoordinatorContextImpl() {
	}

	public CoordinatorContextImpl(CoordinatorContext context) {
		this.customerNumber = context.getCustomerNumber();
		this.businessContext = context.getBusinessContext();
		this.lineItems = context.getLineItems();
		for (LineItemContext l : lineItems) {
			l.setContext(this);
		}
		this.fulfillmentSequence = context.getFulfillmentSequence();
		for (FulfillmentLocationContext f : context.getFulfillmentSequence()) {
			f.setOrder(this);
		}
		this.fulfillmentOptions = context.getFulfillmentOptions();
		this.billingOptions = context.getBillingOptions();
		this.inquiryOptions = context.getInquiryOptions();
		this.requestTime = context.getRequestTime();
		this.documentId = context.getDocumentId();
		this.customerDocumentId = context.getCustomerDocumentId();
		this.transactionTime = context.getTransactionTimeStamp();
		this.procurementGroups = context.getProcurementGroups();
		this.transactionalContext = context.getTransactionContext();
		this.notes = context.getNotes();
		this.referenceDocument = context.getReferenceDocument();
		this.supplier = context.getSupplier();
		this.processingStage = context.getProcessingPhase();
		this.shipTo = context.getShipTo();
		this.serviceClass = context.getServiceClass();
		this.deliveryMethod = context.getDeliveryMethod();
		this.sourceDocumentType = context.getSourceDocumentType();
		this.sourceContext = context.getSourceContext();
		this.relatedContexts = context.getRelatedContexts();
		this.configuration = context.getConfiguration();
		this.procurementGroupContext = context.getProcurementGroupContext();
		this.isResolved = context.isResolved();
		this.orderType = context.getOrderType();
		this.freightOptions = context.getFreightOptions();
	}

	@Override
	public BusinessContext getBusinessContext() {
		return businessContext;
	}

	public void setBusinessContext(BusinessContext businessContext) {
		this.businessContext = businessContext;
	}

	@Override
	public List<LineItemContextImpl> getLineItems() {
		return lineItems;
	}

	@Override
	public void setLineItems(List<LineItemContextImpl> lineItems) {
		this.lineItems = lineItems;
	}

	@Override
	public Long getCustomerNumber() {
		return this.customerNumber;
	}

	public void setCustomerNumber(Long customerNumber) {
		this.customerNumber = customerNumber;
	}

	@Override
	public LinkedHashSet<FulfillmentLocationContext> getFulfillmentSequence() {
		return this.fulfillmentSequence;
	}

	@Override
	public void setFulfillmentSequence(LinkedHashSet<FulfillmentLocationContext> sequence) {
		this.fulfillmentSequence = sequence;
	}

	@Override
	public ServiceClass getServiceClass() {
		return serviceClass;
	}

	@Override
	public DeliveryMethod getDeliveryMethod() {
		return deliveryMethod;
	}

	public ZonedDateTime getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(ZonedDateTime requestTime) {
		this.requestTime = requestTime;
	}

	public InquiryOptions getInquiryOptions() {
		return inquiryOptions;
	}

	public void setInquiryOptions(InquiryOptions inquiryOptions) {
		this.inquiryOptions = inquiryOptions;
	}

	public FulfillmentOptions getFulfillmentOptions() {
		return fulfillmentOptions;
	}

	public void setFulfillmentOptions(FulfillmentOptions fulfillmentOptions) {
		this.fulfillmentOptions = fulfillmentOptions;
	}

	public BillingOptions getBillingOptions() {
		return billingOptions;
	}

	public void setBillingOptions(BillingOptions billingOptions) {
		this.billingOptions = billingOptions;
	}

	@Override
	public SourceDocumentType getSourceDocumentType() {
		return this.sourceDocumentType;
	}

	@Override
	public String getDocumentId() {
		return documentId;
	}

	@Override
	public LocalDate getTransactionDate() {
		if (transactionTime != null) {
			return transactionTime.toLocalDate();
		}
		return null;
	}

	@Override
	public LocalTime getTransactionTime() {
		if (transactionTime != null) {
			return transactionTime.toLocalTime();
		}
		return null;
	}

	@Override
	public Number getTransactionAmount() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number getTotalPieces() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessContext getTo() {
		return businessContext;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public void setTransactionTime(ZonedDateTime transactionTime) {
		this.transactionTime = transactionTime;
	}

	public String getSupplierDocumentId() {
		return supplierDocumentId;
	}

	public void setSupplierDocumentId(String supplierDocumentId) {
		this.supplierDocumentId = supplierDocumentId;
	}

	public String getSupplierMessage() {
		return supplierMessage;
	}

	public void setSupplierMessage(String supplierMessage) {
		this.supplierMessage = supplierMessage;
	}

	@Override
	public List<ProcurementGroup> getProcurementGroups() {
		return (List<ProcurementGroup>) procurementGroups;
	}

	@Override
	public void setProcurementGroups(List<ProcurementGroup> procurementGroups) {
		this.procurementGroups = procurementGroups;
	}

	public TransactionalContext getTransactionContext() {
		return transactionalContext;
	}

	public void setTransactionContext(TransactionContext transactionContext) {
		if (transactionContext instanceof TransactionalContext) {
			this.transactionalContext = (TransactionalContext) transactionContext;
		} else {
			this.transactionalContext = new TransactionalContext(transactionContext);
		}
	}

	public TransactionStatus getTransactionStatus() {
		return transactionStatus;
	}

	public void setTransactionStatus(TransactionStatus transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	public TransactionStage getTransactionStage() {
		return transactionStage;
	}

	public void setTransactionStage(TransactionStage transactionStage) {
		this.transactionStage = transactionStage;
	}

	public List<SupplyChainNote> getNotes() {
		return notes;
	}

	public void setNotes(List<SupplyChainNote> notes) {
		this.notes = notes;
	}

	public PurchaseOrderType getOrderType() {
		return orderType;
	}

	public void setOrderType(PurchaseOrderType orderType) {
		this.orderType = orderType;
	}

	public Object getReferenceDocument() {
		return referenceDocument;
	}

	public void setReferenceDocument(Object referenceDocument) {
		this.referenceDocument = referenceDocument;
	}

	public BusinessContext getSupplier() {
		return supplier;
	}

	public void setSupplier(BusinessContext supplier) {
		this.supplier = supplier;
	}

	public CoordinatorProcessingPhase getProcessingPhase() {
		return processingStage;
	}

	public void setProcessingPhase(CoordinatorProcessingPhase processingStage) {
		this.processingStage = processingStage;
	}

	@Override
	public BusinessContext getFrom() {
		return getSupplier();
	}

	public SystemType getSystemType() {
		return systemType;
	}

	public void setSystemType(SystemType systemType) {
		this.systemType = systemType;
	}

	public void setProcessingStage(CoordinatorProcessingPhase processingStage) {
		this.processingStage = processingStage;
	}

	public void setServiceClass(ServiceClass serviceClass) {
		this.serviceClass = serviceClass;
	}

	public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
		this.deliveryMethod = deliveryMethod;
	}

	public void setSourceDocumentType(SourceDocumentType sourceDocumentType) {
		this.sourceDocumentType = sourceDocumentType;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public FreightOptions getFreightOptions() {
		return freightOptions;
	}

	public void setFreightOptions(FreightOptions freightOptions) {
		this.freightOptions = freightOptions;
	}

	public CoordinatorContext getSourceContext() {
		return sourceContext;
	}

	public void setSourceContext(CoordinatorContext sourceContext) {
		this.sourceContext = sourceContext;
	}

	@Override
	public List<CoordinatorContext> getRelatedContexts() {
		return relatedContexts;
	}

	public void setRelatedContexts(List<CoordinatorContext> relatedContexts) {
		this.relatedContexts = relatedContexts;
	}

	public String getCustomerDocumentId() {
		return customerDocumentId;
	}

	public void setCustomerDocumentId(String customerDocumentId) {
		this.customerDocumentId = customerDocumentId;
	}

	public ProcurementGroupContext getProcurementGroupContext() {
		return procurementGroupContext;
	}

	public void setProcurementGroupContext(ProcurementGroupContext procurementGroupContext) {
		this.procurementGroupContext = procurementGroupContext;
	}

	public boolean isResolved() {
		return isResolved;
	}

	public void setResolved(boolean isResolved) {
		this.isResolved = isResolved;
	}

	@Override
	public void setNotes(ArrayList<SupplyChainNote> notes) {
		this.notes = notes;
	}

	@Override
	public Collection<ChargesImpl> getChargesAndDiscounts() {
		return chargesAndDiscounts;
	}

	public void setChargesAndDiscounts(Collection<ChargesImpl> chargesAndDiscounts) {
		this.chargesAndDiscounts = chargesAndDiscounts;
	}

}
