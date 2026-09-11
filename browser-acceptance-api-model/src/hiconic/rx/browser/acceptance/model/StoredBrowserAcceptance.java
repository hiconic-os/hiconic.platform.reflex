package hiconic.rx.browser.acceptance.model;

import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Internal persistence form. Never expose this type from the administrative API. */
public interface StoredBrowserAcceptance extends BrowserAcceptance {
	EntityType<StoredBrowserAcceptance> T = EntityTypes.T(StoredBrowserAcceptance.class);
	@Confidential String getTokenHash();
	void setTokenHash(String tokenHash);
}
