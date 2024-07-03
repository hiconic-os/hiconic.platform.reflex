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
package hiconic.rx.hibernate.processing;

import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelTypeReflection;

/**
 * Interceptor which provides correct instances for {@link GenericEntity}s and resolves their names.
 */
public class GmAdaptionInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = -5221026406788869293L;
	private static final GenericModelTypeReflection typeReflection = GMF.getTypeReflection();

	@Override
	public String getEntityName(Object object) {
		if (object instanceof GenericEntity) {
			return ((GenericEntity) object).entityType().getTypeSignature();

		} else {
			return super.getEntityName(object);
		}
	}

	@Override
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
		if (entityMode == EntityMode.POJO) {
			EntityType<?> entityType = typeReflection.getType(entityName);

			GenericEntity ge = entityType.createPlain();
			ge.setId(id);

			return ge;

		} else {
			return super.instantiate(entityName, entityMode, id);
		}

	}

}
