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
package hiconic.platform.reflex.web_server.processing;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import hiconic.rx.module.api.wire.RxConfigurationContract;
import hiconic.rx.web.server.model.config.WebServerConfiguration;

public record SslConfig(int port, SSLContext sslContext) {

	public static SslConfig buildFromConfig(WebServerConfiguration config, RxConfigurationContract configuration) {
		Integer port = config.getSslPort();
		if (port == null)
			return null;

		String certPath = config.getSslKeyStore();
		if (certPath == null)
			return null;
		
		String password = config.getSslKeyStorePassword();
		if (password == null)
			return null;

        // Set up the SSL context
		SSLContext sslContext;
		try {
			// Load the keystore
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			try (var keystoreStream = openKeyStore(certPath, configuration)) {
			    keyStore.load(keystoreStream, password.toCharArray());
			}

			// Initialize KeyManagerFactory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password.toCharArray());

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

		} catch (Exception e) {
			throw new RuntimeException("Error while reading ssl configuration", e);
		}
        
        return new SslConfig(port, sslContext);
	}

	private static InputStream openKeyStore(String path, RxConfigurationContract configuration) throws Exception {
		if (!path.startsWith("classpath:"))
			return new FileInputStream(path);

		String resourcePath = path.substring("classpath:".length());
		while (resourcePath.startsWith("/"))
			resourcePath = resourcePath.substring(1);

		return configuration.indexedClasspathResource(resourcePath).asStream();
	}
}
