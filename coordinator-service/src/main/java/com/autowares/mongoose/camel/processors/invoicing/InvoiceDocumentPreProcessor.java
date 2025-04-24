package com.autowares.mongoose.camel.processors.invoicing;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.FreightOptions;
import com.autowares.xmlgateway.edi.EdiLine;
import com.autowares.xmlgateway.edi.EdiSourceDocument;

@Component
public class InvoiceDocumentPreProcessor implements Processor {

	private static final Logger log = LoggerFactory.getLogger(InvoiceDocumentPreProcessor.class);

	// Define possible freight-related part numbers
	private static final Set<String> FREIGHT_PART_NUMBERS = Set.of("FREIGHT", "SHIPPING", "DELIVERY", "FEDEX", "FRTOUT");

	@Override
	public void process(Exchange exchange) throws Exception {
		EdiSourceDocument ediSourceDocument = exchange.getIn().getBody(EdiSourceDocument.class);

		correctFreightRule(ediSourceDocument);
		correctPartNumbersRule(ediSourceDocument);
	}

	private void correctFreightRule(EdiSourceDocument ediSourceDocument) {
		if (ediSourceDocument == null || ediSourceDocument.getLineItems() == null) {
			return; // Avoid NullPointerException
		}

		Iterator<EdiLine> iterator = ediSourceDocument.getLineItems().iterator();
		EdiLine freightLine = null;

		while (iterator.hasNext()) {
			EdiLine line = iterator.next();
			String partNumber = line.getPartNumber().toUpperCase();

			if (FREIGHT_PART_NUMBERS.contains(partNumber)) {
				freightLine = line;
				iterator.remove(); // Safely remove to prevent ConcurrentModificationException
				log.info("Removed freight line item: {}", partNumber);
				break; // Exit early after finding the first match
			}
		}

		if (freightLine != null) {
			FreightOptions freightOptions = ediSourceDocument.getFreightOptions();
			if (freightOptions == null) {
				freightOptions = new FreightOptions();
				ediSourceDocument.setFreightOptions(freightOptions);
			}

			Freight selectedFreight = freightOptions.getSelectedFreight();
			if (selectedFreight == null) {
				selectedFreight = new Freight();
				freightOptions.setSelectedFreight(selectedFreight);
			}

			BigDecimal freightCost = freightLine.getPrice() != null ? freightLine.getPrice() : BigDecimal.ZERO;
			selectedFreight.setCost(
					selectedFreight.getCost() != null ? selectedFreight.getCost().add(freightCost) : freightCost);
			selectedFreight.setDescription(freightLine.getPartNumber());

			log.info("Updated freight cost: {}", selectedFreight.getCost());
		}
	}

	private void correctPartNumbersRule(EdiSourceDocument ediSourceDocument) {
		if (ediSourceDocument == null || ediSourceDocument.getLineItems() == null) {
			return;
		}

		ediSourceDocument.getLineItems().forEach(line -> {
			String manufacturerCode = line.getManufacturerCode();
			String partNumber = line.getPartNumber();

			if (manufacturerCode != null && partNumber != null && partNumber.startsWith(manufacturerCode)) {
				String updatedPartNumber = partNumber.substring(manufacturerCode.length());
				line.setPartNumber(updatedPartNumber);
				log.info("Updated part number from {} to {}", partNumber, updatedPartNumber);
			}
		});
	}
}
