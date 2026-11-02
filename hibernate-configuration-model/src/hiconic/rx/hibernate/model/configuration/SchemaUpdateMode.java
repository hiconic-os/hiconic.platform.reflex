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
package hiconic.rx.hibernate.model.configuration;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Value for Hibernate's Environment.HBM2DDL_AUTO property.
 */
public enum SchemaUpdateMode implements EnumBase<SchemaUpdateMode> {
	/**
	 * Updates the schema (and runs the DbUpdateStatements) if the mappings changed since the last update. The changes are tracked in the
	 * TF_SCHEMA_UPDATE_TMP table.
	 */
	update,
	/** Validates the schema against the mappings and fails if they do not match. Does not change the DB. */
	validate,
	/** Neither updates nor validates the schema. Does not change the DB. */
	none;

	public static final EnumType<SchemaUpdateMode> T = EnumTypes.T(SchemaUpdateMode.class);

	@Override
	public EnumType<SchemaUpdateMode> type() {
		return T;
	}
}
