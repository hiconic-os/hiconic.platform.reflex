package hiconic.rx.check.wire.space;

import static com.braintribe.wire.api.util.Lists.list;

import java.util.function.Supplier;

import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.thread.api.DeferringThreadContextScoping;
import com.braintribe.thread.api.ThreadContextScope;
import com.braintribe.thread.impl.ThreadContextScopingImpl;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.check.api.CheckContract;
import hiconic.rx.check.api.CheckServiceDomain;
import hiconic.rx.check.model.api.request.AuthorizedCheckRequest;
import hiconic.rx.check.model.api.request.CheckRequest;
import hiconic.rx.check.model.api.request.RunVitalityChecks;
import hiconic.rx.check.processing.BasicCheckProcessorRegistry;
import hiconic.rx.check.processing.CheckResponseHtmlMarshaller;
import hiconic.rx.check.processing.CheckRxProcessor;
import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.webapi.model.meta.ResponseMimeType;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class CheckRxModuleSpace implements RxModuleContract, CheckContract {

	@Import
	private RxPlatformContract platform;

	// ###############################################
	// ##. . . . . . . . Marshaller . . . . . . . . ##
	// ###############################################

	/** Mime type of the {@link CheckResponseHtmlMarshaller}, i.e. of the rendered check result page. */
	private static final String CHECK_RESPONSE_HTML = "text/html;spec=check-response";

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		configurator.marshallerRegistry().registerMarshaller(CHECK_RESPONSE_HTML, checkResultToHtmlMarshaller());
	}

	@Managed
	@Override
	public Marshaller checkResultToHtmlMarshaller() {
		return new CheckResponseHtmlMarshaller();
	}

	// ###############################################
	// ## . . . . . . . Service Domain. . . . . . . ##
	// ###############################################

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration checkSd = configurations.byId(CheckServiceDomain.check);
		checkSd.bindRequest(CheckRequest.T, this::checkRxProcessor);

		/* Every request that returns a CheckResponse is rendered as HTML by default. RunHealthChecks is left out on
		 * purpose: it returns a map of results and stays JSON, like in CX. */
		checkSd.configureModel(editor -> {
			editor.onEntityType(AuthorizedCheckRequest.T).addMetaData(checkResponseHtmlMimeType());
			editor.onEntityType(RunVitalityChecks.T).addMetaData(checkResponseHtmlMimeType());
		});
	}

	private ResponseMimeType checkResponseHtmlMimeType() {
		ResponseMimeType bean = ResponseMimeType.T.create();
		bean.setMimeType(CHECK_RESPONSE_HTML);
		return bean;
	}

	@Managed
	private CheckRxProcessor checkRxProcessor() {
		CheckRxProcessor bean = new CheckRxProcessor();
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setInstanceId(platform.application().instanceId());
		bean.setThreadContextScoping(threadContextScoping());
		bean.setRegistry(checkProcessorRegistry());

		return bean;
	}

	@Override
	@Managed
	public BasicCheckProcessorRegistry checkProcessorRegistry() {
		BasicCheckProcessorRegistry bean = new BasicCheckProcessorRegistry();

		return bean;
	}

	// TODO move thread context scoping elsewhere
	// source: CurrentUserAuthContextSpace.threadContextScoping()
	@Managed
	private DeferringThreadContextScoping threadContextScoping() {
		ThreadContextScopingImpl bean = new ThreadContextScopingImpl();
		bean.setScopeSuppliers(list(serviceRequestContextThreadContextScopeSupplier()));
		return bean;
	}

	@Managed
	private StandardRequestContextThreadContextScopeSupplier serviceRequestContextThreadContextScopeSupplier() {
		StandardRequestContextThreadContextScopeSupplier bean = new StandardRequestContextThreadContextScopeSupplier();
		return bean;
	}

	public class StandardRequestContextThreadContextScopeSupplier implements Supplier<ThreadContextScope> {

		@Override
		public ThreadContextScope get() {

			AttributeContext callerContext = AttributeContexts.peek();

			return new ThreadContextScope() {
				@Override
				public void push() {
					AttributeContexts.push(callerContext);
				}

				@Override
				public void pop() {
					AttributeContexts.pop();
				}
			};
		}
	}

}
