package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.autowares.apis.ids.model.VendorMaster;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.model.SupplierMfgBusinessInfo;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.PartLineItem;
import com.autowares.servicescommon.model.ShortageCode;
import com.autowares.supplychain.model.DemandModel;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.xmlgateway.edi.EdiLine;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class LineItemContextImpl implements LineItemContext {

	protected PartLineItem lineItem;
	protected Part part;
	protected Boolean invalid = false;
	private Boolean mustGo;
	private Long id;
	private CoordinatorContext context;
	private OrderDetailFulfillment plannedFulfillment;
	private Integer originalQuantity;
	private Integer quantity;
	private DemandModel demand;
	private List<FulfillmentContext> fulfillmentDetails = new ArrayList<>();
	private BigDecimal price;
	private FreightOptions freightOptions = new FreightOptions();
	private List<Availability> availability = new ArrayList<>();
	private HandlingOptions handlingOptions = new HandlingOptions();
	private List<OperationalItem> operationalItems = new ArrayList<>();
	private List<SupplyChainNote> notes = new ArrayList<>();
	private ShortageCode notFillableCode;
	private VendorMaster vendorMaster;
	

	public LineItemContextImpl() {
	}

	public LineItemContextImpl(PartLineItem lineItem) {
		this.lineItem = lineItem;
		this.originalQuantity = lineItem.getQuantity();
		this.quantity = lineItem.getQuantity();
		this.price = lineItem.getPrice();
	}

	public LineItemContextImpl(LineItemContext context) {
		this((PartLineItem) context);
		this.part = context.getPart();
		this.quantity = context.getQuantity();
		this.mustGo = context.getMustGo();
		this.id = context.getId();
		this.demand = context.getDemand();
		this.availability = context.getAvailability();
		this.context = context.getContext();
		this.vendorMaster = context.getVendorMaster();
	}

	public LineItemContextImpl(CoordinatorContext coordinatorContext, PartLineItem lineItem) {
		this(lineItem);
		this.context = coordinatorContext;
	}

	@Override
	public BigDecimal getPrice() {
		return price;
	}

	@Override
	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	@Override
	public Integer getLineNumber() {
		return lineItem.getLineNumber();
	}

	@Override
	public void setLineNumber(Integer lineNumber) {
		lineItem.setLineNumber(lineNumber);
	}

	@Override
	public Integer getQuantity() {
		return quantity;
	}

	@Override
	public Number getCustomerLineNumber() {
		return lineItem.getCustomerLineNumber();
	}

	@Override
	public Long getProductId() {
		if (part != null) {
			return part.getProductId();
		}
		return lineItem.getProductId();
	}

	@Override
	public String getPartNumber() {
		if (part != null) {
			return part.getPartNumber();
		}
		return lineItem.getPartNumber();
	}

	@Override
	public String getLineCode() {
		if (part != null) {
			return part.getLineCode();
		}
		return lineItem.getLineCode();
	}

	@Override
	public String getVendorCodeSubCode() {
		if (part != null) {
			return part.getVendorCodeSubCode();
		}
		return lineItem.getVendorCodeSubCode();
	}

	@Override
	public String getCounterWorksLineCode() {
		if (part != null) {
			return part.getCounterWorksLineCode();
		}
		return lineItem.getCounterWorksLineCode();
	}

	@Override
	public String getBrandAaiaId() {
		if (part != null) {
			return part.getBrandAaiaId();
		}
		return lineItem.getBrandAaiaId();
	}
	
	@Override
	public Part getPart() {
		return part;
	}

	@Override
	public void setPart(Part part) {
		this.part = part;
	}

	public void setLineItem(PartLineItem lineItem) {
		this.lineItem = lineItem;
	}

	public Boolean getInvalid() {
		return invalid;
	}

	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}

	public List<FulfillmentContext> getFulfillmentDetails() {
		return fulfillmentDetails;
	}

	public void setFulfillmentDetails(List<FulfillmentContext> fulfillmentDetails) {
		this.fulfillmentDetails = fulfillmentDetails;
	}

	@Override
	public String getVendorCode() {
		return getLineCode();
	}

	@Override
	public Boolean getMustGo() {
		return mustGo;
	}

	@Override
	public Long getId() {
		return id;
	}

	@JsonIgnore
	@Override
	public CoordinatorContext getContext() {
		return context;
	}

	@Override
	public OrderDetailFulfillment getPlannedFulfillment() {
		return plannedFulfillment;
	}

	@Override
	public void setPlannedFulfillment(OrderDetailFulfillment solution) {
		this.plannedFulfillment = solution;
	}

	@Override
	public Integer getOriginalQuantity() {
		return originalQuantity;
	}

	@Override
	public void setOriginalQuantity(Integer orderQuantity) {
		this.originalQuantity = orderQuantity;
	}

	@Override
	public void setQuantity(Integer newQuantity) {
		this.quantity = newQuantity;
	}

	@Override
	public DemandModel getDemand() {
		return demand;
	}

	@Override
	public void setDemand(DemandModel demand) {
		this.demand = demand;
	}

	@JsonIgnore
	public PartLineItem getLineItem() {
		return lineItem;
	}

	public void setMustGo(Boolean mustGo) {
		this.mustGo = mustGo;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setContext(CoordinatorContext orderContext) {
		this.context = orderContext;
	}

	@JsonIgnore
	public CoordinatorOrderContext getOrderContext() {
		if (context instanceof CoordinatorOrderContext) {
			return (CoordinatorOrderContext) context;
		}
		return null;
	}

	public FreightOptions getFreightOptions() {
		return freightOptions;
	}

	public void setFreightOptions(FreightOptions freightOptions) {
		this.freightOptions = freightOptions;
	}

	@Override
	public List<String> getDocumentReferenceIds() {
		if (lineItem.getDocumentReferenceIds() != null) {
			return lineItem.getDocumentReferenceIds();
		}
		return new ArrayList<>();
	}

	public List<Availability> getAvailability() {
		return availability;
	}

	public void setAvailability(List<Availability> availability) {
		this.availability = availability;
	}

	public HandlingOptions getHandlingOptions() {
		return handlingOptions;
	}

	public void setHandlingOptions(HandlingOptions handlingOptions) {
		this.handlingOptions = handlingOptions;
	}

	public List<OperationalItem> getOperationalItems() {
		return operationalItems;
	}

	public void setOperationalItems(List<OperationalItem> operationalItems) {
		this.operationalItems = operationalItems;
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
		if(this.lineItem instanceof EdiLine) {
			EdiLine ediLine = (EdiLine) this.lineItem;
			return ediLine.getManufacturerCode();
		}
		return null;
	}


}
