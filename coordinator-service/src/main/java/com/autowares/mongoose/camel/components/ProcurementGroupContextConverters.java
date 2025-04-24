package com.autowares.mongoose.camel.components;

import org.apache.camel.Converter;
import org.springframework.stereotype.Component;

import com.autowares.mongoose.model.ProcurementGroupContext;
import com.autowares.supplychain.model.ProcurementGroup;

@Component
@Converter(generateLoader = true)
public class ProcurementGroupContextConverters {
	
	@Converter
	public static ProcurementGroup contextToProcurementGroup(ProcurementGroupContext context) {
		return new ProcurementGroup(context);
	}
	
	@Converter
	public static ProcurementGroupContext procurementGroupToContext(ProcurementGroup group) {
		return new ProcurementGroupContext(group);
	}

}
