package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.autowares.apis.ids.model.VendorMaster;
import com.autowares.apis.partservice.Part;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.PartLineItem;
import com.autowares.servicescommon.model.ShortageCode;
import com.autowares.supplychain.model.DemandModel;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainNote;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface LineItemContext extends PartLineItem {

	@JsonIgnore
	Part getPart();

	void setPart(Part part);

	Boolean getInvalid();

	void setInvalid(Boolean invalid);

	List<FulfillmentContext> getFulfillmentDetails();

	void setFulfillmentDetails(List<FulfillmentContext> orderFillContext);

	//String getPartNumber();

	String getVendorCode();

	Boolean getMustGo();
	void setMustGo(Boolean value);

	Long getId();
	void setId(Long id);

	@JsonIgnore
	CoordinatorContext getContext();
	
	void setContext(CoordinatorContext context);

	FreightOptions getFreightOptions();

	void setFreightOptions(FreightOptions freightOptions);

	OrderDetailFulfillment getPlannedFulfillment();

	void setPlannedFulfillment(OrderDetailFulfillment solution);

	Integer getOriginalQuantity();

	void setOriginalQuantity(Integer orderQuantity);

	void setQuantity(Integer newQuantity);

	void setLineNumber(Integer lineNumber);

	DemandModel getDemand();

	void setDemand(DemandModel demand);
	
	List<SupplyChainNote> getNotes();
	
	void setNotes(List<SupplyChainNote> notes);

	void setPrice(BigDecimal price);
	
	List<Availability> getAvailability();
	void setAvailability(List<Availability> availability);
	
	HandlingOptions getHandlingOptions();
	
	void setHandlingOptions(HandlingOptions options);
	
	List<OperationalItem> getOperationalItems();
	void setOperationalItems(List<OperationalItem> operationalItems);
	
	Long getPrimarySupplierId();
	
	ShortageCode getShortageCode();
	void setShortageCode(ShortageCode code);
	
	VendorMaster getVendorMaster();
	
	String getManufacturerLineCode();
	
	void setVendorMaster(VendorMaster v);
	
	default void updateOrderLog(String message) {
		if (getDemand() !=null) {
			getDemand().addNote(SupplyChainNote.builder().withMessage(message).build());
		}
	}

	default String getDocumentReference() {
        if (getDocumentReferenceIds() != null) {
        	Optional<String> optionalDocumentReference = getDocumentReferenceIds().stream().filter(i -> i != null).findAny();
        	if (optionalDocumentReference.isPresent()) {
                return optionalDocumentReference.get();
            } 
        }
        return null;
    }
}
