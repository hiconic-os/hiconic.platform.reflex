// ============================================================================
package hiconic.rx.check.model.api.request;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.check.model.aspect.CheckCoverage;
import hiconic.rx.check.model.aspect.CheckLatency;

@Abstract
public interface HasCheckFilters extends GenericEntity {

	EntityType<HasCheckFilters> T = EntityTypes.T(HasCheckFilters.class);
	
	Set<String> getLabel();
	void setLabel(Set<String> label);

	Set<String> getName();
	void setName(Set<String> name);

	Set<String> getNode();
	void setNode(Set<String> node);

	CheckLatency getLatency();
	void setLatency(CheckLatency latency);

	Set<CheckCoverage> getCoverage();
	void setCoverage(Set<CheckCoverage> coverage);

}
