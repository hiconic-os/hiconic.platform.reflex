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
package dev.hiconic.servlet.decoder.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.user.Group;
import com.braintribe.model.user.User;
import com.braintribe.testing.model.test.technical.features.AnotherComplexEntity;
import com.braintribe.testing.model.test.technical.features.CollectionEntity;
import com.braintribe.testing.model.test.technical.features.ComplexEntity;
import com.braintribe.testing.model.test.technical.features.EnumEntity;
import com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity;
import com.braintribe.testing.model.test.technical.features.PrimitiveTypesEntity;
import com.braintribe.testing.model.test.technical.features.SimpleEnum;
import com.braintribe.testing.model.test.technical.features.SimpleTypesEntity;
import com.braintribe.testing.model.test.technical.limits.ManyValuesEnum;
import com.braintribe.utils.CollectionTools;

import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;

public class QueryParsingDecoderTest {
	
	private <T extends GenericEntity> T decodeParams(EntityType<T> type, String ... params) {
		return decode(type, String.join("&", params));
	}
	
	private <T extends GenericEntity> T decode(EntityType<T> type, String queryString) {
		T entity = type.create();
		
		QueryParamDecoder decoder = new QueryParamDecoder(HttpRequestEntityDecoderOptions.defaults());
		decoder.registerTarget("testTarget", entity);
		decoder.decode(queryString);
		
		return entity;
	}

	@Test
	public void testMixed() {
		
		User user = decodeParams(User.T, //
				"firstName=John", // 
				"lastName=Doe", //
				"@g1=com.braintribe.model.user.Group",
				"groups=@g1", //
				"g1.email=emailOne", //
				"g1.localizedName=@ls1", //
				"ls1.localizedValues=@l1", //
				"l1.key=en", //
				"l1.value=english-name", //
				"groups=@g2", //
				"g2.localizedName=@ls2", //
				"ls2.localizedValues=@l1");
		
		assertThat(user).isNotNull();
		assertThat(user.getFirstName()).isEqualTo("John");
		assertThat(user.getLastName()).isEqualTo("Doe");
		assertThat(user.getGroups())
			.hasSize(2)
			.doesNotContainNull()
			.hasOnlyElementsOfTypes(Group.class)
			.extracting("localizedName.localizedValues")
			.extracting("en")
			.contains("english-name", "english-name");
	}
	
	@Test
	public void testNoParams() {
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T);
		
		assertThat(complexEntity).isNotNull();
		assertThat(complexEntity.getComplexEntityProperty()).isNull();

	}
	
	@Test
	public void testMapProperty() {
		CollectionEntity entity = decodeParams(CollectionEntity.T, //
				"integerToIntegerMap=@1",
				"integerToIntegerMap=@2",
				"integerToIntegerMap=@3",
				"1.key=1",
				"1.value=1",
				"2.key=-2",
				"2.value=2",
				"3.key=3",
				"3.value=-9");
		
		assertThat(entity.getIntegerToIntegerMap()).containsExactly( //
				entry(1,1), //
				entry(-2,2), //
				entry(3,-9));
		
		entity = decodeParams(CollectionEntity.T, //
				"simpleEntityToComplexEntityMap=@1",
				"simpleEntityToComplexEntityMap=@2",
				"simpleEntityToComplexEntityMap=@3",
				"@c2=com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity",
				"1.key=@s1",
				"1.value=@c1",
				"2.key=@s2",
				"2.value=@c2",
				"3.key=@s3",
				"3.value=@c1",
				"s1.stringProperty=s1",
				"s2.stringProperty=s2",
				"s3.stringProperty=s3",
				"c1.stringProperty=c1");
		
		assertThat(entity.getSimpleEntityToComplexEntityMap()).hasSize(3);
		
		entity.getSimpleEntityToComplexEntityMap().forEach((k,v) -> {
			String stringProperty = k.getStringProperty();
			
			if (stringProperty.equals("s1") || stringProperty.equals("s3")) {
				assertThat(v)
					.isInstanceOf(ComplexEntity.class)
					.isNotInstanceOf(ExtendedComplexEntity.class)
					.isNotNull();
				assertThat(v.getStringProperty()).isEqualTo("c1");
			} 
			else if (stringProperty.equals("s2")){
				assertThat(v).isInstanceOf(ExtendedComplexEntity.class).isNotNull();
			} else {
				fail("Unexpected map key: " + k);
			}
		});
	}
	
	@Test
	public void testLinearCollectionProperty() {
		CollectionEntity entity = decodeParams(CollectionEntity.T, //
				"stringList=one", //
				"stringList=two", //
				"stringList=three");
		
		assertThat(entity.getStringList()).containsExactly("one", "two", "three");
		
		entity = decodeParams(CollectionEntity.T, //
				"stringSet=one", //
				"stringSet=two", //
				"stringSet=three");
		
		assertThat(entity.getStringSet()).containsExactly("one", "two", "three");
		
		entity = decodeParams(CollectionEntity.T, //
				"simpleEntityList=@1", //
				"simpleEntityList=@2", //
				"simpleEntityList=@1", //
				"1.stringProperty=1", //
				"2.stringProperty=2");

		assertThat(entity.getSimpleEntityList())
			.hasSize(3)
			.extracting("stringProperty")
			.containsExactly("1", "2", "1");
		
		entity = decodeParams(CollectionEntity.T, //
				"simpleEntitySet=@1", //
				"simpleEntitySet=@2", //
				"simpleEntitySet=@1", //
				"1.stringProperty=1", //
				"2.stringProperty=2");
		
		assertThat(entity.getSimpleEntitySet())
			.hasSize(2)
			.extracting("stringProperty")
			.containsExactly("1", "2");
	}

	private void testSimpleObjectProperty(String value, Object result) {
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T, //
				"objectProperty=" + value);
		
		assertThat(complexEntity.getObjectProperty()).isEqualTo(result);
	}
	
	@Test
	public void testObjectPropertyScalar() {
		testSimpleObjectProperty("mystring", "mystring");
		testSimpleObjectProperty("'mystring'", "mystring");
		testSimpleObjectProperty("11", 11);
		testSimpleObjectProperty("'11'", "11");
		testSimpleObjectProperty("''11''", "'11'");
		testSimpleObjectProperty("11L", 11L);
		testSimpleObjectProperty("1.234D", 1.234D);
		testSimpleObjectProperty("1.234f", 1.234F);
		testSimpleObjectProperty("enum(com.braintribe.testing.model.test.technical.limits.ManyValuesEnum,VALUE_007)", ManyValuesEnum.VALUE_007);

		// There was a bug that would decode a non-quoted string that ends with the letter 'e' as the boolean value 'false' 
		testSimpleObjectProperty("aStringThatEndsWithe", "aStringThatEndsWithe");
		testSimpleObjectProperty("aStringThatEndsWithE", "aStringThatEndsWithE");

		// case insensitivity for boolean values also didn't work consistently which was fixed as well
		testSimpleObjectProperty("true", true);
		testSimpleObjectProperty("tRue", true);
		testSimpleObjectProperty("truE", true);
		testSimpleObjectProperty("TRUE", true);
		testSimpleObjectProperty("False", false);
	}
	
	@Test
	public void testObjectPropertyEntity() {
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T, //
				"@1=com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity",
				"objectProperty=@1");
		
		assertThat(complexEntity.getObjectProperty())
			.isNotNull()
			.isInstanceOf(ExtendedComplexEntity.class);

		// =====
		
		complexEntity = decodeParams(ComplexEntity.T, //
				"@1=com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity",
				"objectProperty=@1",
				"1.stringProperty=stringy");
		
		ComplexEntity object = (ComplexEntity) complexEntity.getObjectProperty();
		assertThat(object)
			.isNotNull()
			.isInstanceOf(ExtendedComplexEntity.class);
		
		assertThat(object.getStringProperty()).isEqualTo("stringy");
		
		
		// ===
		
		complexEntity = decodeParams(ComplexEntity.T, //
				"objectProperty=map[1,[0,,0,,0,,0],'3',{4,,5,,6,,7}]"
				);
		
		Map<Object, Object> map = (Map<Object, Object>) complexEntity.getObjectProperty();
		assertThat(map)
			.isNotNull()
			.hasSize(2)
			.containsOnlyKeys(1, "3");
		
		List<Integer> list = (List<Integer>) map.get(1);
		assertThat(list).hasSize(4).contains(0,0,0,0);
		
		Set<Integer> set = (Set<Integer>) map.get("3");
		assertThat(set).hasSize(4).contains(4,5,6,7);
	}
	
	@Test
	public void testSubclass() {
		// Assign empty extended entity
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T, //
				"@1=com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity",
				"complexEntityProperty=@1");
		
		assertThat(complexEntity.getComplexEntityProperty()) //
			.isInstanceOf(ExtendedComplexEntity.class) //
			.isNotNull();
		
		// Assign extended entity with string property
		complexEntity = decodeParams(ComplexEntity.T, //
				"@1=com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity", //
				"1.stringProperty=astring", //
				"complexEntityProperty=@1");
		
		assertThat(complexEntity.getComplexEntityProperty()) //
			.isInstanceOf(ExtendedComplexEntity.class) //
			.isNotNull();
		assertThat(complexEntity.getComplexEntityProperty().getStringProperty()).isEqualTo("astring");
		
		// Assign extended entity and set string property later
		complexEntity = decodeParams(ComplexEntity.T, //
				"@1=com.braintribe.testing.model.test.technical.features.ExtendedComplexEntity", //
				"complexEntityProperty=@1", //
				"1.stringProperty=astring");
		
		assertThat(complexEntity.getComplexEntityProperty()) //
			.isInstanceOf(ExtendedComplexEntity.class) //
			.isNotNull();
		assertThat(complexEntity.getComplexEntityProperty().getStringProperty()).isEqualTo("astring");
		
		// Assign entity with unnecessary (but valid) type signature specification
		complexEntity = decodeParams(ComplexEntity.T, //
				"@1=com.braintribe.testing.model.test.technical.features.ComplexEntity", //
				"complexEntityProperty=@1", //
				"1.stringProperty=astring");
		
		assertThat(complexEntity.getComplexEntityProperty()) //
			.isInstanceOf(ComplexEntity.class) //
			.isNotInstanceOf(ExtendedComplexEntity.class) //
			.isNotNull();
		assertThat(complexEntity.getComplexEntityProperty().getStringProperty()).isEqualTo("astring");
	}
	
	@Test
	public void testNestedEntityProperties() {
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T, //
				"complexEntityProperty=@1", //
				"1.stringProperty=thestring", //
				"1.complexEntityProperty=@2", //
				"2.stringProperty=otherstring", //
				"2.complexEntityProperty=@3");
		
		assertThat(complexEntity.getComplexEntityProperty()).isNotNull();
		assertThat(complexEntity.getComplexEntityProperty().getStringProperty()).isEqualTo("thestring");
		
		ComplexEntity thirdLevelEntity = complexEntity.getComplexEntityProperty().getComplexEntityProperty();
		assertThat(thirdLevelEntity).isNotNull();
		assertThat(thirdLevelEntity.getStringProperty()).isEqualTo("otherstring");
		assertThat(thirdLevelEntity.getComplexEntityProperty()).isNotNull();
		assertThat(thirdLevelEntity.getComplexEntityProperty().getComplexEntityProperty()).isNull();
	}
	
	@Test
	public void testSimpleEntityProperties() {
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T,
				"complexEntityProperty=@1",
				"1.stringProperty=thestring");
		
		assertThat(complexEntity.getComplexEntityProperty()).isNotNull();
		assertThat(complexEntity.getComplexEntityProperty().getStringProperty()).isEqualTo("thestring");
		
	}
	
	
	@Test
	public void testEnumProperties() {
		EnumEntity entity = decodeParams(EnumEntity.T, //
				"simpleEnum=ONE", //
				"manyValuesEnum=VALUE_009");
		
		assertThat(entity.getSimpleEnum()).isEqualTo(SimpleEnum.ONE);
		assertThat(entity.getManyValuesEnum()).isEqualTo(ManyValuesEnum.VALUE_009);
	}
	
	@Test
	public void testSimpleProperties() {
		SimpleTypesEntity simple = decodeParams(SimpleTypesEntity.T, // 
				"stringProperty=abc",
				"booleanProperty=true",
				"doubleProperty=1.234D",
				"floatProperty=5.678F",
				"integerProperty=5",
				"longProperty=2000000000");
		
		assertThat(simple.getStringProperty()).isEqualTo("abc");
		assertThat(simple.getBooleanProperty()).isTrue();
		assertThat(simple.getDoubleProperty()).isEqualTo(1.234d);
		assertThat(simple.getFloatProperty()).isEqualTo(5.678f);
		assertThat(simple.getIntegerProperty()).isEqualTo(5);
		assertThat(simple.getLongProperty()).isEqualTo(2000000000);
		
		PrimitiveTypesEntity primitive = decodeParams(PrimitiveTypesEntity.T, // 
				"primitiveBooleanProperty=true",
				"primitiveDoubleProperty=1.234D",
				"primitiveFloatProperty=5.678F",
				"primitiveIntegerProperty=5",
				"primitiveLongProperty=2000000000");
		
		assertThat(primitive.getPrimitiveBooleanProperty()).isTrue();
		assertThat(primitive.getPrimitiveDoubleProperty()).isEqualTo(1.234d);
		assertThat(primitive.getPrimitiveFloatProperty()).isEqualTo(5.678f);
		assertThat(primitive.getPrimitiveIntegerProperty()).isEqualTo(5);
		assertThat(primitive.getPrimitiveLongProperty()).isEqualTo(2000000000);
				
	}
	

	@Test
	public void testDotsInAliases() {
		ComplexEntity complexEntity = decodeParams(ComplexEntity.T, //
				"complexEntityProperty=@1.2.3", //
				"1.2.3.stringProperty=thestring", //
				"1.2.3.complexEntityProperty=@2", //
				"2.stringProperty=otherstring", //
				"2.complexEntityProperty=@3.0");
		
		assertThat(complexEntity.getComplexEntityProperty()).isNotNull();
		assertThat(complexEntity.getComplexEntityProperty().getStringProperty()).isEqualTo("thestring");
		
		ComplexEntity thirdLevelEntity = complexEntity.getComplexEntityProperty().getComplexEntityProperty();
		assertThat(thirdLevelEntity).isNotNull();
		assertThat(thirdLevelEntity.getStringProperty()).isEqualTo("otherstring");
		assertThat(thirdLevelEntity.getComplexEntityProperty()).isNotNull();
		assertThat(thirdLevelEntity.getComplexEntityProperty().getComplexEntityProperty()).isNull();
		
	}
	
	@Test
	public void testMultipleAssignment() {
		
		User user = decodeParams(User.T, //
				"firstName=Bla", // 
				"lastName=Blu", //
				"firstName=John", // 
				"lastName=Doe", //
				"@g1=com.braintribe.model.user.Group",
				"groups=@g1", //
				"g1.email=emailOne", //
				"g1.localizedName=@ls1", //
				"g1.localizedName=@ls1", //
				"ls1.localizedValues=@l1", //
				"l1.key=en", //
				"l1.value=english-name", //
				"groups=@g2", //
				"g2.localizedName=@ls2", //
				"ls2.localizedValues=@l1");
		
		assertThat(user).isNotNull();
		assertThat(user.getFirstName()).isEqualTo("John");
		assertThat(user.getLastName()).isEqualTo("Doe");
		assertThat(user.getGroups())
			.hasSize(2)
			.doesNotContainNull()
			.hasOnlyElementsOfTypes(Group.class)
			.extracting("localizedName.localizedValues")
			.extracting("en")
			.contains("english-name", "english-name");
	}
	
	@Test
	public void testOverwrite() {
		AnotherComplexEntity anotherComplexEntityProperty = AnotherComplexEntity.T.create(); 
		anotherComplexEntityProperty.setSimpleEnum(SimpleEnum.ONE);
		
		ComplexEntity entity = ComplexEntity.T.create();
		entity.setBooleanProperty(true);
		entity.setStringList(CollectionTools.getList("a","b","c"));
		entity.setAnotherComplexEntityProperty(anotherComplexEntityProperty);
		
		QueryParamDecoder decoder = new QueryParamDecoder(HttpRequestEntityDecoderOptions.defaults());
		decoder.registerTarget("testTarget", entity);
		decoder.registerTarget("testTarget.another", anotherComplexEntityProperty);
		decoder.decode("testTarget.another.doubleProperty=19d&stringList=d&anotherComplexEntityProperty=@another&another.simpleEnum=TWO");
		
		assertThat(anotherComplexEntityProperty).isNotEqualTo(entity.getAnotherComplexEntityProperty());
		assertThat(entity.getAnotherComplexEntityProperty().getSimpleEnum()).isEqualTo(SimpleEnum.TWO);
		assertThat(entity.getAnotherComplexEntityProperty().getDoubleProperty()).isNull();
		assertThat(entity.getStringList()).containsExactly("a","b","c","d");

		// ------------- set entity to null
		
		anotherComplexEntityProperty = AnotherComplexEntity.T.create(); 
		anotherComplexEntityProperty.setSimpleEnum(SimpleEnum.ONE);
		
		entity = ComplexEntity.T.create();
		entity.setBooleanProperty(true);
		entity.setStringList(CollectionTools.getList("a","b","c"));
		entity.setAnotherComplexEntityProperty(anotherComplexEntityProperty);
		
		decoder = new QueryParamDecoder(HttpRequestEntityDecoderOptions.defaults());
		decoder.registerTarget("testTarget", entity);
		decoder.registerTarget("testTarget.another", anotherComplexEntityProperty);
		decoder.decode("testTarget.another.doubleProperty=19d&stringList=d&anotherComplexEntityProperty=@another&another.simpleEnum=TWO&anotherComplexEntityProperty=@testTarget.another&anotherComplexEntityProperty=null");
		
		assertThat(entity.getAnotherComplexEntityProperty()).isNull();
		assertThat(anotherComplexEntityProperty.getSimpleEnum()).isEqualTo(SimpleEnum.ONE);
		assertThat(anotherComplexEntityProperty.getDoubleProperty()).isEqualTo(19d);
		assertThat(entity.getStringList()).containsExactly("a","b","c","d");
		
		// ------------- reset entity and put new values

		anotherComplexEntityProperty = AnotherComplexEntity.T.create(); 
		anotherComplexEntityProperty.setSimpleEnum(SimpleEnum.ONE);
		
		entity = ComplexEntity.T.create();
		entity.setBooleanProperty(true);
		entity.setStringList(CollectionTools.getList("a","b","c"));
		entity.setAnotherComplexEntityProperty(anotherComplexEntityProperty);
		
		decoder = new QueryParamDecoder(HttpRequestEntityDecoderOptions.defaults());
		decoder.registerTarget("testTarget", entity);
		decoder.registerTarget("testTarget.another", anotherComplexEntityProperty);
		decoder.decode("testTarget.another.doubleProperty=19d&stringList=d&anotherComplexEntityProperty=@another&another.simpleEnum=TWO&anotherComplexEntityProperty=@testTarget.another&anotherComplexEntityProperty=null&anotherComplexEntityProperty=@newAnother&newAnother.simpleEnum=THREE");
		
		assertThat(entity.getAnotherComplexEntityProperty()).isNotNull().isNotEqualTo(anotherComplexEntityProperty);
		assertThat(entity.getAnotherComplexEntityProperty().getSimpleEnum()).isEqualTo(SimpleEnum.THREE);
		assertThat(anotherComplexEntityProperty.getDoubleProperty()).isEqualTo(19d);
		assertThat(entity.getAnotherComplexEntityProperty().getDoubleProperty()).isEqualTo(null);
		assertThat(entity.getStringList()).containsExactly("a","b","c","d");
	}
	
}
