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

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.accessapi.CustomPersistenceRequest;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.PersistenceRequest;
import com.braintribe.model.accessapi.QueryAndSelect;
import com.braintribe.model.accessapi.QueryEntities;
import com.braintribe.model.accessapi.QueryProperty;
import com.braintribe.model.accessapi.ReferencesRequest;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;

public class RxPersistenceProcessor implements ReasonedServiceProcessor<PersistenceRequest, Object> {
	private AccessDomains accessDomains;
	
	@Required
	public void setAccessDomains(AccessDomains accessDomains) {
		this.accessDomains = accessDomains;
	}
	
	private Maybe<IncrementalAccess> getDelegate(String domainId) {
		AccessDomain accessDomain = accessDomains.byId(domainId);
		
		if (accessDomain != null)
			return Maybe.complete(accessDomain.incrementalAccess());
		
		return Reasons.build(InvalidArgument.T).text("No such access: " + domainId).toMaybe();
	}
	
	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, PersistenceRequest request) {
		Maybe<IncrementalAccess> accessMaybe = getDelegate(context.getDomainId());
		
		if (accessMaybe.isUnsatisfied())
			return accessMaybe;
		
		IncrementalAccess access = accessMaybe.get();

		final Object response;
		
		try {
			switch (request.persistenceRequestType()) {
			case Custom: response = access.processCustomRequest(context, (CustomPersistenceRequest)request); break;
			case GetModel: response = access.getMetaModel(); break;
			case GetPartitions: response = access.getPartitions(); break;
			case ManipulationRequest: response = access.applyManipulation((ManipulationRequest)request); break;
			case QueryAndSelect: response = access.query(((QueryAndSelect)request).getQuery()); break; 
			case QueryEntities: response = access.queryEntities(((QueryEntities)request).getQuery()); break;
			case QueryProperty: response = access.queryProperty(((QueryProperty)request).getQuery()); break;
			case ReferencesRequest: response = access.getReferences((ReferencesRequest)request); break;
			default:
				return Reasons.build(UnsupportedOperation.T) //
						.text("Unsupported request type: " + request.entityType().getTypeSignature()) //
						.toMaybe();
			}
		}
		catch (UnsatisfiedMaybeTunneling t) {
			return t.getMaybe();
		}

		return Maybe.complete(response);
	}
}
