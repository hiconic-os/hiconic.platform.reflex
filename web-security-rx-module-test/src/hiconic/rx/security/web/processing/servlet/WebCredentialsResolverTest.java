package hiconic.rx.security.web.processing.servlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;

public class WebCredentialsResolverTest {

	@Test
	public void absentProviderFallsThrough() {
		WebCredentialsProvider absent = request -> Reasons.build(NotFound.T).text("not applicable").toMaybe();
		ExistingSessionCredentials expected = ExistingSessionCredentials.of("session");
		WebCredentialsProvider matching = request -> Maybe.complete(expected);

		Maybe<Credentials> result = WebCredentialsResolver.resolve(null, List.of(absent, matching));

		assertThat(result.isSatisfied()).isTrue();
		assertThat(result.get()).isSameAs(expected);
	}

	@Test
	public void invalidProviderStopsResolution() {
		AtomicBoolean fallbackCalled = new AtomicBoolean();
		WebCredentialsProvider invalid = request -> Reasons.build(InvalidCredentials.T).text("corrupt credential").toMaybe();
		WebCredentialsProvider fallback = request -> {
			fallbackCalled.set(true);
			return Maybe.complete(ExistingSessionCredentials.of("fallback-session"));
		};

		Maybe<Credentials> result = WebCredentialsResolver.resolve(null, List.of(invalid, fallback));

		assertThat(result.isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
		assertThat(fallbackCalled.get()).isFalse();
	}
}
