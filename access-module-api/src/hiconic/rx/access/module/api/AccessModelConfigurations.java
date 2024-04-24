package hiconic.rx.access.module.api;

import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelReference;

public interface AccessModelConfigurations {
	
	AccessModelConfiguration byName(String modeName);
	
	AccessModelConfiguration byReference(ModelReference modelReference);
	
	AccessModelConfiguration byConfiguration(ModelConfiguration modelConfiguration);
	
}
