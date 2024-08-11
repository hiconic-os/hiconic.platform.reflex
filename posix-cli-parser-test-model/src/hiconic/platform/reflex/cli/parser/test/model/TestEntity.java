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
package hiconic.platform.reflex.cli.parser.test.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface TestEntity extends GenericEntity {

	EntityType<TestEntity> T = EntityTypes.T(TestEntity.class);

	// primitive data types
	@Alias("b")
	boolean getPrimitiveBooleanValue();
	void setPrimitiveBooleanValue(boolean booleanValue);

	@Alias("i")
	int getIntValue();
	void setIntValue(int intValue);

	@Alias("l")
	long getPrimitiveLongValue();
	void setPrimitiveLongValue(long longValue);

	@Alias("f")
	float getPrimitiveFloatValue();
	void setPrimitiveFloatValue(float floatValue);

	@Alias("d")
	double getPrimitiveDoubleValue();
	void setPrimitiveDoubleValue(double doubleValue);
	
	// numbers
	@Alias("D")
	BigDecimal getDecimalValue();
	void setDecimalValue(BigDecimal decimalValue);

	// date
	@Alias("t")
	Date getDateValue();
	void setDateValue(Date stringValue);
	
	@Alias("e")
	TestEnum getEnumValue();
	void setEnumValue(TestEnum enumValue);

	// object types
	Object getObjectValue();
	void setObjectValue(Object objectValue);

	Boolean getBooleanValue();
	void setBooleanValue(Boolean booleanValue);

	Integer getIntegerValue();
	void setIntegerValue(Integer intValue);

	Long getLongValue();
	void setLongValue(Long longValue);

	Float getFloatValue();
	void setFloatValue(Float floatValue);

	Double getDoubleValue();
	void setDoubleValue(Double doubleValue);

	String getStringValue();
	void setStringValue(String stringValue);

	// maps
	Map<String, String> getStringMap();
	void setStringMap(Map<String, String> stringMap);

	Map<String, Integer> getStringIntegerMap();
	void setStringIntegerMap(Map<String, Integer> stringIntegerMap);

	Map<String, Long> getStringLongMap();
	void setStringLongMap(Map<String, Long> stringLongMap);

	Map<String, Double> getStringDoubleMap();
	void setStringDoubleMap(Map<String, Double> stringDoubleMap);

	Map<String, Float> getStringFloatMap();
	void setStringFloatMap(Map<String, Float> stringFloatMap);

	Map<String, Object> getStringObjectMap();
	void setStringObjectMap(Map<String, Object> stringObjectMap);

	Map<Object, String> getObjectStringMap();
	void setObjectStringMap(Map<Object, String> objectStringMap);

	Map<Object, Object> getObjectObjectMap();
	void setObjectObjectMap(Map<Object, Object> objectObjectMap);

	Map<TestEnum, String> getEnumMap();
	void setEnumMap(Map<TestEnum, String> enumMap);

	// sets
	Set<String> getStringSet();
	void setStringSet(Set<String> stringSet);

	Set<Integer> getIntegerSet();
	void setIntegerSet(Set<Integer> integerSet);

	Set<Long> getLongSet();
	void setLongSet(Set<Long> longSet);

	Set<Double> getDoubleSet();
	void setDoubleSet(Set<Double> doubleSet);

	Set<Float> getFloatSet();
	void setFloatSet(Set<Float> floatSet);

	Set<Boolean> getBooleanSet();
	void setBooleanSet(Set<Boolean> booleanSet);

	Set<Object> getObjectSet();
	void setObjectSet(Set<Object> objectSet);

	// lists
	List<String> getStringList();
	void setStringList(List<String> stringList);

	List<Integer> getIntegerList();
	void setIntegerList(List<Integer> integerList);

	List<Long> getLongList();
	void setLongList(List<Long> longList);

	List<Double> getDoubleList();
	void setDoubleList(List<Double> doubleList);

	List<Float> getFloatList();
	void setFloatList(List<Float> floatList);

	List<Boolean> getBooleanList();
	void setBooleanList(List<Boolean> booleanList);

	List<Object> getObjectList();
	void setObjectList(List<Object> objectList);


	Set<Date> getDateSet();
	void setDateSet(Set<Date> dateSet);

	List<Date> getDateList();
	void setDateList(List<Date> dateList);

	Map<String, Date> getStringDateMap();
	void setStringDateMap(Map<String, Date> stringDateMap);

	// complex object
	TestEntity getEntityValue();
	void setEntityValue(TestEntity entity);
}