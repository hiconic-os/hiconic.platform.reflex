package hiconic.platform.reflex.explorer.processing.access;

import java.util.function.Predicate;

import com.braintribe.common.lcd.EmptyReadWriteLock;
import com.braintribe.model.access.ModelAccessException;
import com.braintribe.model.access.smood.api.ManipulationStorage;
import com.braintribe.model.access.smood.basic.SmoodAccess;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.manipulation.Manipulation;

/**
 * @author peter.gazdik
 */
public class ReadOnlySmoodAccess extends SmoodAccess {

	public ReadOnlySmoodAccess() {
		setReadWriteLock(EmptyReadWriteLock.INSTANCE);
	}

	@Override
	public void setManipulationBuffer(ManipulationStorage manipulationBuffer) {
		throw new UnsupportedOperationException("Method 'ReadOnlySmoodAccess.setManipulationBuffer' is not supported!");
	}

	@Override
	public void setManipulationBufferFilter(Predicate<Manipulation> manipulationBufferFilter) {
		throw new UnsupportedOperationException("Method 'ReadOnlySmoodAccess.setManipulationBufferFilter' is not supported!");
	}
	
	@Override
	public ManipulationResponse applyManipulation(ManipulationRequest manipulationRequest) throws ModelAccessException {
		throw new UnsupportedOperationException("Method 'ReadOnlySmoodAccess.applyManipulation' is not supported!");
	}

}
