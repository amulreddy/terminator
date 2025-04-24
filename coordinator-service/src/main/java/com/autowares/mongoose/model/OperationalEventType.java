package com.autowares.mongoose.model;

public enum OperationalEventType {
	Assigned,
	Unassigned,
	Pulled,
	Packed,
	Zeroed,
	Canceled,
	Rejected,
	Stocked,
	
	/** Container operations methods **/
	MoveToteToRack,
	CloseToteInRack,
	Shipped,
	Loaded,
	CreatedPallet,
	Labeled,
	
	/** Other Events */
	StockJobAssigned,
	PullPrioritySet,
	BreakStarted, /* ?? */
	BreakEnded, /* ?? */
}
