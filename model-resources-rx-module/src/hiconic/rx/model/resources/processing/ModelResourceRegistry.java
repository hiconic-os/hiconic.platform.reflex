// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.processing;

import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.model.generic.reflection.GmReflectionTools;
import com.braintribe.model.processing.session.api.common.GmSessions;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.resource.Resource;

import hiconic.rx.model.resources.api.ModelResourcesContract;

public class ModelResourceRegistry {
	private final Map<String, Resource> resources = new LinkedHashMap<>();
	private PersistenceGmSessionFactory sessionFactory;

	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public synchronized Resource publish(String stableId, Resource resource) {
		if (stableId == null || stableId.isBlank())
			throw new IllegalArgumentException("Model resource id must not be blank");
		if (resource == null)
			throw new IllegalArgumentException("Model resource must not be null");
		if (resource.getResourceSource() == null)
			throw new IllegalArgumentException("Model resource '" + stableId + "' must have a resource source");
		if (resources.putIfAbsent(stableId, resource) != null)
			throw new IllegalStateException("Duplicate model resource id: " + stableId);

		Resource reference = GmReflectionTools.makeShallowCopy(resource);
		reference.setId(stableId);
		reference.setGlobalId(stableId);
		reference.setPartition(ModelResourcesContract.ACCESS_ID);
		reference.setResourceSource(null);
		return reference;
	}

	public synchronized void persistAll() {
		PersistenceGmSession session = sessionFactory.newSession(ModelResourcesContract.ACCESS_ID);
		for (Map.Entry<String, Resource> entry : resources.entrySet()) {
			Resource persisted = GmSessions.cloneIntoSession(entry.getValue(), session);
			persisted.setId(entry.getKey());
			persisted.setGlobalId(entry.getKey());
			persisted.setPartition(ModelResourcesContract.ACCESS_ID);
		}
		session.commit();
	}
}
