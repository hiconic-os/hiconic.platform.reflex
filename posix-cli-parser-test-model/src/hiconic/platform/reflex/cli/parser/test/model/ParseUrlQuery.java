package hiconic.platform.reflex.cli.parser.test.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ParseUrlQuery extends GenericEntity {
	EntityType<ParseUrlQuery> T = EntityTypes.T(ParseUrlQuery.class);
	
	@Alias("q")
	String getQuery();
	void setQuery(String query);
}
