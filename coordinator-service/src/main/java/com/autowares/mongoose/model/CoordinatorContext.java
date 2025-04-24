package com.autowares.mongoose.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.servicescommon.model.Account;
import com.autowares.servicescommon.model.BillingOptions;
import com.autowares.servicescommon.model.BusinessLocationImpl;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.FulfillmentOptions;
import com.autowares.servicescommon.model.InquiryOptions;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.Party;
import com.autowares.servicescommon.model.PartyImpl;
import com.autowares.servicescommon.model.PartyType;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SourceDocument;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.WorkingDocument;
import com.autowares.supplychain.model.ProcurementGroup;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.TransactionContext;

public interface CoordinatorContext extends WorkingDocument, SourceDocument {
	
	Long getCustomerNumber();

	FulfillmentOptions getFulfillmentOptions();
	
	BillingOptions getBillingOptions();

	InquiryOptions getInquiryOptions();

	ServiceClass getServiceClass();
	
	void setServiceClass(ServiceClass serviceClass);

	DeliveryMethod getDeliveryMethod();
	
	void setDeliveryMethod(DeliveryMethod deliveryMethod);
	
	ZonedDateTime getRequestTime();

	List<ProcurementGroup> getProcurementGroups();
	void setProcurementGroups(List<ProcurementGroup> procurementGroups);
	
	TransactionalContext getTransactionContext();
	void setTransactionContext(TransactionContext transactionContext);

	// Customer Details
	BusinessContext getBusinessContext();
	
	BusinessContext getSupplier();
	
	void setSupplier(BusinessContext supplier);
	
	BusinessContext getShipTo();
	
	void setShipTo(BusinessContext shipTo);

	LinkedHashSet<FulfillmentLocationContext> getFulfillmentSequence();

	void setFulfillmentSequence(LinkedHashSet<FulfillmentLocationContext> sequence);

	List<LineItemContextImpl> getLineItems();
	
	List<SupplyChainNote> getNotes();

	void setLineItems(List<LineItemContextImpl> lineItems);
	
	PurchaseOrderType getOrderType();
	
	Object getReferenceDocument();
	
	void setReferenceDocument(Object referenceDocument);
	
	CoordinatorProcessingPhase getProcessingPhase();
	
	void setProcessingPhase(CoordinatorProcessingPhase processingPhase);
	
	String getSupplierDocumentId();
	
	void setSupplierDocumentId(String supplierDocumentId);
	
	String getCustomerDocumentId();
	
	void setCustomerDocumentId(String supplierDocumentId);

	SystemType getSystemType();
	
	void setSystemType(SystemType systemType);
	
	Configuration getConfiguration();
	
	void setConfiguration(Configuration config);
	
	FreightOptions getFreightOptions();
	
	void setFreightOptions(FreightOptions f);
	
	CoordinatorContext getSourceContext();
	void setSourceContext(CoordinatorContext sourceContext);
	
	List<CoordinatorContext> getRelatedContexts();
	
	ProcurementGroupContext getProcurementGroupContext();
	
	boolean isResolved();
	
	void setResolved(boolean value);
	
	
	default Boolean saveWarehouseOrder() {
       if (this instanceof CoordinatorOrderContext) {
           if (PurchaseOrderType.SpecialOrder.equals(getOrderType())) {
               return false;
           }
           if (PurchaseOrderType.DropShip.equals(getOrderType())) {
        	   return false;
           }
           return true;
       }
       return false;
	}
	
	default Account getBuyingAccount() {
		return this.getBusinessContext();
	}

	default Party getSellingParty() {
		if(getSupplier() != null) {
			return this.getSupplier();
		} 
		PartyImpl selling = new PartyImpl();
		BusinessLocationImpl member = new BusinessLocationImpl();
		selling.setPartyType(PartyType.Selling);
		member.setBusinessId(4836l); // Auto-wares Corporate
		member.setId(4836l);
		member.setName("Auto-Wares Inc");
		selling.setMember(member);
		return selling;
	}
	
	default Boolean hasThirdPartyFulfullment() {
		if (getFulfillmentSequence() != null) {
			Optional<FulfillmentLocationContext> var = getFulfillmentSequence().stream()
					.filter(i -> LocationType.Vendor.equals(i.getLocationType()))
					.findAny();
			return var.isPresent();
		}
		return false;
	}
	
	default void updateProcessingLog(String message) {
		getNotes().add(SupplyChainNote.builder().withMessage(message).build());
	}

	default Boolean isManual() {
		if (getFulfillmentSequence() != null) {
			Optional<FulfillmentLocationContext> var = getFulfillmentSequence().stream()
					.filter(i -> SystemType.Manual.equals(i.getSystemType()))
					.findAny();
			return var.isPresent();
		}
		return false;
	};

	default Boolean isInternal() {
		if (getInquiryOptions() != null && getInquiryOptions().getInternalOnly() != null) {
			return getInquiryOptions().getInternalOnly();
		}
		return false;
	}

	void setProcurementGroupContext(ProcurementGroupContext procurementGroupContext);

	void setBusinessContext(BusinessContext account);

	void setOrderType(PurchaseOrderType specialorder);

	void setNotes(ArrayList<SupplyChainNote> notes);
}
