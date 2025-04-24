package com.autowares.mongoose.camel.processors.conditiondetection;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.logisticsservice.model.LogisticsCustomerRun;
import com.autowares.logistix.model.Location;
import com.autowares.logistix.model.OrderCutoff;
import com.autowares.logistix.model.Shipment;
import com.autowares.logistix.model.TruckRun;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.service.LogistixService;
import com.autowares.notification.model.SimpleNotificationRequest;
import com.autowares.notification.model.SimpleNotificationRequestBuilder;
import com.autowares.servicescommon.model.RunType;
import com.autowares.servicescommon.model.ServiceClass;

@Component
public class DetectShippingDifference implements Processor {

	private static Logger log = LoggerFactory.getLogger(DetectShippingDifference.class);

	@Autowired
	private LogistixService logistixService;

	@Override
	public void process(Exchange exchange) throws Exception {

		Object object = exchange.getIn().getBody();

		if (object instanceof FulfillmentLocationContext) {
			FulfillmentLocationContext context = (FulfillmentLocationContext) object;
			CoordinatorContext orderContext = context.getOrder();
			Shipment shipment = context.getShipment();
			LogisticsCustomerRun logisticsCustomerRun = context.getLogisticsCustomerRun();
			TruckRun logistixDepartureRun = shipment.getDepartureRun();
			String desc = null;
			if (logistixDepartureRun != null) {
				desc = logistixDepartureRun.getDescription();
				if (desc == null) {
					desc = logistixDepartureRun.getRunName();
				}
			}

			if (shipment != null && logisticsCustomerRun != null) {
				ZonedDateTime logistixCutoff = shipment.getExpireTime();
				LocalTime viperCutoff = logisticsCustomerRun.getTruckRun().getCutOffTime();
				RunType logistixRunType = context.getDeliveryRunType();
				RunType viperRunType = RunType.find(logisticsCustomerRun.getTruckRun().getDeliveryMethod());

				if (logistixRunType != null && viperRunType != null) {

					boolean nightDelivery = RunType.nightDelivery.equals(logistixRunType)
							&& RunType.nightDelivery.equals(viperRunType);
					boolean missingViperExpress = RunType.expressDelivery.equals(logistixRunType)
							&& RunType.nightDelivery.equals(viperRunType);
					boolean missingLogistixExpress = RunType.expressDelivery.equals(viperRunType)
							&& RunType.nightDelivery.equals(logistixRunType);

					if (ServiceClass.Express.equals(orderContext.getServiceClass()) && nightDelivery) {
						// Ordered express but going night from both systems, no discrepancy?
						log.info("Possibly OK shipping condition");
					}

					if (missingViperExpress) {
						log.info("Logistix express connection missing in viper via express run: "
								+ shipment.getDepartureRun().getRunName());
						OrderCutoff orderCutoff = shipmentToOrderCutoff(shipment);
						orderCutoff.setViperRunType(viperRunType);
						orderCutoff.setViperCutoff(viperCutoff);
						orderCutoff.setDiscrepancy(true);
						updateOrderCutoff(orderCutoff);
					}

					if (missingLogistixExpress) {
						log.info("Viper express configuration missing in logistix via express run: "
								+ logisticsCustomerRun.getTruckRun().getTruckRunName());
					}

					// Cutoff issues reported only if logistics service does not respond with 00:00
					if (!LocalTime.MIDNIGHT.equals(viperCutoff)) {
						if (Duration.between(logistixCutoff.toLocalTime(), viperCutoff).abs().toMinutes() > 5) {
							String message = "CutoffTime discrepancy for customer "
									+ context.getOrder().getCustomerNumber() + " from: " + context.getLocation()
									+ " ViperCutoff: " + viperCutoff + " LogistixCutoff: "
									+ logistixCutoff.toLocalTime() + " Viper runType: " + viperRunType
									+ " LogistixRunType: " + logistixRunType + " Departure on: " + desc;
							log.error(message);
							@SuppressWarnings("unused")
							SimpleNotificationRequest request = new SimpleNotificationRequestBuilder()
									.withMessage(message).withRecipientEmailAddress("kcml@autowares.com")
									.withSender("noreply@autowares.com").withSubject("CutoffTime discrepancy").build();

							if (RunType.wareHouseTransfer.equals(shipment.getDepartureRun().getRunType())) {
								// notificationService.notify(request);
								OrderCutoff orderCutoff = shipmentToOrderCutoff(shipment);
								orderCutoff.setViperRunType(viperRunType);
								orderCutoff.setViperCutoff(viperCutoff);
								orderCutoff.setDiscrepancy(true);
								updateOrderCutoff(orderCutoff);
							}

						}
					}
				}

			}

		}
	}

	private void updateOrderCutoff(OrderCutoff orderCutoff) {
		try {
			logistixService.updateOrderCutoff(orderCutoff);
		} catch (Exception e) {
			log.error("failed to update cutoff");
		}
	}

	private OrderCutoff shipmentToOrderCutoff(Shipment shipment) {
		OrderCutoff orderCutoff = new OrderCutoff();
		orderCutoff.setSourceLocation(Location.builder().fromBusinessLocation(shipment.getFrom()).build());
		orderCutoff.setDestinationLocation(Location.builder().fromBusinessLocation(shipment.getTo()).build());
		orderCutoff.setDeliveryMethod(shipment.getChosenDeliveryMethod());
		orderCutoff.setServiceClass(shipment.getServiceClass());
		orderCutoff.setCutoffTime(shipment.getExpireTime().toLocalTime());
		return orderCutoff;
	}
}
