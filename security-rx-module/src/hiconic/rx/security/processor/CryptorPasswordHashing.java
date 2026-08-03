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
package hiconic.rx.security.processor;

import com.braintribe.cfg.Required;
import com.braintribe.crypto.Cryptor;
import com.braintribe.model.processing.securityservice.api.exceptions.SecurityServiceError;

import hiconic.rx.security.api.PasswordHashing;

/** Password-at-rest hashing backed by the same cryptor configuration used for authentication. */
public class CryptorPasswordHashing implements PasswordHashing {

	private Cryptor cryptor;

	@Required
	public void setCryptor(Cryptor cryptor) {
		this.cryptor = cryptor;
	}

	@Override
	public String hash(String plaintextPassword) {
		if (plaintextPassword == null)
			return null;

		try {
			return cryptor.forEncrypting().encrypt(plaintextPassword).result().asString();
		} catch (Exception e) {
			throw new SecurityServiceError("Unable to hash user password", e);
		}
	}

	@Override
	public boolean matches(String plaintextPassword, String storedPassword) {
		if (plaintextPassword == null || storedPassword == null)
			return false;

		try {
			return cryptor.is(plaintextPassword).equals(storedPassword);
		} catch (Exception e) {
			throw new SecurityServiceError("Unable to verify user password", e);
		}
	}
}
