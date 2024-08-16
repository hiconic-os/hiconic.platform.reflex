package hiconic.rx.model.service.processing.md;


import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

@Description("Maps a BinaryStreamer from the service-api using the transient 'associate' property. It can be applied to ServiceRequest entity types.")
public interface StreamWith extends EntityTypeMetaData, HasTransientAssociate {
	EntityType<StreamWith> T = EntityTypes.T(StreamWith.class);
	
	static StreamWith create(Object streamProcessor) {
		StreamWith processWith = StreamWith.T.create();
		processWith.setAssociate(streamProcessor);
		return processWith;
	}
}
