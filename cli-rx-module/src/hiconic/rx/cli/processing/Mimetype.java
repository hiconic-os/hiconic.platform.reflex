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
package hiconic.rx.cli.processing;

import java.util.HashMap;
import java.util.Map;

public class Mimetype {
	private static final String MIME_PARAM_CHARSET = "charset";
	private Map<String, String> parameters;
	private String mimeType;
	private String subType;
	private String type;

	public Mimetype(String mimeType) {
		this.mimeType = mimeType;
		this.parameters = new HashMap<>();
	}

	public Mimetype(String mimeType, Map<String, String> parameters) {
		this.mimeType = mimeType;
		this.parameters = parameters;
	}

	public String getCharset() {
		String charset = getParameter(MIME_PARAM_CHARSET);

		if (charset == null) {
			charset = getType().equals("text") ? "ISO-8859-1" : "UTF-8";
		}

		return charset;
	}

	public boolean hasExplicitCharset() {
		return parameters.containsKey(MIME_PARAM_CHARSET);
	}

	public String getSubType() {
		ensureTypeSplit();
		return subType;
	}

	public String getType() {
		ensureTypeSplit();
		return type;
	}

	private void ensureTypeSplit() {
		if (subType == null || type == null) {
			int index = mimeType.indexOf('/');
			subType = index != -1 ? mimeType.substring(index + 1) : "";
			type = index != -1 ? mimeType.substring(0, index) : mimeType;
		}
	}

	public Mimetype setParameter(String name, String value) {
		this.parameters.put(name, value);
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getParameter(String name) {
		return parameters.get(name);
	}

	public boolean matches(Mimetype format) {
		if (!mimeType.equals(format.getMimeType()))
			return false;

		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			if (!format.getParameter(entry.getKey()).equals(entry.getValue()))
				return false;
		}

		return true;
	}
}
