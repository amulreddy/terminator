package com.autowares.mongoose.command;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.springframework.web.util.UriBuilder;

import com.autowares.apis.partservice.Part;
import com.autowares.apis.partservice.PartAvailability;
import com.autowares.mongoose.model.LineItemContext;
import com.autowares.servicescommon.client.BaseResillience4JClient;
import com.autowares.servicescommon.client.DiscoverService;
import com.autowares.stockcheck.InquiryType;
import com.autowares.stockcheck.InventoryRequest;
import com.autowares.stockcheck.InventoryResponse;
import com.autowares.stockcheck.InventoryResponseMulti;
import com.autowares.stockcheck.PartRequestItem;
import com.google.common.collect.Lists;

@DiscoverService(name = "stores", path = "/stores/inventory/current/live")
public class StoreInventoryCommand extends BaseResillience4JClient {

	public CompletableFuture<InventoryResponseMulti> lookupStoreAvailability(Collection<? extends LineItemContext> details,
			String storeAwiAccountNo) {
		InventoryRequest request = new InventoryRequest();
		request.setCustnum("7");
		request.setItems(convertOrderDetailstoItems(details));
		Supplier<InventoryResponseMulti> supplier = () -> this.inventoryCall(request, storeAwiAccountNo,
				InquiryType.STORE);
		CompletableFuture<InventoryResponseMulti> storeInventory = decorateAsync(supplier, Duration.ofSeconds(5));
		return storeInventory;
	}

	public Collection<? extends LineItemContext> getStoreAvailability(Collection<? extends LineItemContext> details,
			CompletableFuture<InventoryResponseMulti> future, String storeAwiAccountNo) {
		if (future != null) {
			InventoryResponseMulti storeInventory = new InventoryResponseMulti();
			try {
				storeInventory = future.get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
			if (storeInventory != null && storeInventory.getItems() != null) {
				for (InventoryResponse response : storeInventory.getItems()) {
					for (LineItemContext detail : details) {
						if (response.getLineNumber().equals(detail.getLineNumber())) {
							if (response.getQuantityOnHand() != null && response.getQuantityOnHand() > 0) {
								PartAvailability availability = new PartAvailability();
								availability.setBuildingCode(storeAwiAccountNo);
								availability.setQuantityOnHand(response.getQuantityOnHand());
								if (detail.getPart() == null) {
									Part part = new Part();
									part.setAvailability(Lists.newArrayList());
									detail.setPart(part);
								}
								detail.getPart().getAvailability().add(availability);
							}
						}
					}
				}
			}
		}
		return details;
	}

	private List<PartRequestItem> convertOrderDetailstoItems(Collection<? extends LineItemContext> details) {
		List<PartRequestItem> partItems = new ArrayList<PartRequestItem>();
		Integer lineNumber = 1;
		for (LineItemContext detail : details) {
			PartRequestItem item = new PartRequestItem();
			if (detail.getLineNumber() == null) {
				detail.setLineNumber(lineNumber);
			}
			item.setLineNumber(detail.getLineNumber());
			item.setLineCode(detail.getCounterWorksLineCode());
			item.setPartNumber(detail.getPartNumber());
			item.setRequestQty(detail.getQuantity());
			lineNumber = lineNumber + 1;
			partItems.add(item);
		}
		return partItems;
	}

	private InventoryResponseMulti inventoryCall(InventoryRequest request, String storeAwiAccountNo,
			InquiryType inquiryType) {
		UriBuilder uriBuilder = getUriBuilder();
		uriBuilder.queryParam("storeAwiAccountNo", storeAwiAccountNo);
		uriBuilder.queryParam("inquiryType", inquiryType);
		return postForObject(uriBuilder, request, InventoryResponseMulti.class);
	}

}
