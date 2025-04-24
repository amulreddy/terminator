package com.autowares.mongoose.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.logisticsservice.model.LogisticsCustomerRun;
import com.autowares.logistix.model.HandlingResult;
import com.autowares.logistix.model.Shipment;
import com.autowares.servicescommon.model.ChargesImpl;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.LineItem;
import com.autowares.servicescommon.model.LocationType;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SystemType;
import com.autowares.servicescommon.model.TransactionStage;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.model.WorkingDocument;
import com.autowares.servicescommon.util.DateConversion;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.SupplyChainNote;
import com.autowares.supplychain.model.SupplyChainSourceDocument;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class FulfillmentLocationContext implements Document, WorkingDocument {

	@JsonIgnore
	private CoordinatorContext order;
	private String location;
	private String description;
	private Shipment shipment;
	@JsonIgnore
	private List<FulfillmentContext> fulfillmentDetails = new CopyOnWriteArrayList<>();
	private Duration travelTime;
	private Integer transfers;
	private String departureRunName;
	private Long departureRunId;
	private RunType departureRunType;
	private ZonedDateTime nextDeparture;
	private Double score;
	private RunType deliveryRunType;
	private Long viperTruckRunId;
	private LogisticsCustomerRun logisticsCustomerRun;
	private Integer cutOffMinutes;
	private HandlingResult handlingResult = new HandlingResult();
	private WarehouseMaster warehouseMaster;
	private LocationType locationType;
	private SystemType systemType;
	@JsonIgnore
	private CoordinatorContext nonStockContext = new CoordinatorContextImpl();
	private Boolean quoted = false;
	private Boolean servicingLocation = false;
	private SupplyChainSourceDocument referenceDocument;
	private String fulfillmentLocationId = UUID.randomUUID().toString();
	private String trackingNumber;
	@JsonIgnore
	private List<Availability> lineItemAvailability = new CopyOnWriteArrayList<>();
	private UUID procurementGroupId;
	private TransactionStage transactionStage = TransactionStage.Ready;
	private TransactionStatus transactionStatus = TransactionStatus.Processing;
	private DeliveryMethod deliveryMethod;
	private ServiceClass serviceClass;
	private DeliveryMethod shippingMethod;
	private OperationalContext operationalContext;
	private ZonedDateTime expireTime;
	private DocumentContext documentContext;
	private Collection<ChargesImpl> chargesAndDiscounts = new ArrayList<>();

	public FulfillmentLocationContext() {
		super();
	}

	public FulfillmentLocationContext(CoordinatorContext order, String location) {
		this();
		this.location = location;
		this.order = order;
		order.getFulfillmentSequence().add(this);
	}

	public CoordinatorContext getOrder() {
		return order;
	}

	public void setOrder(CoordinatorContext order) {
		this.order = order;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public List<FulfillmentContext> getFulfillmentDetails() {
		return fulfillmentDetails;
	}

	public void setFulfillmentDetails(List<FulfillmentContext> fulfillmentDetails) {
		this.fulfillmentDetails = fulfillmentDetails;
	}

	public Duration getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(Duration travelTime) {
		this.travelTime = travelTime;
	}

	public Integer getTransfers() {
		return transfers;
	}

	public void setTransfers(Integer transfers) {
		this.transfers = transfers;
	}

	public String getDepartureRunName() {
		return departureRunName;
	}

	public void setDepartureRunName(String departureRunName) {
		this.departureRunName = departureRunName;
	}

	public Long getDepartureRunId() {
		return departureRunId;
	}

	public void setDepartureRunId(Long departureRunId) {
		this.departureRunId = departureRunId;
	}

	public RunType getDepartureRunType() {
        return departureRunType;
    }

    public void setDepartureRunType(RunType departureRunType) {
        this.departureRunType = departureRunType;
    }

    public ZonedDateTime getNextDeparture() {
		return nextDeparture;
	}

	public void setNextDeparture(ZonedDateTime nextDeparture) {
		this.nextDeparture = nextDeparture;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public RunType getDeliveryRunType() {
		return deliveryRunType;
	}

	public void setDeliveryRunType(RunType deliveryRunType) {
		this.deliveryRunType = deliveryRunType;
	}

	public Long getDaysToDeliver() {
		if (travelTime != null) {
			return travelTime.toDays();
		}
		return null;
	}

	public ZonedDateTime getArrivalDate() {
		if (shipment != null) {
			return DateConversion.convert(shipment.getArrivalDate());
		}
		return null;
	}
	
	public void setArrivalDate(ZonedDateTime arrivalDate) {
		if (shipment == null) {
			shipment = new Shipment();
		}
		shipment.setArrivalDate(DateConversion.convert(arrivalDate));
	}
	
	public String getDeliveryRunName() {
		if (shipment != null && shipment.getArrivalRun() !=null) {
			return shipment.getArrivalRun().getRunName();
		}
		return null;
	}


	public Long getViperTruckRunId() {
		return viperTruckRunId;
	}

	public void setViperTruckRunId(Long viperTruckRunId) {
		this.viperTruckRunId = viperTruckRunId;
	}

	public LogisticsCustomerRun getLogisticsCustomerRun() {
		return logisticsCustomerRun;
	}

	public void setLogisticsCustomerRun(LogisticsCustomerRun logisticsCustomerRun) {
		this.logisticsCustomerRun = logisticsCustomerRun;
	}

	public boolean isBeingFilledFrom() {
		return getLineItemAvailability() != null && getLineItemAvailability().stream().filter(i -> i.getFillQuantity() >0).findAny().isPresent();
	}

	public Integer getCutOffMinutes() {
		return cutOffMinutes;
	}

	public void setCutOffMinutes(Integer cutOffMinutes) {
		this.cutOffMinutes = cutOffMinutes;
	}

	public HandlingResult getHandlingResult() {
		return handlingResult;
	}

	public void setHandlingResult(HandlingResult handlingResult) {
		this.handlingResult = handlingResult;
	}

	public void setWarehouseMaster(WarehouseMaster warehouseMaster) {
		this.warehouseMaster = warehouseMaster;
	}

	public WarehouseMaster getWarehouseMaster() {
		return warehouseMaster;
	}

	public LocationType getLocationType() {
		return locationType;
	}

	public void setLocationType(LocationType locationType) {
		this.locationType = locationType;
	}

	public CoordinatorContext getNonStockContext() {
		return nonStockContext;
	}

	public void setNonStockContext(CoordinatorContext nonStockContext) {
		this.nonStockContext = nonStockContext;
	}

	public SystemType getSystemType() {
		return systemType;
	}

	public void setSystemType(SystemType systemType) {
		this.systemType = systemType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean isOrder() {
		return (order instanceof CoordinatorOrderContext); 
	}

	public Boolean isQuoted() {
		return quoted;
	}

	public void setQuoted(Boolean quoted) {
		this.quoted = quoted;
	}

	public String getFulfillmentLocationId() {
		return fulfillmentLocationId;
	}

	public void setFulfillmentLocationId(String fulfillmentLocationId) {
		this.fulfillmentLocationId = fulfillmentLocationId;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public void setTrackingNumber(String trackingNumber) {
		this.trackingNumber = trackingNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location, order);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FulfillmentLocationContext other = (FulfillmentLocationContext) obj;
		return Objects.equals(location, other.location) && Objects.equals(order, other.order);
	}

	public Boolean getServicingLocation() {
		return servicingLocation;
	}

	public void setServicingLocation(Boolean servicingLocation) {
		this.servicingLocation = servicingLocation;
	}

	public List<Availability> getLineItemAvailability() {
		return lineItemAvailability;
	}

	public void setLineItemAvailability(List<Availability> lineItemAvailability) {
		this.lineItemAvailability = lineItemAvailability;
	}

	public UUID getProcurementGroupId() {
		return procurementGroupId;
	}

	public void setProcurementGroupId(UUID procurementGroupId) {
		this.procurementGroupId = procurementGroupId;
	}

	public SupplyChainSourceDocument getReferenceDocument() {
		return referenceDocument;
	}

	public void setReferenceDocument(SupplyChainSourceDocument referenceDocument) {
		this.referenceDocument = referenceDocument;
	}

    @Override
    public String getDocumentId() {
        if (referenceDocument != null) {
            return referenceDocument.getDocumentId();
        }
        return null;
    }

    @Override
    public void setDocumentId(String documentId) {
        if (referenceDocument != null) {
             referenceDocument.setDocumentId(documentId);
        }
    }

    @Override
    public Collection<? extends LineItem> getLineItems() {
        if (referenceDocument != null) {
            return referenceDocument.getLineItems();
        }
        return null;
    }

    public TransactionStage getTransactionStage() {
        return transactionStage;
    }

    public void setTransactionStage(TransactionStage transactionStage) {
        this.transactionStage = transactionStage;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public Boolean getIsBeingFilledFrom() {
        return isBeingFilledFrom();
    }

    public Boolean getQuoted() {
        return quoted;
    }
    
    void updateProcessingLog(String message) {
		referenceDocument.getNotes().add(SupplyChainNote.builder().withMessage(message).build());
	}

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public ServiceClass getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(ServiceClass serviceClass) {
        this.serviceClass = serviceClass;
    }

    public DeliveryMethod getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(DeliveryMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

	public OperationalContext getOperationalContext() {
		return operationalContext;
	}

	public void setOperationalContext(OperationalContext operationalContext) {
		this.operationalContext = operationalContext;
	}
		
	public ZonedDateTime getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(ZonedDateTime expireTime) {
		this.expireTime = expireTime;
	}

	public DocumentContext getDocumentContext() {
		return documentContext;
	}

	public void setDocumentContext(DocumentContext documentContext) {
		this.documentContext = documentContext;
	}

	public Collection<ChargesImpl> getChargesAndDiscounts() {
		return chargesAndDiscounts;
	}

	public void setChargesAndDiscounts(Collection<ChargesImpl> chargesAndDiscounts) {
		this.chargesAndDiscounts = chargesAndDiscounts;
	}


}
