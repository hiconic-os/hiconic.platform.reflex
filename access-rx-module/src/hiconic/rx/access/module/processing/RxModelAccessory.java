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
package hiconic.rx.access.module.processing;

import com.braintribe.model.generic.reflection.ConfigurableCloningContext;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.session.api.managed.ManagedGmSession;
import com.braintribe.model.processing.session.api.managed.ModelAccessory;
import com.braintribe.model.processing.session.impl.managed.BasicManagedGmSession;
import com.braintribe.utils.lcd.LazyInitialized;

public class RxModelAccessory implements ModelAccessory {
	private CmdResolver cmdResolver;
	private LazyInitialized<ManagedGmSession> lazySession = new LazyInitialized<ManagedGmSession>(this::newSession);
	
	public RxModelAccessory(CmdResolver cmdResolver) {
		this.cmdResolver = cmdResolver;
	}

	@Override
	public CmdResolver getCmdResolver() {
		return cmdResolver;
	}
	
	@Override
	public GmMetaModel getModel() {
		return cmdResolver.getModelOracle().getGmMetaModel();
	}
	
	@Override
	public ManagedGmSession getModelSession() {
		return lazySession.get();
	}
	
	private ManagedGmSession newSession() {
		BasicManagedGmSession session = new BasicManagedGmSession();
		GmMetaModel model = getModel();


		session.setMetaModel(GmMetaModel.T.getModel().getMetaModel());
		
		ConfigurableCloningContext cloningContext = ConfigurableCloningContext.build().supplyRawCloneWith(session).done();
		model.clone(cloningContext);

		session.setModelAccessory(this);
		
		return session;
	}
	
	@Override
	public ModelOracle getOracle() {
		return cmdResolver.getModelOracle();
	}

}
