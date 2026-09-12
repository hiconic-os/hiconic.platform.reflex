package hiconic.rx.browser.acceptance.model.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Retires a terminal device/browser acceptance while preserving its audit history. */
public interface ForgetBrowserAcceptance extends ChangeBrowserAcceptance {
	EntityType<ForgetBrowserAcceptance> T = EntityTypes.T(ForgetBrowserAcceptance.class);
}
