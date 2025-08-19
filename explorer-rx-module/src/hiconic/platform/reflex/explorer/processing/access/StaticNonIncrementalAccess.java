package hiconic.platform.reflex.explorer.processing.access;

import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.model.access.ModelAccessException;
import com.braintribe.model.access.NonIncrementalAccess;
import com.braintribe.model.generic.reflection.GenericModelException;
import com.braintribe.model.meta.GmMetaModel;

/**
 * @author peter.gazdik
 */
public class StaticNonIncrementalAccess implements NonIncrementalAccess {

	private Supplier<?> dataSupplier;
	private Supplier<GmMetaModel> modelSupplier;

	@Required
	public void setDataSupplier(Supplier<?> dataSupplier) {
		this.dataSupplier = dataSupplier;
	}
	
	@Required
	public void setModelSupplier(Supplier<GmMetaModel> modelSupplier) {
		this.modelSupplier = modelSupplier;
	}

	@Override
	public Object loadModel() throws ModelAccessException {
		return dataSupplier.get();
	}

	@Override
	public GmMetaModel getMetaModel() throws GenericModelException {
		return modelSupplier.get();
	}

	@Override
	public void storeModel(Object model) throws ModelAccessException {
		throw new UnsupportedOperationException("Method 'StaticNonIncrementalAccess.storeModel' is not supported!");
	}

}
