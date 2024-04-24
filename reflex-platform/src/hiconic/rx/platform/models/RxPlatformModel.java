package hiconic.rx.platform.models;

import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;

public class RxPlatformModel extends AbstractRxConfiguredModel {

	private GmMetaModel model;
	
	public RxPlatformModel(RxConfiguredModels configuredModels, GmMetaModel model) {
		super(configuredModels);
		this.model = model;
	}

	@Override
	public String modelName() {
		return model.getName();
	}

	@Override
	protected ModelOracle buildModelOracle() {
		return new BasicModelOracle(model);
	}
}
