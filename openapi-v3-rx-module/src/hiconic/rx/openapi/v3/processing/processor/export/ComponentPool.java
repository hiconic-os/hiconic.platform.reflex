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
package hiconic.rx.openapi.v3.processing.processor.export;

import java.util.Map;

import com.braintribe.model.generic.reflection.CustomType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.openapi.v3_0.JsonReferencable;
import com.braintribe.model.openapi.v3_0.OpenapiComponents;
import com.braintribe.model.openapi.v3_0.OpenapiParameter;
import com.braintribe.model.openapi.v3_0.OpenapiRequestBody;
import com.braintribe.model.openapi.v3_0.OpenapiResponse;
import com.braintribe.model.openapi.v3_0.OpenapiSchema;

import hiconic.rx.openapi.v3.processing.processor.export.attributes.FlatSchemaAttribute;
import hiconic.rx.openapi.v3.processing.processor.reference.InliningReferenceBuilder;
import hiconic.rx.openapi.v3.processing.processor.reference.JsonReferenceBuilder;
import hiconic.rx.openapi.v3.processing.processor.reference.NonOptimizingReferenceBuilder;
import hiconic.rx.openapi.v3.processing.processor.reference.ReferenceRecycler;
import hiconic.rx.openapi.v3.processing.processor.reference.ReferenceRecyclingContext;

/**
 * Definition and access point to the {@link JsonReferenceBuilder}s for OpenApi components.
 * 
 * @author Neidhart.Orlich
 */
public class ComponentPool {

	private final OpenapiComponents components;
	private final ReferenceRecyclingContext<OpenapiContext> recyclingContext;

	public ComponentPool(OpenapiComponents components, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
		this.recyclingContext = recyclingContext;
		this.components = components;
	}
	
	public JsonReferenceBuilder<OpenapiParameter, OpenapiContext> parameter(EntityType<?> entityType, Property property, String prefix) {
		return prefix == null ?
				new ParameterReference(components, entityType, property, recyclingContext) :
				new ParameterReference(components, entityType, property, prefix, recyclingContext);
	}

	public JsonReferenceBuilder<OpenapiParameter, OpenapiContext> parameter(String refKey) {
		return new ParameterReference(components, refKey, recyclingContext);
	}

	public JsonReferenceBuilder<OpenapiResponse, OpenapiContext> response(GenericModelType type, String status) {
		return new ResponseReference(components, type, status, recyclingContext);
	}

	public JsonReferenceBuilder<OpenapiSchema, OpenapiContext> schema(EntityType<?> entityType, Property property) {
		boolean inlineSchema = recyclingContext.publicApiContext().getAttributes().findOrDefault(FlatSchemaAttribute.class, false);
		if (inlineSchema) {
			return new InliningReferenceBuilder<>("Schema of property " + property.getName() + " of entity type " + entityType.getTypeSignature(), recyclingContext);
		}
		return new SchemaReference(components, entityType, property, recyclingContext);
	}

	public JsonReferenceBuilder<OpenapiSchema, OpenapiContext> schema(CustomType type) {
		boolean inlineSchema = recyclingContext.publicApiContext().getAttributes().findOrDefault(FlatSchemaAttribute.class, false);
		if (inlineSchema) {
			return new InliningReferenceBuilder<>("Schema of type " + type.getTypeSignature(), recyclingContext);
		}
		return new SchemaReference(components, type, recyclingContext);
	}

	public JsonReferenceBuilder<OpenapiRequestBody, OpenapiContext> requestBody(GenericModelType type) {
		return new RequestBodyReference(components, type, recyclingContext);
	}

	private abstract static class TypeAwareReferenceBuilder<T extends JsonReferencable> extends ReferenceRecycler<T, OpenapiContext> {
		protected GenericModelType respectiveType; // type that should be present in model of context to be valid

		public TypeAwareReferenceBuilder(EntityType<T> type, Map<String, T> components, String componentCategory, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(type, components, "#/components/" + componentCategory, recyclingContext);
		}

		@Override
		protected boolean isValidInContext(ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			return respectiveType == null || recyclingContext.publicApiContext().supportsType(respectiveType);
		}
		
		@Override
		protected String getContextAwareRefKey(ReferenceRecyclingContext<OpenapiContext> context) {
			return overrideRefKey(context, _overrideRefKey(), super.getContextAwareRefKey(context));
		}

		protected abstract String _overrideRefKey();
		
		protected String getShortName() {
			if (respectiveType instanceof CustomType) {
				return ((CustomType) respectiveType).getShortName();
			}
			
			return null;
		}
	}
	
	private static String overrideRefKey(ReferenceRecyclingContext<OpenapiContext> context, String overridingRefKey, String originalRefKey) {
		ComponentScope componentScope = context.publicApiContext().getComponentScope();
		ReferenceRecyclingContext<OpenapiContext> parentContext = context.getParentContext();
		
		if (parentContext != null && !parentContext.isSealed() && parentContext.publicApiContext().getComponentScope() != componentScope)
			return originalRefKey;
		
		if (overridingRefKey == null)
			return originalRefKey;
		
		String fullRefKey = componentScope.getFullRefKey(overridingRefKey);
		
		if (fullRefKey != null && !originalRefKey.equals(fullRefKey))
			return originalRefKey;
		
		componentScope.registerShortRefKey(overridingRefKey, originalRefKey);
		
		return overridingRefKey;
	}

	private static class SchemaReference extends TypeAwareReferenceBuilder<OpenapiSchema> {

		private String suffix;

		public SchemaReference(OpenapiComponents components, GenericModelType type, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiSchema.T, components.getSchemas(), "schemas", recyclingContext);

			super.respectiveType = type;
		}

		public SchemaReference(OpenapiComponents components, EntityType<?> entityType, Property property, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiSchema.T, components.getSchemas(), "schemas", recyclingContext);

			super.respectiveType = entityType;
			this.suffix = "." + property.getName();
		}

		@Override
		public String getRefKey() {
			return withSuffix(respectiveType.getTypeSignature());
		}
		
		private String withSuffix(String string) {
			return suffix == null || string == null ? string : string + suffix;
		}
		
		@Override
		protected String _overrideRefKey() {
			return withSuffix(getShortName());
		}
	}

	private static class ResponseReference extends TypeAwareReferenceBuilder<OpenapiResponse> {

		private final String typeSignature;
		private final String status;

		public ResponseReference(OpenapiComponents components, GenericModelType type, String status, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			this(components, type.getTypeSignature(), status, recyclingContext);
			respectiveType = type;
		}

		public ResponseReference(OpenapiComponents components, String typeSignature, String status, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiResponse.T, components.getResponses(), "responses", recyclingContext);
			this.typeSignature = typeSignature;
			this.status = status;
		}

		@Override
		public String getRefKey() {
			String refKey = typeSignature;
			
			if (!"200".equals(status)) {
				refKey += "-" + status;
			}
			
			return refKey;
		}
		
		@Override
		protected String _overrideRefKey() {
			return getShortName();
		}
	}

	// References to OpenapiParameters don't need to be optimized because they are never complex 
	private static class ParameterReference extends NonOptimizingReferenceBuilder<OpenapiParameter, OpenapiContext> {

		private static final String PREFIX_SEPARATOR = "-";
		
		private final String prefix;
		private final Property property;
		private final EntityType<?> respectiveType;
		private final ReferenceRecyclingContext<OpenapiContext> context;

		public ParameterReference(OpenapiComponents components, String refKey, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiParameter.T, components.getParameters(), "#/components/parameters", recyclingContext);
			this.prefix = refKey;
			this.context = recyclingContext;
			this.property = null;
			this.respectiveType = null;
		}

		public ParameterReference(OpenapiComponents components, EntityType<?> entityType, Property property, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiParameter.T, components.getParameters(), "#/components/parameters", recyclingContext);
			this.property = property;
			this.context = recyclingContext;
			this.prefix = null;
			this.respectiveType = entityType;
		}
		
		public ParameterReference(OpenapiComponents components, EntityType<?> entityType, Property property, String prefix, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiParameter.T, components.getParameters(), "#/components/parameters", recyclingContext);
			this.property = property;
			this.prefix = prefix;
			this.respectiveType = entityType;
			this.context = recyclingContext;
		}

		@Override
		public String getRefKey() {
			String fullRefKey = getFullRefKey() + context.getKeySuffix();
			String actualRefKey = overrideRefKey(context, getShortRefKey(), fullRefKey);
			return actualRefKey;
		}
		
		public String getFullRefKey() {
			if (prefix == null)
				return fullPropertyName();
			
			if (property == null)
				return prefix;
			
			return prefix + PREFIX_SEPARATOR + fullPropertyName();
		}
		
		private String fullPropertyName() {
			return respectiveType.getTypeSignature() + "." + property.getName();
		}
		
		protected String getShortRefKey() {
			if (property == null)
				return null;
			
			if (prefix == null)
				return respectiveType.getShortName() + "." + property.getName();
			
			return prefix + "." + property.getName();
		}
	}

	private static class RequestBodyReference extends TypeAwareReferenceBuilder<OpenapiRequestBody> {

		public RequestBodyReference(OpenapiComponents components, GenericModelType type, ReferenceRecyclingContext<OpenapiContext> recyclingContext) {
			super(OpenapiRequestBody.T, components.getRequestBodies(), "requestBodies", recyclingContext);
			respectiveType = type;
		}

		@Override
		public String getRefKey() {
			return respectiveType.getTypeSignature();
		}
		
		@Override
		protected String _overrideRefKey() {
			return getShortName();
		}
	}

	
}
