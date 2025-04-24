package com.autowares.mongoose.command;

import java.time.Duration;

import org.springframework.web.util.UriBuilder;

import com.autowares.ipov3.po.PurchaseOrder;
import com.autowares.ipov3.po.PurchaseOrderResponseImpl;
import com.autowares.ipov3.quote.AddQuoteResponseImpl;
import com.autowares.ipov3.quote.RequestForQuote;
import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.client.DiscoverService;
import com.autowares.servicescommon.util.PrettyPrint;

@DiscoverService(name = "vic-ipo-service", path = "/ipov3")
public class VicIpoServiceClient extends BaseResillience4JClient {

	public AddQuoteResponseImpl quote(RequestForQuote requestForQuote) {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("quote");
		AddQuoteResponseImpl objects = postForObject(uriBuilder, requestForQuote,
				AddQuoteResponseImpl.class, Duration.ofMinutes(2));

		return objects;
	}
	
	public PurchaseOrderResponseImpl purchaseOrder(PurchaseOrder purchaseOrder) {
		PrettyPrint.print(purchaseOrder);
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.pathSegment("purchaseOrder");
		PurchaseOrderResponseImpl objects = postForObject(uriBuilder, purchaseOrder,
				PurchaseOrderResponseImpl.class, Duration.ofMinutes(2));

		return objects;
	}

}
