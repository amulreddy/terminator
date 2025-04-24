package com.autowares.mongoose.camel.processors.lookup;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpClientErrorException;

import com.autowares.logistix.model.Shipment;
import com.autowares.logistix.model.ShipmentOptimizationStrategy;
import com.autowares.logistix.model.ShipmentOptions;
import com.autowares.logistix.model.ShipmentRequest;
import com.autowares.logistix.model.TruckRun;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.LogistixService;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.DeliverySetting;
import com.autowares.servicescommon.model.ServiceClass;
import com.autowares.servicescommon.model.SystemType;

@Component
public class LogistixShipmentProcessor implements Processor {

	@Autowired
	private LogistixService shipmentClient;
	private static Logger log = LoggerFactory.getLogger(LogistixShipmentProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {

		Object o = exchange.getIn().getBody(Object.class);
		CoordinatorContext coordinatorContext = null;
		FulfillmentLocationContext orderFillContext = null;
		if (o instanceof CoordinatorContext) {
			coordinatorContext = exchange.getIn().getBody(CoordinatorContext.class);
		}
		if (o instanceof FulfillmentLocationContext) {
			orderFillContext = exchange.getIn().getBody(FulfillmentLocationContext.class);
			coordinatorContext = orderFillContext.getOrder();
		}
		if (o instanceof DocumentContext) {
			coordinatorContext = exchange.getIn().getBody(DocumentContext.class).getContext();
			orderFillContext = exchange.getIn().getBody(DocumentContext.class).getFulfillmentLocationContext();
		}
		
		String shipTo = coordinatorContext.getBusinessContext().getAccountNumber();
		String servicingLocation = coordinatorContext.getBusinessContext().getBusinessDetail().getServicingWarehouse().getBuildingMnemonic();
		String shipFrom = orderFillContext.getLocation();

		if (coordinatorContext.getShipTo() != null && coordinatorContext.getShipTo().getAccountNumber() != null) {
			shipTo = coordinatorContext.getShipTo().getAccountNumber();
		}

		if (SystemType.AwiWarehouse.equals(orderFillContext.getSystemType())
				|| SystemType.CounterWorks.equals(orderFillContext.getSystemType())
				|| SystemType.MotorState.equals(orderFillContext.getSystemType())) {
			StopWatch sw = new StopWatch();
			sw.start();

			ZonedDateTime timeStamp = coordinatorContext.getRequestTime();
			if (coordinatorContext instanceof CoordinatorOrderContext) {
				CoordinatorOrderContext orderContext = (CoordinatorOrderContext) coordinatorContext;
				if (orderContext.getOrderTime() != null) {
					timeStamp = orderContext.getOrderTime();
				}
			}
			if (timeStamp == null || ZonedDateTime.now().isAfter(timeStamp.plusMinutes(5))) {
				timeStamp = ZonedDateTime.now();
			}
			if (coordinatorContext.getInquiryOptions().getProcessAsOf()!=null) {
				timeStamp = coordinatorContext.getInquiryOptions().getProcessAsOf();
			}
			if (exchange.getIn().getHeader("processAsOf", ZonedDateTime.class) != null) {
				timeStamp = exchange.getIn().getHeader("processAsOf", ZonedDateTime.class);
			}

			ShipmentRequest shipmentRequest = new ShipmentRequest();
			shipmentRequest.setDestinationLocation(shipTo);
			shipmentRequest.setSourceLocation(shipFrom);
			shipmentRequest.setDeliveryMethod(coordinatorContext.getDeliveryMethod());
			shipmentRequest.setServiceClass(coordinatorContext.getServiceClass());
			shipmentRequest.getHandlingOptions().setUseOrderCutoffTime(true);
			shipmentRequest.setRequestedTime(timeStamp);
			
			//TODO CHI to MAD early shuttle hack.  See Jira card WMS-146
	         if ("CHI".equals(shipFrom)) {
	             ShipmentOptions shipmentOptions = new ShipmentOptions();
	             shipmentOptions.setOptimizationStrategy(ShipmentOptimizationStrategy.byFirstPathToLeave);
	             shipmentRequest.setShipmentOptions(shipmentOptions);
	         }
			Shipment shipment = null;

			Configuration configuration = coordinatorContext.getConfiguration();
			if (configuration != null) {
				List<DeliverySetting> deliverySettings = configuration.getDeliverySettings();
				if (deliverySettings != null) {
					Optional<DeliverySetting> optionalDeliverySetting = deliverySettings.stream()
							.filter(i -> shipFrom.equals(i.getLocation())).findAny();
					if (optionalDeliverySetting.isPresent()) {
						if(optionalDeliverySetting.get().getDeliveryPreference() != null) {
							shipmentRequest.setDeliveryMethod(optionalDeliverySetting.get().getDeliveryPreference());
						}
						if(optionalDeliverySetting.get().getServicePreference() != null) {
							shipmentRequest.setServiceClass(optionalDeliverySetting.get().getServicePreference());
						}
					}
				}
			}

//			if (orderFillContext.getTrackingNumber() != null) {
//				log.info("Looking up tracking number: " + orderFillContext.getTrackingNumber());
//				try {
//					shipment = shipmentClient.getShipment(orderFillContext.getTrackingNumber());
//				} catch (HttpClientErrorException clientError) {
//					if (HttpStatus.NOT_FOUND.equals(clientError.getStatusCode())) {
//						shipment = shipmentClient.requestShipment(shipmentRequest);
//					}
//				}
//			} else 
			if (orderFillContext.isBeingFilledFrom()) {
				try {
					shipment = shipmentClient.requestShipment(shipmentRequest);
				} catch (HttpClientErrorException clientError) {
					if (HttpStatus.NOT_FOUND.equals(clientError.getStatusCode())) {
						throw new RetryException(clientError.getMessage());
					}
					// 400 Bad request is what's currently coming back from a
					// server side constraint violation, should probably be better mapped
					if (clientError.getMessage().contains("duplicate key")) {
						throw new RetryException(clientError.getMessage());
					}
				}
			} else {
				try {
					shipment = shipmentClient.calculateShipment(shipmentRequest);
				} catch (HttpClientErrorException clientError) {
					if (HttpStatus.NOT_FOUND.equals(clientError.getStatusCode())) {
						throw new AbortProcessingException(clientError.getMessage());
					}
				}
			}
			sw.stop();
			if (shipment != null) {
				log.info("Shipment: " + shipFrom + "->" + shipTo + ":" + coordinatorContext.getServiceClass() + " in "
						+ sw.getTotalTimeMillis() + "ms");
				orderFillContext.setShipment(shipment);
				if (shipment.getTrackingNumber() != null) {
					orderFillContext.setTrackingNumber(shipment.getTrackingNumber().toString());
					log.info("got tracking number: " + shipment.getTrackingNumber());
				}
				if (shipment.getNumberOfTransfers() != null) {
					orderFillContext.setTransfers(shipment.getNumberOfTransfers().intValue());
				}

				if (shipment.getArrivalDate() != null) {
					orderFillContext.setTravelTime(Duration.ofMinutes(ChronoUnit.MINUTES.between(ZonedDateTime.now(),
							ZonedDateTime.ofInstant(shipment.getArrivalDate().toInstant(), ZoneId.systemDefault()))));
				}
				orderFillContext.setExpireTime(shipment.getExpireTime());
				TruckRun departureRun = shipment.getDepartureRun();
				TruckRun arrivalRun = shipment.getArrivalRun();

				if (departureRun != null && arrivalRun != null) {
					orderFillContext.setDeliveryRunType(arrivalRun.getRunType());
					orderFillContext.setViperTruckRunId(arrivalRun.getViperTruckRunId());
					orderFillContext.setDeliveryMethod(arrivalRun.getRunType().getDeliveryMethod());
					orderFillContext.setDepartureRunId(departureRun.getId());
					orderFillContext.setDepartureRunName(departureRun.getRunName());
					orderFillContext.setNextDeparture(departureRun.getScheduledDepartureTime());
					orderFillContext.setDepartureRunType(departureRun.getRunType());
					orderFillContext.setCutOffMinutes(departureRun.getCutOffMinutes());
					orderFillContext.setShippingMethod(departureRun.getRunType().getDeliveryMethod());
					orderFillContext.setServiceClass(departureRun.getRunType().getServiceClass());
					if (ServiceClass.Express.equals(arrivalRun.getRunType().getServiceClass())) {
						orderFillContext.setViperTruckRunId(departureRun.getViperTruckRunId());
					}
				} else {
					orderFillContext.setDeliveryMethod(coordinatorContext.getDeliveryMethod());
					orderFillContext.setShippingMethod(coordinatorContext.getDeliveryMethod());
					orderFillContext.setServiceClass(coordinatorContext.getServiceClass());
				}
				if (shipment.getHandling() != null) {
					orderFillContext.setHandlingResult(shipment.getHandling());
				}
			}
		}
	}

}
