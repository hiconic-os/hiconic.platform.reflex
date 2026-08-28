package hiconic.rx.webapi.client.model.configuration;

import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.logging.LogLevel;

/** Configuration of a remote service processor backed by a metadata-driven Web API client. */
public interface WebApiRemoteProcessor extends GenericEntity {

	EntityType<WebApiRemoteProcessor> T = EntityTypes.T(WebApiRemoteProcessor.class);

	/** Name under which the processor is exposed in the platform service processor registry. */
	String getName();
	void setName(String name);

	String getBaseUrl();
	void setBaseUrl(String baseUrl);

	/** Metadata use-cases applied when mapping requests handled by this remote processor. */
	Set<String> getResolverUseCases();
	void setResolverUseCases(Set<String> resolverUseCases);

	HttpCredentials getCredentials();
	void setCredentials(HttpCredentials credentials);

	@Name("Client Certificate")
	@Description("Certificate and private key presented during the TLS handshake, i.e. the client side of mutual TLS. "
			+ "If not set, the client does not authenticate itself on the transport layer.")
	HttpClientCertificate getClientCertificate();
	void setClientCertificate(HttpClientCertificate clientCertificate);

	@Name("Verify Server Certificate")
	@Description("Whether the certificate chain presented by the server is validated against the trust store of the JVM. "
			+ "If unset, the module's legacy TLS behaviour is preserved. A client certificate requires this property to be true.")
	Boolean getVerifyServerCertificate();
	void setVerifyServerCertificate(Boolean verifyServerCertificate);

	@Name("Verify Server Hostname")
	@Description("Whether the server certificate has to be issued for the host that was actually addressed. This is an independent check from "
			+ "the chain validation: without it, any certificate trusted by the trust store is accepted for any host, so an attacker who can "
			+ "redirect traffic and holds a certificate for a domain of their own can impersonate the counterpart. "
			+ "If unset, the module's legacy TLS behaviour is preserved. Set this explicitly for newly configured remote processors.")
	Boolean getVerifyServerHostname();
	void setVerifyServerHostname(Boolean verifyServerHostname);

	@Name("Default Headers")
	@Confidential
	@Description("Static HTTP headers sent with every request of this client, e.g. gateway credentials like API key and API secret headers. "
			+ "Headers of the individual request take precedence, i.e. a default header is only added if the request does not carry it already.")
	Map<String, String> getDefaultHeaders();
	void setDefaultHeaders(Map<String, String> defaultHeaders);

	String getProxy();
	void setProxy(String proxy);

	String getLocalAddress();
	void setLocalAddress(String address);

	String getCookieSpec();
	void setCookieSpec(String cookieSpec);

	Integer getConnectTimeout();
	void setConnectTimeout(Integer connectTimeout);

	Integer getConnectionRequestTimeout();
	void setConnectionRequestTimeout(Integer connectionRequestTimeout);

	Integer getSocketTimeout();
	void setSocketTimeout(Integer socketTimeout);

	@Initializer("50")
	Integer getMaxRedirects();
	void setMaxRedirects(Integer maxRedirects);

	@Initializer("true")
	boolean getAuthenticationEnabled();
	void setAuthenticationEnabled(boolean authenticationEnabled);

	@Initializer("true")
	boolean getRedirectsEnabled();
	void setRedirectsEnabled(boolean redirectsEnabled);

	@Initializer("true")
	boolean getRelativeRedirectsAllowed();
	void setRelativeRedirectsAllowed(boolean relativeRedirectsAllowed);

	@Initializer("true")
	boolean getContentCompressionEnabled();
	void setContentCompressionEnabled(boolean contentCompressionEnabled);

	@Name("Request Logging")
	@Description("Dynamic LogLevel for request logging")
	LogLevel getRequestLogging();
	void setRequestLogging(LogLevel requestLogging);

	@Name("Response Logging")
	@Description("Dynamic LogLevel for response logging")
	LogLevel getResponseLogging();
	void setResponseLogging(LogLevel responseLogging);
}
