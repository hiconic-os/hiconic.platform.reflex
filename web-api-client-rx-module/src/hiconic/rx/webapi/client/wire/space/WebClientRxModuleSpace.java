package hiconic.rx.webapi.client.wire.space;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;

import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.transport.http.DefaultHttpClientProvider;
import com.braintribe.transport.http.HttpClientProvider;
import com.braintribe.transport.ssl.SslSocketFactoryProvider;
import com.braintribe.transport.ssl.impl.EasySslSocketFactoryProvider;
import com.braintribe.transport.ssl.impl.PemSslSocketFactoryProvider;
import com.braintribe.transport.ssl.impl.StrictSslSocketFactoryProvider;
import com.braintribe.utils.lcd.CollectionTools2;
import com.braintribe.utils.logging.LogLevels;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceProcessorRegistry;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxServiceProcessingContract;
import hiconic.rx.webapi.client.api.ClientsFactory;
import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.WebApiClientContract;
import hiconic.rx.webapi.client.model.configuration.HttpClientCertificate;
import hiconic.rx.webapi.client.model.configuration.HttpCredentials;
import hiconic.rx.webapi.client.model.configuration.HttpUsernamePasswordCredentials;
import hiconic.rx.webapi.client.model.configuration.WebApiRemoteProcessor;
import hiconic.rx.webapi.client.model.configuration.WebApiRemoteProcessorConfiguration;
import hiconic.rx.webapi.client.processing.FixedClientContextResolver;
import hiconic.rx.webapi.client.processing.GmHttpClient;
import hiconic.rx.webapi.client.processing.WebApiClientServiceProcessor;

/**
 * This spaces provides implementation for HttpClientContract and WebApiClientContract.
 */
@Managed
public class WebClientRxModuleSpace implements RxModuleContract, WebApiClientContract, ClientsFactory {

	@Import
	private RxPlatformContract platform;

	@Import
	private RxServiceProcessingContract serviceProcessing;

	@Override
	public ClientsFactory clientsFactory() {
		return this;
	}

	// ################################################
	// ## . . . . . . HttpClientContract . . . . . . ##
	// ################################################

	@Override
	public HttpClient createHttpClient(WebApiRemoteProcessor deployable) {
		GmHttpClient bean = new GmHttpClient();
		bean.setBaseUrl(deployable.getBaseUrl());
		bean.setMimeTypeRegistry(platform.transientData().mimeTypeRegistry());
		bean.setMarshallerRegistry(platform.marshalling().marshallers());
		bean.setHttpClientProvider(clientProviderFor(deployable));
		bean.setHttpRequestConfig(buildRequestConfig(deployable));
		bean.setCredentials(getCredentials(deployable));
		bean.setDefaultHeaders(deployable.getDefaultHeaders());
		bean.setRequestLogging(LogLevels.convert(deployable.getRequestLogging()));
		bean.setResponseLogging(LogLevels.convert(deployable.getResponseLogging()));
		bean.setEvaluator(serviceProcessing.systemEvaluator());
		bean.setStreamPipeFactory(platform.transientData().streamPipeFactory());
		bean.setDomainIdToCmdResolver(this::resolveDomainIdToCmd);
		return bean;
	}

	/**
	 * The client provider for a concrete remote processor. Processors which leave the TLS related properties at their defaults share the
	 * module's {@link #clientProvider() default provider} and thus its connection pool; anything deviating - a client certificate or relaxed
	 * certificate/hostname verification - gets its own provider with its own pool and SSL context.
	 */
	private HttpClientProvider clientProviderFor(WebApiRemoteProcessor deployable) {
		boolean defaultTls = deployable.getClientCertificate() == null //
				&& deployable.getVerifyServerCertificate() //
				&& deployable.getVerifyServerHostname();

		return defaultTls ? clientProvider() : tlsSpecificClientProvider(deployable);
	}

	@Managed
	private HttpClientProvider clientProvider() {
		DefaultHttpClientProvider bean = new DefaultHttpClientProvider();
		bean.setSslSocketFactoryProvider(sslSocketFactoryProvider());
		bean.setHostnameVerifier(new DefaultHostnameVerifier());
		return bean;
	}

	@Managed
	private SslSocketFactoryProvider sslSocketFactoryProvider() {
		return new StrictSslSocketFactoryProvider();
	}

	/** Not managed on purpose: the TLS settings behind it are per remote processor. */
	private HttpClientProvider tlsSpecificClientProvider(WebApiRemoteProcessor deployable) {
		DefaultHttpClientProvider bean = new DefaultHttpClientProvider();
		bean.setSslSocketFactoryProvider(tlsSpecificSslSocketFactoryProvider(deployable));

		// the transport defaults to no hostname verification, so this has to be set explicitly
		if (deployable.getVerifyServerHostname())
			bean.setHostnameVerifier(new DefaultHostnameVerifier());

		return bean;
	}

	/**
	 * Provides the client side identity (mutual TLS) if a client certificate is configured, and in any case honours the certificate validation
	 * setting of the remote processor.
	 */
	private SslSocketFactoryProvider tlsSpecificSslSocketFactoryProvider(WebApiRemoteProcessor deployable) {
		boolean trustAll = !deployable.getVerifyServerCertificate();
		HttpClientCertificate certificate = deployable.getClientCertificate();

		if (certificate == null)
			return trustAll ? new EasySslSocketFactoryProvider() : new StrictSslSocketFactoryProvider();

		PemSslSocketFactoryProvider bean = new PemSslSocketFactoryProvider();
		bean.setCertificatePem(certificate.getCertificate());
		bean.setPrivateKeyPem(certificate.getPrivateKey());
		bean.setTrustAll(trustAll);

		return bean;
	}

	private Credentials getCredentials(WebApiRemoteProcessor deployable) {
		HttpCredentials credentials = deployable.getCredentials();

		if (credentials != null) {
			if (credentials instanceof HttpUsernamePasswordCredentials) {
				HttpUsernamePasswordCredentials userPasswordCredentials = (HttpUsernamePasswordCredentials) credentials;
				return new UsernamePasswordCredentials(userPasswordCredentials.getUser(), userPasswordCredentials.getPassword());
			} else {
				throw new IllegalArgumentException("Unsuppored HttpCredentials configured: " + credentials);
			}
		}

		return null;
	}

	private RequestConfig buildRequestConfig(WebApiRemoteProcessor deployable) {
		Builder configBuilder = RequestConfig.custom();

		configBuilder.setAuthenticationEnabled(deployable.getAuthenticationEnabled());
		configBuilder.setRedirectsEnabled(deployable.getRedirectsEnabled());
		configBuilder.setRelativeRedirectsAllowed(deployable.getRelativeRedirectsAllowed());
		configBuilder.setContentCompressionEnabled(deployable.getContentCompressionEnabled());

		setIfNotNull(deployable::getProxy, v -> configBuilder.setProxy(HttpHost.create(v)));
		setIfNotNull(deployable::getLocalAddress, v -> setLocalAddress(configBuilder, v));
		setIfNotNull(deployable::getCookieSpec, configBuilder::setCookieSpec);
		setIfNotNull(deployable::getConnectTimeout, configBuilder::setConnectTimeout);
		setIfNotNull(deployable::getConnectionRequestTimeout, configBuilder::setConnectionRequestTimeout);
		setIfNotNull(deployable::getSocketTimeout, configBuilder::setSocketTimeout);
		setIfNotNull(deployable::getMaxRedirects, configBuilder::setMaxRedirects);

		return configBuilder.build();
	}

	private void setLocalAddress(Builder configBuilder, String localAddress) {
		try {
			configBuilder.setLocalAddress(InetAddress.getByName(localAddress));
		} catch (UnknownHostException e) {
			throw new RuntimeException("Unknown local address: " + localAddress + " supplied", e);
		}
	}

	private <T> void setIfNotNull(Supplier<T> supplier, Consumer<T> consumer) {
		T value = supplier.get();
		if (value != null)
			consumer.accept(value);
	}

	// ################################################
	// ## . . . . . WebApiClientContract . . . . . . ##
	// ################################################

	@Override
	public ServiceProcessor<ServiceRequest, Object> createMdBasedWebApiClientProcessor(HttpClient client, Set<String> mdUseCases) {
		WebApiClientServiceProcessor bean = new WebApiClientServiceProcessor();
		bean.setHttpContextResolver(fixedClientContextResolver(client, mdUseCases));
		return bean;
	}

	private FixedClientContextResolver fixedClientContextResolver(HttpClient client, Set<String> mdUseCases) {
		FixedClientContextResolver bean = new FixedClientContextResolver();
		bean.setHttpClient(client);
		bean.setDomainIdToCmdResolver(this::resolveDomainIdToCmd);

		if (!CollectionTools2.isEmpty(mdUseCases))
			bean.setResolverUseCases(mdUseCases);

		return bean;
	}

	@Override
	public void registerServiceProcessors(ServiceProcessorRegistry registry) {
		WebApiRemoteProcessorConfiguration configuration = platform.configuration().readConfig(WebApiRemoteProcessorConfiguration.T).get();
		for (WebApiRemoteProcessor processorConfiguration : configuration.getProcessors()) {
			String name = processorConfiguration.getName();
			if (name == null || name.isBlank())
				throw new IllegalArgumentException("Configured WebApiRemoteProcessor requires a non-blank name.");

			registry.register(name,
					() -> createMdBasedWebApiClientProcessor(createHttpClient(processorConfiguration), processorConfiguration.getResolverUseCases()),
					() -> processorConfiguration);
		}
	}

	// ################################################
	// ## . . . . . . . . . Misc. . . . . . . . . . .##
	// ################################################

	private CmdResolver resolveDomainIdToCmd(String domainId) {
		ServiceDomain sd = serviceProcessing.serviceDomains().byId(domainId);
		// It used to be request user related in Cortex
		return sd != null ? sd.contextCmdResolver() : null;
	}

}
