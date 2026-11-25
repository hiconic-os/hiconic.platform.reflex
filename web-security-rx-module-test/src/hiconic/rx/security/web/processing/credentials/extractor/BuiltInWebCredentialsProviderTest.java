package hiconic.rx.security.web.processing.credentials.extractor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.model.securityservice.credentials.Credentials;

public class BuiltInWebCredentialsProviderTest {

	@Test
	public void incompleteBasicCredentialIsInvalid() {
		assertThat(new BasicAuthCredentialsProvider().provideCredentials("Basic").isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
		assertThat(new BasicAuthCredentialsProvider().provideCredentials("Basic   ").isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
	}

	@Test
	public void incompleteBearerCredentialIsInvalid() {
		assertThat(new JwtCredentialsProvider().provideCredentials("Bearer").isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
		assertThat(new JwtCredentialsProvider().provideCredentials("Bearer   ").isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
	}

	@Test
	public void sessionCredentialProviderPreservesInvalidReason() {
		ExistingSessionWebCredentialProvider provider = request -> Reasons.build(InvalidCredentials.T).text("empty session id").toMaybe();

		Maybe<Credentials> result = provider.provideCredentials(null);

		assertThat(result.isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
	}
}
