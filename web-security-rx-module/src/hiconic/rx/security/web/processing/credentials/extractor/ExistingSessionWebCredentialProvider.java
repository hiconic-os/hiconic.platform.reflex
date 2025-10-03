// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.security.web.processing.credentials.extractor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.MissingCredentials;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;

import hiconic.rx.security.web.processing.servlet.WebCredentialsProvider;
import jakarta.servlet.http.HttpServletRequest;

public interface ExistingSessionWebCredentialProvider extends WebCredentialsProvider {

	Maybe<String> findSessionId(HttpServletRequest request);

	@Override
	default Maybe<Credentials> provideCredentials(HttpServletRequest request) {
		Maybe<String> sessionIdMaybe = findSessionId(request);

		if (sessionIdMaybe.isUnsatisfied())
			return Reasons.build(MissingCredentials.T).text("No session id found in http request").cause(sessionIdMaybe.whyUnsatisfied()).toMaybe();

		ExistingSessionCredentials c = ExistingSessionCredentials.T.create();
		c.setExistingSessionId(sessionIdMaybe.get());
		c.setReuseSession(true);
		c.setIgnoreReferenceCounterIncrement(false);

		return Maybe.complete(c);
	}

}
