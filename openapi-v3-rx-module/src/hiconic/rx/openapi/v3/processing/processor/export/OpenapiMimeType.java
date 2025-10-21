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
package hiconic.rx.openapi.v3.processing.processor.export;

import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.resource.Resource;

/**
 * {@link OpenApi} components are resolved a bit differently between mime types. E.g. for JSON, a {@link Resource}
 * entity is represented as the entities assembly (its properties and values) where in multipart/form-data it is
 * represented as a binary type (the resource's binary data).
 * <p>
 * This enum is used to hold the information of the mime type being resolved at a moment.
 *
 * @author Neidhart.Orlich
 *
 */
public enum OpenapiMimeType {
	MULTIPART_FORMDATA("multipart/form-data"),
	URLENCODED("application/x-www-form-urlencoded"),
	APPLICATION_JSON("application/json"),
	ALL("*/*");

	private String mime;

	private OpenapiMimeType(String mime) {
		this.mime = mime;
	}

	public String getMimeString() {
		return mime;
	}
}
