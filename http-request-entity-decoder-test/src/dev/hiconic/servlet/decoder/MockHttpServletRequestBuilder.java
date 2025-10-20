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
package dev.hiconic.servlet.decoder;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;


public class MockHttpServletRequestBuilder {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	public MockHttpServletRequestBuilder header(String name, String ...values) {
		List<String> headers = request.getHeaderMap().get(name);
		if(headers == null) {
			headers = new ArrayList<>();
			request.getHeaderMap().put(name, headers);
		}
		for(String value : values) {
			headers.add(value);
		}
		
		return this;
	}
	
	public MockHttpServletRequestBuilder parameter(String name, String ...values) {
		String[] existing = request.getParameterMap().put(name, values);
		if(existing != null) {
			throw new IllegalStateException("The servlet request already contains the parameter with name \"" + name + "\", please add all values at once.");
		}
		return this;
	}
	
	public HttpServletRequest build() {
		return request;
	}
}
