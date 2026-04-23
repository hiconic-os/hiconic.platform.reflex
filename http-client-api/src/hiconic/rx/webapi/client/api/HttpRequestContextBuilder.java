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
package hiconic.rx.webapi.client.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;

import hiconic.rx.webapi.client.api.builder.BasicRequestContextBuilder;

public interface HttpRequestContextBuilder extends HttpConstants {

	static HttpRequestContextBuilder instance(HttpClient httpClient) {
		return BasicRequestContextBuilder.instance(httpClient);
	}

	HttpRequestContextBuilder requestPath(String requestPath);

	HttpRequestContextBuilder requestMethod(String requestMethod);

	HttpRequestContextBuilder consumes(String consumes);

	HttpRequestContextBuilder produces(String produces);

	HttpRequestContextBuilder addSuccessCodes(Set<Integer> successCodes);

	HttpRequestContextBuilder addSuccessCodes(Integer... successCodes);

	HttpRequestContextBuilder addSuccessCode(Integer successCode);

	HttpRequestContextBuilder allSuccess();

	HttpRequestContextBuilder payload(Object payload);

	HttpRequestContextBuilder payloadIfEmpty(Object payload);

	HttpRequestContextBuilder payloadType(GenericModelType payloadType);

	HttpRequestContextBuilder addResponseTypes(Map<Integer, GenericModelType> responseTypes);

	HttpRequestContextBuilder addResponseType(Integer responseCode, GenericModelType responseType);

	HttpRequestContextBuilder defaultSuccessResponseType(GenericModelType responseType);

	HttpRequestContextBuilder defaultFailureResponseType(GenericModelType responseType);

	HttpRequestContextBuilder addQueryParameters(List<HttpParameter> queryParameters);

	HttpRequestContextBuilder addQueryParameter(HttpParameter queryParameter);

	HttpRequestContextBuilder addQueryParameter(String name, String value);

	HttpRequestContextBuilder addHeaderParameters(List<HttpParameter> headerParameters);

	HttpRequestContextBuilder addHeaderParameter(HttpParameter headerParameter);

	HttpRequestContextBuilder addHeaderParameter(String name, String value);

	HttpRequestContextBuilder payloadMarshallingOptions(GmSerializationOptions payloadMarshallingOptions);

	HttpRequestContextBuilder addStatusCodeInfo(int responseCode, boolean withOriginalStatusCode);

	HttpRequestContextBuilder propertyTypeInference(BiFunction<EntityType<?>, Property, GenericModelType> propertyTypeInference);

	HttpRequestContextBuilder dateFormatting(String dateFormat);

	HttpRequestContextBuilder dateFormatting(String dateFormat, String defaultZone, String defaultLocale);

	HttpRequestContextBuilder streamResourceContent(boolean streamResourceContent);

	HttpRequestContextBuilder responseBodyParameterTranslation(BiFunction<EntityType<?>, String, Property> responseBodyPropertyTranslation);

	HttpRequestContextBuilder responseHeaderParameterTranslation(BiFunction<EntityType<?>, String, Property> responseBodyPropertyTranslation);

	HttpRequestContextBuilder requestBodyParameterTranslation(Function<Property, String> requestBodyPropertyTranslation);

	HttpRequestContext build();

	HttpRequestContextBuilder streamContentResponseResourceProperty(String streamContentResponseResourceProperty);

}