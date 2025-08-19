// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package com.braintribe.model.bapi;


import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.PlatformRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.StandardRequest;
import com.braintribe.model.user.User;

@Description("Returns basic information of the user associated to the current user session.")
public interface CurrentUserInformationRequest extends StandardRequest, PlatformRequest {
	
	EntityType<CurrentUserInformationRequest> T = EntityTypes.T(CurrentUserInformationRequest.class);
	
	@Override
	public EvalContext<? extends User> eval(Evaluator<ServiceRequest> evaluator);

}
