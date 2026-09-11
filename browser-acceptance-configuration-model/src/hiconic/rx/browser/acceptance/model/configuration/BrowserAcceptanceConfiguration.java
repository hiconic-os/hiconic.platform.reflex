package hiconic.rx.browser.acceptance.model.configuration;

import java.util.List;
import java.util.Set;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface BrowserAcceptanceConfiguration extends GenericEntity {
	EntityType<BrowserAcceptanceConfiguration> T=EntityTypes.T(BrowserAcceptanceConfiguration.class);
	@Initializer("false") boolean getEnabled(); void setEnabled(boolean value);
	@Initializer("'user-sessions'") String getDataSource(); void setDataSource(String value);
	@Initializer("'__Host-hiconic-browser-context'") String getCookieName(); void setCookieName(String value);
	@Initializer("2592000") int getCookieMaxAgeSeconds(); void setCookieMaxAgeSeconds(int value);
	@Initializer("86400") int getPendingLifetimeSeconds(); void setPendingLifetimeSeconds(int value);
	@Initializer("7776000") int getAcceptanceLifetimeSeconds(); void setAcceptanceLifetimeSeconds(int value);
	@Initializer("'BA_'") String getTableNamePrefix(); void setTableNamePrefix(String value);
	@Initializer("{'browser-acceptance-approver'}") Set<String> getApproverRoles(); void setApproverRoles(Set<String> value);
	List<BrowserAcceptancePolicy> getPolicies(); void setPolicies(List<BrowserAcceptancePolicy> value);
}
