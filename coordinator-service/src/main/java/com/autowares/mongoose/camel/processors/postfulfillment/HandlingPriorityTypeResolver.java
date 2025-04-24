package com.autowares.mongoose.camel.processors.postfulfillment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.logisticsservice.model.LogisticsCustomerRun;
import com.autowares.mongoose.model.Availability;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.partyconfiguration.model.Configuration;
import com.autowares.partyconfiguration.model.DeliverySetting;
import com.autowares.servicescommon.model.DeliveryMethod;
import com.autowares.servicescommon.model.HandlingPriorityType;
import com.autowares.servicescommon.model.RunType;

@Component
public class HandlingPriorityTypeResolver implements Processor {

	Logger log = LoggerFactory.getLogger(HandlingPriorityTypeResolver.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		FulfillmentLocationContext fulfillmentLocation = exchange.getIn().getBody(FulfillmentLocationContext.class);
		for (Availability availability : fulfillmentLocation.getLineItemAvailability()) {
			availability.setHandlingPriorityType(resolveHandlingPriorityType(availability));
			log.debug("HandlingProrityType = " + availability.getHandlingPriorityType());
		}
	}

	public HandlingPriorityType resolveHandlingPriorityType(Availability availability) {

		if (availability == null || availability.getFulfillmentLocation().getLogisticsCustomerRun() == null) {
			return null;
		}

		FulfillmentLocationContext fulfillmentLocation = availability.getFulfillmentLocation();
		LogisticsCustomerRun logisticsCustomerRun = fulfillmentLocation.getLogisticsCustomerRun();
		LineItemContext detail = availability.getLineItem();
		CoordinatorContext context = detail.getContext();

		// Delayed Processing Check.
		ZonedDateTime time = ZonedDateTime.now();
		if (context != null) {
			time = context.getRequestTime();
			if (time == null || ZonedDateTime.now().isAfter(time.plusMinutes(5))) {
				time = ZonedDateTime.now();
			}
			if (context.getInquiryOptions().getProcessAsOf() != null) {
				time = context.getInquiryOptions().getProcessAsOf();
			}
		}

		Boolean pastCutOffTime = false;
		Boolean shippingWithinOneDay = true;
		Boolean deliverToday = true;

		DeliveryMethod shippingMethod = fulfillmentLocation.getShippingMethod();
		RunType deliveryRunType = RunType.find(logisticsCustomerRun.getTruckRun().getDeliveryMethod());

		if (time.toLocalTime().isAfter(logisticsCustomerRun.getTruckRun().getCutOffTime())) {
			pastCutOffTime = true;
		}

		if (fulfillmentLocation.getNextDeparture() != null) {
			shippingWithinOneDay = time.until(fulfillmentLocation.getNextDeparture(), ChronoUnit.DAYS) < 1;
		}

		if (fulfillmentLocation.getArrivalDate() != null) {
			deliverToday = time.getDayOfYear() == fulfillmentLocation.getArrivalDate().getDayOfYear();
		}

		DayOfWeek dayOfWeek = time.getDayOfWeek();
		if (!shippingWithinOneDay || pastCutOffTime) {
			if (!dayOfWeek.equals(DayOfWeek.FRIDAY) && !dayOfWeek.equals(DayOfWeek.SATURDAY)) {
				return HandlingPriorityType.OrderOnHold;
			}
		}

		Configuration configuration = context.getConfiguration();
		if (configuration != null) {
			List<DeliverySetting> deliverySettings = configuration.getDeliverySettings();
			if (deliverySettings != null) {
				Optional<DeliverySetting> optionalDeliverySetting = deliverySettings.stream()
						.filter(i -> fulfillmentLocation.getLocation().equals(i.getLocation())).findAny();
				if (optionalDeliverySetting.isPresent()) {
					if (DeliveryMethod.CustomerPickUp.equals(optionalDeliverySetting.get().getDeliveryPreference())) {
						return HandlingPriorityType.Pickup;
					}
				}
			}
		}

		if (fulfillmentLocation.getHandlingResult() != null
				&& fulfillmentLocation.getHandlingResult().getHandlingPriorityType() != null) {
			log.info("Overriding handlingPriorityType based off the handlingResult.");
			return fulfillmentLocation.getHandlingResult().getHandlingPriorityType();
		}

		// orderType = 'N' means daytime pulling (time sensitive).
		// Set orderType='N' for Pickup at the customer's servicing location.
		if (deliverToday) {
			if (DeliveryMethod.CustomerPickUp.equals(shippingMethod) || DeliveryMethod.Carrier.equals(shippingMethod)) {
				return HandlingPriorityType.Pickup;
			}
		}

		if (DayOfWeek.FRIDAY.equals(dayOfWeek) && DeliveryMethod.CustomerPickUp.equals(shippingMethod)) {
			// Handle case with Saturday pick up logistix configuration, but viper has no
			// matching configuration.
			return HandlingPriorityType.Pickup;
		}

		// Set orderType='N' any time that we're delivering today and it's Express.
		if (deliverToday
				&& (RunType.expressDelivery.equals(deliveryRunType) || RunType.expressPickup.equals(deliveryRunType))) {
			return HandlingPriorityType.Express;
		}

		PartAvailability partAvailability = availability.getPartAvailability();
		if (LocalTime.now().isBefore(LocalTime.of(15, 0))) {
			//  TODO Only check to set MustGo if after 15:00.  Need to remove after
			//  Logistix refactor regarding Handling (See WMS-221).
			if (Boolean.TRUE.equals(fulfillmentLocation.getHandlingResult().getMustGoActive())) {
				if (Boolean.TRUE.equals(detail.getMustGo())) {
					if (partAvailability != null && partAvailability.getQuantityOnHand() != null
							&& partAvailability.getQuantityOnHand() - availability.getFillQuantity() < 10) {
						return HandlingPriorityType.DaytimeMustGo;
					}
				}
			}
		}

		WarehouseMaster warehouseMaster = fulfillmentLocation.getWarehouseMaster();
		if (warehouseMaster != null) {
			if (warehouseMaster.getPrintNowOrdersOnly()) {
				return HandlingPriorityType.NightPaper;
			}
		}

		return HandlingPriorityType.NightBinBulk;

	}

}
