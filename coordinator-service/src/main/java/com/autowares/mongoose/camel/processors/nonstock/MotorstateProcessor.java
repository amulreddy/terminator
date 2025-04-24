package com.autowares.mongoose.camel.processors.nonstock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.LineItemContext;
import com.autowares.mongoose.model.MotorstateContext;
import com.autowares.motorstateservice.model.Error;
import com.autowares.motorstateservice.model.HeaderError;
import com.autowares.motorstateservice.model.Line;
import com.autowares.motorstateservice.model.MotorstatePurchaseOrder;
import com.autowares.motorstateservice.model.Warning;
import com.autowares.servicescommon.util.PrettyPrint;

@Component
public class MotorstateProcessor implements Processor {

	Logger log = LoggerFactory.getLogger(MotorstateProcessor.class);

	public void process(Exchange exchange) {

		log.info("Process Motorstate Order record(s).");

		MotorstateContext motorstateContext = exchange.getIn().getBody(MotorstateContext.class);
		
		if (!motorstateContext.getCoordinatorOrderContext().getLineItems().isEmpty()) {
			//  Only go through this if we have valid line item(s) received.
			
			Integer counter = 0;
			Map<Integer, LineItemContext> correlationMap = new HashMap<>();
			for (LineItemContext lineItemContext : motorstateContext.getCoordinatorOrderContext().getLineItems()) {
				counter++;
				correlationMap.put(counter, lineItemContext);
			}

			// The exchange that is sent in is a MotorstateContext.
			// The coordinator context sits on the MotorstateContext.

			MotorstatePurchaseOrder mPO = motorstateContext.getMotorstatePurchaseOrder();

			System.out.println("Checkpoint x.");	
			Object validated = exchange.getIn().getHeader("validated");
			System.out.println("Validated : ");
			PrettyPrint.print(validated);
			Object sentLivePO = exchange.getIn().getHeader("sentLivePO");
			System.out.println("sentLivePO : ");
			PrettyPrint.print(sentLivePO);

			// Check for No response from Motorstate here.
			Object noMotorstateResponse = exchange.getIn().getHeader("noMotorstateResponse");
			System.out.println("noMotorstateResponse : ");
			PrettyPrint.print(noMotorstateResponse);

			if (noMotorstateResponse.equals(false)) {
				// Only go through this if we received a response from Motorstate.

				if (mPO.getLines().size() > 0) {
					// Verify we have any valid lines on the PO.

					// This will serve as the check for both the VALIDATE and the LIVE PO.
					// We'll check the context header to see if we've validated the PO previously.

					Boolean hasErrors = false;
					Boolean noResponse = false;
					String errorCode = null;
					String errorDescription = null;
					String errorFlag = null;

					if (mPO != null) {
						PrettyPrint.print(mPO);
						try {
							hasErrors = mPO.getHasErrors().booleanValue();
							// Does the PO have 'errors'? Meaning, did Motorstate flag this
							// for a bad part number? Insufficient quantity? Some other problem on their
							// end or ours?
						} catch (Exception e) {
							e.printStackTrace();
						}
//
						if (hasErrors) {
							System.out.println("Purchase Order has errors!");
							// Need to go through the headerErrors object(s) here?
							List<HeaderError> headerError = mPO.getHeaderErrors();
							// The HeaderErrors List comes back empty (at least for now 2022.03.18)
							// and NOT null, so this check is OK.

							if (headerError != null) {

								// To remove bad line items (not found, etc) use old style looping and do
								// .remove().line(i)?
								// or .line(i).remove()?

								if (mPO.getWarnings().size() > 0) {
									// The PO has Warnings for individual parts.
									List<Line> warningLines = new ArrayList<>();
									for (Warning warningLine : mPO.getWarnings()) {
										// Check for lineNumber matches.
										for (Line poLines : mPO.getLines()) {
											if (poLines.getLineNumber() == warningLine.getLineNumber()) {
												warningLines.add(poLines);
												// Loop through the coordinatorOrderContext.getLineItems() to set the
												// .setInvalid(
												// on the appropriate lineItem for user further down the line?

												// This is a line that has a warning. We will remove it from the PO
												// before we send it to Motorstate by adding this to our
												// warningLines List.

												// Set the '.setInvalid' flag on the coordinatorOrderContext here so
												// that we know Motorstate could/did not ship this.
												//
												// !Left off here!
												//
												break;
											}
										}
									}
									mPO.getLines().removeAll(warningLines);
								}
								System.out.println("Updated mPO w/warning lines removed.");
							}

							// If we have errors, save Error object data into the MOA_ORDERS record.
							Boolean hasLineErrors = false;
							//
							// Loop through the Lines in the response here to look for errors.
							// IF there are any. If we send an invalid part number, the Motorstate response
							// seems to strip out ALL of the lines and return a error message regarding just
							// the part that has an issue.
							//
							for (Line motorstateLine : mPO.getLines()) {
								try {
									List<Error> lineError = motorstateLine.getErrors();
									hasLineErrors = true;
									// If we encounter a line item error, deal with it here.
								} catch (Exception e) {
									//
								}
							}

							if (!hasLineErrors) {
								// In some cases, the error code/message resides in Errors area of the Lines
								// object. If these are present, I will probably need to do the same kind of
								// thing that I did w/the Warnings. Match them up and remove them.
								System.out.println("The PO has line item error messages.");
							}
							if (errorCode == null) {
								// If we don't have an error code inside of the part(s) themselves, check the
								// HeaderErrors and Errors objects.
								Boolean hasHeaderErrors = mPO.getHeaderErrors().isEmpty();
								Boolean hasMainErrors = mPO.getErrors().isEmpty();
								if (!hasHeaderErrors) {
									// Check to see if the HeaderErrors object has data.
									errorCode = mPO.getHeaderErrors().get(0).getCode();
									errorDescription = mPO.getHeaderErrors().get(0).getMessage();
								} else {
									// If there is no HeaderErrors data, try the Errors object data.
									if (!hasMainErrors) {
										errorCode = mPO.getErrors().get(0).getCode();
										errorDescription = mPO.getErrors().get(0).getMessage();
									}
								}
							}
							if (errorCode != null) {
								// If we have an ErrorCode from one of the two Error objects, use this to
								// populate
								// the MOA_ORDERS record fields.
//						mOrd.setMsErrorCode(errorCode);
//						mOrd.setMsErrorDescription(errorDescription);
							} else {
								// Do I want to put something here in case there is an error, but both Error
								// objects are null?
								errorCode = "???";
								errorDescription = "Problem with the PO, but no Error Code.";
							}

							errorFlag = "Z";
//					mOrd.setErrorFlag(errorFlag);
							// Update the Error_Flag field in MOA_ORDERS.
						}

					} else {
						// We didn't receive a response from Motorstate. This was most
						// likely a timeout. In this case, we want the record to be processed again,
						// so we don't want to set any fields in the MOA_ORDERS record.
						noResponse = true;
						log.info("Didn't receive a response from Motorstate - most likely a timeout.");
					}

					if (mPO.getLines().size() > 0 && sentLivePO.equals(true)) {
						// Only send the live PO to Motorstate if we have sent the VALIDATE,
						// and only if we have valid line items to send.

						System.out.println("Valid line item count = " + mPO.getLines().size());

						if (errorCode == "40") {
							log.info("Error code = 40  (Problem on Motorstate's end).");
						}
						if (noResponse == false && errorCode != "40") {
							// Don't do anything if we don't get a response, or if the Motorstate
							// error code is 40 "server not responding" (500?). In this case, we want
							// to resend the records on the next iteration.

							log.info("----Motorstate LIVE Send Response : -----");
							PrettyPrint.print(mPO);

							if (mPO.getOrderNumber() != null) {
								System.out.println("Save MS order ID to object.");
								System.out.println("orderNumber = " + mPO.getOrderNumber());
							}

							System.out.println("Final PO after processing : ");
							PrettyPrint.print(mPO);
							log.info("Update where necessary here.");
						}
					} else {
						log.info("No valid line items to send to Motorstate -or- livePO not yet sent.");
					}
				} else {
					log.info("No valid line items to send to Motorstate.");
				}

			}
		}
	}
}
