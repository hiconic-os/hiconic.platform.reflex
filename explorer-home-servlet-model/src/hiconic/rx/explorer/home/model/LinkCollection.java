// ============================================================================
package hiconic.rx.explorer.home.model;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LinkCollection extends Link {

	final EntityType<LinkCollection> T = EntityTypes.T(LinkCollection.class);

	void setNestedLinks(List<Link> nestedLinks);
	List<Link> getNestedLinks();

	boolean getHasErrors();
	void setHasErrors(boolean hasErrors);
}
