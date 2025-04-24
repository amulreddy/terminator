package com.autowares.mongoose.model;

import java.math.BigDecimal;

import com.autowares.servicescommon.model.ChargeType;
import com.autowares.servicescommon.model.ChargesImpl;

public class ChargesBuilder {

	private ChargesImpl charges = new ChargesImpl();

	public static ChargesBuilder builder() {
		return new ChargesBuilder();
	}

	public ChargesBuilder withCharge(BigDecimal charge) {
		this.charges.setCharge(charge);
		return this;
	}

	public ChargesBuilder withDescription(String description) {
		this.charges.setDescription(description);
		return this;
	}

	public ChargesBuilder withChargeType(ChargeType chargeType) {
		this.charges.setChargeType(chargeType);
		return this;
	}

	public ChargesImpl build() {
		return charges;
	}

}
