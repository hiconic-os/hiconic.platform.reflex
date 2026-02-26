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
package hiconic.rx.access.module.processing.enriching;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.generic.session.GmSession;
import com.braintribe.model.resource.specification.ResourceSpecification;

import hiconic.rx.access.module.api.ResourceSpecificationDetector;

public class DelegatingSpecificationDetector implements ResourceSpecificationDetector<ResourceSpecification> {

	
	private Map<String, ResourceSpecificationDetector<?>> detectorMap;
	
	@Configurable
	@Required
	public void setDetectorMap(Map<String, ResourceSpecificationDetector<?>> detectorMap) {
		this.detectorMap = detectorMap;
	}
	
	@Override
	public ResourceSpecification getSpecification(InputStream in, String mimeType, GmSession session) throws IOException {
		ResourceSpecificationDetector<?> delegate = detectorMap.get(mimeType);
		if (delegate != null) {
			return delegate.getSpecification(in, mimeType, session);
		}
		
		return null;
	}
	
}
