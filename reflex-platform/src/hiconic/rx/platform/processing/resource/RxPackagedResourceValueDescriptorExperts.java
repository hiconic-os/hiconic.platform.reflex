// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.processing.resource;

import java.util.function.Predicate;

import com.braintribe.model.processing.resource.packaged.PackagedResourceValueDescriptorExperts;
import com.braintribe.model.processing.resource.packaged.api.PackagedResourceResolver;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionProjection;
import com.braintribe.model.processing.vde.reasoned.api.ValueDescriptorSourceContext;
import com.braintribe.model.resource.Resource;

/**
 * Adapts the RX resolver to the generic {@link PackagedResourceResolver}, so that RX gets the whole packaged resource vocabulary without adding
 * anything of its own.
 * <p>
 * There is nothing RX specific left in the vocabulary itself. This class exists only because the RX resolver has its own interface.
 */
public final class RxPackagedResourceValueDescriptorExperts {

	private RxPackagedResourceValueDescriptorExperts() {
	}

	/**
	 * Projects packaged sources and, when the caller explicitly approves it, entire regenerable Resources. The predicate is deliberately external:
	 * the shape of a source alone is no proof that Resource metadata may be discarded.
	 */
	public static ValueDescriptorExpressionProjection projection(ValueDescriptorSourceContext target,
			Predicate<? super Resource> regenerableResource) {
		return PackagedResourceValueDescriptorExperts.projection(target, regenerableResource, source -> false);
	}

}
