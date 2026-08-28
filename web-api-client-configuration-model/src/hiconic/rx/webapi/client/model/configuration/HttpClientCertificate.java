package hiconic.rx.webapi.client.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * The identity an HTTP client presents during the TLS handshake, i.e. the client side of mutual TLS: a certificate and the private key
 * belonging to it.
 * <p>
 * The material is held as PEM text rather than as a reference to a key store file, so it can come from wherever the configuration is able to
 * carry a string - a YAML file, an environment variable or a mounted secret resolved by a property placeholder. The key store required by the
 * TLS machinery is assembled in memory, which is also why no key store password is part of this configuration.
 * <p>
 * A client certificate is always issued for one concrete counterpart server, and a {@link WebApiRemoteProcessor} is configured for one
 * {@link WebApiRemoteProcessor#getBaseUrl() base url}. Therefore a client holds <b>at most one</b>, which is why this is modeled as a single
 * valued property rather than a collection.
 */
@Name("HTTP Client Certificate")
@Description("Certificate and private key an HTTP client presents during the TLS handshake (mutual TLS).")
public interface HttpClientCertificate extends GenericEntity {

	EntityType<HttpClientCertificate> T = EntityTypes.T(HttpClientCertificate.class);

	@Mandatory
	@Name("Certificate")
	@Description("The client certificate in PEM form, i.e. including the BEGIN/END CERTIFICATE lines. "
			+ "If the issuer handed out intermediate certificates as well, the whole chain can be concatenated here, leaf certificate first.")
	String getCertificate();
	void setCertificate(String certificate);

	@Mandatory
	@Confidential
	@Name("Private Key")
	@Description("The private key belonging to the certificate, in unencrypted PKCS#8 PEM form, i.e. including the BEGIN/END PRIVATE KEY lines. "
			+ "An OpenSSL PKCS#1 key (BEGIN RSA PRIVATE KEY) or an encrypted key has to be converted first: "
			+ "openssl pkcs8 -topk8 -nocrypt -in client.key -out client-pkcs8.key")
	String getPrivateKey();
	void setPrivateKey(String privateKey);

}
