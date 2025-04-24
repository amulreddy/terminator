package com.autowares.mongoose.camel.components;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.BusinessBase;
import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.mongoose.exception.ContinueProcessingException;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.Shop;
import com.autowares.myplaceintegration.model.MyPlaceBaseNotification;
import com.autowares.myplaceintegration.model.MyPlaceOrderNotification;

@Component
@Converter(generateLoader = true)
public class MyPlaceNotificationTypeConverters implements TypeConverters {

	@Converter
	public static MyPlaceOrderNotification coordinatorOrderContextToMyplaceNotification(
			FulfillmentLocationContext fillContext) {
		CoordinatorOrderContext context = (CoordinatorOrderContext) fillContext.getOrder();

		MyPlaceOrderNotification orderNotification = new MyPlaceOrderNotification();
		orderNotification.setDocumentId(fillContext.getDocumentId());
		orderNotification.setStatus("Order Processed");
		orderNotification.setOrderNumber(context.getDocumentId());
		orderNotification.setPurchaseOrderNumber(context.getPurchaseOrder());
		orderNotification.setOrderDate(context.getOrderTime());
		orderNotification.setExpectedDeliveryTime(fillContext.getArrivalDate());
		if (context.getOrderSource() != null) {
			orderNotification.setOrigin(context.getOrderSource().name());
		}
		if (fillContext.getDeliveryRunType() != null) {
			orderNotification.setShipVia(fillContext.getDeliveryRunType().name());
		}
		if (fillContext.getWarehouseMaster() != null) {
			BusinessBase businessDetail = fillContext.getWarehouseMaster().getBusinessBase();
			orderNotification.setShipFromName(fillContext.getWarehouseMaster().getWarehouseName());
			orderNotification.setShipFromAddress(businessDetail.getAddress() + ", " + businessDetail.getCity() + ", "
					+ businessDetail.getStateProv());
		}
		if (context.getBusinessContext() != null) {
			BusinessDetail businessDetail = context.getBusinessContext().getBusinessDetail();
			orderNotification.setCustomerId(String.valueOf(businessDetail.getBusEntId()));
			orderNotification.setSellFromId(String.valueOf(context.getCustomerNumber()));
			orderNotification.setSellFromName(businessDetail.getBusinessName());
			orderNotification.setShipToAddress(businessDetail.getAddress() + ", " + businessDetail.getCity() + ", "
					+ businessDetail.getStateProv());
			if (context.getBusinessContext().getShop() != null) {
				Shop shop = context.getBusinessContext().getShop();
				orderNotification.setSellToId(shop.getStoreAccountNumber());
				orderNotification.setSellToName(shop.getBusinessName());
				orderNotification.setCustomerName(shop.getBusinessName());
				orderNotification.setCustomerId(shop.getStoreAccountNumber());
			}
		}

		if (fillContext.getFulfillmentDetails() != null && !fillContext.getFulfillmentDetails().isEmpty()) {
			orderNotification.setTotalCount(
					fillContext.getFulfillmentDetails().stream().mapToInt(i -> i.getFillQuantity()).sum());
			orderNotification
					.setTotalCost(
							BigDecimal
									.valueOf(fillContext.getFulfillmentDetails().stream()
											.mapToDouble(i -> i.getLineItem().getPart().getListPrice()
													.multiply(BigDecimal.valueOf(i.getFillQuantity())).doubleValue())
											.sum()));
		}
		return orderNotification;
	}

	@SuppressWarnings("unchecked")
	public static MyPlaceOrderNotification cwOrderEventToMyplaceNotification(Map<String, Object> event) {

		if (event.get("deliveryTracking") != null && event.get("deliveryTracking") instanceof Map) {

			Map<String, Object> deliveryTracking = (Map<String, Object>) event.get("deliveryTracking");

			MyPlaceOrderNotification orderNotification = new MyPlaceOrderNotification();
			orderNotification.setStatus("Order Processed"); 
			orderNotification.setDocumentId(getValue(deliveryTracking, "storeId") + "-" + getValue(deliveryTracking, "invoiceNumber"));
			orderNotification.setCustomerId(getValue(deliveryTracking, "customerId"));
			orderNotification.setInvoiceNumber(getValue(deliveryTracking, "invoiceNumber"));
			orderNotification.setConfirmationNumber(getValue(deliveryTracking,"invoiceNumber"));
			orderNotification.setSellFromId(getValue(deliveryTracking, "storeId"));
			//orderNotification.setMemberId(getValue(deliveryTracking, "storeId"));
			// orderNotification.setInvoiceDate(event.get("invoiceDate").toString());
			orderNotification.setSellToName(getValue(deliveryTracking, "name"));
			orderNotification.setSellToId(getValue(deliveryTracking, "customerId"));
			orderNotification.setShipToAddress(getValue(deliveryTracking,"addr1") + " " + getValue(deliveryTracking, "city") + " "
					+ getValue(deliveryTracking,"zip"));
			
			orderNotification.setOrderDate(getZonedDateTime(deliveryTracking,"orderTime"));
			orderNotification.setInvoiceDate(getZonedDateTime(deliveryTracking, "invoiceDate"));
			orderNotification.setCustomerName(getValue(deliveryTracking, "name"));
			// orderNotification.setTotalCost(event.get("total"));

			return orderNotification;
		}
		
		throw new ContinueProcessingException("event doesnt contain deliveryTracking data");
	}
	
	@Converter
	public static MyPlaceBaseNotification cwEventToMyplaceNotification(Map<String, Object> event) {
		String eventType = getValue(event,"eventType");
		if(eventType.equals("DashCwOrderEvent")) {
			return cwOrderEventToMyplaceNotification(event);
		}
		
		if(eventType.equals("DashDispatchStoreDriver")) {
//			return cwOrderEventToMyplaceNotification(event);
		}
		
		if(eventType.equals("DashDeliveredPartToCust")) {
//			return cwOrderEventToMyplaceNotification(event);
		}
		
		throw new ContinueProcessingException("event doesnt contain deliveryTracking data");
	}
	
	
	private static String getValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if(value == null) {
			return null;
		}
		return String.valueOf(value);
	}
	
	public static ZonedDateTime getZonedDateTime(Map<String, Object> map, String key) {
		String value = getValue(map, key);
		if(value == null) {
			return null;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a",Locale.US);
		LocalDateTime localDateTime = LocalDateTime.parse(value.toUpperCase(),formatter);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("America/New_York"));
		
		return zonedDateTime;
	}

}


