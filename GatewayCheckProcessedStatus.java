package com.autowares.mongoose.camel.processors.gateway;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.service.NotificationService;
import com.autowares.notification.model.SimpleNotificationRequestBuilder;
import com.autowares.xmlgateway.client.XmlGatewayCommand;
import com.autowares.xmlgateway.model.GatewayOrder;

@Component
public class GatewayCheckProcessedStatus implements Processor {

	private XmlGatewayCommand xmlGatewayCommand = new XmlGatewayCommand();

	@Autowired
	private NotificationService notification;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

	@Override
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext context = exchange.getIn().getBody(CoordinatorContext.class);
		if (context != null && context instanceof CoordinatorOrderContext) {
			GatewayOrder orderInput = exchange.getIn().getHeader("gatewayOrder", GatewayOrder.class);
			if (context.getDocumentId() != null && orderInput != null) {
				Optional<GatewayOrder> optionalOrder = xmlGatewayCommand
						.getOrder(Long.valueOf(context.getDocumentId()));
				if (optionalOrder.isPresent()) {
					GatewayOrder order = optionalOrder.get();

					if (order.getProcessedFlag() || order.getProcessInViper()) {
		                SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
		                        .withMessage("Trying to process an already processed order.  id = " + order.getXmlOrderId())
		                        .withSubject("Coordinator " + activeProfile)
		                        .withSender("noreply@autowares.com");
						notification.notify(requestBuilder.build());

						throw new RetryLaterException("Trying to process an already processed order.");
					}
				}
			}
		}
	}

}
