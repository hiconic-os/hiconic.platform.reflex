// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.api;

import com.braintribe.model.resource.Resource;

import hiconic.rx.module.api.wire.RxExportContract;

/** Publishes resources referenced by transient model metadata through one stable access. */
public interface ModelResourcesContract extends RxExportContract {

	String ACCESS_ID = "model-resources";

	/** The access clients must use for resources referenced by a model environment. */
	default String accessId() {
		return ACCESS_ID;
	}

	/**
	 * Registers the complete resource graph under a deterministic id and returns a detached,
	 * shallow reference suitable for model metadata. The returned reference intentionally has
	 * no {@link com.braintribe.model.resource.source.ResourceSource}.
	 */
	Resource publish(String stableId, Resource resource);
}
