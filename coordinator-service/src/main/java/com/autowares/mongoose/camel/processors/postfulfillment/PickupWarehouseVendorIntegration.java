package com.autowares.mongoose.camel.processors.postfulfillment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.autowares.apis.ids.command.WarehouseMasterClient;
import com.autowares.apis.ids.model.WarehouseMaster;
import com.autowares.mongoose.model.criteria.PkCriteria;

@Component
public class PickupWarehouseVendorIntegration implements Processor {

	private static List<PkCriteria> pkCriteriaList = new ArrayList<PkCriteria>();
	private static Logger log = LoggerFactory.getLogger(PickupWarehouseVendorIntegration.class);

	WarehouseMasterClient warehouseMasterClient = new WarehouseMasterClient();

	@Override
	public void process(Exchange exchange) throws Exception {
		String fileName = exchange.getIn().getHeader("CamelFileNameConsumed", String.class);
		String buildingCode = fileNameToBuildingCode(fileName);
		if (buildingCode != null) {
			List<PkCriteria> testVar = parseVendorCodeSubCodes(buildingCode, exchange.getIn().getBody(String.class));
			pkCriteriaList.addAll(testVar);
		}
	}

	public String fileNameToBuildingCode(String fileName) {

		String regEx = ".*([\\d]{2})$";
		Matcher testVar = Pattern.compile(regEx).matcher(fileName);
		if (testVar.matches()) {
			Integer warehouseNumber = Integer.valueOf(testVar.group(1));
			Page<WarehouseMaster> warehouseMasters = warehouseMasterClient.findWarehouse(null, warehouseNumber, null,
					true);
			if (warehouseMasters != null && warehouseMasters.getContent() != null
					&& warehouseMasters.getContent().size() == 1) {
				return warehouseMasters.getContent().get(0).getBuildingMnemonic();
			} 
			log.error("Unable to map warehouse number: " + warehouseNumber + " to a buildingCode");
		}
		return null;
	}

	public List<PkCriteria> parseVendorCodeSubCodes(String buildingCode, String fileContents) {

		List<PkCriteria> pkCriteria = new ArrayList<PkCriteria>();
		String regEx = "([A-Z0-9]{4})[A-Z0-9]";
		Matcher testVar = Pattern.compile(regEx).matcher(fileContents);
		while (testVar.find()) {
			PkCriteria pkC = new PkCriteria();
			pkC.setVendorCode(testVar.group(1));
			pkC.setBuildingCode(buildingCode);
			pkCriteria.add(pkC);
		}
		return pkCriteria;
	}

	public List<PkCriteria> getPkCriteriaList() {
		return pkCriteriaList;
	}

}
