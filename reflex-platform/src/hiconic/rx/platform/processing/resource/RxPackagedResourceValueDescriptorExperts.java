// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.processing.resource;

import java.io.InputStream;
import java.util.function.Predicate;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.processing.resource.packaged.PackagedResourceValueDescriptorExperts;
import com.braintribe.model.processing.resource.packaged.api.PackagedResourceResolver;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionCodec;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionProjection;
import com.braintribe.model.processing.vde.reasoned.api.ValueDescriptorSourceContext;
import com.braintribe.model.processing.vde.reasoned.impl.ValueDescriptorExpertRegistry;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.PackagedSource;

import hiconic.rx.module.api.resource.RxPackagedResourceResolver;

/**
 * Adapts the RX resolver to the generic {@link PackagedResourceResolver}, so that RX gets the whole packaged resource vocabulary without adding
 * anything of its own.
 * <p>
 * There is nothing RX specific left in the vocabulary itself. This class exists only because the RX resolver has its own interface.
 */
public final class RxPackagedResourceValueDescriptorExperts {

	private RxPackagedResourceValueDescriptorExperts() {
	}

	public static ValueDescriptorExpressionCodec expressionCodec() {
		return PackagedResourceValueDescriptorExperts.expressionCodec();
	}

	public static void register(ValueDescriptorExpertRegistry registry, RxPackagedResourceResolver resolver) {
		PackagedResourceValueDescriptorExperts.register(registry, packagedResolver(resolver));
	}

	/** @see PackagedResourceValueDescriptorExperts#projection(ValueDescriptorSourceContext) */
	public static ValueDescriptorExpressionProjection projection(ValueDescriptorSourceContext target) {
		return PackagedResourceValueDescriptorExperts.projection(target);
	}

	/**
	 * Projects packaged sources and, when the caller explicitly approves it, entire regenerable Resources. The predicate is deliberately external:
	 * the shape of a source alone is no proof that Resource metadata may be discarded.
	 */
	public static ValueDescriptorExpressionProjection projection(ValueDescriptorSourceContext target,
			Predicate<? super Resource> regenerableResource) {
		return PackagedResourceValueDescriptorExperts.projection(target, regenerableResource, source -> false);
	}

	private static PackagedResourceResolver packagedResolver(RxPackagedResourceResolver resolver) {
		if (resolver instanceof PackagedResourceResolver packagedResolver)
			return packagedResolver;

		return new PackagedResourceResolver() {
			@Override
			public Maybe<Resource> resolveResource(String artifact, String path) {
				try {
					return Maybe.complete(resolver.resource(artifact, path).asPersistableResource());
				} catch (IllegalArgumentException e) {
					return NotFound.create(e.getMessage()).asMaybe();
				}
			}

			@Override
			public Maybe<PackagedSource> resolveSource(String artifact, String path) {
				try {
					return Maybe.complete(resolver.resource(artifact, path).asSource());
				} catch (IllegalArgumentException e) {
					return NotFound.create(e.getMessage()).asMaybe();
				}
			}

			@Override
			public Maybe<InputStream> openStream(String artifact, String path) {
				try {
					return Maybe.complete(resolver.resource(artifact, path).asHandle().asStream());
				} catch (IllegalArgumentException e) {
					return NotFound.create(e.getMessage()).asMaybe();
				}
			}
		};
	}
}
