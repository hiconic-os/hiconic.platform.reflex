// ============================================================================
package hiconic.platform.reflex.explorer.processing.servlet.auth.providers;

import java.util.Comparator;

import com.braintribe.cfg.Configurable;
import com.braintribe.model.security.service.config.HttpRequestSelector;
import com.braintribe.model.security.service.config.OpenUserSessionConfiguration;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import hiconic.platform.reflex.explorer.processing.servlet.auth.Constants;
import hiconic.platform.reflex.explorer.processing.servlet.auth.OpenUserSessionConfigurationProvider;
import jakarta.servlet.http.HttpServletRequest;

public class OpenUserSessionConfigurationProviderImpl implements OpenUserSessionConfigurationProvider {

	private OpenUserSessionConfiguration openUserSessionConfiguration = OpenUserSessionConfiguration.T.create();

	private final LoadingCache<HttpSelectorRecord, OpenUserSessionEntryPoint> entryPointByHttpSelector;

	record HttpSelectorRecord(String origin, String host) {
	}

	private static final OpenUserSessionEntryPoint NOT_FOUND = OpenUserSessionEntryPoint.T.create();

	public OpenUserSessionConfigurationProviderImpl(int cacheSize) {
		entryPointByHttpSelector = Caffeine.newBuilder().maximumSize(cacheSize).build(this::selectEntryPoint);
	}

	public OpenUserSessionConfigurationProviderImpl() {
		this(100);
	}

	@Configurable
	public void setOpenUserSessionConfiguration(OpenUserSessionConfiguration openUserSessionConfiguration) {
		this.openUserSessionConfiguration = openUserSessionConfiguration;
	}

	@Override
	public String findEntryPointName(HttpServletRequest request) {
		OpenUserSessionEntryPoint entryPoint = findEntryPoint(request);
		return entryPoint != null ? entryPoint.getName() : null;
	}

	@Override
	public OpenUserSessionEntryPoint findEntryPoint(HttpServletRequest request) {
		String origin = request.getHeader("Origin");

		// normalizing to standardized explicit "null" or lowercase value
		String normalizedOrigin = origin == null ? "null" : origin.toLowerCase();

		String host = request.getHeader("X-Forwarded-Host");

		if (host == null)
			host = request.getHeader("Host");

		// normalizing to standardized explicit "null" or lowercase value
		String normalizedHost = host == null ? "null" : host.toLowerCase();

		return entryPointByHttpSelector.get(new HttpSelectorRecord(normalizedOrigin, normalizedHost));
	}

	record HttpActivationMatch(OpenUserSessionEntryPoint entryPoint, int entryPos, HttpRequestSelector selector, int selectorPos,
			int selectorSpecifity) {
	}

	private static final Comparator<HttpActivationMatch> httpActivationMatchComparator = Comparator //
			.comparing(HttpActivationMatch::selectorSpecifity) //
			.thenComparing(Comparator.comparing(HttpActivationMatch::entryPos).reversed()) //
			.thenComparing(Comparator.comparing(HttpActivationMatch::selectorPos).reversed());

	private OpenUserSessionEntryPoint selectEntryPoint(HttpSelectorRecord selector) {
		HttpActivationMatch bestMatch = new HttpActivationMatch(NOT_FOUND, Integer.MAX_VALUE, null, Integer.MAX_VALUE, 0);

		int entryPos = 0;
		for (OpenUserSessionEntryPoint entry : openUserSessionConfiguration.getEntryPoints()) {
			int selectorPos = 0;

			for (HttpRequestSelector activationSelector : entry.getHttpActivations()) {
				Integer specifity = matches(selector, activationSelector);
				if (specifity != null) {
					HttpActivationMatch match = new HttpActivationMatch(entry, entryPos, activationSelector, selectorPos, specifity);

					if (httpActivationMatchComparator.compare(bestMatch, match) < 0)
						bestMatch = match;
				}
				selectorPos++;
			}

			entryPos++;
		}

		OpenUserSessionEntryPoint matchEntryPoint = bestMatch.entryPoint();

		return matchEntryPoint != NOT_FOUND ? matchEntryPoint : null;
	}

	private Integer matches(HttpSelectorRecord requestSelector, HttpRequestSelector entrySelector) {
		String o1 = requestSelector.origin();
		String o2 = entrySelector.getOrigin();
		String h1 = requestSelector.host();
		String h2 = entrySelector.getHost();
		int specifity = 0;

		if (h1.equals(h2)) {
			specifity += 2;
		} else if (!(h2 == null || h2.equals("*")))
			return null;

		if (o1.equals(o2)) {
			specifity += 1;
		} else if (!(o2 == null || o2.equals("*")))
			return null;

		return specifity;
	}

	@Override
	public String getCookieName(HttpServletRequest request) {
		OpenUserSessionEntryPoint entryPoint = findEntryPoint(request);

		if (entryPoint == null)
			return Constants.COOKIE_SESSIONID;

		return entryPoint.sessionCookieName();
	}
}
