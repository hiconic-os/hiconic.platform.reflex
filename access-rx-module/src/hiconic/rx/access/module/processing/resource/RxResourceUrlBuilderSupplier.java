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
package hiconic.rx.access.module.processing.resource;

import java.net.URL;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.resource.Resource;

public class RxResourceUrlBuilderSupplier implements Function<Resource, RxResourceUrlBuilder> {

	protected URL baseStreamingUrl;
	protected Supplier<String> sessionIdProvider;
	protected String responseMimeType;

	@Required
	@Configurable
	public void setBaseStreamingUrl(URL baseStreamingUrl) {
		this.baseStreamingUrl = baseStreamingUrl;
	}

	@Required
	@Configurable
	public void setSessionIdProvider(Supplier<String> sessionIdProvider) {
		this.sessionIdProvider = sessionIdProvider;
	}

	@Configurable
	public void setResponseMimeType(String responseMimeType) {
		this.responseMimeType = responseMimeType;
	}

	@Override
	public RxResourceUrlBuilder apply(Resource resource) {

		RxResourceUrlBuilder urlBuilder = new RxResourceUrlBuilder();
		urlBuilder.setBaseStreamingUrl(baseStreamingUrl);
		urlBuilder.setResponseMimeType(responseMimeType);
		urlBuilder.setSessionId(sessionIdProvider.get());
		urlBuilder.setResource(resource);

		return urlBuilder;

	}

}
