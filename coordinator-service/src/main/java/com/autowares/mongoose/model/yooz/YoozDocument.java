package com.autowares.mongoose.model.yooz;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvBindByPosition;

@JsonIgnoreProperties({"action"})
public class YoozDocument {

    @CsvBindByPosition(position = 0)
    private String action = "CREATE";
    
    @JsonProperty("YZ_THIRD")
    @CsvBindByPosition(position = 1)
    private String vendorCode;
    
    @CsvBindByPosition(position = 2)
    private String vendorName;
    
    @JsonProperty("YZ_NUMBER")
    @CsvBindByPosition(position = 3)
    private String orderNumber;
    
    @JsonProperty("YZ_DATE")
    @CsvBindByPosition(position = 4)
    private String orderDate;
    
    @JsonProperty("YZ_TOTAL_AMOUNT")
    @CsvBindByPosition(position = 5)
    private BigDecimal documentTaxedAmount;
    
    @JsonProperty("YZ_AMOUNT")
    @CsvBindByPosition(position = 6)
    private BigDecimal documentUntaxedAmount;
    
    @JsonProperty("YZ_CURRENCY")
    @CsvBindByPosition(position = 7)
    private String currency;
    
    @JsonProperty("YZ_DELIVERY_NOTE")
    @CsvBindByPosition(position = 8)
    private String orderCreator;
    
    @JsonProperty("YZ_APPROUVERS")
    @CsvBindByPosition(position = 9)
    private String orderApprovers;
    
    @JsonProperty("YZ_ERP_ORDER_STATUS")
    @CsvBindByPosition(position = 10)
    private String orderStatus;
    
    @JsonProperty("YZ_EXTERNAL_LINE_ID")
    @CsvBindByPosition(position = 11)
    private String lineNumber;
    
    @JsonProperty("YZ_PRODUCT_CODE")
    @CsvBindByPosition(position = 12)
    private Long clientItemCode;
    
    @JsonProperty("YZ_DESCRIPION")
    @CsvBindByPosition(position = 13)
    private String itemDescription;
    
    @JsonProperty("YZ_UNIT_PRICE")
    @CsvBindByPosition(position = 14)
    private BigDecimal itemUnitPrice;
    
    @JsonProperty("YZ_QUANTITY")
    @CsvBindByPosition(position = 15)
    private Integer quantityOrdered;
    
    @JsonProperty("YZ_QUANTITY_RECEIVED")
    @CsvBindByPosition(position = 16)
    private Integer quantityReceived;
    
    @JsonProperty("YZ_QUANTITY_INVOICED")
    @CsvBindByPosition(position = 17)
    private Integer quantityCharged;
    
    @JsonProperty("YZ_AMOUNT")
    @CsvBindByPosition(position = 18)
    private BigDecimal amountCharged;
    
    @JsonProperty("YZ_DISCOUNT")
    @CsvBindByPosition(position = 19)
    private BigDecimal discountedAmount;
    
    @JsonProperty("YZ_TAX_CODE")
    @CsvBindByPosition(position = 20)
    private String taxProfileCode;
    
    @JsonProperty("YZ_TAX_AMOUNT")
    @CsvBindByPosition(position = 21)
    private BigDecimal taxAmount;
    
    @JsonProperty("YZ_ACCOUNT")
    @CsvBindByPosition(position = 22)
    private String glAccount;
    
    @CsvBindByPosition(position = 23)
    private String dimensionsCodes;
    
    @CsvBindByPosition(position = 24)
    private String costCenterChartsCodes;
    
    @JsonProperty("YZ_ORDER_SITE")
    @CsvBindByPosition(position = 25)
    private String subsidiary;
    
    @JsonProperty("YZ_SUPPLIER_ITEM_CODE")
    @CsvBindByPosition(position = 26)
    private Long supplierItem;
    
    @CsvBindByPosition(position = 27)
    private String headerLevelCustomData;
    
    @CsvBindByPosition(position = 28)
    private String lineLevelCustomData;
    
    @JsonProperty("YZ_RECEPTION_COMMENT")
    @CsvBindByPosition(position = 29)
    private String receptionComment;
    
    @JsonProperty("YZ_RECEPTION_DATE")
    @CsvBindByPosition(position = 30)
    private String receptionDate;
    
    @JsonProperty("YZ_DELIVERY_DATE")
    @CsvBindByPosition(position = 31)
    private String plannedDeliveryDate;
    
    @JsonProperty("YZ_TORECEIVE")
    @CsvBindByPosition(position = 32)
    private String toBeRecevied;
    
    @JsonProperty("YZ_ORDER_TYPE")
    @CsvBindByPosition(position = 33)
    private String typeOfPO;
    
    @JsonProperty("YZ_DELIVERY_ADDRESS")
    @CsvBindByPosition(position = 34)
    private String deliveryAddress;
    
    @JsonProperty("YZ_BILLING_ADDRESS")
    @CsvBindByPosition(position = 35)
    private String invoicingAddress;
    
    @CsvBindByPosition(position = 36)
    private String yoozNumber;
    
    @CsvBindByPosition(position = 37)
    private String typeOfLine;
    
    @CsvBindByPosition(position = 38)
    private String sublinesManagement;
    
    @CsvBindByPosition(position = 39)
    private String budgetPeriodCode;
    
    @CsvBindByPosition(position = 40)
    private String budgetCode;
    
    @CsvBindByPosition(position = 41)
    private String startDate;
    
    @CsvBindByPosition(position = 42)
    private String endDate;
    
    @CsvBindByPosition(position = 43)
    private String label;
    
    @JsonProperty("YZ_SUP_REF_CODE")
    @CsvBindByPosition(position = 44)
    private String vendorItemCode;
    
    @JsonProperty("YZ_PURCHASE_ORDER_NUMBER")
    private String invoiceNumber;
    
    @JsonProperty("YZ_DUE_DATE")
    private String dueDate;
    
    @JsonProperty("YZ_PAYMENT_METHOD")
    private String paymentMethod;
    
    private String unitOfMeasure;
    
    private String buyerCode;

    // Getters and setters for all fields

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getDocumentTimeStamp() {
        return orderDate;
    }

    public void setDocumentTimeStamp(String date) {
        this.orderDate = date;
    }

    public BigDecimal getDocumentTaxedAmount() {
        return documentTaxedAmount;
    }

    public void setDocumentTaxedAmount(BigDecimal documentTaxedAmount) {
        this.documentTaxedAmount = documentTaxedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOrderCreator() {
        return orderCreator;
    }

    public void setOrderCreator(String orderCreator) {
        this.orderCreator = orderCreator;
    }

    public String getOrderApprovers() {
        return orderApprovers;
    }

    public void setOrderApprovers(String approvers) {
        this.orderApprovers = approvers;
    }

    public Long getProductCode() {
        return clientItemCode;
    }

    public void setProductCode(Long productCode) {
        this.clientItemCode = productCode;
    }

    public String getDescription() {
        return itemDescription;
    }

    public void setDescription(String description) {
        this.itemDescription = description;
    }

    public BigDecimal getProductUnitPrice() {
        return itemUnitPrice;
    }

    public void setProductUnitPrice(BigDecimal productUnitPrice) {
        this.itemUnitPrice = productUnitPrice;
    }

    public Integer getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(Integer quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public Integer getQuantityInvoices() {
        return quantityCharged;
    }

    public void setQuantityInvoices(Integer quantityInvoices) {
        this.quantityCharged = quantityInvoices;
    }

    public BigDecimal getAmount() {
        return amountCharged;
    }

    public void setAmount(BigDecimal amount) {
        this.amountCharged = amount;
    }

    public BigDecimal getDiscountedAmout() {
        return discountedAmount;
    }

    public void setDiscountedAmout(BigDecimal discountedAmout) {
        this.discountedAmount = discountedAmout;
    }

    public String getTaxProfileCode() {
        return taxProfileCode;
    }

    public void setTaxProfileCode(String taxProfileCode) {
        this.taxProfileCode = taxProfileCode;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getGlAccount() {
        return glAccount;
    }

    public void setGlAccount(String glAccount) {
        this.glAccount = glAccount;
    }

    public String getCostCenterChartsCodes() {
		return costCenterChartsCodes;
	}

	public void setCostCenterChartsCodes(String costCenterChartsCodes) {
		this.costCenterChartsCodes = costCenterChartsCodes;
	}

	public String getSubsidiary() {
		return subsidiary;
	}

	public void setSubsidiary(String subsidiary) {
		this.subsidiary = subsidiary;
	}

	public Long getSupplierProduct() {
		return supplierItem;
	}

	public void setSupplierProduct(Long supplierProduct) {
		this.supplierItem = supplierProduct;
	}

	public String getHeader() {
		return headerLevelCustomData;
	}

	public void setHeader(String header) {
		this.headerLevelCustomData = header;
	}

	public String getLines() {
		return lineLevelCustomData;
	}

	public void setLines(String lines) {
		this.lineLevelCustomData = lines;
	}

	public String getReceptionComment() {
		return receptionComment;
	}

	public void setReceptionComment(String receptionComment) {
		this.receptionComment = receptionComment;
	}

	public String getReceptionDate() {
        return receptionDate;
    }

    public void setReceptionDate(String receptionDate) {
        this.receptionDate = receptionDate;
    }

    public String getPlannedDeliveryDate() {
        return plannedDeliveryDate;
    }

    public void setPlannedDeliveryDate(String deliveryDate) {
        this.plannedDeliveryDate = deliveryDate;
    }

    public String getToBeRecevied() {
        return toBeRecevied;
    }

    public void setToBeRecevied(String string) {
        this.toBeRecevied = string;
    }

    public String getTypeOfPO() {
        return typeOfPO;
    }

    public void setTypeOfPO(String typeOfPO) {
        this.typeOfPO = typeOfPO;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getInvoicingAddress() {
        return invoicingAddress;
    }

    public void setInvoicingAddress(String invoicingAddress) {
        this.invoicingAddress = invoicingAddress;
    }

    public String getYoozNumber() {
        return yoozNumber;
    }

    public void setYoozNumber(String yoozNumber) {
        this.yoozNumber = yoozNumber;
    }

    public String getTypeOfLine() {
        return typeOfLine;
    }

    public void setTypeOfLine(String typeOfLine) {
        this.typeOfLine = typeOfLine;
    }

    public String getSublinesManagement() {
        return sublinesManagement;
    }

    public void setSublinesManagement(String sublinesManagement) {
        this.sublinesManagement = sublinesManagement;
    }

    public String getBudgetPeriodCode() {
        return budgetPeriodCode;
    }

    public void setBudgetPeriodCode(String budgetPeriodCode) {
        this.budgetPeriodCode = budgetPeriodCode;
    }

    public String getBudgetCode() {
        return budgetCode;
    }

    public void setBudgetCode(String budgetCode) {
        this.budgetCode = budgetCode;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVendorItemCode() {
        return vendorItemCode;
    }

    public void setVendorItemCode(String vendorItemCode) {
        this.vendorItemCode = vendorItemCode;
    }

	public String getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}

	public BigDecimal getDocumentUntaxedAmount() {
		return documentUntaxedAmount;
	}

	public void setDocumentUntaxedAmount(BigDecimal documentUntaxedAmount) {
		this.documentUntaxedAmount = documentUntaxedAmount;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(String lineNumber) {
		this.lineNumber = lineNumber;
	}

	public Long getClientItemCode() {
		return clientItemCode;
	}

	public void setClientItemCode(Long clientItemCode) {
		this.clientItemCode = clientItemCode;
	}

	public String getItemDescription() {
		return itemDescription;
	}

	public void setItemDescription(String itemDescription) {
		this.itemDescription = itemDescription;
	}

	public BigDecimal getItemUnitPrice() {
		return itemUnitPrice;
	}

	public void setItemUnitPrice(BigDecimal itemUnitPrice) {
		this.itemUnitPrice = itemUnitPrice;
	}

	public Integer getQuantityCharged() {
		return quantityCharged;
	}

	public void setQuantityCharged(Integer quantityCharged) {
		this.quantityCharged = quantityCharged;
	}

	public BigDecimal getAmountCharged() {
		return amountCharged;
	}

	public void setAmountCharged(BigDecimal amountCharged) {
		this.amountCharged = amountCharged;
	}

	public String getDimensionsCodes() {
		return dimensionsCodes;
	}

	public void setDimensionsCodes(String dimensionsCodes) {
		this.dimensionsCodes = dimensionsCodes;
	}

	public Long getSupplierItem() {
		return supplierItem;
	}

	public void setSupplierItem(Long supplierItem) {
		this.supplierItem = supplierItem;
	}

	public String getHeaderLevelCustomData() {
		return headerLevelCustomData;
	}

	public void setHeaderLevelCustomData(String headerLevelCustomData) {
		this.headerLevelCustomData = headerLevelCustomData;
	}

	public String getLineLevelCustomData() {
		return lineLevelCustomData;
	}

	public void setLineLevelCustomData(String lineLevelCustomData) {
		this.lineLevelCustomData = lineLevelCustomData;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getUnitOfMeasure() {
		return unitOfMeasure;
	}

	public void setUnitOfMeasure(String unitOfMeasure) {
		this.unitOfMeasure = unitOfMeasure;
	}

	public String getBuyerCode() {
		return buyerCode;
	}

	public void setBuyerCode(String buyerCode) {
		this.buyerCode = buyerCode;
	}

}
