package com.autowares.mongoose.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.util.Pair;

import com.autowares.events.AwiEvent;
import com.autowares.servicescommon.model.Entity;
import com.autowares.servicescommon.model.Location;
import com.autowares.supplychain.model.OperationalContext;

public class IntegrationContext {

	private Entity actor; // system automation (?) (coordinator, kcml process) warehouse employee customer
	private AwiEvent event;
	private Location location;
	private ZonedDateTime actionTimestamp;
	private CoordinatorContext order;
	private List<LineItemContext> lineItems = new ArrayList<>();
	private String originatingSystem; // wms-core / viper
	private List<CoordinatorContext> coordinatorContexts = new ArrayList<>();
	private List<OperationalEventContext> operationalEvents = new ArrayList<>();
	private Pair<OperationalContext, OperationalStateManager> sourceContext;
	private List<Pair <OperationalContext, OperationalStateManager>> targetContexts = new ArrayList<>();

	public Entity getActor() {
		return actor;
	}

	public void setActor(Entity actor) {
		this.actor = actor;
	}

	public AwiEvent getEvent() {
		return event;
	}

	public void setEvent(AwiEvent event) {
		this.event = event;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public ZonedDateTime getActionTimestamp() {
		return actionTimestamp;
	}

	public void setActionTimestamp(ZonedDateTime actionTimestamp) {
		this.actionTimestamp = actionTimestamp;
	}

	public CoordinatorContext getOrder() {
		return order;
	}

	public void setOrder(CoordinatorContext order) {
		this.order = order;
	}

	public List<LineItemContext> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItemContext> lineItems) {
		this.lineItems = lineItems;
	}

	public String getOriginatingSystem() {
		return originatingSystem;
	}

	public void setOriginatingSystem(String originatingSystem) {
		this.originatingSystem = originatingSystem;
	}

	public List<CoordinatorContext> getCoordinatorContexts() {
		return coordinatorContexts;
	}

	public void setCoordinatorContexts(List<CoordinatorContext> coordinatorContexts) {
		this.coordinatorContexts = coordinatorContexts;
	}

	public List<OperationalEventContext> getOperationalEvents() {
		return operationalEvents;
	}

	public void setOperationalEvents(List<OperationalEventContext> operationalEvents) {
		this.operationalEvents = operationalEvents;
	}

	public Pair<OperationalContext, OperationalStateManager> getSourceContext() {
		return sourceContext;
	}

	public void setSourceContext(Pair<OperationalContext, OperationalStateManager> sourceContext) {
		this.sourceContext = sourceContext;
	}

	public List<Pair<OperationalContext, OperationalStateManager>> getTargetContexts() {
		return targetContexts;
	}

	public void setTargetContexts(List<Pair<OperationalContext, OperationalStateManager>> targetContexts) {
		this.targetContexts = targetContexts;
	}


}
