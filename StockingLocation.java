package com.autowares.mongoose.optaplanner.domain;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class StockingLocation {

	private String locationName;
	private Boolean isAwi = true;
	private Boolean isStore = false;
	private Boolean pkLocation = false;
	private int qoh;
	private int logisticalScore;
	private int transfers;
	private Integer preMark;
	private ZonedDateTime arrivalDate;
	private List<StockPutawayDetail> putawayDetails = new ArrayList<>();
	private Duration untilDeparture;
	private int minFillableArrivalDate;
	private int arrivalDateAsInt;
	static private Long orderEpoch = ZonedDateTime.now().toEpochSecond();

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Boolean getIsAwi() {
		return isAwi;
	}

	public void setIsAwi(Boolean isAwi) {
		this.isAwi = isAwi;
	}

	public int getQoh() {
		return qoh;
	}

	public void setQoh(int qoh) {
		this.qoh = qoh;
	}

	public int getLogisticalScore() {
		return logisticalScore;
	}

	public void setLogisticalScore(int logisticalScore) {
		this.logisticalScore = logisticalScore;
	}
	
	public Boolean availableToday() {
		if (arrivalDate != null && ZonedDateTime.now().getDayOfYear() == arrivalDate.getDayOfYear()) {
			return true;
		}
		return false;
	}


	@Override
	public String toString() {
		return this.locationName;
	}
    
	@JsonIgnore
	public List<StockPutawayDetail> getPutawayDetails() {
		return putawayDetails;
	}

	public void setStockDetails(List<StockPutawayDetail> putawayDetails) {
		this.putawayDetails = putawayDetails;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((locationName == null) ? 0 : locationName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StockingLocation other = (StockingLocation) obj;
		if (locationName == null) {
			if (other.locationName != null)
				return false;
		} else if (!locationName.equals(other.locationName))
			return false;
		return true;
	}

	public Boolean getIsStore() {
		return isStore;
	}

	public void setIsStore(Boolean isStore) {
		this.isStore = isStore;
	}

	public int getArrivalDateAsInt() {
		return this.arrivalDateAsInt;
	}
	
	public void setArrivalDateAsInt(int arrivalDateAsInt) {
		this.arrivalDateAsInt = arrivalDateAsInt;
	}

	public int getTransfers() {
		return transfers;
	}

	public void setTransfers(int transfers) {
		this.transfers = transfers;
	}

	public ZonedDateTime getArrivalDate() {
		return arrivalDate;
	}

	public void setArrivalDate(ZonedDateTime arrivalDate) {
		this.arrivalDate = arrivalDate;
		if (arrivalDate != null) {
			this.arrivalDateAsInt = (int) (this.arrivalDate.toInstant().getEpochSecond() - orderEpoch);
 		}
	}

	public Boolean getPkLocation() {
		return pkLocation;
	}

	public void setPkLocation(Boolean pkLocation) {
		this.pkLocation = pkLocation;
	}

	public Duration getTimeToDepart() {
		return untilDeparture;
	}

	public void setTimeToDepart(Duration timeToDepart) {
		this.untilDeparture = timeToDepart;
	}

	public int getMinFillableArrivalDate() {
		return minFillableArrivalDate;
	}

	public void setMinFillableArrivalDate(int minFillableArrivalDate) {
		this.minFillableArrivalDate = minFillableArrivalDate;
	}
	
	public Integer getPreMark() {
		return preMark;
	}

	public void setPreMark(Integer preMark) {
		this.preMark = preMark;
	}



}
