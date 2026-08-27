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
package hiconic.rx.aop.sp.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.braintribe.cfg.Required;
import com.braintribe.model.access.AccessIdentificationLookup;
import com.braintribe.model.access.IncrementalAccess;

/**
 * @author pit
 *
 */
public class MapBasedAccessIdentificationLookup implements AccessIdentificationLookup {
	
	private Map<String,IncrementalAccess> idToAccessMap;
	private Map<IncrementalAccess, String> accessToIdMap;
	
	@Required
	public void setIdToAccessMap(Map<String, IncrementalAccess> idToAccessMap) {
		this.idToAccessMap = idToAccessMap;
		accessToIdMap = new HashMap<IncrementalAccess, String>();
		for (Entry<String, IncrementalAccess> entry : idToAccessMap.entrySet()) {
			accessToIdMap.put( entry.getValue(), entry.getKey());
		}
	}

	@Override
	public String lookupAccessId(IncrementalAccess access) {
		return accessToIdMap.get( access);
	}

	@Override
	public IncrementalAccess lookupAccess(String id) {
		return idToAccessMap.get( id);
	}

}
