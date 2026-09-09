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
package hiconic.rx.web.ddra.mapping;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import hiconic.rx.webapi.model.meta.RequestPath;
import hiconic.rx.webapi.model.meta.RequestPathPrefix;

public class StandardWebApiMappingOracleTest {

	@Test
	public void explicitEmptyPathMapsExactlyToItsPrefixes() {
		RequestPathPrefix apiPrefix = prefix("v1");
		RequestPathPrefix sectionPrefix = prefix("excerpt");
		RequestPath emptyPath = RequestPath.T.create();
		emptyPath.setPath("");

		StandardWebApiMappingOracle.MappingMds mapping = new StandardWebApiMappingOracle.MappingMds();
		mapping.pathPrefixes = List.of(apiPrefix, sectionPrefix);
		mapping.path = emptyPath;

		String path = mapping.pathWithNoSlashesOrNull();
		assertEquals("", path);
		assertEquals("/default.access.objectstore/v1/excerpt",
				StandardWebApiMappingOracle.composePath("/default.access.objectstore/", mapping.pathPrefix(), path));
	}

	private RequestPathPrefix prefix(String value) {
		RequestPathPrefix result = RequestPathPrefix.T.create();
		result.setPrefix(value);
		return result;
	}
}
