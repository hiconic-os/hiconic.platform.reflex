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
	String sslKeyStore = "sslKeyStore";
	String sslKeyStorePassword = "sslKeyStorePassword";
	String defaultEndpointsBasePath = "defaultEndpointsBasePath";

	@Initializer("'localhost'")
	String getHostName();
	void setHostName(String hostName);

	@Initializer("8080")
	int getPort();
	void setPort(int port);

	Integer getSslPort();
	void setSslPort(Integer sslPort);

	String getSslKeyStore();
	void setSslKeyStore(String sslKeyStore);

	@Confidential
	String getSslKeyStorePassword();
	void setSslKeyStorePassword(String sslKeyStorePassword);

	CorsConfiguration getCorsConfiguration();
	void setCorsConfiguration(CorsConfiguration corsConfiguration);

	@Description("Public URL of the server. Does not include defaultEndpointsBasePath. The value must not end with '/'")
	@Pattern(".*[^\\/]$")
	String getPublicUrl();
	void setPublicUrl(String publicUrl);

	@Description("Default path under which configured endpoints (websockets, servlets, filters) appear. "
			+ "If not given those endpoints will appear at root level. The value must not end with '/'")
	@Pattern(".*[^\\/]$")
	String getDefaultEndpointsBasePath();
	void setDefaultEndpointsBasePath(String defaultEndpointsBasePath);

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
