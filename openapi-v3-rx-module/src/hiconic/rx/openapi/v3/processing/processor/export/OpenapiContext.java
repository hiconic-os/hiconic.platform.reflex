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

import static hiconic.rx.openapi.v3.processing.processor.export.OpenapiMimeType.MULTIPART_FORMDATA;
import static hiconic.rx.openapi.v3.processing.processor.export.OpenapiMimeType.URLENCODED;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.AttributeContextBuilder;
import com.braintribe.model.generic.reflection.CustomType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.openapi.v3_0.OpenapiComponents;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.openapi.v3.processing.processor.export.attributes.FlatSchemaAttribute;
import hiconic.rx.openapi.v3.processing.processor.export.attributes.MimeTypeAttribute;
import hiconic.rx.openapi.v3.processing.processor.export.attributes.ReflectSubtypesAttribute;
import hiconic.rx.openapi.v3.processing.processor.export.attributes.ReflectSupertypesAttribute;
import hiconic.rx.openapi.v3.processing.processor.reference.JsonReferenceBuilder;
import hiconic.rx.openapi.v3.processing.processor.reference.ReferenceRecyclingContext;
import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMapping;

/**
 * The context to be used for {@link JsonReferenceBuilder}s for all kinds of OpenApi processors. It provides access to the {@link ComponentPool},
 * metadata resolving, model- and type information of the current {@link ComponentScope} and other things that are useful for resolving
 * {@link OpenApi} documents.
 *
 * @author Neidhart.Orlich
 */
public class OpenapiContext {

	private final ComponentPool componentPool;
	private final ReferenceRecyclingContext<OpenapiContext> recyclingContext;
	private final ComponentScope componentScope;
	private SingleDdraMapping mapping;
	private Set<String> useCases;
	private final AttributeContext attributes;

	private OpenapiContext(ReferenceRecyclingContext<OpenapiContext> recyclingContext, ComponentScope componentScope, AttributeContext attributes,
			SingleDdraMapping mapping) {
		this.recyclingContext = recyclingContext;
		this.componentScope = componentScope;
		this.attributes = attributes;
		this.componentPool = new ComponentPool(componentScope.components(), recyclingContext);
		this.mapping = mapping;
	}

	public static OpenapiContext create(String keySuffix, ComponentScope componentScope, OpenapiMimeType mimeType) {
		AttributeContext attributeContext = AttributeContexts.attributeContext() //
				.set(MimeTypeAttribute.class, mimeType) //
				.build();

		return new ReferenceRecyclingContext<OpenapiContext>(//
				null, keySuffix, //
				r -> new OpenapiContext(r, componentScope, attributeContext, null) //
		).publicApiContext();
	}

	public OpenapiContext childContext(String key) {
		return childContext(key, componentScope, attributes);
	}

	public OpenapiContext childContext(String keySuffix, AttributeContext attributes) {
		return childContext(keySuffix, componentScope, attributes);
	}

	public OpenapiContext childContext(OpenapiMimeType mime) {
		if (getMimeType() == mime) {
			return this;
		}

		return childContext(mime.toString(), componentScope, mime);
	}

	public OpenapiContext childContext(String keySuffix, ComponentScope componentScope, OpenapiMimeType mime) {
		AttributeContextBuilder attributes = getAttributes().derive().set(MimeTypeAttribute.class, mime);

		List<OpenapiMimeType> flatMimeTypes = Arrays.asList(MULTIPART_FORMDATA, URLENCODED);
		if (flatMimeTypes.contains(mime)) {
			attributes.set(FlatSchemaAttribute.class, true);
			attributes.set(ReflectSubtypesAttribute.class, false);
			attributes.set(ReflectSupertypesAttribute.class, false);
		}

		OpenapiContext mimeTypeContext = childContext(keySuffix, componentScope, attributes.build());
		mimeTypeContext.addUseCases(ComponentScope.USECASE_OPENAPI + ":" + mime.getMimeString());

		return mimeTypeContext;
	}

	public OpenapiContext childContext(String keySuffix, ComponentScope componentScope, AttributeContext childAttributeContext) {
		String fullKey = fullChildContextKeySuffix(keySuffix);
		OpenapiContext childContext = recyclingContext
				.childContext(fullKey, r -> new OpenapiContext(r, componentScope, childAttributeContext, mapping)).publicApiContext();
		return childContext;
	}

	public AttributeContext getAttributes() {
		return attributes;
	}

	private String fullChildContextKeySuffix(String key) {
		// A null key resets the suffix chain which allows for shorter suffixes but also fur duplicate suffixes if not careful.
		// Ideally, like on the time of writing, there is only one and on the base of the chain.
		return key == null || recyclingContext.getKeySuffix() == null || recyclingContext.getKeySuffix().isEmpty() //
				? key //
				: recyclingContext.getKeySuffix() + "-" + key;
	}

	public ComponentScope getComponentScope() {
		return componentScope;
	}

	public void setMapping(SingleDdraMapping mapping) {
		this.mapping = mapping;
	}

	public SingleDdraMapping getMapping() {
		return mapping;
	}

	public OpenapiMimeType getMimeType() {
		return attributes.findAttribute(MimeTypeAttribute.class).orElse(OpenapiMimeType.ALL);
	}

	public ModelMdResolver getMetaData() {
		if (useCases == null) {
			ensureInitialized();
		}

		ModelMdResolver mdResolver = componentScope.getCmdResolver().getMetaData();

		return mdResolver.useCases(useCases);
	}

	public void ensureInitialized() {
		if (useCases == null)
			setUseCases();
	}

	public void addUseCases(Set<String> useCases) {
		ensureInitialized();
		this.useCases.addAll(useCases);
	}

	public void addUseCases(String... useCases) {
		addUseCases(CollectionTools.getSet(useCases));
	}

	public void setUseCases(String... useCases) {
		this.useCases = new HashSet<>();

		if (recyclingContext.getParentContext() != null) {
			OpenapiContext parentContext = recyclingContext.getParentContext().publicApiContext();
			if (parentContext.useCases == null) {
				parentContext.ensureInitialized();
			}
			for (String u : parentContext.useCases) {
				this.useCases.add(u);
			}
		}

		for (String u : useCases) {
			this.useCases.add(u);
		}
	}

	public void setUseCasesForDdraAndOpenapi(String... useCases) {
		ensureInitialized();

		for (String u : useCases) {
			this.useCases.add(ComponentScope.USECASE_DDRA + ":" + u);
			this.useCases.add(ComponentScope.USECASE_OPENAPI + ":" + u);
		}
	}

	public EntityTypeOracle getEntityTypeOracle(EntityType<?> type) {
		return componentScope.getModelOracle().getEntityTypeOracle(type);
	}

	public ComponentPool components() {
		return componentPool;
	}

	public void transferRequestDataFrom(OpenapiContext context) {
		addUseCases(context.useCases);
	}

	public Set<String> getUseCases() {
		return useCases;
	}

	public void seal() {
		recyclingContext.seal();

		OpenapiComponents components = componentScope.components();
		for (Property p : OpenapiComponents.T.getDeclaredProperties()) {
			Map<Object, Object> unmodifiableMap = Collections.unmodifiableMap(p.get(components));
			p.set(components, unmodifiableMap);
		}
	}

	public boolean isSealed() {
		return recyclingContext.isSealed();
	}

	public boolean supportsType(GenericModelType type) {
		if (type instanceof CustomType) {
			boolean declared = getComponentScope().getModelOracle().findTypeOracle((CustomType) type) != null;
			return declared;
		}

		return true;
	}

	public String contextDescription() {
		return "'" + recyclingContext.getKeySuffix() + "' with model '" + getComponentScope().getModel().getName() + "'";
	}

	@Override
	public String toString() {
		return contextDescription();
	}

}
