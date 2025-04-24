package com.autowares.mongoose.service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.camel.spi.IdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.autowares.assetmanagement.client.AssetClient;
import com.autowares.assetmanagement.model.Asset;
import com.autowares.assetmanagement.model.AssetType;
import com.autowares.assetmanagement.model.OwningEntity;
import com.autowares.servicescommon.model.Document;
import com.autowares.servicescommon.model.EntityType;

@Component
@EnableScheduling
public class LockedDocuments implements IdempotentRepository {

	public static final Set<Asset> documents = new CopyOnWriteArraySet<>();
	private static Logger log = LoggerFactory.getLogger(LockedDocuments.class);
	public static final AssetClient assetClient = new AssetClient();
	public static final String instanceId = UUID.randomUUID().toString();

	public static Set<Asset> getDocuments() {
		return documents;
	}

	public static boolean isLocked(Document document) {
		return isLocked(document.getDocumentId());
	}

	public static void lockDocument(Document context) {
		lock(context.getDocumentId());
		MDC.put("orderId", context.getDocumentId());
	}

	public static void unlockDocument(Document document) {
		unlock(document.getDocumentId());
		MDC.clear();
	}

	public static boolean isLocked(String key) {
		if (key != null && documents.stream().anyMatch(i -> i.getAssetId().equals(key))) {
			return true;
		}
		return false;
	}

	public static void lock(String key) {
		Asset asset = getOrCreateAsset(key);
		asset = assetClient.lockAsset(asset);
		documents.add(asset);
	}

	public static void unlock(String key) {
		Asset asset = getOrCreateAsset(key);
		documents.remove(asset);
		assetClient.deleteAsset(key, AssetType.Document);
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean add(String key) {
		try {
			if (!isLocked(key)) {
				lock(key);
				return true;
			}
		} catch (HttpClientErrorException e) {
			if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
				log.info("Failed to lock " + key + " is locked.");
			}
		} catch (Exception e) {
			log.error("Failed to lock " + key, e);
		}
		return false;
	}

	@Override
	public boolean contains(String key) {
		return isLocked(key);
	}

	@Override
	public boolean remove(String key) {
		try {
			if (isLocked(key)) {
				unlock(key);
				return true;
			}
		} catch (Exception e) {
			log.error("Failed to unlock " + key, e);
		}
		return false;
	}

	@Override
	public boolean confirm(String key) {
		return remove(key);
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	private static Asset getOrCreateAsset(String key) {
		Asset asset = createAsset(key);
		Optional<Asset> existing = documents.stream().filter(i -> i.equals(asset)).findAny();
		if (existing.isPresent()) {
			return existing.get();
		}
		return asset;
	}

	private static Asset createAsset(String key) {
		Asset document = new Asset();
		document.setAssetId(key);
		document.setAssetType(AssetType.Document);
		OwningEntity owningEntity = new OwningEntity();
		owningEntity.setEntityId(LockedDocuments.instanceId);
		owningEntity.setEntityType(EntityType.Person);
		document.setOwningEntity(owningEntity);
		return document;
	}

	private void refreshLock(Asset asset) {
		assetClient.lockAsset(asset);
	}

	@Scheduled(fixedRate = 30000)
	private void watchDog() {
		for (Asset document : getDocuments()) {
			long lockedMinutes = ChronoUnit.MINUTES.between(document.getLockTimestamp(), ZonedDateTime.now());
			if (lockedMinutes > 4) {
				// Don't hold on forever,
				log.info("Giving up lock on: " + document.getAssetId());
				documents.remove(document);
//			    SimpleNotificationRequestBuilder requestBuilder = SimpleNotificationRequestBuilder.builder()
//                        .withMessage("Giving up lock. documentId: " + document.getAssetId())
//                        .withSubject("Coordinator " + activeProfile )
//                        .withSender("noreply@autowares.com")
//                        .withRecipientEmailAddress("kcml@autowares.com");
//				notificationClient.notify(requestBuilder.build());
			} else {
				log.info("Refreshing lock on: " + document.getAssetId());
				refreshLock(document);
			}
		}
	}

}
