package com.autowares.mongoose.optaplanner.domain;

import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class FulfillmentLocationStrengthComparator  implements Comparator<FulfillmentLocation> {

	@Override
	public int compare(FulfillmentLocation a, FulfillmentLocation b) {
		 if (a != null && b != null) {
	       return new CompareToBuilder()
	                .append(a.getOverStockedScore(), b.getOverStockedScore())
	                .toComparison();
		 }
		 return 0;
	}

}
