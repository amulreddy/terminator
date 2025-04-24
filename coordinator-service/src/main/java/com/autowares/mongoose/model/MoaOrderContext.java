package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.autowares.orders.model.Item;
import com.autowares.orders.model.Order;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.servicescommon.model.Account;
import com.autowares.servicescommon.model.AccountImpl;
import com.autowares.servicescommon.model.BillingOptions;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.InquiryOptions;
import com.autowares.servicescommon.model.OrderSource;
import com.autowares.servicescommon.model.Party;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.TransactionContext;

public class MoaOrderContext implements CoordinatorOrderContext {

	private Order order;
	private List<LineItemContextImpl> orderDetails = new ArrayList<>();
	private LinkedHashSet<FulfillmentLocationContext> fulfillmentSequence = new LinkedHashSet<>();
	private FulfillmentOptions fulfillmentOptions = new FulfillmentOptions();
	private BillingOptions billingOptions = new BillingOptions();
	private InquiryOptions inquiryOptions = new InquiryOptions();
	private Boolean psxToBeDelivered = false;
	private PurchaseOrderType orderType = PurchaseOrderType.PurchaseOrder;
	private Boolean invalid = false;
	private BusinessContext businessContext = new BusinessContext();
	private String documentId;
	private List<ProcurementGroup> procurementGroups = new ArrayList<>();
	private TransactionStatus transactionStatus;
	private TransactionStage transactionStage;
	private TransactionalContext transactionContext;
	private List<SupplyChainNote> notes = new ArrayList<>();
	private Object referenceDocument;
	private BusinessContext supplier;
	private CoordinatorProcessingPhase processingStage = CoordinatorProcessingPhase.One;
	private String supplierDocumentId;
	private BusinessContext shipTo;
	private SystemType systemType;
	private ServiceClass serviceClass;
	private DeliveryMethod deliveryMethod;
	private Account buyingAccount;
	private Party sellingParty;
	private BigDecimal transactionAmount;
	private Integer totalPieces;
	private OrderSource orderSource;
	private Configuration configuration;
	private FreightOptions freightOptions;
	private CoordinatorContext sourceContext;
	private List<CoordinatorContext> relatedContexts = new ArrayList<>();
	private ProcurementGroupContext procurementGroupContext;
	private boolean isResolved = false;
	private Collection<ChargesImpl> chargesAndDiscounts = new ArrayList<>();

	public BusinessContext getShipTo() {
		return shipTo;
	}

	public void setShipTo(BusinessContext shipTo) {
		this.shipTo = shipTo;
	}

	public MoaOrderContext(Order order) {
		this.order = order;
		AccountImpl account = new AccountImpl();
		account.setAccountNumber(order.getCustomerNumber());
		this.buyingAccount = account;
		Map<Integer, List<Item>> lineMap = order.getItems().stream()
				.collect(Collectors.groupingBy(Item::getCustomerLineNumber));

		for (Integer lineNumber : lineMap.keySet()) {
			MoaOrderLineItemContext detailContext = new MoaOrderLineItemContext(this, lineMap.get(lineNumber));
			orderDetails.add(new LineItemContextImpl(detailContext));
		}

		this.documentId = String.valueOf(order.getXmlOrderId());
	}

	@Override
	public Account getBuyingAccount() {
		return this.buyingAccount;
	}

	@Override
	public Party getSellingParty() {
		return this.sellingParty;
	}

	@Override
	public LocalDate getTransactionDate() {
		return order.getOrderTime().toLocalDate();
	}

	@Override
	public BigDecimal getTransactionAmount() {
		return this.transactionAmount;
	}

	@Override
	public Integer getTotalPieces() {
		return this.totalPieces;
	}

	@Override
	public String getXmlOrderId() {
		return order.getXmlOrderId();
	}

	@Override
	public Long getCustomerNumber() {
		if (order.getCustomerNumber() != null) {
			return Long.valueOf(order.getCustomerNumber());
		}
		return null;
	}

	@Override
	public String getPurchaseOrder() {
		return order.getPurchaseOrderNumber();
	}

	@Override
	public void setPurchaseOrder(String purchaseOrder) {
		order.setPurchaseOrderNumber(purchaseOrder);
	}

	@Override
	public ZonedDateTime getOrderTime() {
		return ZonedDateTime.from(order.getOrderTime());
	}

	@Override
	public DeliveryMethod getDeliveryMethod() {
		return this.deliveryMethod;
	}

	@Override
	public OrderSource getOrderSource() {
		return this.orderSource;
	}

	@Override
	public ServiceClass getServiceClass() {
		return this.serviceClass;
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
		return orderDetails;
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
	public LocalTime getTransactionTime() {
		return order.getOrderTime().toLocalTime();
	}

	@Override
	public Long getSourceOrderId() {
		return order.getSourceOrderId();
	}

	@Override
	public Boolean getPsxToBeDelivered() {
		return psxToBeDelivered;
	}

	public void setPsxToBeDelivered(Boolean psxToBeDelivered) {
		this.psxToBeDelivered = psxToBeDelivered;
	}

	@Override
	public String getDocumentId() {
		return this.documentId;
	}

	@Override
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	@Override
	public PurchaseOrderType getOrderType() {
		return orderType;
	}

	@Override
	public void setOrderType(PurchaseOrderType orderType) {
		this.orderType = orderType;
	}

	@Override
	public Boolean getInvalid() {
		return invalid;
	}

	@Override
	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}

	public Order getOrder() {
		return order;
	}

	public void setLineItems(List<LineItemContextImpl> orderDetails) {
		this.orderDetails = orderDetails;
	}

	@Override
	public ZonedDateTime getRequestTime() {
		return getOrderTime();
	}

	@Override
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
	public InquiryOptions getInquiryOptions() {
		return inquiryOptions;
	}

	public void setInquiryOptions(InquiryOptions inquiryOptions) {
		this.inquiryOptions = inquiryOptions;
	}

	@Override
	public List<ProcurementGroup> getProcurementGroups() {
		return procurementGroups;
	}

	@Override
	public void setProcurementGroups(List<ProcurementGroup> procurementGroups) {
		this.procurementGroups = procurementGroups;
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

	public TransactionalContext getTransactionContext() {
		return transactionContext;
	}

	public void setTransactionContext(TransactionContext transactionContext) {
		this.transactionContext = new TransactionalContext(transactionContext);
	}

	public List<SupplyChainNote> getNotes() {
		return notes;
	}

	public void setNotes(List<SupplyChainNote> notes) {
		this.notes = notes;
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

	public String getSupplierDocumentId() {
		return supplierDocumentId;
	}

	public void setSupplierDocumentId(String supplierDocumentId) {
		this.supplierDocumentId = supplierDocumentId;
	}

	@Override
	public Party getFrom() {
		return getSupplier();
	}

	@Override
	public Party getTo() {
		return getBusinessContext();
	}

	public SystemType getSystemType() {
		return systemType;
	}

	public List<LineItemContextImpl> getOrderDetails() {
		return orderDetails;
	}

	public void setOrderDetails(List<LineItemContextImpl> orderDetails) {
		this.orderDetails = orderDetails;
	}

	public CoordinatorProcessingPhase getProcessingStage() {
		return processingStage;
	}

	public void setProcessingStage(CoordinatorProcessingPhase processingStage) {
		this.processingStage = processingStage;
	}

	public void setServiceClass(ServiceClass serviceClass) {
		this.serviceClass = serviceClass;
	}

	public void setSystemType(SystemType systemType) {
		this.systemType = systemType;
	}

	@Override
	public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
		this.deliveryMethod = deliveryMethod;
	}

	public void setBuyingAccount(Account buyingAccount) {
		this.buyingAccount = buyingAccount;
	}

	public void setSellingParty(Party sellingParty) {
		this.sellingParty = sellingParty;
	}

	public void setTransactionAmount(BigDecimal transactionAmount) {
		this.transactionAmount = transactionAmount;
	}

	public void setTotalPieces(Integer totalPieces) {
		this.totalPieces = totalPieces;
	}

	public void setOrderSource(OrderSource orderSource) {
		this.orderSource = orderSource;
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

	public List<CoordinatorContext> getRelatedContexts() {
		return relatedContexts;
	}

	public void setRelatedContexts(List<CoordinatorContext> relatedContexts) {
		this.relatedContexts = relatedContexts;
	}

	@Override
	public String getCustomerDocumentId() {
		if (order != null) {
			return order.getPurchaseOrderNumber();
		}
		return null;
	}

	@Override
	public void setCustomerDocumentId(String supplierDocumentId) {
		if (order != null) {
			order.setPurchaseOrderNumber(supplierDocumentId);
		}

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
		return this.chargesAndDiscounts;
	}

	public void setChargesAndDiscounts(Collection<ChargesImpl> chargesAndDiscounts) {
		this.chargesAndDiscounts = chargesAndDiscounts;
	}

}
