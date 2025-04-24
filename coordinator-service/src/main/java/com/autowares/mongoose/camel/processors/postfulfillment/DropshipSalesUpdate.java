package com.autowares.mongoose.camel.processors.postfulfillment;

import java.util.List;
import java.util.Optional;

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

import com.autowares.apis.partservice.viperpartupdate.PartUpdate;
import com.autowares.mongoose.command.ViperPartUpdateClient;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.service.NotificationService;
import com.autowares.mongoose.utils.MatchingUtils;
import com.autowares.notification.model.SimpleNotificationRequestBuilder;

@Component
public class DropshipSalesUpdate implements Processor {

	ViperPartUpdateClient viperPartUpdateClient = new ViperPartUpdateClient();

	@Autowired
	private NotificationService notificationClient;

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	Logger log = LoggerFactory.getLogger(DropshipSalesUpdate.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		if (exchange.getIn().getBody() instanceof DocumentContext) {
			DocumentContext context = (DocumentContext) exchange.getIn().getBody(DocumentContext.class);
			for (LineItemContextImpl lineItem : context.getLineItems()) {
				String response = null;

				try {
//					For multiple vendors will need to match on the vendor id to get the correct procurementGroupContext
					Optional<LineItemContextImpl> matchedLineItem = Optional.empty();
					if (context.getProcurementGroupContexts() != null && !context.getProcurementGroupContexts().isEmpty() && context.getProcurementGroupContexts().get(0).getSupplierContext() != null) {
						List<LineItemContextImpl> lineItems = context.getProcurementGroupContexts().get(0)
								.getSupplierContext().getOrderContext().getLineItems();
						matchedLineItem = MatchingUtils.matchByLineNumber(lineItem, lineItems);
					}
					if (matchedLineItem.isPresent()) {
						PartUpdate partUpdate = new PartUpdate();
						partUpdate.setVendorCode(matchedLineItem.get().getVendorCodeSubCode());
						partUpdate.setPartNumber(matchedLineItem.get().getPartNumber());
						partUpdate.setColumnName1("DS_SALES");
						partUpdate.setColumnValue1(matchedLineItem.get().getQuantity().toString());
						
						if (partUpdate.getVendorCode() != null && partUpdate.getPartNumber() != null) {
							response = viperPartUpdateClient.updatePart(partUpdate);
						}
					}
				} catch (RemoteTimeoutException e) {
					SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
							.withMessage("Detail line number : " + lineItem.getLineNumber() + " stack trace : \n"
									+ ExceptionUtils.getStackTrace(e))
							.withSubject("Dropship sales update timeout " + activeProfile + " orderId : "
									+ context.getContext().getDocumentId())
							.withSender("noreply@autowares.com").withRecipientEmailAddress("kcml@autowares.com");
					notificationClient.notify(requestBuilder.build());
					throw new AbortProcessingException(e);
				} catch (Exception e) {
					SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
							.withMessage("Detail line number : " + lineItem.getLineNumber() + " stack trace : \n"
									+ ExceptionUtils.getStackTrace(e))
							.withSubject("Dropship Sales Update exception " + activeProfile + " orderId : "
									+ context.getContext().getDocumentId())
							.withSender("noreply@autowares.com").withRecipientEmailAddress("kcml@autowares.com");
					notificationClient.notify(requestBuilder.build());
					throw new RetryException(e.getMessage());
				}

				if (response != null && !response.equals("OK")) {
					log.debug("Dropship Sales Update failed.  orderId = " + context.getContext().getDocumentId());
				}
			}
		}
	}
}
