package com.autowares.mongoose.camel.processors.nonstock;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.motorstateservice.client.MotorstatePurchaseOrderClient;
import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;
import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class SendMotorstateLivePO implements Processor {

	Logger log = LoggerFactory.getLogger(MotorstateProcessor.class);

	@Value("${motorstate.apiurl}")
	String apiUrl;

	@Value("${motorstate.apikey}")
	String apiKey;

	MotorstatePurchaseOrderClient motorstatePurchaseOrderClient;

	@PostConstruct
	public void initializeClients() {
		motorstatePurchaseOrderClient = new MotorstatePurchaseOrderClient(apiUrl, apiKey);
	}

	@Override
	public void process(Exchange exchange) throws Exception {

		MotorstatePurchaseOrder mPO = exchange.getIn().getHeader("motorstatePurchaseOrder",
				MotorstatePurchaseOrder.class);
		// Instead of using the header, use the transaction state of the
		// nonStockContext.
		if (mPO != null) {
			// Only send the Live PO if we received a response from Motorstate in the
			// Validate.
			
			// Only send the Live PO if the Validate didn't have errors.
			boolean validateHasErrors = false;
			if(mPO.getHasErrors() != null) {
				validateHasErrors = mPO.getHasErrors();
			}
			if (!validateHasErrors) {

				log.info("----  Before sending LIVE PO to Motorstate.  Inside SendMotorstateLivePO.");
				// Send the 'Validate' PO to Motorstate.

				if (mPO.getLines().size() > 0) {
					// Verify we have any valid lines to send.
					System.out.println("Inside LIVE PO send.");

					MotorstatePurchaseOrder motorstateLiveResponsePO = motorstatePurchaseOrderClient
							.sendMotorstatePO(mPO);
					// Sending the LIVE PO to Motorstate. We've already validated previously.

					if (motorstateLiveResponsePO != null) {
						Boolean hasErrors = false;
						log.info("Motorstate live response : ");
						PrettyPrint.print(motorstateLiveResponsePO);
						if (motorstateLiveResponsePO.getHasErrors() != null) {
							hasErrors = motorstateLiveResponsePO.getHasErrors();
						}
						if (hasErrors) {
							// Errors in the Live PO send.
							log.info("Live PO has errors.");
						} else {
							// No errors. Set
							log.info("No errors in live PO.");
							String orderId = motorstateLiveResponsePO.getOrderNumber();
							log.info("Order ID = " + orderId);
						}

					}

					exchange.getIn().setHeader("motorstatePurchaseOrder", motorstateLiveResponsePO);
				} else {
					log.info("No valid lines to send to Motorstate for LIVE PO.");
				}
				exchange.getIn().setHeader("sentLivePO", true);
			}
		}
	}

	// Send LIVE PO to Motorstate here using the MotorstateContext.
}
