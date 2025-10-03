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
package addi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmLinearCollectionType;
import com.braintribe.model.meta.GmMapType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.GmProperty;
import com.braintribe.model.meta.GmType;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.utils.StringTools;

import hiconic.platform.reflex._DemoModel_;

public class ReflectionLab {
	
	public static void main(String[] args) {
		GmMetaModel model = GMF.getTypeReflection().getModel(_DemoModel_.name).getMetaModel();
		
		BasicModelOracle oracle = new BasicModelOracle(model);
		
//		oracle.getDependencies().includeSelf().transitive()
		
		Set<GmEntityType> types = oracle.getEntityTypeOracle(GenericEntity.T) //
			.getSubTypes() //
			.includeSelf() //
			.transitive() //
			.asGmTypes();
		
		Map<String, String> primitiveTypeMap = new HashMap<String, String>();
		primitiveTypeMap.put("object", "any");
		primitiveTypeMap.put("double", "number");
		primitiveTypeMap.put("integer", "number");
		primitiveTypeMap.put("float", "number");
		primitiveTypeMap.put("date", "Date");
		
		Map<GmMetaModel, String> importMap = new HashMap<GmMetaModel, String>();
		
		AtomicInteger counter = new AtomicInteger(0);
		
		for (GmMetaModel dep: model.getDependencies()) {
			String importName = importMap.computeIfAbsent(dep, m -> "m" + counter.getAndIncrement());
			
			String shortName = StringTools.getSubstringAfterLast(dep.getName(), ":");
			
			System.out.println("import * as " + importName + " from './" + shortName + ".js;");
		}
		
		for (GmEntityType type: model.entityTypes().toList()) {
			String shortName = getSimpleName(type.getTypeSignature());
			System.out.print("export interface " + shortName);
			
			List<GmEntityType> superTypes = type.getSuperTypes();
			
			if (!superTypes.isEmpty()) {
				System.out.print(" extends ");
				boolean first = true;
				for (GmEntityType superType: superTypes) {
					if (first)
						first = false;
					else
						System.out.print(", ");
						
					String superTypeName = normalizeTypeName(primitiveTypeMap, superType);
					
					if (superType.getDeclaringModel() != model) {
						String importName = importMap.get(superType.getDeclaringModel());
						superTypeName = importName + "." + superTypeName;
					}
					
					System.out.print(superTypeName);
				}
			}
			
			System.out.println(" {");
			
			for (GmProperty property: type.getProperties()) {
				
				GmType propertyType = property.getType();

				final String typeName;
				
				if (propertyType.isGmCollection()) {
					if (propertyType instanceof GmMapType)
						continue;
					
					GmLinearCollectionType collectionType = (GmLinearCollectionType)propertyType;
					typeName = normalizeTypeName(primitiveTypeMap, collectionType.getElementType()) + "[]";
				}
				else {
					typeName = normalizeTypeName(primitiveTypeMap, propertyType);
				}
				
				System.out.println("  " + property.getName() + " " + typeName + ";");
			}
			
			System.out.println("}");
		}
		
	}

	private static String normalizeTypeName(Map<String, String> primitiveTypeMap, GmType type) {
		final String typeName;
		String pType = getSimpleName(type.getTypeSignature());
		typeName = primitiveTypeMap.getOrDefault(pType, pType);
		return typeName;
	}
	
	private static String getSimpleName(String typeName) {
		int index = typeName.lastIndexOf('.');
		if (index == -1)
			return typeName;
		
		return typeName.substring(index + 1);
	}
}
