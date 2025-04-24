package com.autowares.mongoose.camel.processors.nonstock;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.ContextToMotorstatePurchaseOrder;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.motorstateservice.client.MotorstatePurchaseOrderClient;
import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;
import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class SendMotorstateValidatePO implements Processor {

	// Here is where we'll take the Motorstate PO that we've built previously
	// and send it to Motorstate's Validate endpoint. Use the Motorstate Context.

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

	public void process(Exchange exchange) {

		FulfillmentLocationContext motorstateContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
		
		if (exchange.getIn().getBody() instanceof FulfillmentLocationContext) {
			motorstateContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
		}

		if (exchange.getIn().getBody() instanceof DocumentContext) {
			motorstateContext = exchange.getIn().getBody(DocumentContext.class).getFulfillmentLocationContext();
		}

		MotorstatePurchaseOrder mPO = ContextToMotorstatePurchaseOrder.contextToMotorstatePurchaseOrder(motorstateContext);
		// The converter (@Converter) will build our Motorstate PO. We'll use this
		// module to contact
		// Motorstate after that.

		log.info("----  Before sending VALIDATE PO to Motorstate.  Inside SendMotorstateValidatePO.");
		// Send the 'Validate' PO to Motorstate.

		if (mPO.getLines().size() > 0) {
			// Verify we have any valid lines to send.

			MotorstatePurchaseOrder motorstateValidateResponsePO = motorstatePurchaseOrderClient
					.validateMotorstatePO(mPO);
			// Do a 'VALIDATE' first. This just sends a test order request to Motorstate
			// which is basically an order, but as a 'test' to check for inventory, bad part
			// numbers, etc.
			System.out.println("Inside send validate checking for no response.");
			PrettyPrint.print(motorstateValidateResponsePO);
			Boolean hasErrors = false;
			if (motorstateValidateResponsePO.getHasErrors() != null) {
				hasErrors = motorstateValidateResponsePO.getHasErrors();
			}
			if(hasErrors) {
				//  Set Fields based on this.  Will probably have to go through
				//  each Warning response to match records up to their respective
				//  errors so that we can get the correct error into each part for
				//  the PER people.
				log.info("Validate has errors!");
			}
			
			exchange.getIn().setHeader("motorstatePurchaseOrder", motorstateValidateResponsePO);
//			motorstateContext.setMotorstatePurchaseOrder(motorstateValidateResponsePO);
			exchange.getIn().setBody(motorstateContext);
			if (motorstateValidateResponsePO != null) {
				exchange.getIn().setHeader("validated", true);
				exchange.getIn().setHeader("sentLivePO", false);
				exchange.getIn().setHeader("noMotorstateResponse", false);
				//  Check for errors here, and populate those fields in the .getOrder()?
			} else {
				// No response from Motorstate.
				exchange.getIn().setHeader("noMotorstateResponse", true);
			}
			// ^ I think that I want these in the code where the response is validated.
		} else {
			log.info("No valid lines to send to Motorstate for VALIDATE.");
		}

	}

	// Next, the MotorstateValidateProcessor will go through
	// the VALIDATE and look for errors, warnings, etc 
	// and remove items from the PO accordingly.
	// If there are any valid items on the PO, we will then 
	// proceed to the SendMotorstateLivePO
	// processor, which will send the Live PO to Motorstate. 

}
