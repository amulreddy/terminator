package com.autowares.mongoose.camel.processors.lookup;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpServerErrorException;

import com.autowares.logisticsservice.commands.CustomerRunClient;
import com.autowares.logisticsservice.model.LogisticsCustomerRun;
import com.autowares.logistix.model.HandlingResult;
import com.autowares.logistix.model.TargetType;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.exception.RetryLaterException;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContextImpl;
import com.autowares.servicescommon.exception.ServiceDiscoveryException;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.HandlingPriorityType;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.util.DateConversion;

@Component
public class TruckRunResolvingService implements Processor {

	CustomerRunClient customerRunClient = new CustomerRunClient();
	private static Logger log = LoggerFactory.getLogger(TruckRunResolvingService.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fillContext = exchange.getIn().getBody(FulfillmentLocationContext.class);

		CoordinatorContext context = fillContext.getOrder();

		Long customerNumber = context.getCustomerNumber();
		if (context.getShipTo() != null && context.getShipTo().getAccountNumber() != null) {
			try {
				customerNumber = Long.valueOf(context.getShipTo().getAccountNumber());
			} catch (Exception e) {
				throw new AbortProcessingException(e);
			}
		}
		String location = fillContext.getLocation();
		DeliveryMethod shippingMethod = fillContext.getShippingMethod();
		ServiceClass serviceClass = fillContext.getServiceClass();

		ZonedDateTime timeStamp = context.getRequestTime();
		if (context instanceof CoordinatorOrderContext) {
			CoordinatorOrderContext orderContext = (CoordinatorOrderContext) context;
			if (orderContext.getOrderTime() != null) {
				timeStamp = orderContext.getOrderTime();
			}
		}
		if (timeStamp == null || ZonedDateTime.now().isAfter(timeStamp.plusMinutes(5))) {
			timeStamp = ZonedDateTime.now();
		}
		if (context.getInquiryOptions().getProcessAsOf() != null) {
			timeStamp = context.getInquiryOptions().getProcessAsOf();
		}
		if (exchange.getIn().getHeader("processAsOf", ZonedDateTime.class) != null) {
			timeStamp = exchange.getIn().getHeader("processAsOf", ZonedDateTime.class);
		}
		/*
		 * If it is departing the warehouse on a non-express run or not being delivered
		 * today set it to Standard
		 */
		ZonedDateTime logistixDepartureTime = null;
		if (fillContext.getShipment() != null && fillContext.getShipment().getDepartureRun() != null) {
			RunType departureRunType = fillContext.getShipment().getDepartureRun().getRunType();
			ServiceClass departureServiceClass = departureRunType.getServiceClass();
			logistixDepartureTime = DateConversion.convert(fillContext.getShipment().getDepartureDate());
			if (ServiceClass.Standard.equals(departureServiceClass)) {
				serviceClass = ServiceClass.Standard;
			}
		}

		if (shippingMethod == null) {
			log.info("null deliveryMethod changing to AWIDelivery");
			shippingMethod = DeliveryMethod.AWIDelivery;
		}

		LogisticsCustomerRun logisticsCustomerRun = null;
		HandlingResult handlingResult = fillContext.getHandlingResult();

		StopWatch sw = new StopWatch();
		sw.start();

		if (handlingResult != null) {

			// override for A9PK/G9PK runs being looked up as pickup,
			// TODO consider a PickupNight RunType
			if (HandlingPriorityType.NightBinBulk.equals(handlingResult.getHandlingPriorityType())) {
				shippingMethod = DeliveryMethod.AWIDelivery;
			}

			if (HandlingPriorityType.Express.equals(handlingResult.getHandlingPriorityType())) {
				serviceClass = ServiceClass.Express;
			}

			if (handlingResult.getLoadTarget() != null
					&& handlingResult.getLoadTarget().getTargetType().equals(TargetType.TruckRun)) {
				String truckRun = fillContext.getHandlingResult().getLoadTarget().getTargetId();
				Optional<LogisticsCustomerRun> optionalLogisticsCustomerRun = customerRunClient
						.getCustomerRun(customerNumber, truckRun, location);
				context.updateProcessingLog("Truck Run Override defined as " + truckRun);
				if (optionalLogisticsCustomerRun.isPresent()) {
					logisticsCustomerRun = optionalLogisticsCustomerRun.get();
				} else {
					context.updateProcessingLog("Truck Run Override could not be found.");
				}
			}
		}

		if (logisticsCustomerRun == null) {
			logisticsCustomerRun = getCustomerRun(customerNumber, location, shippingMethod, serviceClass, timeStamp);
			

			if (logisticsCustomerRun == null && ServiceClass.Standard != serviceClass) {
				logisticsCustomerRun = getCustomerRun(customerNumber, location, shippingMethod, ServiceClass.Standard,
						timeStamp);
			//  If ordered express and we are past the night cut off, search for an express run the next day.
				if (logisticsCustomerRun != null) {
					LocalTime cutOffTime = logisticsCustomerRun.getTruckRun().getCutOffTime();
					if (timeStamp.toLocalTime().isAfter(cutOffTime)) {
						if (RunType.nightDelivery
								.equals(RunType.find(logisticsCustomerRun.getTruckRun().getDeliveryMethod()))) {
							if (ServiceClass.Express.equals(serviceClass) && logistixDepartureTime != null) {
								ZonedDateTime testTimeStamp = logistixDepartureTime.minusMinutes(60);
								LogisticsCustomerRun potentialExpressCustomerRun = getCustomerRun(customerNumber, location,
										shippingMethod, serviceClass, testTimeStamp);
								if (potentialExpressCustomerRun != null) {
									if (RunType.expressDelivery.equals(
											RunType.find(potentialExpressCustomerRun.getTruckRun().getDeliveryMethod()))) {
										logisticsCustomerRun = potentialExpressCustomerRun;
										log.info("Express ordered after night cut off time saved on next day Express run.");
										context.updateProcessingLog("Express ordered after night cut off time saved on next day Express run.");
									}
								}
							}
						}
					}
				}
			}

			if (logisticsCustomerRun == null && DeliveryMethod.CustomerPickUp.equals(shippingMethod)) {
				logisticsCustomerRun = findCustomerPickUpRun(customerNumber, location);
				if (logisticsCustomerRun == null) {
					logisticsCustomerRun = getCustomerRun(customerNumber, location, DeliveryMethod.AWIDelivery,
							ServiceClass.Standard, timeStamp);
				}
			}

			if (logisticsCustomerRun == null) {
				String messageDetails = customerNumber + " from: " + location + " " + serviceClass + " "
						+ shippingMethod + "   Timestamp: " + timeStamp;

				log.error("Unable to resolve customer run for: " + messageDetails);
				
				/**
				 *  We can't fill from this location because there are no truck runs available.
				 *  but we don't need to error the entire transaction. Just remove this location.
				 * */
				for(LineItemContextImpl line : fillContext.getOrder().getLineItems()) {
					List<Availability> lineItemAvailability = line.getAvailability().stream()
							.filter(a -> a != null)
							.filter(a -> a.getFulfillmentLocation().equals(fillContext))
							.toList();
					log.info("Found: " + lineItemAvailability.size());
					
					if(lineItemAvailability.size() > 0) {
						line.getAvailability().removeAll(lineItemAvailability);
					}
				}
				
				fillContext.setLineItemAvailability(null);
				fillContext.getOrder().getFulfillmentSequence().remove(fillContext);
				log.warn("Removed line item availability for: " + fillContext.getLocation());
			}
		}
		sw.stop();
		log.info("Looking up Customer run took: " + sw.getTotalTimeMillis() + "ms");
		fillContext.setLogisticsCustomerRun(logisticsCustomerRun);

	}

	private LogisticsCustomerRun findCustomerPickUpRun(Long customerNumber, String location) {
		try {
			return customerRunClient.findCustomerRuns(customerNumber, null, location, null,
					DeliveryMethod.CustomerPickUp, ServiceClass.Standard).stream().findAny().get();
		} catch (Exception e) {
			dealWithExceptionTypes(e);
		}
		return null;
	}

	private LogisticsCustomerRun getCustomerRun(Long customerNumber, String location, DeliveryMethod deliveryMethod,
			ServiceClass serviceClass, ZonedDateTime timeStamp) {
		String messageDetails = customerNumber + " from: " + location + " " + serviceClass + " " + deliveryMethod
				+ "   Timestamp: " + timeStamp;

		log.info("Looking up Customer run for: " + messageDetails);
		try {

			return customerRunClient.getCustomerRun(customerNumber, location, deliveryMethod, serviceClass, timeStamp);
		} catch (Exception e) {
			dealWithExceptionTypes(e);
		}
		return null;
	}

	private void dealWithExceptionTypes(Exception e) {
		if (e instanceof ServiceDiscoveryException || e instanceof HttpServerErrorException) {
			throw new RetryLaterException(e);
		}
	}
}
