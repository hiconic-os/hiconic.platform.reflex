// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.access;

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
