package com.autowares.mongoose.camel.processors.communication;

import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.service.NotificationService;
import com.autowares.notification.model.SimpleNotificationRequest;
import com.autowares.notification.model.SimpleNotificationRequestBuilder;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.EmailAddress;
import com.autowares.servicescommon.model.PurchaseOrderType;
import com.autowares.supplychain.model.SupplyChainSourceDocument;

@Component
public class YoozCommunicationClient implements Processor {

	@Autowired
	private NotificationService notificationClient;

	private String activeProfile;

	@Override
	public void process(Exchange exchange) throws Exception {

		SimpleNotificationRequest header = exchange.getIn().getHeader("notification", SimpleNotificationRequest.class);

		if (header != null) {
			notificationClient.notify(header);
			return;
		}

		Document document = exchange.getIn().getBody(Document.class);
		String documentId = document.getDocumentId();
		@SuppressWarnings("unused")
		String documentType = null;
		String account = null;
		String accountName = null;
		@SuppressWarnings("unused")
		String vendorName = null;
		String customerPurchaseOrder = null;
		@SuppressWarnings("unused")
		String contactInformation = null;
		Optional<EmailAddress> emailAddress = Optional.empty();

		if (document instanceof SupplyChainSourceDocument) {
			SupplyChainSourceDocument sourceDocument = (SupplyChainSourceDocument) document;
			documentType = sourceDocument.getSourceDocumentType().name();
			if (!PurchaseOrderType.PurchaseOrder.equals(sourceDocument.getPurchaseOrderType())) {
				documentType += " " + sourceDocument.getPurchaseOrderType();
			}
		}
		if (document instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext context = (CoordinatorOrderContext) document;
			documentType = context.getSourceDocumentType().name();
			if (!PurchaseOrderType.PurchaseOrder.equals(context.getOrderType())) {
				documentType += " " + context.getOrderType();
			}
			account = String.valueOf(context.getCustomerNumber());
			accountName = context.getBusinessContext().getBusinessDetail().getBusinessName();
//			TODO - vendorName = context.getSupplier().getBusinessDetail().getBusinessName();
			customerPurchaseOrder = context.getCustomerDocumentId();
//			TODO - contract information
			if (context.getBusinessContext() != null) {
				List<EmailAddress> emails = context.getBusinessContext().getBusinessDetail().getEmailAddresses();
				emailAddress = emails.stream().filter(i -> 'P' == (i.getEmailTypeCode())).findAny();
			}
		}

		String urlBase = "prod".equals(activeProfile) ? "https://www.autowaresgroup.com" : "https://order-admin.test.sd";
		String defaultMessage = String.format("%s/order-admin/#/warehouseorderdetail/%s", urlBase, documentId);
		if (account != null) {
			defaultMessage = defaultMessage + "\n\n" + "Account Number " + account;
		}
		if (accountName != null) {
			defaultMessage = defaultMessage + "\n\n" + "Account Name " + accountName;
		}
		if (customerPurchaseOrder != null) {
			defaultMessage = defaultMessage + "\n\n" + "Customer Purchase Order " + customerPurchaseOrder;
		}
		if(emailAddress.isPresent()) {
			defaultMessage = defaultMessage + "\n\n" + "Email sent for " + emailAddress.get().getEmailAddress();
		}
		String message = exchange.getIn().getHeader("notificationMessage",
				defaultMessage,
				String.class);
		String subject = exchange.getIn().getHeader("notificationSubject", "Order status update", String.class);
		String envRecipient = "kcml@autowares.com";
		String recipient = exchange.getIn().getHeader("notificationRecipient", envRecipient, String.class);
		SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
				.withMessage(message)
				.withSubject(subject)
				.withSender("noreply@autowares.com");

		requestBuilder.withRecipientEmailAddress(recipient);
		notificationClient.notify(requestBuilder.build());
	}

}
