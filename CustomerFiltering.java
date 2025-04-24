package com.autowares.mongoose.camel.processors.gateway;

import java.util.List;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.exception.UnimplementedException;
import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.mongoose.model.CoordinatorOrderContext;

@Component
public class CustomerFiltering implements Processor {

	@Value("#{'${enabledCustomers}'.split(' ')}")
	private List<String> enabledCustomers;

	@Override
	@Transactional
	public void process(Exchange exchange) throws Exception {

		CoordinatorContext request = exchange.getIn().getBody(CoordinatorContext.class);

		if (request instanceof CoordinatorOrderContext) {
			Boolean processCustomer = true;
			if (enabledCustomers != null && !enabledCustomers.isEmpty()) {
					processCustomer=false;
					for (String customer : enabledCustomers) {
						if (Pattern.compile(customer).matcher(request.getCustomerNumber().toString()).matches()) {
							processCustomer=true;
						}
					}
			}
			if (!processCustomer) {
				throw new UnimplementedException("Customer not configured for coordinator - processing in viper.");
			}
		}

	}

}
