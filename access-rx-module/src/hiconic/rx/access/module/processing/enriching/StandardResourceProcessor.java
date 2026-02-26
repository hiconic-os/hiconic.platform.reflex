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
package hiconic.rx.access.module.processing.enriching;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface StandardResourceProcessor {

	default boolean unset(String e) {
		return e == null || e.isEmpty();
	}

	default MessageDigest createMessageDigest(String digestAlgorithm) {
		try {
			return MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(digestAlgorithm + " digest is not available", e);
		}
	}

	default String digest(MessageDigest md) {
		return convertToHex(md.digest());
	}

	default String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	default UncheckedIOException unchecked(String message, IOException e) {
		return new UncheckedIOException(message + (e.getMessage() != null ? ": " + e.getMessage() : ""), e);
	}

}
