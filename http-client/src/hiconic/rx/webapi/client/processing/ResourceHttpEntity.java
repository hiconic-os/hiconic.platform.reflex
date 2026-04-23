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
package hiconic.rx.webapi.client.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resourceapi.stream.GetBinaryResponse;
import com.braintribe.model.resourceapi.stream.GetResource;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.IOTools;

public class ResourceHttpEntity extends AbstractHttpEntity {

	private final Resource resource;
	private Evaluator<ServiceRequest> evaluator;

	public ResourceHttpEntity(Resource resource, Evaluator<ServiceRequest> evaluator) {
		this.resource = resource;
		this.evaluator = evaluator;
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public long getContentLength() {
		Long fileSize = resource.getFileSize();
		if (fileSize == null || fileSize <= 0) {
			return -1;
		}
		return fileSize;
	}

	@Override
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		final Resource transientResource;

		if (!resource.isTransient()) {
			GetResource gr = GetResource.T.create();
			gr.setDomainId(resource.getPartition());
			gr.setResource(resource);
			GetBinaryResponse response = gr.eval(evaluator).get();
			transientResource = response.getResource();
		} else {
			transientResource = resource;
		}
		return transientResource.openStream();
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		try (InputStream in = getContent()) {
			IOTools.transferBytes(in, outstream);
		}
	}

	@Override
	public boolean isStreaming() {
		return true;
	}

	@Override
	public String toString() {
		return "Resource Stream: " + resource.getName() + " (" + resource.getFileSize() + ")";
	}
}
