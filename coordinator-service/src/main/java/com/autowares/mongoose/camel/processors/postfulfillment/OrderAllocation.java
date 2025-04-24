package com.autowares.mongoose.camel.processors.postfulfillment;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import com.autowares.apis.partservice.viperpartupdate.PartUpdate;
import com.autowares.mongoose.command.ViperPartUpdateClient;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.service.NotificationService;
import com.autowares.notification.model.SimpleNotificationRequestBuilder;
import com.autowares.servicescommon.model.LocationType;

@Component
public class OrderAllocation implements Processor {

	ViperPartUpdateClient viperPartUpdateClient = new ViperPartUpdateClient();

	@Autowired
	private NotificationService notificationClient;
	
	@Value("${spring.profiles.active:local}")
    private String activeProfile;

	Logger log = LoggerFactory.getLogger(OrderAllocation.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		Availability availability = exchange.getIn().getBody(Availability.class);
		LineItemContext detail = availability.getLineItem();

		// filling at this location.
		if (LocationType.Warehouse.equals(availability.getFulfillmentLocation().getLocationType())) {
			Integer warehouseNumber = (int) availability.getPartAvailability().getWarehouseNumber();
			PartUpdate partUpdate = new PartUpdate();
			partUpdate.setVendorCode(detail.getVendorCodeSubCode());
			partUpdate.setPartNumber(detail.getPartNumber());
			partUpdate.setWarehouseNumber(String.valueOf(warehouseNumber));
			partUpdate.setColumnName1("Allocate");
			partUpdate.setColumnValue1(String.valueOf(availability.getFillQuantity()));
			String response = null;

			try {
				response = viperPartUpdateClient.updatePart(partUpdate);
			} catch (RemoteTimeoutException e) {
				SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
						.withMessage("Detail line number : " +  detail.getLineNumber() + " stack trace : \n" + ExceptionUtils.getStackTrace(e))
						.withSubject("Allocation timeout " + activeProfile + " orderId : " + detail.getContext().getDocumentId())
						.withSender("noreply@autowares.com")
						.withRecipientEmailAddress("kcml@autowares.com");
				notificationClient.notify(requestBuilder.build());
				throw new AbortProcessingException(e);
			} catch (ResourceAccessException e) {
				SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
						.withMessage("Detail line number : " +  detail.getLineNumber() + " stack trace : \n" + ExceptionUtils.getStackTrace(e))
						.withSubject("Allocation resource access exception " + activeProfile + " orderId : " + detail.getContext().getDocumentId())
						.withSender("noreply@autowares.com")
						.withRecipientEmailAddress("kcml@autowares.com");
				notificationClient.notify(requestBuilder.build());
				throw new RetryLaterException(e.getMessage());
			} catch (Exception e) {
				SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
						.withMessage("Detail line number : " +  detail.getLineNumber() + " stack trace : \n" + ExceptionUtils.getStackTrace(e))
						.withSubject("Allocation exception " + activeProfile + " orderId : " + detail.getContext().getDocumentId())
						.withSender("noreply@autowares.com")
						.withRecipientEmailAddress("kcml@autowares.com");
				notificationClient.notify(requestBuilder.build());
				throw new RetryException(e.getMessage());
			}

			if (response != null && !response.equals("OK")) {
				availability.getFulfillmentLocation().getOrder().updateProcessingLog("Zeroing line #: "
						+ detail.getLineNumber() + " in warehouse: " + warehouseNumber + " due to failure to allocate");
				availability.setFillQuantity(0);
				availability.getFulfillments().clear();
			}
		}

	}

}
