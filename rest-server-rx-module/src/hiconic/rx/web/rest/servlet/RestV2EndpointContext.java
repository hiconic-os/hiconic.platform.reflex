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
package hiconic.rx.web.rest.servlet;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.web.ddra.endpoints.api.DdraEndpointContext;
import hiconic.rx.web.rest.servlet.handlers.RestV2Handler;
import hiconic.rx.webapi.endpoints.v2.DdraUrlPathParameters;
import hiconic.rx.webapi.endpoints.v2.RestV2Endpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestV2EndpointContext<E extends RestV2Endpoint> extends DdraEndpointContext<E> {
	
	private DdraUrlPathParameters parameters;
	
	private CrudRequestTarget target;
	
	private EntityType<?> entityType;
	
	private Property property;
	
	protected Evaluator<ServiceRequest> evaluator;
	
	private RestV2Handler<E> handler;

	public RestV2EndpointContext(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	public DdraUrlPathParameters getParameters() {
		return parameters;
	}
	public void setParameters(DdraUrlPathParameters parameters) {
		this.parameters = parameters;
	}
	
	public void setTarget(CrudRequestTarget target) {
		this.target = target;
	}
	
	public void setHandler(RestV2Handler<E> handler) {
		this.handler = handler;
	}
	
	public CrudRequestTarget getTarget() {
		return target;
	}
	
	public EntityType<?> getEntityType() {
		return entityType;
	}
	
	public void setEntityType(EntityType<?> entityType) {
		this.entityType = entityType;
	}
	
	public void setProperty(Property property) {
		this.property = property;
	}
	
	public Property getProperty() {
		return property;
	}
	
	public RestV2Handler<E> getHandler() {
		return handler;
	}
	
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}
	
	public Evaluator<ServiceRequest> getEvaluator() {
		return this.evaluator;
	}
}
