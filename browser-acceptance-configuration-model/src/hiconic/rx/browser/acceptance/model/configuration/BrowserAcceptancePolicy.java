package hiconic.rx.browser.acceptance.model.configuration;

import java.util.Set;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Empty entryPoints means every named entry point and the absence of an entry point. Excluded roles take precedence. */
public interface BrowserAcceptancePolicy extends GenericEntity {
	EntityType<BrowserAcceptancePolicy> T=EntityTypes.T(BrowserAcceptancePolicy.class);
	Set<String> getEntryPoints(); void setEntryPoints(Set<String> value);
	Set<String> getIncludedRoles(); void setIncludedRoles(Set<String> value);
	Set<String> getExcludedRoles(); void setExcludedRoles(Set<String> value);
}
