package com.autowares.mongoose.camel.processors.lookup;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.command.LocationLookupClient;
import com.autowares.apis.ids.model.BusinessDetail;
import com.autowares.apis.ids.model.BusinessOptionalFields;
import com.autowares.apis.ids.model.LocationLookupRequest;
import com.autowares.mongoose.exception.AbortProcessingException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;
import com.autowares.mongoose.model.DocumentContext;
import com.autowares.mongoose.model.FulfillmentLocationContext;
import com.autowares.mongoose.model.ProcurementGroupContext;
import com.google.common.collect.Lists;

@Component
public class OrderCustomerLookup implements Processor {

	private LocationLookupClient idsLocationClient = new LocationLookupClient();

	private static Logger log = LoggerFactory.getLogger(OrderCustomerLookup.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		
		Object o = exchange.getIn().getBody(Object.class);
		CoordinatorContext request = null;
		if (o instanceof CoordinatorContext) {
			request = exchange.getIn().getBody(CoordinatorContext.class);
		}
		if (o instanceof FulfillmentLocationContext) {
		    request = exchange.getIn().getBody(FulfillmentLocationContext.class).getOrder();
		}
		if (o instanceof DocumentContext) {
			DocumentContext documentContext = exchange.getIn().getBody(DocumentContext.class);
			if(documentContext.getContext() != null) {
				request = documentContext.getContext();
			} else {
				//request = documentContext.getSourceDocument();
				if(documentContext.getProcurementGroupContexts() != null && documentContext.getProcurementGroupContexts().size() == 1) {
					ProcurementGroupContext procurementGroup = documentContext.getProcurementGroupContexts().get(0);
					if(procurementGroup != null && procurementGroup.getCustomerContext() != null) {
						request = procurementGroup.getCustomerContext().getOrderContext();
					}
				}
			}
		}
		
		if(request == null) {
			String message = "Failed to lookup customer order";
			log.error(message);
			throw new AbortProcessingException(message);
		}
		
		LocationLookupRequest req = new LocationLookupRequest();
		req.setAwiAccountNo(String.valueOf(request.getCustomerNumber()));
        req.setOptionalFields(Lists.newArrayList(BusinessOptionalFields.businessAttributes,BusinessOptionalFields.emailAddresses));
		BusinessDetail acct = idsLocationClient.findBusinessLocation(req);
		if (acct == null) {
			throw new AbortProcessingException("Account " + request.getCustomerNumber() + " not found.");
		}
		if (request.getShipTo() != null) {
			LocationLookupRequest lLR = new LocationLookupRequest();
			if (request.getShipTo().getBusinessDetail().getBusEntId() != 0) {
				lLR.setBusEntId(String.valueOf(request.getShipTo().getBusinessDetail().getBusEntId()));
			} else {
				lLR.setAwiAccountNo(request.getShipTo().getAccountNumber());
			}
			if (lLR.getBusEntId() != null || lLR.getAwiAccountNo() != null) {
				BusinessDetail acctShipTo = idsLocationClient.findBusinessLocation(lLR);
				request.getShipTo().setBusinessDetail(acctShipTo);
			}
		}
		request.getBusinessContext().setBusinessDetail(acct);

		if (request instanceof CoordinatorOrderContext) {
			if (acct.getOnCreditHold()) {
				String message = "Account: " + request.getCustomerNumber() + " on credit hold";
				log.error(message + " - process in viper.");
				throw new AbortProcessingException(message);
			}
		}

	}

}
