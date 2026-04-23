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
package hiconic.rx.webapi.client.api.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.SimpleTypes;

import hiconic.rx.webapi.client.api.HttpParameter;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpResponse;
import hiconic.rx.webapi.client.api.HttpResponseBuilder;

public class BasicResponseBuilder implements HttpResponseBuilder {

	private Object payload;
	private boolean generic = false;
	private final List<HttpParameter> headerParameters = new ArrayList<>();
	private final HttpRequestContext context;
	
	// ***************************************************************************************************
	// Instantiation
	// ***************************************************************************************************

	private BasicResponseBuilder(HttpRequestContext context) {
		this.context = context;
	}
	
	public static HttpResponseBuilder instance(HttpRequestContext context) {
		return new BasicResponseBuilder(context);
	}
	
	// ***************************************************************************************************
	// Builder
	// ***************************************************************************************************

	@Override
	public HttpResponseBuilder payload(Object payload) {
		this.payload = payload;
		return this;
	}

	@Override
	public HttpResponseBuilder isGeneric() {
		this.generic = true;
		return this;
	}
	
	@Override
	public HttpResponseBuilder addHeaderParameters(List<HttpParameter> headerParameters) {
		this.headerParameters.addAll(headerParameters);
		return this;
	}

	@Override
	public HttpResponseBuilder addHeaderParameter(HttpParameter headerParameter) {
		this.headerParameters.add(headerParameter);
		return this;
	}

	@Override
	public HttpResponseBuilder addHeaderParameter(String name, String value) {
		this.headerParameters.add(new HttpParameter(name, value));
		return this;
	}

	@Override
	public HttpResponse build() {
		return new HttpResponse() {
			
			@Override
			public <T> T combinedResponse() {
				T payload = payload();
				if (payload instanceof GenericEntity) {
					GenericEntity entityPayload = (GenericEntity) payload;
					EntityType<GenericEntity> entityType = entityPayload.entityType();
					headerParameters().forEach(hp -> {
						Property responseProperty = context.responseHeaderParameterTranslation(entityType, hp.getName());
						if (responseProperty != null && responseProperty.getType() == SimpleTypes.TYPE_STRING) {
							responseProperty.set(entityPayload, hp.getValue());
						}
					});
				}
				return payload;
			}
			
			@Override
			public <T> T payload() {
				return (T) payload;
			}
			
			@Override
			public Class<?> payloadType() {
				return (payload != null) ? payload.getClass() : null;
			}
			
			@Override
			public boolean isGeneric() {
				return generic;
			}
			
			@Override
			public String headerValue(String name) {
				return headerParameters()
					.filter(p -> name.equals(p.getName()))
					.map(HttpParameter::getValue)
					.findFirst()
					.orElse(null);
			}
			
			@Override
			public Stream<HttpParameter> headerParameters() {
				return headerParameters.stream();
			}
		};
	}
	
	
}
