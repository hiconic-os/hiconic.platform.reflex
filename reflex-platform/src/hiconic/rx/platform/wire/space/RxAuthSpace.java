package hiconic.rx.platform.wire.space;

import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.processing.auth.RxUserSessionSupplier;

@Managed
public class RxAuthSpace implements RxAuthContract {
	
	@Import
	private RxPlatformContract platform;

	@Override
	public Supplier<String> contextUserSessionIdSupplier() {
		return contextUserSessionSupplier()::findSessionId;
	}

	@Override
	public Supplier<String> systemUserSessionIdSupplier() {
		return systemUserSessionSupplier()::findSessionId;
	}
	
	@Override
	public Supplier<String> userSessionIdSupplier(AttributeContext attributeContext) {
		return userSessionSupplier(attributeContext)::findSessionId;
	}

	@Override
	@Managed
	public RxUserSessionSupplier contextUserSessionSupplier() {
		return new RxUserSessionSupplier();
	}

	@Override
	@Managed
	public RxUserSessionSupplier systemUserSessionSupplier() {
		return new RxUserSessionSupplier(systemAttributeContextSupplier());
	}
	
	@Override
	public RxUserSessionSupplier userSessionSupplier(AttributeContext attributeContext) {
		return new RxUserSessionSupplier(() -> attributeContext);
	}

	@Override
	public Supplier<AttributeContext> contextAttributeContextSupplier() {
		return AttributeContexts::peek;
	}

	@Override
	public Supplier<AttributeContext> systemAttributeContextSupplier() {
		return platform.systemAttributeContextSupplier();
	}
}
