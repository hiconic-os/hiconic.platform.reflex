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

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import hiconic.rx.web.server.model.config.WebServerConfiguration;

public record SslConfig(int port, SSLContext sslContext) {
	public static SslConfig buildFromConfig(WebServerConfiguration config) {
		Integer port = config.getSslPort();
		if (port == null)
			return null;
		
		String certPath = config.getSslKeyStore();
		if (certPath == null)
			return null;
		
		File certificateFile = new File(certPath);
		
		String password = config.getSslKeyStorePassword();
		
		if (password == null)
			return null;
		
		
		String keystorePath = "server.p12"; // Path to your keystore file
        String keystorePassword = "password"; // Keystore password

        // Set up the SSL context
		SSLContext sslContext;
		try {
			// Load the keystore
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			try (var keystoreStream = new FileInputStream(certificateFile)) {
			    keyStore.load(keystoreStream, password.toCharArray());
			}

			// Initialize KeyManagerFactory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keystorePassword.toCharArray());

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
		} catch (Exception e) {
			throw new RuntimeException("Error while reading ssl configuration", e);
		}
        
        return new SslConfig(port, sslContext);
	}
}
