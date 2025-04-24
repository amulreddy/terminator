package com.autowares.mongoose.model;

import java.util.ArrayList;
import java.util.List;

import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.autowares.supplychain.model.TransactionContext;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TransactionalContext extends TransactionContext {
	
	@JsonIgnore
	private SupplyChainSourceDocument quote;
	@JsonIgnore
	private CoordinatorContext quoteContext;
	@JsonIgnore
	private SupplyChainSourceDocument order;
	@JsonIgnore
	private CoordinatorContext orderContext;
	@JsonIgnore
	private List<SupplyChainSourceDocument> packslips = new ArrayList<>();
	@JsonIgnore
	private List<SupplyChainSourceDocument> invoices = new ArrayList<>();
	@JsonIgnore
	private List<SupplyChainSourceDocument> shippingEstimates = new ArrayList<>();
	@JsonIgnore
	private SupplyChainSourceDocument transactionalShortage;
	@JsonIgnore
	private DocumentContext documentContext;
	private boolean isResolved = false;
	
	public TransactionalContext() {
	}
	
	public TransactionalContext(TransactionContext context) {
		super(context);
	}

	public SupplyChainSourceDocument getQuote() {
		return quote;
	}

	public void setQuote(SupplyChainSourceDocument quote) {
		this.quote = quote;
	}

	public CoordinatorContext getQuoteContext() {
		return quoteContext;
	}

	public void setQuoteContext(CoordinatorContext quoteContext) {
		this.quoteContext = quoteContext;
	}

	public SupplyChainSourceDocument getOrder() {
		return order;
	}

	public void setOrder(SupplyChainSourceDocument order) {
		this.order = order;
	}

	public CoordinatorContext getOrderContext() {
		return orderContext;
	}

	public void setOrderContext(CoordinatorContext orderContext) {
		this.orderContext = orderContext;
	}

	public List<SupplyChainSourceDocument> getPackslips() {
		return packslips;
	}

	public void setPackslips(List<SupplyChainSourceDocument> packslips) {
		this.packslips = packslips;
	}

	public List<SupplyChainSourceDocument> getInvoices() {
		return invoices;
	}

	public void setInvoices(List<SupplyChainSourceDocument> invoices) {
		this.invoices = invoices;
	}

	public List<SupplyChainSourceDocument> getShippingEstimates() {
		return shippingEstimates;
	}

	public void setShippingEstimates(List<SupplyChainSourceDocument> shippingEstimates) {
		this.shippingEstimates = shippingEstimates;
	}

	public SupplyChainSourceDocument getTransactionalShortage() {
		return transactionalShortage;
	}

	public void setTransactionalShortage(SupplyChainSourceDocument transactionalShortage) {
		this.transactionalShortage = transactionalShortage;
	}

	public DocumentContext getDocumentContext() {
		return documentContext;
	}

	public void setDocumentContext(DocumentContext documentContext) {
		this.documentContext = documentContext;
	}
	
	public boolean isResolved() {
		return isResolved;
	}
	
	public void setResolved(boolean isResolved) {
		this.isResolved = isResolved;
	}
	

}
