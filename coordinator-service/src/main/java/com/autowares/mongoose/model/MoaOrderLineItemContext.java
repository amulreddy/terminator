package com.autowares.mongoose.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.autowares.apis.ids.model.VendorMaster;
import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.model.SupplierMfgBusinessInfo;
import com.autowares.mongoose.optaplanner.domain.OrderDetailFulfillment;
import com.autowares.orders.model.Item;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.servicescommon.model.HandlingOptions;
import com.autowares.servicescommon.model.PriceLevel;
import com.autowares.servicescommon.model.ShortageCode;
import com.autowares.supplychain.model.DemandModel;
import com.autowares.supplychain.model.OperationalItem;
import com.autowares.supplychain.model.SupplyChainNote;
import com.google.common.collect.Lists;

public class MoaOrderLineItemContext implements LineItemContext {

	private MoaOrderContext moaOrderContext;
	private Item invoiceMoaOrderDetail;
	private List<Item> moaOrderDetails = new ArrayList<>();
	
	private List<FulfillmentContext> fulfillmentDetails = new ArrayList<>();
	private Boolean invalid = false;
	private OrderDetailFulfillment plannedFulfillment;
	private Integer originalQuantity;
	private Integer quantity;
	private DemandModel demand;
	private Part part;
	private List<Availability> availability = new ArrayList<>();
	private List<PriceLevel> priceLevels = new ArrayList<>();
	private HandlingOptions handlingOptions = new HandlingOptions();
	private List<OperationalItem> operationalItems = new ArrayList<>();
	private Boolean updateProcessStage = false;
	private List<SupplyChainNote> notes = new ArrayList<>();
	private ShortageCode notFillableCode;
	private VendorMaster vendorMaster;

	public MoaOrderLineItemContext(MoaOrderContext moaOrderContex, List<Item> details) {
		this.moaOrderContext = moaOrderContex;
		this.invoiceMoaOrderDetail = details.get(0);
		this.moaOrderDetails = details;
	}

	public MoaOrderLineItemContext(MoaOrderContext coordinatorContext, Item item) {
		this(coordinatorContext, Lists.newArrayList(item));
	}

	@Override
	public BigDecimal getPrice() {
		return invoiceMoaOrderDetail.getBillprice();
	}

	@Override
	public void setPrice(BigDecimal price) {
		this.invoiceMoaOrderDetail.setBillprice(price);
	}

	@Override
	public Integer getLineNumber() {
		return invoiceMoaOrderDetail.getCustomerLineNumber();
	}

	@Override
	public void setLineNumber(Integer lineNumber) {
		// TODO
	}

	@Override
	public Long getProductId() {
		if (part != null) {
			return part.getProductId();
		}
		if (invoiceMoaOrderDetail.getPartHeaderId() != null) {
			return invoiceMoaOrderDetail.getPartHeaderId().longValue();
		}
		return null;
	}

	@Override
	public Part getPart() {
		return this.part;
	}

	@Override
	public void setPart(Part part) {
		this.part = part;
	}

	@Override
	public Integer getQuantity() {
		if (this.quantity == null) {
			this.quantity = invoiceMoaOrderDetail.getOrderQuantity();
		}
		return this.quantity;
	}

	@Override
	public String getPartNumber() {
		if (part != null) {
			return part.getPartNumber();
		}
		return invoiceMoaOrderDetail.getPartNumber();
	}

	@Override
	public String getVendorCode() {
		if (part != null) {
			return part.getVendorCodeSubCode();
		}
		return invoiceMoaOrderDetail.getVendorCode();
	}

	@Override
	public Boolean getMustGo() {
		return invoiceMoaOrderDetail.getMustGo();
	}
	
	@Override
	public void setMustGo(Boolean value) {
		invoiceMoaOrderDetail.setMustGo(value);
	}

	@Override
	public Long getId() {
		return invoiceMoaOrderDetail.getOrderItemId().longValue();
	}
	
	@Override
	public void setId(Long id) { }

	@Override
	public List<FulfillmentContext> getFulfillmentDetails() {
		return fulfillmentDetails;
	}

	@Override
	public void setFulfillmentDetails(List<FulfillmentContext> orderFillContext) {
		this.fulfillmentDetails = orderFillContext;
	}

	public CoordinatorOrderContext getOrderContext() {
		return this.moaOrderContext;
	}

	@Override
	public Number getCustomerLineNumber() {
		return this.invoiceMoaOrderDetail.getCustomerLineNumber();
	}

	@Override
	public Boolean getInvalid() {
		return invalid;
	}

	@Override
	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}

	@Override
	public OrderDetailFulfillment getPlannedFulfillment() {
		return plannedFulfillment;
	}

	@Override
	public void setPlannedFulfillment(OrderDetailFulfillment solution) {
		plannedFulfillment = solution;
	}

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

	public DemandModel getDemand() {
		return demand;
	}

	public void setDemand(DemandModel demand) {
		this.demand = demand;
	}

	@Override
	public String getLineCode() {
		if (part != null) {
			return part.getLineCode();
		}
		return invoiceMoaOrderDetail.getLineCode();
	}

	@Override
	public String getVendorCodeSubCode() {
		if (part != null) {
			return part.getVendorCodeSubCode();
		}
		return invoiceMoaOrderDetail.getVendorCode();
	}

	@Override
	public String getCounterWorksLineCode() {
		if (part != null) {
			return part.getCounterWorksLineCode();
		}
		return invoiceMoaOrderDetail.getLineCode();
	}

	@Override
	public String getBrandAaiaId() {
		if (part != null) {
			return part.getBrandAaiaId();
		}
		return null;
	}

	public Item getInvoiceItem() {
		return this.invoiceMoaOrderDetail;
	}
	
	public void setInvoiceItem(Item item) {
		this.invoiceMoaOrderDetail = item;
	}
	
	public List<Item> getItems() {
		return this.moaOrderDetails;
	}

	@Override
	public CoordinatorContext getContext() {
		return moaOrderContext;
	}

	@Override
	public FreightOptions getFreightOptions() {
		return null;
	}

	@Override
	public void setFreightOptions(FreightOptions freightOptions) {
	}

	@Override
	public List<Availability> getAvailability() {
		return this.availability;
	}

	@Override
	public void setAvailability(List<Availability> availability) {
		this.availability = availability;
	}

	public List<PriceLevel> getPriceLevels() {
		return priceLevels;
	}

	public void setPriceLevels(List<PriceLevel> priceLevels) {
		this.priceLevels = priceLevels;
	}

	@Override
	public void setContext(CoordinatorContext context) {
		this.moaOrderContext = (MoaOrderContext) context;

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

	public void setUpdateProcessStage(boolean updateProcessStage) {
		this.updateProcessStage = updateProcessStage;
	}

	public Boolean getUpdateProcessStage() {
		return this.updateProcessStage;
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
		return null;
	}
}
