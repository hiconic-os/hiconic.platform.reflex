package hiconic.rx.model.service.processing.md;


import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

@Description("Maps a ServiceProcessor from the service-api using the transient 'associate' property. It can be applied to ServiceRequest entity types.")
public interface ProcessWith extends EntityTypeMetaData, HasTransientAssociate {
	EntityType<ProcessWith> T = EntityTypes.T(ProcessWith.class);
	
	static ProcessWith create(Object serviceProcessor) {
		ProcessWith processWith = ProcessWith.T.create();
		processWith.setAssociate(serviceProcessor);
		return processWith;
	}
}
