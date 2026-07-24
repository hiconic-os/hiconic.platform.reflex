package hiconic.rx.webapi.client.model.configuration;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
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
