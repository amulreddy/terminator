package com.autowares.mongoose.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.ServiceDiscovery;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.orders.clients.OrderClient;
import com.autowares.orders.model.Item;
import com.autowares.servicescommon.util.DateConversion;

@Component
public class ViperToWmsCoreUtils {

	private static Logger log = LoggerFactory.getLogger(ViperToWmsCoreUtils.class);

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	private OrderClient orderProd = new OrderClient();
	private OrderClient orderTest = new OrderClient();

	public ViperToWmsCoreUtils() {
		orderProd.withServiceDomain("consul");
		orderTest.withServiceDomain("testconsul");
//		orderProd.withLocalService();
	}

	public Item resolveOrderId(Item order) {
		// In test, we need to look up what the OrderItemId is, we are getting prod
		// events in test.
		if (ServiceDiscovery.getDomain().equals("testconsul")) {

			Item prodOrder = order;// prod.getItemById(order.getOrderItemId().longValue());
			if (prodOrder != null) {
				Item testOrder = null;
				try {
					testOrder = lookupTestOrderByXmlOrderIdLineNumberBuilding(prodOrder.getOrder().getXmlOrderId(),
							prodOrder.getCustomerLineNumber(), prodOrder.getBuilding());

				} catch (Exception e) {
					throw new RetryLaterException(
							"Failed to find test order for xmlOrderId: " + prodOrder.getOrder().getXmlOrderId(), e);
				}

				if (testOrder != null) {
					log.trace("Test order: " + testOrder.getOrder().getOrderId());
					log.trace("Prod order: " + prodOrder.getOrder().getOrderId());

					order = testOrder;
				} else {
					return null;
				}
			}

		}
		return order;
	}

	public Item lookupProdOrderByDetails(Long orderItemId) {
		return lookupOrderByDetails(orderItemId, orderProd);
	}

	public Item lookupTestOrderByDetails(Long orderItemId) {
		return lookupOrderByDetails(orderItemId, orderTest);
	}

	public Item lookupOrderByDetails(Long orderItemId, OrderClient orderClient) {
		try {
			return orderClient.getItem(orderItemId).get();
		} catch (Exception e) {
			throw new RetryLaterException(e);
		}
	}

	public Item lookupProdOrderByDetails(String customerNumber, String partNumber, String vendorCode, Date orderTime) {
		return lookupOrderByDetails(customerNumber, partNumber, vendorCode, orderTime, orderProd);
	}

	public Item lookupTestOrderByDetails(String customerNumber, String partNumber, String vendorCode, Date orderTime) {
		return lookupOrderByDetails(customerNumber, partNumber, vendorCode, orderTime, orderTest);
	}

	public Item lookupOrderByDetails(String customerNumber, String partNumber, String vendorCode, Date orderTime,
			OrderClient client) {
		try {
			Page<Item> items = client.findItems(Integer.valueOf(customerNumber), null, vendorCode, partNumber, null,
					null, DateConversion.convert(orderTime), DateConversion.convert(orderTime), null, 0, 1);
//					client.findOrderItem(partNumber, vendorCode,
//					customerNumber, null, null, orderTime);

			if (items.getTotalElements() != 1) {
				System.err.println(
						"Too many elements: customer=" + customerNumber + " partNumber=" + partNumber + " vendorCode="
								+ vendorCode + " orderTime=" + orderTime + " found=" + items.getTotalElements());
				for (Item i : items.getContent()) {
					System.err.print(i.getBuilding() + " transfer:" + i.getTransferFlag() + " ship="
							+ i.getShipQuantity() + " orderQty=" + i.getOrderQuantity());
				}
			}
			if (items.getTotalElements() > 0) {
				return items.getContent().iterator().next();
			}
		} catch (Exception e) {
			throw new RetryLaterException(e);
		}
		return null;
	}

	public Item lookupProdOrderByXmlOrderIdLineNumberBuilding(String xmlOrderId, int lineNumber, String buildingCode) {
		return lookupOrderByXmlOrderIdLineNumberBuilding(xmlOrderId, lineNumber, buildingCode, orderProd);
	}

	public Item lookupTestOrderByXmlOrderIdLineNumberBuilding(String xmlOrderId, int lineNumber, String buildingCode) {
		return lookupOrderByXmlOrderIdLineNumberBuilding(xmlOrderId, lineNumber, buildingCode, orderTest);
	}

	public Item lookupOrderByXmlOrderIdLineNumberBuilding(String xmlOrderId, int lineNumber, String buildingCode,
			OrderClient client) {
		Page<Item> results = client.getOrderByXMLOrderId(buildingCode, xmlOrderId, lineNumber);
		if (results != null && results.hasContent() && results.getTotalElements() == 1) {
			return results.getContent().get(0);
		}
		return null;
	}

	public Item lookupProdOrderByPackSlipIdLineNumber(String packSlipId, int lineNumber) {
		return lookupOrderByPackSlipIdLineNumber(packSlipId, lineNumber, orderProd);
	}

	public Item lookupTestOrderByPackSlipIdLineNumber(String packSlipId, int lineNumber) {
		return lookupOrderByPackSlipIdLineNumber(packSlipId, lineNumber, orderTest);
	}

	public Item lookupOrderByPackSlipIdLineNumber(String packSlipId, int lineNumber, OrderClient client) {
		Page<Item> results = client.findItem(packSlipId, lineNumber);
		if (results != null && results.hasContent() && results.getTotalElements() == 1) {
			return results.getContent().get(0);
		}
		return null;

	}

	public OrderClient getClient() {
		if ("prod".equals(activeProfile)) {
			return orderProd;
		}
		return orderTest;
	}

	public OrderClient getOrderProd() {
		return orderProd;
	}

	public OrderClient getOrderTest() {
		return orderTest;
	}

}
