package com.autowares.mongoose.model.yooz;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class DataContainer {
	@JsonProperty("data")
	private Data data;

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public static class Data {
		@JsonProperty("dataBlocks")
		private DataBlocks dataBlocks;

		public DataBlocks getDataBlocks() {
			return dataBlocks;
		}

		public void setDataBlocks(DataBlocks dataBlocks) {
			this.dataBlocks = dataBlocks;
		}
	}

	@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
	public static class DataBlocks {
		@JsonProperty("YZ_COMMONS")
		private YZCommons YZ_COMMONS;
		@JsonProperty("YZ_INVOICE")
		private YZInvoice YZ_INVOICE;

		public YZCommons getYZ_COMMONS() {
			return YZ_COMMONS;
		}

		public void setYZ_COMMONS(YZCommons YZ_COMMONS) {
			this.YZ_COMMONS = YZ_COMMONS;
		}

		public YZInvoice getYZ_INVOICE() {
			return YZ_INVOICE;
		}

		public void setYZ_INVOICE(YZInvoice YZ_INVOICE) {
			this.YZ_INVOICE = YZ_INVOICE;
		}
	}

	@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
	public static class YZCommons {
		@JsonProperty("YZ_NAME")
		private YZField<String> YZ_NAME;
		@JsonProperty("YZ_ORGANIZATIONAL_UNIT")
		private YZField<String> YZ_ORGANIZATIONAL_UNIT;
		@JsonProperty("YZ_DOCUMENT_TYPE")
		private YZField<String> YZ_DOCUMENT_TYPE = new YZField<String>("YZ_PURCHASE_INVOICE_ON_ORDER");
		@JsonProperty("YZ_THIRD")
		private YZField<String> YZ_THIRD;
		@JsonProperty("YZ_THIRD_ACCOUNT")
		private YZField<String> YZ_THIRD_ACCOUNT= new YZField<String>("099-21000");
		@JsonProperty("YZ_DATE")
		private YZField<LocalDate> YZ_DATE;
		@JsonProperty("YZ_DUE_DATE")
		private YZField<LocalDate> YZ_DUE_DATE;
		@JsonProperty("YZ_NUMBER")
		private YZField<String> YZ_NUMBER;
		@JsonProperty("YZ_AMOUNT")
		private YZField<BigDecimal> YZ_AMOUNT;
		@JsonProperty("YZ_TAX_AMOUNT")
		private YZField<BigDecimal> YZ_TAX_AMOUNT;
		@JsonProperty("YZ_TOTAL_AMOUNT")
		private YZField<BigDecimal> YZ_TOTAL_AMOUNT;
		@JsonProperty("YZ_CURRENCY")
		private YZField<String> YZ_CURRENCY;
		@JsonProperty("YZ_PAYMENT_METHOD")
		private YZField<String> YZ_PAYMENT_METHOD;
		@JsonProperty("YZ_RECEPTION_DATE")
		private YZField<LocalDate> YZ_RECEPTION_DATE;

		public YZField<String> getYZ_NAME() {
			return YZ_NAME;
		}

		public void setYZ_NAME(YZField<String> yZ_NAME) {
			YZ_NAME = yZ_NAME;
		}

		public YZField<String> getYZ_ORGANIZATIONAL_UNIT() {
			return YZ_ORGANIZATIONAL_UNIT;
		}

		public void setYZ_ORGANIZATIONAL_UNIT(YZField<String> yZ_ORGANIZATIONAL_UNIT) {
			YZ_ORGANIZATIONAL_UNIT = yZ_ORGANIZATIONAL_UNIT;
		}

		public YZField<String> getYZ_DOCUMENT_TYPE() {
			return YZ_DOCUMENT_TYPE;
		}

		public void setYZ_DOCUMENT_TYPE(YZField<String> yZ_DOCUMENT_TYPE) {
			YZ_DOCUMENT_TYPE = yZ_DOCUMENT_TYPE;
		}

		public YZField<String> getYZ_THIRD() {
			return YZ_THIRD;
		}

		public void setYZ_THIRD(YZField<String> yZ_THIRD) {
			YZ_THIRD = yZ_THIRD;
		}

		public YZField<String> getYZ_THIRD_ACCOUNT() {
			return YZ_THIRD_ACCOUNT;
		}

		public void setYZ_THIRD_ACCOUNT(YZField<String> yZ_THIRD_ACCOUNT) {
			YZ_THIRD_ACCOUNT = yZ_THIRD_ACCOUNT;
		}

		public YZField<LocalDate> getYZ_DATE() {
			return YZ_DATE;
		}

		public void setYZ_DATE(YZField<LocalDate> yZ_DATE) {
			YZ_DATE = yZ_DATE;
		}

		public YZField<LocalDate> getYZ_DUE_DATE() {
			return YZ_DUE_DATE;
		}

		public void setYZ_DUE_DATE(YZField<LocalDate> yZ_DUE_DATE) {
			YZ_DUE_DATE = yZ_DUE_DATE;
		}

		public YZField<String> getYZ_NUMBER() {
			return YZ_NUMBER;
		}

		public void setYZ_NUMBER(YZField<String> yZ_NUMBER) {
			YZ_NUMBER = yZ_NUMBER;
		}

		public YZField<BigDecimal> getYZ_AMOUNT() {
			return YZ_AMOUNT;
		}

		public void setYZ_AMOUNT(YZField<BigDecimal> yZ_AMOUNT) {
			YZ_AMOUNT = yZ_AMOUNT;
		}

		public YZField<BigDecimal> getYZ_TAX_AMOUNT() {
			return YZ_TAX_AMOUNT;
		}

		public void setYZ_TAX_AMOUNT(YZField<BigDecimal> yZ_TAX_AMOUNT) {
			YZ_TAX_AMOUNT = yZ_TAX_AMOUNT;
		}

		public YZField<BigDecimal> getYZ_TOTAL_AMOUNT() {
			return YZ_TOTAL_AMOUNT;
		}

		public void setYZ_TOTAL_AMOUNT(YZField<BigDecimal> yZ_TOTAL_AMOUNT) {
			YZ_TOTAL_AMOUNT = yZ_TOTAL_AMOUNT;
		}

		public YZField<String> getYZ_CURRENCY() {
			return YZ_CURRENCY;
		}

		public void setYZ_CURRENCY(YZField<String> yZ_CURRENCY) {
			YZ_CURRENCY = yZ_CURRENCY;
		}

		public void setYZ_PAYMENT_METHOD(YZField<String> yZ_PAYMENT_METHOD) {
			YZ_PAYMENT_METHOD = yZ_PAYMENT_METHOD;
			
		}

		public void setYZ_RECEPTION_DATE(YZField<LocalDate> yZ_RECEPTION_DATE) {
			YZ_RECEPTION_DATE = yZ_RECEPTION_DATE;
		}

	}

	@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
	public static class YZInvoice {
		@JsonProperty("CUSTOM_1")
		private YZField<String> CUSTOM_1;
		@JsonProperty("CUSTOM_2")
		private YZField<String> CUSTOM_2;
		@JsonProperty("YZ_DEDUCED_AMOUNT")
		private YZField<BigDecimal> YZ_DEDUCED_AMOUNT;
		@JsonProperty("YZ_PURCHASE_ORDER_NUMBER")
		private YZField<String> YZ_PURCHASE_ORDER_NUMBER;

		public YZField<String> getCUSTOM_1() {
			return CUSTOM_1;
		}

		public void setCUSTOM_1(YZField<String> cUSTOM_1) {
			CUSTOM_1 = cUSTOM_1;
		}
		
		public YZField<String> getCUSTOM_2() {
			return CUSTOM_2;
		}

		public void setCUSTOM_2(YZField<String> cUSTOM_2) {
			CUSTOM_2 = cUSTOM_2;
		}

		public YZField<BigDecimal> getYZ_DEDUCED_AMOUNT() {
			return YZ_DEDUCED_AMOUNT;
		}

		public void setYZ_DEDUCED_AMOUNT(YZField<BigDecimal> yZ_DEDUCED_AMOUNT) {
			YZ_DEDUCED_AMOUNT = yZ_DEDUCED_AMOUNT;
		}

		public YZField<String> getYZ_PURCHASE_ORDER_NUMBER() {
			return YZ_PURCHASE_ORDER_NUMBER;
		}

		public void setYZ_PURCHASE_ORDER_NUMBER(YZField<String> yZ_PURCHASE_ORDER_NUMBER) {
			YZ_PURCHASE_ORDER_NUMBER = yZ_PURCHASE_ORDER_NUMBER;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL) 
	public static class YZField<T> {
		@JsonProperty("value")
		private T value;
		@JsonProperty("setAutomatically")
		private Boolean setAutomatically;
		@JsonProperty("setByAutoAssignment")
		private Boolean setByAutoAssignment;

		public YZField(T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}

		public Boolean getSetAutomatically() {
			return setAutomatically;
		}

		public void setSetAutomatically(Boolean setAutomatically) {
			this.setAutomatically = setAutomatically;
		}

		public Boolean getSetByAutoAssignment() {
			return setByAutoAssignment;
		}

		public void setSetByAutoAssignment(Boolean setByAutoAssignment) {
			this.setByAutoAssignment = setByAutoAssignment;
		}
	}
	@JsonInclude(JsonInclude.Include.NON_NULL) 
	public static class YZLine<T> {
		@JsonProperty("value")
		private T value;
		@JsonProperty("lineNumber")
		private String lineNumber;
		@JsonProperty("qty")
		private String qty;
		@JsonProperty("UOM")
		private String UOM;
		@JsonProperty("unitPrice")
		private String unitPrice;
		@JsonProperty("buyerCode")
		private String buyerCode;
		@JsonProperty("productCode")
		private String productCode;
	}
}
