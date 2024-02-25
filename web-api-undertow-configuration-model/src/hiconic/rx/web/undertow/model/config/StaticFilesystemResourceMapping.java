package hiconic.rx.web.undertow.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface StaticFilesystemResourceMapping extends GenericEntity {
	EntityType<StaticFilesystemResourceMapping> T = EntityTypes.T(StaticFilesystemResourceMapping.class);
	
	String path = "path";
	String rootDir = "rootDir";
	
	String getPath();
	void setPath(String path);
	
	String getRootDir();
	void setRootDir(String rootDir);
}
