package com.autowares.mongoose.camel.processors.errorhandling;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.autowares.mongoose.exception.UnimplementedException;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.service.NotificationService;
import com.autowares.notification.model.SimpleNotificationRequestBuilder;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.TransactionStatus;
import com.autowares.servicescommon.model.WorkingDocument;

@Component
public class ErrorNotification implements Processor {

	@Autowired
	private NotificationService notificationClient;

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	@Override
	public void process(Exchange exchange) throws Exception {

		String message = "Unknown Error Exception";
		Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
		if (caused != null) {
			if (caused instanceof UnimplementedException) {
				return;
			}
			message = ExceptionUtils.getStackTrace(caused);
		}
		message = " route: " + exchange.getFromEndpoint() + "\n  " + message;

		if (exchange.getIn().getBody() instanceof Document) {
			Document document = exchange.getIn().getBody(Document.class);
			SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
					.withMessage(message)
					.withSubject("Coordinator " + activeProfile + " xmlOrderId: " + document.getDocumentId())
					.withSender("noreply@autowares.com");
			if (document instanceof DocumentContext) {
				document = ((DocumentContext) document).getSourceDocument();
			}
			if (document instanceof WorkingDocument) {
				WorkingDocument workingDocument = (WorkingDocument) document;
				if (TransactionStatus.Error == workingDocument.getTransactionStatus()) {

					if ((!"prod".equals(activeProfile)) && caused.getCause() instanceof HttpClientErrorException) {
						HttpClientErrorException clientError = (HttpClientErrorException) caused.getCause();
						if (409 == clientError.getRawStatusCode()) {
							return;
						}
					}

					if ((!"prod".equals(activeProfile)) && caused instanceof RuntimeException) {
						RuntimeException clientError = (RuntimeException) caused;
						if (clientError.getMessage() != null && clientError.getMessage().startsWith("Allocation")) {
							return;
						}
					}
				}
				requestBuilder.withRecipientEmailAddress("kcml@autowares.com");
				notificationClient.notify(requestBuilder.build());
			}

		}
	}
}
