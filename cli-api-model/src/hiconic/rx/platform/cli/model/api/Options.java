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
package hiconic.rx.platform.cli.model.api;

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * @author Christina Wilpernig, Dirk Scheffler
 */
@Description("This type models typesafe command line options for Jinni.")
@Alias("options")
public interface Options extends GenericEntity {

	EntityType<Options> T = EntityTypes.T(Options.class);

	@Description("Protocol more details and exception stacktraces in case of errors.")
	@Alias("v")
	boolean getVerbose();
	void setVerbose(boolean verbose);

	@Description("Protocol starts with an output of the Jinni version.")
	boolean getPrintVersion();
	void setPrintVersion(boolean printVersion);

	@Description("Route the response of the request to a channel defined by the given value. The response is serialized in a format defined by 'responseMimeType'. "
			+ "Supported channels: stdout, stderr, none or a filepath.")
	@Initializer("'stdout'")
	@Alias("r")
	String getResponse();
	void setResponse(String response);

	@Description("The mimetype of the serialization format for the response output. Possible values: text/yaml, application/x-yaml")
	@Initializer("'application/json'")
	@Mandatory
	String getResponseMimeType();
	void setResponseMimeType(String responseMimeType);

	@Description("Route the protocol of the request processing to a channel defined by the given value. "
			+ "The output is styled in case the channel supports ANSI escape sequences. " + "Supported channels: stdout, stderr, none or a filepath.")
	@Initializer("'stdout'")
	@Alias("p")
	String getProtocol();
	void setProtocol(String protocol);

	@Description("Activates the ansi escaped sequence styled output of the protocol.")
	@Initializer("true")
	boolean getColored();
	void setColored(boolean ansi);

	@Description("The charset being used to write the protocol")
	String getProtocolCharset();
	void setProtocolCharset(String protocolCharset);

	@Description("Protocol the executing command including all its defined properties in yaml format.")
	@Alias("e")
	boolean getEchoCommand();
	void setEchoCommand(boolean echoCommand);

	@Description("Configures environment variables for the Virtual Environment which overrides System.env variables.")
	@Alias("env")
	Map<String, String> getEnvironmentVariables();
	void setEnvironmentVariables(Map<String, String> environmentVariables);

}
