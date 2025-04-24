package com.autowares.mongoose.camel.processors.lookup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.model.Origcode;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorOrderContextImpl;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.motorstateservice.client.MotorStateProductClient;
import com.autowares.motorstateservice.model.MotorStateProductResponse;
import com.autowares.motorstateservice.model.Product;
import com.autowares.servicescommon.model.PartLineItemImpl;
import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class LookUpMotorstatePart implements Processor {

	Logger log = LoggerFactory.getLogger(LookUpMotorstatePart.class);

	@Value("${motorstate.apiurl}")
	String apiUrl;

	@Value("${motorstate.apikey}")
	String apiKey;

	MotorStateProductClient motorstateProductClient;

	@PostConstruct
	public void initializeClients() {
		motorstateProductClient = new MotorStateProductClient(apiUrl, apiKey);
	}

	public void process(Exchange exchange) {

		// Look up the requested part(s) @ Motorstate to determine if
		// the part(s) is/are good with them.
		// The only thing that we're "saving" here is the .invalid( on the
		// CoordinatorContext
		// for use later in the process.

		log.info("Look up Motorstate part(s) via MotorstateProduct Service.");
		FulfillmentLocationContext fulfillmentLocation = null;

		if (exchange.getIn().getBody() instanceof FulfillmentLocationContext) {
			fulfillmentLocation = exchange.getIn().getBody(FulfillmentLocationContext.class);
		}

		if (exchange.getIn().getBody() instanceof DocumentContext) {
			fulfillmentLocation = exchange.getIn().getBody(DocumentContext.class).getFulfillmentLocationContext();
		}
		
		// Warehouse that we're looking to fill from. In this case, w06/Motorstate.

		Integer counter = 0;
		List<String> msProductPart = new ArrayList<String>(); // Array list of parts that we will send to MS.
		Map<String, Availability> correlationMap = new HashMap<>();
		CoordinatorOrderContextImpl nonStockContext = new CoordinatorOrderContextImpl();
		fulfillmentLocation.setNonStockContext(nonStockContext);

		for (Availability fulfillment : fulfillmentLocation.getLineItemAvailability()) {
			// Build String of linecode/pns to send to MS product endpoint.
			// After they respond, we'll go through the response and match up the parts.
			// These are the individual VC/PNs for w06/Motorstate that we're attempting
			// to fill.
			counter++;
			LineItemContext lineItem = fulfillment.getLineItem();
			// Line item on main context. Customer's line.

			Long mPHId = null;
			try {
				mPHId = lineItem.getPart().getProductId();
			} catch (Exception e) {

			}

			List<Origcode> moaPartOrig = null;
			if (mPHId != null) {
				moaPartOrig = lineItem.getPart().getOrigcodes();
			}

			String tempMsPart = lineItem.getCounterWorksLineCode() + lineItem.getPartNumber();

			if (moaPartOrig != null) {
				// We have ORIGCODE record(s) for this part.

				for (Origcode tempMPO : moaPartOrig) {
					// Go thru the MTRS ORIGCODE record(s), but really, we should only ever have 1.
					if (tempMPO.getSourceSystemCode().equals("MTRS")) {

						// Do a lookup here to verify the ORIGCODE record part number.
						// Maybe we can send email where these records are incorrect?
						tempMsPart = tempMPO.getSourceSystemLineCode() + tempMPO.getSourceSystemPartNumber();
						log.info("ORIGCODE record found for line item # " + counter + ".");
					}
				}
			} else {
				// No ORIGCODE record(s) for this partHdrId.
				// Add this to the Product Client lookup with the part number
				// that we have on file.

				log.info("No origcode record for line item # " + counter + ".");

			}
			msProductPart.add(tempMsPart);
			PrettyPrint.print(msProductPart);
			correlationMap.put(tempMsPart, fulfillment);
			PartLineItemImpl partLineItemImpl = new PartLineItemImpl();
			LineItemContextImpl lineItemContextImpl = new LineItemContextImpl(partLineItemImpl);
			partLineItemImpl.setPartNumber(tempMsPart);
			partLineItemImpl.setLineNumber(counter);
			nonStockContext.getLineItems().add(lineItemContextImpl);
		}

		// Look up all parts here in one shot and then go through the response.
		List<MotorStateProductResponse> mSPR = new ArrayList<>();
		try {
			mSPR = motorstateProductClient.getProduct(msProductPart);
		} catch (Exception e) {
			String msg = "Failed call to lookup product at motorstate: " + msProductPart;
			fulfillmentLocation.getOrder().updateProcessingLog(msg);
			log.error(msg);
			throw new RetryLaterException(msg);
		}
		// Send our List of Parts (one big string - msProductPart) to Motorstate for
		// validation.
		if (mSPR != null) {
			// Loop thru part record(s) in the response - if we received one.
			// Also update the line code used in the context if it changed b/c of an
			// origcode. We will want to use that later on in building the Motorstate
			// PO and we won't want to do any calls to Motorstate for this purpose later
			// in the process.
			for (MotorStateProductResponse motorstateResponse : mSPR) {
				// Loop thru the Motorstate response records.
				PrettyPrint.print(motorstateResponse);
				LineItemContext motorstateLineItem = nonStockContext.getLineItems().stream()
						.filter(i -> i.getPartNumber().equals(motorstateResponse.getPartNumber())).findAny().get();
				// Match the current part from the response to the line items in the
				// nonStockContext that match
				// the part number that we're working on.
				Availability fulfillmentContext = correlationMap.get(motorstateResponse.getPartNumber());
				LineItemContext lineItemContext = fulfillmentContext.getLineItem();
				// Get line item context for this counter value (set above when building the
				// query).

				Boolean remove = false;

				if (!motorstateResponse.getFound()) {
					remove = true;
				} else {
					// populate.
					Product product = motorstateResponse.getProduct();
					if (product != null && product.getQuantity() < lineItemContext.getQuantity()) {
						// If the record shows less than what we requested, set the invalid parameter.
						remove = true;
					}
					motorstateLineItem.setPrice(BigDecimal.valueOf(product.getCustomerPrice()));
					// TODO assign product values on Motorstate line item. Price, etc.
				}

				if (remove) {
					// If the record was not found, set the invalid parameter to true.
					fulfillmentLocation.getFulfillmentDetails().remove(fulfillmentContext);
					lineItemContext.setInvalid(true);
					// Do anything else with the customer's lineItemContext?
					nonStockContext.getLineItems().remove(motorstateLineItem);
					log.info("Removing bad part : " + motorstateResponse.getPartNumber()
							+ " from Motorstate order process.");
				}
			}
		}
	}
}
