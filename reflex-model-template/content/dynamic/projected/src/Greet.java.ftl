<#import '/imports/context.ftl' as context>
${template.relocate(context.relocation(context.greetRequestFull))}package ${context.modelApiPackage};

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

public interface Greet extends ServiceRequest {

	EntityType<Greet> T = EntityTypes.T(Greet.class);

	String getName();
	void setName(String name);

}