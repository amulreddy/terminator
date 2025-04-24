package com.autowares.mongoose.model;

import java.util.ArrayList;
import java.util.List;

import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.DocumentModification;
import com.autowares.servicescommon.model.LineItemModification;
import com.autowares.servicescommon.model.SourceDocument;

public class DocumentContext implements Document {

	private String documentId;
	private CoordinatorContext context;
	private List<LineItemContextImpl> lineItems = new ArrayList<>();
	private SourceDocument sourceDocument;
	private FulfillmentLocationContext fulfillmentLocationContext;
	private String action;
	private TransactionalContext transactionalContext;
	private List<ProcurementGroupContext> procurementGroupContexts = new ArrayList<>();
	private boolean isResolved = false;
	
	public DocumentContext() {
		super();
	}
	
	public DocumentContext(Document d) {
		this.documentId = d.getDocumentId();
	}
	
	public DocumentContext(DocumentModification d) {
		this.setDocumentId(d.getDocumentId());
		if (d.getLineItems() !=null) {
			for (LineItemModification li : d.getLineItems()) {
				LineItemContextImpl lineItemContext = new LineItemContextImpl(li);
				lineItemContext.setLineNumber(li.getLineNumber());
				lineItemContext.setQuantity(li.getQuantity());
				this.getLineItems().add(lineItemContext);
			}
		}
		this.setAction(d.getAction());
	}

	public CoordinatorContext getContext() {
		return context;
	}

	public void setContext(CoordinatorContext context) {
		this.context = context;
	}

	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}

	public void setSourceDocument(SourceDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
	}

	public FulfillmentLocationContext getFulfillmentLocationContext() {
		return fulfillmentLocationContext;
	}

	public void setFulfillmentLocationContext(FulfillmentLocationContext fulfillmentLocationContext) {
		this.fulfillmentLocationContext = fulfillmentLocationContext;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public List<LineItemContextImpl> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItemContextImpl> lineItems) {
		this.lineItems = lineItems;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public TransactionalContext getTransactionalContext() {
		return transactionalContext;
	}

	public void setTransactionalContext(TransactionalContext transactionalContext) {
		this.transactionalContext = transactionalContext;
	}

	public List<ProcurementGroupContext> getProcurementGroupContexts() {
		return procurementGroupContexts;
	}

	public void setProcurementGroupContexts(List<ProcurementGroupContext> procurementGroupContexts) {
		this.procurementGroupContexts = procurementGroupContexts;
	}

	public boolean isResolved() {
		return isResolved;
	}

	public void setResolved(boolean isResolved) {
		this.isResolved = isResolved;
	}



}
