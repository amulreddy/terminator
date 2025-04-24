package com.autowares.mongoose.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.camel.components.IntegrationContextTypeConverter;
import com.autowares.mongoose.model.OperationalStateManager;
import com.autowares.servicescommon.model.Freight;
import com.autowares.servicescommon.model.OperationalItem;
import com.autowares.servicescommon.model.SourceDocumentType;
import com.autowares.supplychain.model.GenericLine;
import com.autowares.supplychain.model.OperationalContext;
import com.autowares.supplychain.model.SupplyChainSourceDocumentImpl;

@Component
public class SupplyChainOperationalStateManager implements OperationalStateManager {

	@Autowired
	private SupplyChainService supplyChainService;

	@Override
	public OperationalContext getOperationalContext(Long moaOrderId) {
		return null;
	}
	
	@Override
	public OperationalContext getOperationalContext(String xmlOrderId, String building, Integer lineNumber) {
		Optional<SupplyChainSourceDocumentImpl> optionalOrder = supplyChainService.getClient()
				.findSourceDocumentByDocumentId(xmlOrderId, SourceDocumentType.PurchaseOrder);
		if (optionalOrder.isPresent()) {
			SupplyChainSourceDocumentImpl order = optionalOrder.get();
			List<GenericLine> lines = order.getLineItems().stream().filter(i -> lineNumber.equals(i.getLineNumber()))
					.collect(Collectors.toList());
			for (GenericLine line : lines) {
				for (Freight location : line.getFulfillmentLocations()) {
					if (building.equals(location.getLocationName())) {
						String packSlipId = location.getDocumentReferenceId();
						return getOperationalContext(packSlipId, lineNumber);
					}
				}
			}
		}
		return null;
	}

	@Override
	public OperationalContext getOperationalContext(String packSlipId, Integer lineNumber) {
		Optional<SupplyChainSourceDocumentImpl> optionalPackSlip = supplyChainService.getClient()
				.findSourceDocumentByDocumentId(packSlipId, SourceDocumentType.PackSlip);
		if (optionalPackSlip.isPresent()) {
			return IntegrationContextTypeConverter
					.supplyChainPackSlipToIntegrationContext(optionalPackSlip.get(), lineNumber);
		}
		return null;
	}

	@Override
	public void mergeContexts(OperationalContext sourceContext, OperationalContext targetContext) {
		targetContext.getItems().get(0).getDataSource();
	}

	@Override
	public OperationalContext persist(OperationalContext context) {
		//TODO implement
		return null;
	}

	@Override
	public <T extends OperationalItem> T persist(T operationalItem) {
		// TODO Auto-generated method stub
		return null;
	}

}
