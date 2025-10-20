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
package hiconic.rx.web.rest.servlet;

public enum CrudRequestTarget {
	ENTITY("entities"),
	PROPERTY("properties"),
	SWAGGER("swaggerentities"),
	EXPORT_SWAGGER("exportswaggerentities"),
	SWAGGER_PROPERTIES("swaggerproperties"),
	EXPORT_SWAGGER_PROPERTIES("exportswaggerproperties");
	
	private final String url;
	
	private CrudRequestTarget(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}
}
