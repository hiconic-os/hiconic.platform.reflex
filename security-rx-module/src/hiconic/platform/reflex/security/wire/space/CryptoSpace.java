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
package hiconic.platform.reflex.security.wire.space;

import static com.braintribe.wire.api.util.Maps.entry;
import static com.braintribe.wire.api.util.Maps.map;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.braintribe.codec.Codec;
import com.braintribe.crypto.Cryptor.Encoding;
import com.braintribe.crypto.commons.Base64Codec;
import com.braintribe.crypto.commons.HexCodec;
import com.braintribe.crypto.hash.HashFunction;
import com.braintribe.model.crypto.configuration.encryption.EncryptionConfiguration;
import com.braintribe.model.crypto.configuration.hashing.HashingConfiguration;
import com.braintribe.model.crypto.key.PrivateKey;
import com.braintribe.model.crypto.key.PublicKey;
import com.braintribe.model.crypto.key.SecretKey;
import com.braintribe.model.crypto.key.encoded.EncodedKeyPair;
import com.braintribe.model.crypto.key.encoded.EncodedPrivateKey;
import com.braintribe.model.crypto.key.encoded.EncodedPublicKey;
import com.braintribe.model.crypto.key.encoded.EncodedSecretKey;
import com.braintribe.model.crypto.key.encoded.KeyEncodingStringFormat;
import com.braintribe.model.crypto.key.keystore.KeyStoreCertificate;
import com.braintribe.model.crypto.key.keystore.KeyStoreKeyPair;
import com.braintribe.model.crypto.key.keystore.KeyStoreSecretKey;
import com.braintribe.model.processing.core.expert.impl.ConfigurableGmExpertRegistry;
import com.braintribe.model.processing.crypto.factory.CryptorFactory;
import com.braintribe.model.processing.crypto.token.KeyCodecProvider;
import com.braintribe.model.processing.crypto.token.loader.EncryptionTokenLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.BasicCipherCryptorFactory;
import com.braintribe.model.processing.cryptor.basic.cipher.key.codec.PrivateKeyCodecProvider;
import com.braintribe.model.processing.cryptor.basic.cipher.key.codec.PublicKeyCodecProvider;
import com.braintribe.model.processing.cryptor.basic.cipher.key.codec.SecretKeyCodecProvider;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.EncodedKeyPairLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.EncodedPrivateKeyLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.EncodedPublicKeyLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.EncodedSecretKeyLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.KeyStoreCertificateLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.KeyStoreKeyPairLoader;
import com.braintribe.model.processing.cryptor.basic.cipher.key.loader.KeyStoreSecretKeyLoader;
import com.braintribe.model.processing.cryptor.basic.hash.BasicHasherFactory;
import com.braintribe.model.processing.cryptor.basic.provider.BasicCryptorProvider;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

@Managed
public class CryptoSpace implements WireSpace {

	@Managed
	public ConfigurableGmExpertRegistry keyLoaderExpertRegistry() {
		ConfigurableGmExpertRegistry bean = new ConfigurableGmExpertRegistry();
		bean.add(EncryptionTokenLoader.class, keyLoaderExpertMap());
		return bean;
	}
	
	public Map<Class<?>, EncryptionTokenLoader<?, ?>> keyLoaderExpertMap() {
		// @formatter:off
		Map<Class<?>, EncryptionTokenLoader<?, ?>> bean = 
			map(
				entry(EncodedKeyPair.class		, encodedKeyPairLoader()),
				entry(EncodedPublicKey.class	, encodedPublicKeyLoader()),
				entry(EncodedPrivateKey.class	, encodedPrivateKeyLoader()),
				entry(EncodedSecretKey.class	, encodedSecretKeyLoader()),
				entry(KeyStoreKeyPair.class		, keyStoreKeyPairLoader()),
				entry(KeyStoreCertificate.class	, keyStoreCertificateLoader()),
				entry(KeyStoreSecretKey.class	, keyStoreSecretKeyLoader())
			);
		return bean;
		// @formatter:on
	}

	
	@Managed
	public EncodedKeyPairLoader encodedKeyPairLoader() {
		EncodedKeyPairLoader bean = new EncodedKeyPairLoader();
		bean.setKeyStringCodecs(keyStringCodecs());
		bean.setExpertRegistry(keyCodecProviderRegistry());
		return bean;
	}

	
	@Managed
	public EncodedPublicKeyLoader encodedPublicKeyLoader() {
		EncodedPublicKeyLoader bean = new EncodedPublicKeyLoader();
		bean.setKeyStringCodecs(keyStringCodecs());
		bean.setExpertRegistry(keyCodecProviderRegistry());
		return bean;
	}

	
	@Managed
	public EncodedPrivateKeyLoader encodedPrivateKeyLoader() {
		EncodedPrivateKeyLoader bean = new EncodedPrivateKeyLoader();
		bean.setKeyStringCodecs(keyStringCodecs());
		bean.setExpertRegistry(keyCodecProviderRegistry());
		return bean;
	}

	
	@Managed
	public EncodedSecretKeyLoader encodedSecretKeyLoader() {
		EncodedSecretKeyLoader bean = new EncodedSecretKeyLoader();
		bean.setKeyStringCodecs(keyStringCodecs());
		bean.setExpertRegistry(keyCodecProviderRegistry());
		return bean;
	}

	
	@Managed
	public KeyStoreKeyPairLoader keyStoreKeyPairLoader() {
		KeyStoreKeyPairLoader bean = new KeyStoreKeyPairLoader();
		bean.setCacheKeyStores(false);
		return bean;
	}

	
	@Managed
	public KeyStoreCertificateLoader keyStoreCertificateLoader() {
		KeyStoreCertificateLoader bean = new KeyStoreCertificateLoader();
		bean.setCacheKeyStores(false);
		return bean;
	}

	
	@Managed
	public KeyStoreSecretKeyLoader keyStoreSecretKeyLoader() {
		KeyStoreSecretKeyLoader bean = new KeyStoreSecretKeyLoader();
		bean.setCacheKeyStores(false);
		return bean;
	}

	
	@Managed
	public Map<KeyEncodingStringFormat, Codec<byte[], String>> keyStringCodecs() {
		Map<KeyEncodingStringFormat, Codec<byte[], String>> bean = new HashMap<>();
		bean.put(KeyEncodingStringFormat.base64, base64Codec());
		bean.put(KeyEncodingStringFormat.hex, hexCodec());
		return bean;
	}

	/* ========== Key Codecs ========== */

	
	@Managed
	public PublicKeyCodecProvider publicKeyCodecProvider() {
		PublicKeyCodecProvider bean = new PublicKeyCodecProvider();
		return bean;
	}

	
	@Managed
	public PrivateKeyCodecProvider privateKeyCodecProvider() {
		PrivateKeyCodecProvider bean = new PrivateKeyCodecProvider();
		return bean;
	}

	
	@Managed
	public SecretKeyCodecProvider secretKeyCodecProvider() {
		SecretKeyCodecProvider bean = new SecretKeyCodecProvider();
		return bean;
	}

	
	@Managed
	public ConfigurableGmExpertRegistry keyCodecProviderRegistry() {
		// @formatter:off
		ConfigurableGmExpertRegistry bean = new ConfigurableGmExpertRegistry();
		bean.add(
				KeyCodecProvider.class, 
					map(
						entry(PublicKey.class, publicKeyCodecProvider()),
						entry(PrivateKey.class, privateKeyCodecProvider()),
						entry(SecretKey.class, secretKeyCodecProvider())
					)
				);
		return bean;
		// @formatter:on
	}

	/* ========== Cryptor Provider ========== */

	@Managed
	public BasicCryptorProvider cryptorProvider() {
		BasicCryptorProvider bean = new BasicCryptorProvider();
		bean.setExpertRegistry(cryptorFactoryRegistry());
		return bean;
	}
	
	@Managed
	private ConfigurableGmExpertRegistry cryptorFactoryRegistry() {
		ConfigurableGmExpertRegistry bean = new ConfigurableGmExpertRegistry();
		bean.add(CryptorFactory.class, EncryptionConfiguration.class, cipherCryptorFactory());
		bean.add(CryptorFactory.class, HashingConfiguration.class, hasherFactory());
		return bean;
	}

	@Managed
	private BasicCipherCryptorFactory cipherCryptorFactory() {
		BasicCipherCryptorFactory bean = new BasicCipherCryptorFactory();
		bean.setDefaultTransformations(defaultTransformations());
		bean.setExpertRegistry(keyLoaderExpertRegistry());
		bean.setStringCodecs(stringCodecs());
		return bean;
	}

	@Managed
	private Map<String, String> defaultTransformations() {
		Map<String, String> bean = new HashMap<>();
		bean.put("AES", "AES/ECB/PKCS5Padding");
		bean.put("DES", "DES/ECB/PKCS5Padding");
		bean.put("DESede", "DESede/ECB/PKCS5Padding");
		bean.put("RSA", "RSA/ECB/PKCS1Padding");
		return bean;
	}

	@Managed
	private BasicHasherFactory hasherFactory() {
		BasicHasherFactory bean = new BasicHasherFactory();
		bean.setStringCodecs(stringCodecs());
		return bean;
	}

	@Managed
	private Map<Encoding, Codec<byte[], String>> stringCodecs() {
		Map<Encoding, Codec<byte[], String>> bean = new HashMap<>();
		bean.put(Encoding.base64, base64Codec());
		bean.put(Encoding.hex, hexCodec());
		return bean;
	}


	/* ========== Commons ========== */

	
	public Base64Codec base64Codec() {
		return Base64Codec.INSTANCE;
	}

	
	@Managed
	public HexCodec hexCodec() {
		HexCodec bean = new HexCodec();
		return bean;
	}

	
	@Managed
	public Function<byte[], String> fingerprintFunction() {
		HashFunction bean = new HashFunction();
		bean.setDigestAlgorithm("SHA-256");
		bean.setSeparator(':');
		return bean;
	}

}
