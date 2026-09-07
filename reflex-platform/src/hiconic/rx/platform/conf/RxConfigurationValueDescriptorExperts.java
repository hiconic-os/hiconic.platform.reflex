// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.conf;

import com.braintribe.model.bvd.resource.PackagedResource;
import com.braintribe.model.bvd.resource.PackagedResourceText;
import com.braintribe.model.bvd.resource.PackagedSource;
import com.braintribe.model.bvd.resource.ResourceText;
import com.braintribe.model.processing.vde.expression.ModelBasedValueDescriptorExpressionCodec;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionCodec;
import com.braintribe.model.processing.vde.reasoned.impl.ValueDescriptorExpertRegistry;

import hiconic.rx.module.api.resource.RxPackagedResourceResolver;
import hiconic.rx.platform.model.configuration.vd.Decrypt;
import hiconic.rx.platform.processing.resource.RxPackagedResourceValueDescriptorExperts;

/** The typed value-descriptor vocabulary understood by RX modeled configuration. */
public final class RxConfigurationValueDescriptorExperts {

	private RxConfigurationValueDescriptorExperts() {
	}

	public static ValueDescriptorExpressionCodec expressionCodec() {
		return new ModelBasedValueDescriptorExpressionCodec(Decrypt.T, PackagedSource.T, PackagedResource.T, PackagedResourceText.T,
				ResourceText.T);
	}

	public static void register(ValueDescriptorExpertRegistry registry, RxPackagedResourceResolver resources,
			RxPropertyResolver properties) {
		RxPackagedResourceValueDescriptorExperts.register(registry, resources);
		registry.register(Decrypt.T, (context, descriptor) -> properties.decryptReasoned(descriptor.getValue()));
	}
}
