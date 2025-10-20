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
package hiconic.rx.webapi.common;

import java.util.Comparator;
import java.util.function.Function;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.meta.data.MetaData;
import com.braintribe.model.meta.data.Predicate;
import com.braintribe.model.meta.data.constraint.Mandatory;
import com.braintribe.model.meta.data.prompt.Description;
import com.braintribe.model.meta.data.prompt.Name;
import com.braintribe.model.meta.data.prompt.Priority;
import com.braintribe.model.meta.data.prompt.Visible;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.EnumMdResolver;
import com.braintribe.model.processing.meta.cmd.builders.MdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;

public class MetadataUtils {

	private interface MetadataResolver<T> {
		T resolve(MdResolver<?> activeMdResolver);
	}

	public static class EntityMetadataResolver<T> {
		private final EntityMdResolver entityMdResolver;
		private final MetadataResolver<T> metadataResolver;

		public EntityMetadataResolver(EntityMdResolver entityMdResolver, MetadataResolver<T> metadataResolver) {
			super();
			this.entityMdResolver = entityMdResolver;
			this.metadataResolver = metadataResolver;
		}

		public T atEntity() {
			return metadataResolver.resolve(entityMdResolver);
		}

		public T atProperty(Property p) {
			return metadataResolver.resolve(entityMdResolver.property(p));
		}
	}

	public static class EnumMetadataResolver<T> {
		private final EnumMdResolver enumMdResolver;
		private final MetadataResolver<T> metadataResolver;

		public EnumMetadataResolver(EnumMdResolver enumMdResolver, MetadataResolver<T> metadataResolver) {
			super();
			this.enumMdResolver = enumMdResolver;
			this.metadataResolver = metadataResolver;
		}

		public T atEnum() {
			return metadataResolver.resolve(enumMdResolver);
		}

		public T atConstant(Enum<?> constant) {
			return metadataResolver.resolve(enumMdResolver.constant(constant));
		}
	}
	
	public static class ModelMetadataResolver<T> {
		private final ModelMdResolver modelMdResolver;
		private final MetadataResolver<T> metadataResolver;
		
		public ModelMetadataResolver(ModelMdResolver enumMdResolver, MetadataResolver<T> metadataResolver) {
			super();
			this.modelMdResolver = enumMdResolver;
			this.metadataResolver = metadataResolver;
		}
		
		public T atModel() {
			return metadataResolver.resolve(modelMdResolver);
		}
	}

	public static class ValueResolver<M extends MetaData, T> implements MetadataResolver<T> {

		private final EntityType<M> metadataType;
		private final T defaultValue;
		private final Function<M, T> valueExtractor;

		public ValueResolver(EntityType<M> metadataType, T defaultValue, Function<M, T> valueExtractor) {
			this.metadataType = metadataType;
			this.defaultValue = defaultValue;
			this.valueExtractor = valueExtractor;
		}

		@Override
		public T resolve(MdResolver<?> activeMdResolver) {
			M metadata = activeMdResolver.meta(metadataType).exclusive();

			if (metadata == null) {
				return defaultValue;
			}

			return valueExtractor.apply(metadata);
		}

	}

	public static class PredicateResolver implements MetadataResolver<Boolean> {
		private final EntityType<? extends Predicate> predicateType;

		public PredicateResolver(EntityType<? extends Predicate> predicateType) {
			this.predicateType = predicateType;
		}

		@Override
		public Boolean resolve(MdResolver<?> activeMdResolver) {
			return activeMdResolver.is(predicateType);
		}

	}
	
	public static ModelMetadataResolver<String> description(ModelMdResolver modelMdResolver) {
		return new ModelMetadataResolver<>(modelMdResolver, descriptionResolver());
	}

	public static EntityMetadataResolver<String> description(EntityMdResolver entityMdResolver) {
		return new EntityMetadataResolver<>(entityMdResolver, descriptionResolver());
	}

	public static EnumMetadataResolver<String> description(EnumMdResolver mdResolver) {
		return new EnumMetadataResolver<>(mdResolver, descriptionResolver());
	}

	public static ModelMetadataResolver<String> name(ModelMdResolver modelMdResolver) {
		return new ModelMetadataResolver<>(modelMdResolver, nameResolver());
	}
	
	public static EntityMetadataResolver<String> name(EntityMdResolver entityMdResolver) {
		return new EntityMetadataResolver<>(entityMdResolver, nameResolver());
	}

	public static EnumMetadataResolver<String> name(EnumMdResolver enumMdResolver) {
		return new EnumMetadataResolver<>(enumMdResolver, nameResolver());
	}

	public static EntityMetadataResolver<Double> priority(EntityMdResolver mdResolver) {
		return new EntityMetadataResolver<>(mdResolver, priorityResolver());
	}

	public static EnumMetadataResolver<Double> priority(EnumMdResolver mdResolver) {
		return new EnumMetadataResolver<>(mdResolver, priorityResolver());
	}
	
	public static ModelMetadataResolver<Boolean> isVisible(ModelMdResolver modelMdResolver) {
		return new ModelMetadataResolver<>(modelMdResolver, new PredicateResolver(Visible.T));
	}

	public static EntityMetadataResolver<Boolean> isVisible(EntityMdResolver mdResolver) {
		return new EntityMetadataResolver<>(mdResolver, new PredicateResolver(Visible.T));
	}

	public static EnumMetadataResolver<Boolean> isVisible(EnumMdResolver mdResolver) {
		return new EnumMetadataResolver<>(mdResolver, new PredicateResolver(Visible.T));
	}

	public static EntityMetadataResolver<Boolean> isMandatory(EntityMdResolver mdResolver) {
		return new EntityMetadataResolver<>(mdResolver, new PredicateResolver(Mandatory.T));
	}

	public static Comparator<Property> propertyComparator(EntityMdResolver mdResolver) {
		Comparator<Property> propertyComparator = Comparator
				// reversing sign because high numbers should be first
				.comparing((Property p) -> (-1) * priority(mdResolver).atProperty(p))
				// reversing boolean because true should be < false
				.thenComparing(p -> !isMandatory(mdResolver).atProperty(p)) //
				.thenComparing(Property::getName);
		return propertyComparator;
	}

	private static ValueResolver<Priority, Double> priorityResolver() {
		return new ValueResolver<>(Priority.T, 0d, m -> m.getPriority());
	}

	private static ValueResolver<Description, String> descriptionResolver() {
		return new ValueResolver<>(Description.T, //
				null, //
				m -> m.getDescription().value());
	}
	private static ValueResolver<Name, String> nameResolver() {
		return new ValueResolver<>(Name.T, //
				null, //
				m -> m.getName().value());
	}
}
