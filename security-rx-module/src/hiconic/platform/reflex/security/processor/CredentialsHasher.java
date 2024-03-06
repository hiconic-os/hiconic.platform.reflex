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
