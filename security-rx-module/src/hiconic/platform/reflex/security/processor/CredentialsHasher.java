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
// ============================================================================
package hiconic.platform.reflex.security.processor;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.exception.Exceptions;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.stream.NullOutputStream;

public class CredentialsHasher {
	private YamlMarshaller marshaller;

	public CredentialsHasher() {
		marshaller = new YamlMarshaller();
		marshaller.setWritePooled(true);
	}
	
	public String hash(Credentials credentials, Consumer<Map<String, Object>> enricher) {
		try {
			// TODO: which is the best algorithm here and is HASH good anyways?
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			Map<String, Object> credentialsEnvelope = new LinkedHashMap<>();
			credentialsEnvelope.put("credentials", credentials);
			enricher.accept(credentialsEnvelope);

			try (DigestOutputStream out = new DigestOutputStream(NullOutputStream.getInstance(), digest)) {
				marshaller.marshall(out, credentialsEnvelope);
			}

			return StringTools.toHex(digest.digest());
		} catch (Exception e) {
			throw Exceptions.unchecked(e);
		}
	}
}
