package com.autowares.mongoose.camel.processors.freight;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.CoordinatorContext;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.supplychain.model.SupplyChainNote;

@Component
public class FlatRateShipping implements Processor {
	
	private static Logger log = LoggerFactory.getLogger(FlatRateShipping.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		CoordinatorContext orderContext = exchange.getIn().getBody(CoordinatorContext.class);
		
		CoordinatorContext customerContext = null;
		
		if(orderContext.getProcurementGroupContext() != null) {
			if(orderContext.getProcurementGroupContext().getCustomerContext() != null) {
				customerContext = orderContext.getProcurementGroupContext().getCustomerContext().getQuoteContext();
			}
		}

		if (orderContext == null || orderContext.getInquiryOptions() == null
				|| orderContext.getFreightOptions() == null) {
			return; // Exit if context is missing
		}

		FreightOptions freightOptions = orderContext.getFreightOptions();
		List<Freight> availableFreight = freightOptions.getAvailableFreight();

		if (availableFreight == null || availableFreight.isEmpty()) {
			return; // No freight options available
		}
		
		// filter out will call and customer pickup.
		availableFreight = availableFreight.stream().filter(i -> i.getDescription() != null)
				.filter(i -> !i.getDescription().toLowerCase().contains("will call"))
				.filter(i -> !i.getDescription().toLowerCase().contains("customer pick up"))
				.filter(i -> !i.getDescription().toLowerCase().contains("a.m"))
				.filter(i -> !i.getDescription().toLowerCase().contains("early"))
				.filter(i -> !i.getDescription().toLowerCase().contains("saver"))
				.collect(Collectors.toList());

		// Calculate total selling price
		BigDecimal totalSellingPrice = customerContext.getLineItems().stream()
				.map(lineItem -> (lineItem.getPrice()==null?BigDecimal.ZERO:lineItem.getPrice()).multiply(new BigDecimal(lineItem.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		boolean useFlatRateShipping = false;
		
		if(customerContext != null) {
			useFlatRateShipping = customerContext.getInquiryOptions().getUseFlatRateShipping();
		}

		// Determine if flat-rate shipping should be applied
		for (Freight freight : availableFreight) {
			if (freight.getCost() == null || freight.getCost().compareTo(BigDecimal.ZERO) == 0) {
				useFlatRateShipping = true;
				orderContext.getInquiryOptions().setUseFlatRateShipping(true);
				if(customerContext != null) {
					customerContext.getInquiryOptions().setUseFlatRateShipping(true);
					customerContext.getTransactionContext().getRequest().getInquiryOptions().setUseFlatRateShipping(true);
				}
				break; // Exit early if flat rate is triggered
			}
		}

		if (useFlatRateShipping) {
			log.info("Using Flat Rate Shipping");
			// Remove any freight options that don't match allowed regex patterns
			Iterator<Freight> iterator = availableFreight.iterator();
			while (iterator.hasNext()) {
				Freight option = iterator.next();
				String description = option.getDescription() != null ? option.getDescription().toLowerCase() : "";

				if (!(matchesRegex(description, "(?i).*ground.*")
						|| matchesRegex(description, "(?i).*next.*"))) {
					if(customerContext != null) {
						String message = "Removing shipping option: " + description + " " + option.getCost();
						log.info(message);
//						customerContext.getNotes().add(SupplyChainNote.builder().withMessage(message).build());
					}
					iterator.remove(); // Remove non-matching options
				}
			}

			// Apply flat rate shipping costs to remaining valid options
			for (Freight option : availableFreight) {
				String description = option.getDescription().toLowerCase();
				if (matchesRegex(description, "(?i).*ground.*")) {
					option.setCost(getFlatRateCost(totalSellingPrice, "ground"));
				} else if (matchesRegex(description, "(?i).*next.*")) {
					option.setCost(getFlatRateCost(totalSellingPrice, "next_day"));
				}
			}
			freightOptions.setAvailableFreight(availableFreight);
		}
	}

	private boolean matchesRegex(String text, String regex) {
		return text != null && text.matches(regex);
	}

	private BigDecimal getFlatRateCost(BigDecimal sellingPrice, String shippingType) {
		// Define the flat rate shipping table
		BigDecimal[][] rateTable = {
				{ new BigDecimal("0.01"), new BigDecimal("10.00"), new BigDecimal("7.00"), new BigDecimal("13.00"), new BigDecimal("4.00") },
				{ new BigDecimal("10.01"), new BigDecimal("20.00"), new BigDecimal("9.00"), new BigDecimal("17.00"), new BigDecimal("5.00") },
				{ new BigDecimal("20.01"), new BigDecimal("50.00"), new BigDecimal("13.00"), new BigDecimal("24.00"), new BigDecimal("6.00") },
				{ new BigDecimal("50.01"), new BigDecimal("75.00"), new BigDecimal("16.00"), new BigDecimal("30.00"), new BigDecimal("7.00") },
				{ new BigDecimal("75.01"), new BigDecimal("100.00"), new BigDecimal("18.00"), new BigDecimal("33.00"), new BigDecimal("8.00") },
				{ new BigDecimal("100.01"), new BigDecimal("150.00"), new BigDecimal("20.00"), new BigDecimal("37.00"), new BigDecimal("9.00") },
				{ new BigDecimal("150.01"), new BigDecimal("200.00"), new BigDecimal("25.00"), new BigDecimal("46.00"), new BigDecimal("10.00") },
				{ new BigDecimal("200.01"), new BigDecimal("300.00"), new BigDecimal("30.00"), new BigDecimal("55.00"), new BigDecimal("11.00") },
				{ new BigDecimal("300.01"), new BigDecimal("500.00"), new BigDecimal("35.00"), new BigDecimal("64.00"), new BigDecimal("15.00") },
				{ new BigDecimal("500.01"), new BigDecimal("90000.00"), new BigDecimal("40.00"), new BigDecimal("73.00"), new BigDecimal("20.00") } };

		// Determine the correct shipping cost based on the selling price
		for (BigDecimal[] row : rateTable) {
			if (sellingPrice.compareTo(row[0]) >= 0 && sellingPrice.compareTo(row[1]) <= 0) {
				return (shippingType.equals("ground") ? row[2] : row[3]).add(row[4]);
			}
		}

		// Default to the highest rate if price exceeds the range
		return (shippingType.equals("ground") ? new BigDecimal("40.00") : new BigDecimal("73.00")).add(new BigDecimal("20.00"));
	}
}
