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
