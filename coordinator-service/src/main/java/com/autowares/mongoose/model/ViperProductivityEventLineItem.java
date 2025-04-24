package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.autowares.apis.ids.model.VendorMaster;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.model.SupplierMfgBusinessInfo;
import com.autowares.mongoose.events.ViperProductivityEvent;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.ShortageCode;
import com.autowares.supplychain.model.DemandModel;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainNote;

public class ViperProductivityEventLineItem implements LineItemContext {
	
	private ViperProductivityEvent event;
	private Integer lineNumber = 1;
	private Number customerLineNumber = 1;
	private Part part;
	private Boolean invalid = false;
	private List<FulfillmentContext> fulfillmentDetails = new ArrayList<>();
	private CoordinatorContext coordinatorContext;
	private HandlingOptions handlingOptions = new HandlingOptions();
	private List<OperationalItem> operationalItems = new ArrayList<>();
	private List<SupplyChainNote> notes = new ArrayList<>();
	private ShortageCode notFillableCode;
	private VendorMaster vendorMaster;
	
	@Override
	public BigDecimal getPrice() {
		if (event.getBillPrice() != null) {
			return BigDecimal.valueOf(event.getBillPrice());
		} else {
			return BigDecimal.ZERO;
		}
	}

	@Override
	public Integer getLineNumber() {
		return this.lineNumber ;
	}

	@Override
	public Integer getQuantity() {
		return this.getQuantity();
	}

	@Override
	public Number getCustomerLineNumber() {
		return this.customerLineNumber ;
	}

	@Override
	public Long getProductId() {
		return this.event.getMoaPartHeaderId();
	}

	@Override
	public String getLineCode() {
		return null;
	}

	@Override
	public String getVendorCodeSubCode() {
		return this.event.getVendor();
	}

	@Override
	public String getCounterWorksLineCode() {
		return null;
	}

	@Override
	public String getBrandAaiaId() {
		return null;
	}

	@Override
	public Part getPart() {
		return this.part;
	}

	@Override
	public void setPart(Part part) {
		this.part=part;
	}

	@Override
	public Boolean getInvalid() {
		return this.invalid ;
	}

	@Override
	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}

	@Override
	public List<FulfillmentContext> getFulfillmentDetails() {
		return this.fulfillmentDetails;
	}

	@Override
	public void setFulfillmentDetails(List<FulfillmentContext> orderFillContext) {
		this.fulfillmentDetails = orderFillContext;
	}

	@Override
	public String getPartNumber() {
		return this.event.getPartNumber();
	}

	@Override
	public String getVendorCode() {
		return this.event.getVendor();
	}

	@Override
	public Boolean getMustGo() {
		return null;
	}

	@Override
	public Long getId() {
		return null;
	}
	
	@Override
	public void setId(Long id) { }

	@Override
	public CoordinatorContext getContext() {
		return this.coordinatorContext;
	}

	@Override
	public FreightOptions getFreightOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFreightOptions(FreightOptions freightOptions) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public OrderDetailFulfillment getPlannedFulfillment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPlannedFulfillment(OrderDetailFulfillment solution) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Integer getOriginalQuantity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOriginalQuantity(Integer orderQuantity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setQuantity(Integer newQuantity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLineNumber(Integer lineNumber) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DemandModel getDemand() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDemand(DemandModel demand) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPrice(BigDecimal price) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Availability> getAvailability() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAvailability(List<Availability> availability) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void setContext(CoordinatorContext context) {
        this.coordinatorContext = context;
        
    }

	public HandlingOptions getHandlingOptions() {
		return handlingOptions;
	}

	public void setHandlingOptions(HandlingOptions handlingOptions) {
		this.handlingOptions = handlingOptions;
	}
	
	public ViperProductivityEventLineItem(ViperProductivityEvent event, CoordinatorContext coordinatorContext) {
		this.event = event;
		this.coordinatorContext = coordinatorContext;
	}

	@Override
	public void setMustGo(Boolean value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Long getPrimarySupplierId() {

		if (part != null && part.getActiveProductLine() != null) {
			SupplierMfgBusinessInfo supplier = part.getActiveProductLine().getPrimarySupplier();
			if (supplier != null) {
				return supplier.getBusEntId();
			}
		}

		return null;
	}

    public List<SupplyChainNote> getNotes() {
        return notes;
    }

    public void setNotes(List<SupplyChainNote> notes) {
        this.notes = notes;
    }


	public List<OperationalItem> getOperationalItems() {
		return operationalItems;
	}

	public void setOperationalItems(List<OperationalItem> operationalItems) {
		this.operationalItems = operationalItems;
	}

	public ShortageCode getShortageCode() {
		return notFillableCode;
	}

	public void setShortageCode(ShortageCode notFillableCode) {
		this.notFillableCode = notFillableCode;
	}

	public VendorMaster getVendorMaster() {
		return vendorMaster;
	}

	public void setVendorMaster(VendorMaster vendorMaster) {
		this.vendorMaster = vendorMaster;
	}
	
	@Override
	public String getManufacturerLineCode() {
		if (part != null) {
			return part.getManufacturerLineCode();
		}
		return null;
	}

}
