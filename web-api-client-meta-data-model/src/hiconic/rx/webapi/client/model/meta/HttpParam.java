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
package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.PropertyMetaData;

@Abstract
public interface HttpParam extends PropertyMetaData {

	final EntityType<HttpParam> T = EntityTypes.T(HttpParam.class);
	
	@Description("Optional parameter name. If not specified the name of the associated property is used.")
	String getParamName();
	void setParamName(String paramName);
	
	HttpParamType paramType();
}
