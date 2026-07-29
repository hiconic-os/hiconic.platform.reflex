// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.web.server.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Pattern;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebServerConfiguration extends GenericEntity {

	EntityType<WebServerConfiguration> T = EntityTypes.T(WebServerConfiguration.class);

	String port = "port";
	String sslPort = "sslPort";
	String hostName = "hostName";
	String maxTreads = "maxThreads";
	String coreTreads = "coreThreads";
	String ioTreads = "ioThreads";
	String maxConnections = "maxConnections";
	String sslKeyStore = "sslKeyStore";
	String sslKeyStorePassword = "sslKeyStorePassword";
	String defaultEndpointsBasePath = "defaultEndpointsBasePath";
	String exposeHealthEndpointsAtDefaultEndpointsBasePath = "exposeHealthEndpointsAtDefaultEndpointsBasePath";
	String pushSseEndpointPath = "pushSseEndpointPath";
	String pushWebSocketEndpointPath = "pushWebSocketEndpointPath";

	@Initializer("'localhost'")
	String getHostName();
	void setHostName(String hostName);

	@Initializer("8080")
	int getPort();
	void setPort(int port);

	Integer getSslPort();
	void setSslPort(Integer sslPort);

	// Self-signed certificate can be generated with JDK's keytool:
	// keytool -genkeypair -alias reflex -keyalg RSA -keysize 2048 -keystore keystore-local.jks -validity 1500
	String getSslKeyStore();
	void setSslKeyStore(String sslKeyStore);

	@Confidential
	String getSslKeyStorePassword();
	void setSslKeyStorePassword(String sslKeyStorePassword);

	@Description("Number of XNIO I/O threads. These threads accept connections and perform non-blocking socket I/O; "
			+ "blocking application work is dispatched to the worker task pool.")
	Integer getIoThreads();
	void setIoThreads(Integer ioThreads);

	@Description("Maximum number of threads in Undertow's worker task pool for blocking application work.")
	Integer getMaxThreads();
	void setMaxThreads(Integer maxThreads);

	@Description("Core number of threads retained in Undertow's worker task pool for blocking application work.")
	Integer getCoreThreads();
	void setCoreThreads(Integer coreThreads);

	@Description("Maximum number of concurrently accepted connections. Once reached, Undertow suspends accepts until "
			+ "the active connection count falls below the limit.")
	Integer getMaxConnections();
	void setMaxConnections(Integer maxConnections);

	CorsConfiguration getCorsConfiguration();
	void setCorsConfiguration(CorsConfiguration corsConfiguration);

	@Description("Public URL of the server, should include protocol, hostname and potentially port. Does not include defaultEndpointsBasePath. The value must not end with '/'")
	@Pattern(".*[^\\/]$")
	String getPublicUrl();
	void setPublicUrl(String publicUrl);

	@Description("Default path under which configured endpoints (websockets, servlets, filters) appear. "
			+ "If not given those endpoints will appear at root level. The value must not end with '/'")
	@Pattern(".*[^\\/]$")
	String getDefaultEndpointsBasePath();
	void setDefaultEndpointsBasePath(String defaultEndpointsBasePath);

	@Description("If true, the canonical /livez and /readyz endpoints are additionally exposed below defaultEndpointsBasePath. "
			+ "The aliases are handled by the application-state gate before endpoint/servlet dispatch and therefore retain the canonical "
			+ "bootstrap and readiness semantics.")
	@Initializer("false")
	boolean getExposeHealthEndpointsAtDefaultEndpointsBasePath();
	void setExposeHealthEndpointsAtDefaultEndpointsBasePath(boolean exposeHealthEndpointsAtDefaultEndpointsBasePath);

	@Description("Path of the SSE transport endpoint for platform push, relative to defaultEndpointsBasePath. The value must not end with '/'.")
	@Pattern(".*[^\\/]$")
	@Initializer("'push/sse'")
	String getPushSseEndpointPath();
	void setPushSseEndpointPath(String pushSseEndpointPath);

	@Description("Path of the WebSocket transport endpoint for platform push, relative to defaultEndpointsBasePath. The value must not end with '/'.")
	@Pattern(".*[^\\/]$")
	@Initializer("'push/ws'")
	String getPushWebSocketEndpointPath();
	void setPushWebSocketEndpointPath(String pushWebSocketEndpointPath);

	@Description("Determines which of error message and stacktrace are exposed to the client in the server response:\n" + //
			"full: message and stacktrace\n" + //
			"messageOnly: message only\n" + //
			"none: neither")
	@Initializer("full")
	ExceptionExposure getExceptionExposure();
	void setExceptionExposure(ExceptionExposure exceptionExposure);

	@Description("If true, a `tracebackid` is exposed in the server response in the form of a random UUID.. "
			+ "This `tracebackId` is also appended to the error logs, as its only purpose is to associate a concrete problem with the corresponding log data.")
	@Initializer("true")
	boolean getExposeTracebackId();
	void setExposeTracebackId(boolean exposeTracebackId);

}
