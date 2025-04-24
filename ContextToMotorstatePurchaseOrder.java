package com.autowares.mongoose.camel.components;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Converter;
import org.apache.camel.TypeConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.partservice.model.Origcode;
import com.autowares.mongoose.camel.processors.nonstock.MotorstateProcessor;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.motorstateservice.model.Line;
import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;
import com.autowares.servicescommon.util.PrettyPrint;
import com.autowares.servicescommon.util.SequenceGenerator;

@Component
@Converter(generateLoader = true)
public class ContextToMotorstatePurchaseOrder implements TypeConverters {

	private static SequenceGenerator sequenceGenerator = new SequenceGenerator();

	private static Logger log = LoggerFactory.getLogger(MotorstateProcessor.class);

	public static MotorstatePurchaseOrder contextToMotorstatePurchaseOrder(
			FulfillmentLocationContext fulfillmentLocationContext) {
		// Add parameter to this call that is the requestId? If it has a value, then use
		// that.
		// Otherwise, use the sent requestId. When removing "bad" parts, I want to call
		// this
		// method a 2nd time to build the PO that we will send the LIVE PO to
		// Motorstate.
		// This seems easier than trying to take the validatePO and transferring all of
		// the
		// fields to the new object. In here, we'll check the .isValid and not build
		// those
		// parts in during the 2nd call? Not sure if that will work?

		// This should just build the base Motorstate PO. Don't do any outside contact
		// here. Build what we can with what we have.
		// We have already verified the part is good @ Motorstate.
		// So, we'll check the invalid property, which was updated in the
		// LookUpMotorstatePart processor. If invalid=true, skip adding the part to
		// the PO.

		// I believe the change here is to save the built PO to the MotorstateContext
		// and use that
		// down the line for the Validate and Live PO send.

		String lineCode;
		String partNumber;
		Long sequence;
		
		CoordinatorContext coordinatorOrderContext = fulfillmentLocationContext.getOrder();

		// The first time through, generate the sequence ID. The 2nd time through
		// we want to use the one that we already have.
		
		sequence = sequenceGenerator.nextId();
		log.info("Motorstate sequence ID = " + sequence);
		String motorstateSequence = String.format("%019d", sequence);
		// Zero fill the MS sequence number for the MS PO.

		String motorstatePo = String.format("%04d", coordinatorOrderContext.getCustomerNumber());
		// Zero fill the customer number for the MS PO.
		motorstatePo += "-";
		
//		String tRun = fulfillmentLocationContext.getDepartureRunName();
//		
//		motorstatePo += tRun + "-";
		
		motorstatePo += motorstateSequence;

		List<Line> lines = new ArrayList<Line>();

		for (Availability lineItemAvailability : fulfillmentLocationContext.getLineItemAvailability()) {

			//
			// Check .isValid for each part here. Don't add the part to the request
			// if Motorstate doesn't recognize it.

			// Iterate thru the order records to build the MS order request.
			LineItemContext lineItem = lineItemAvailability.getLineItem();
			if (!lineItem.getInvalid()) {
				// Only add parts to the PO that Motorstate recognizes.
				System.out.println("Valid part.  lineNumber = " + lineItem.getLineNumber());
				lineCode = lineItem.getCounterWorksLineCode();
				// Default to Line Code in the context.
				partNumber = lineItem.getPartNumber();
				String msPartNumber = lineCode + partNumber;
				Long mPHId = lineItem.getProductId();
				List<Origcode> moaPartOrig = null;
				if (mPHId != null) {
					moaPartOrig = lineItem.getPart().getOrigcodes();
				}
				// Get MoaPartHeader record here using the partHdrId. Get the Origcode
				// record(s) below, as we might have to use the source system part #.

				if (moaPartOrig != null) {
					// We have ORIGCODE records for this part.
					log.info("ORIGCODE record found.");
					for (Origcode tempMPO : moaPartOrig) {
						// Go thru the ORIGCODE record(s), but really, we should only ever have 1
						// for a part.
						if (tempMPO.getSourceSystemCode().equals("MTRS")) {
							lineCode = tempMPO.getSourceSystemLineCode();
							partNumber = tempMPO.getSourceSystemPartNumber();
							msPartNumber = lineCode + partNumber;
						}
					}
				} else {
					// No ORIGCODE records for this partHdrId.
					// Add this to the list of parts on the PO to send
					// to Motorstate.
					log.info("No origcode records.");
				}

				System.out.println("lineNumber = " + lineItem.getLineNumber());
				Line line = new Line.Builder(lineItem.getLineNumber())
						.withPartNumber(msPartNumber)
						.withQuantityShip(lineItem.getQuantity())
						.withComment(" ")
						.build();

				lines.add(line);
				// Add this line to the Array of Lines.
			}

			// Motorstate PO object build below. This is for the JSON we'll send to MS
			// for the order.
		}

		MotorstatePurchaseOrder mPO = new MotorstatePurchaseOrder.Builder(motorstatePo)
				.withRequestId(motorstateSequence)
				.withPaymentMethodCode("OnAccount")
				.withCreditCardLast4(" ")
				.withLines(lines)
				.withShippingMethodCode(" ")
				.withSignatureRequired(false)
				.build();
		// Build the Motorstate PO.

		log.info("Motorstate full Purchase Order : ");
		PrettyPrint.print(mPO);

		return mPO;
	}
}
