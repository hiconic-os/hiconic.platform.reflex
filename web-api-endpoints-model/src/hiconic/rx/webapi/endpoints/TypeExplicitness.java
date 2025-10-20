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
package hiconic.rx.webapi.endpoints;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum TypeExplicitness implements EnumBase<TypeExplicitness> {
	/**
	 * The marshaller decides which of the other options it will choose. Look at the inidividual marshaller's to see for the individual case. 
	 */
	auto, 
	
	/**
	 * The types are made explicit to allow to preserve the correct type under all circumstances which means not to rely on any contextual
	 * information to auto convert it from another type.
	 */
	always,
	
	/**
	 * The types are made explicit for entities in all cases and the other values can get simpler types if appropriate and the context of the value
	 * can give the information to reestablish the correct type with an auto conversion.
	 */
	entities, 

	/**
	 * The types are made explicit if the actual type cannot be reestablished from the context of the value which is the case that value is
	 * a concretization of the type given by the context.
	 */
	polymorphic, 
	
	/**
	 * The types are never made explicit under no circumstances.
	 */
	never;

	public static final EnumType<TypeExplicitness> T = EnumTypes.T(TypeExplicitness.class);
	
	@Override
	public EnumType<TypeExplicitness> type() {
		return T;
	}	
}