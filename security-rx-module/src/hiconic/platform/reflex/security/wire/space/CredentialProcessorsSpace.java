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
package hiconic.platform.reflex.security.wire.space;

import java.util.Set;

import com.braintribe.gm._UserModel_;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._SecurityRxModule_;
import hiconic.platform.reflex.auth.processor.ExistingSessionCredentialsAuthenticationServiceProcessor;
import hiconic.platform.reflex.auth.processor.GrantedCredentialsAuthenticationServiceProcessor;
import hiconic.platform.reflex.auth.processor.TrustedCredentialsAuthenticationServiceProcessor;
import hiconic.platform.reflex.auth.processor.UserPasswordCredentialsAuthenticationServiceProcessor;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class CredentialProcessorsSpace implements RxModuleContract {
	@Import
	private UserServicesSpace userServices;
	
	@Import
	private CryptoSpace crypto;
	
	@Managed
	public ExistingSessionCredentialsAuthenticationServiceProcessor existingSession() {
		ExistingSessionCredentialsAuthenticationServiceProcessor bean = new ExistingSessionCredentialsAuthenticationServiceProcessor();
		bean.setUserService(userServices.standardUserService());
		return bean;
	}
	
	@Managed
	public GrantedCredentialsAuthenticationServiceProcessor granted() {
		GrantedCredentialsAuthenticationServiceProcessor bean = new GrantedCredentialsAuthenticationServiceProcessor();
		bean.setUserService(userServices.standardUserService());
		bean.setGrantingRoles(Set.of("rx-admin", "rx-locksmith", "rx-internal"));
		bean.setUnsupportedGrantingCredentialsTypes(null);
		return bean;
	}
	
	@Managed
	public TrustedCredentialsAuthenticationServiceProcessor trusted() {
		TrustedCredentialsAuthenticationServiceProcessor bean = new TrustedCredentialsAuthenticationServiceProcessor();
		bean.setUserService(userServices.standardUserService());
		return bean;
	}
	
	@Managed
	public UserPasswordCredentialsAuthenticationServiceProcessor userPassword() {
		UserPasswordCredentialsAuthenticationServiceProcessor bean = new UserPasswordCredentialsAuthenticationServiceProcessor();
		bean.setUserService(userServices.standardUserService());
		bean.setCryptorProvider(crypto.cryptorProvider());
		bean.setUserModelCmdResolver(cmdResolver());
		bean.setDecryptSecret("c36e99ec-e108-11e8-9f32-f2801f1b9fd1");
		return bean;
	}
	
	@Managed
	public CmdResolver cmdResolver() {
		CmdResolver bean = CmdResolverImpl.create(modelOracle()).done();
		return bean;
	}
	
	@Managed
	public ModelOracle modelOracle() {
		ModelOracle bean = new BasicModelOracle(configuredUserModel());
		return bean;
	}

	
	@Managed 
	private GmMetaModel configuredUserModel() {
		GmMetaModel bean = ConfigurationModels.create(_SecurityRxModule_.groupId, "configured-" + _SecurityRxModule_.artifactId) //
			.addDependency(_UserModel_.reflection) //
			.get();
		
		return bean;
	}
}