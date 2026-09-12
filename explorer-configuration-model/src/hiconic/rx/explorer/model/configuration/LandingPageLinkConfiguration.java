package hiconic.rx.explorer.model.configuration;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** A configurable link shown in the Applications section of the landing page. */
public interface LandingPageLinkConfiguration extends GenericEntity {

	EntityType<LandingPageLinkConfiguration> T = EntityTypes.T(LandingPageLinkConfiguration.class);

	String getDisplayName();
	void setDisplayName(String displayName);

	String getUrl();
	void setUrl(String url);

	String getDescription();
	void setDescription(String description);

	/**
	 * Optional roles controlling discoverability. The target itself remains responsible for
	 * enforcing authorization.
	 */
	Set<String> getRequiredRoles();
	void setRequiredRoles(Set<String> requiredRoles);
}
