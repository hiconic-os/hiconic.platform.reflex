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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.braintribe.model.resourceapi.persistence.DeleteResource;
import com.braintribe.model.resourceapi.persistence.DeletionScope;
import com.braintribe.model.resourceapi.persistence.UpdateResource;
import com.braintribe.model.resourceapi.persistence.UploadResources;
import com.braintribe.model.resourceapi.stream.GetResource;
import com.braintribe.model.securityservice.Logout;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.web.ddra.endpoints.api.v1.SingleDdraMapping;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
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

	@Test
	public void platformMappingsProvideCxCompatibleAuthenticationPaths() {
		StandardWebApiMappingOracle oracle = new StandardWebApiMappingOracle();
		PlatformWebApiMappings.register(oracle);

		SingleDdraMapping authenticate = oracle.get("/authenticate", HttpRequestMethod.POST);
		assertEquals(OpenUserSessionWithUserAndPassword.T, authenticate.getRequestType());
		assertEquals("security", authenticate.getServiceDomain());
		assertEquals("userSession.sessionId", authenticate.getDefaultProjection());

		SingleDdraMapping logout = oracle.get("/logout", HttpRequestMethod.POST);
		assertEquals(Logout.T, logout.getRequestType());
		assertEquals("security", logout.getServiceDomain());
	}

	@Test
	public void platformMappingsProvideCxCompatibleResourcePaths() {
		StandardWebApiMappingOracle oracle = new StandardWebApiMappingOracle();
		PlatformWebApiMappings.register(oracle);

		SingleDdraMapping download = oracle.get("/download", HttpRequestMethod.GET);
		assertEquals(GetResource.T, download.getRequestType());
		assertEquals("resource", download.getDefaultProjection());
		assertEquals(Boolean.TRUE, download.getDefaultDownloadResource());

		SingleDdraMapping upload = oracle.get("/upload", HttpRequestMethod.POST);
		assertEquals(UploadResources.T, upload.getRequestType());
		assertNull(upload.getServiceDomain());
		assertNull(upload.createRequestPrototype());

		SingleDdraMapping update = oracle.get("/upload", HttpRequestMethod.PUT);
		assertEquals(UpdateResource.T, update.getRequestType());
		assertNull(update.getServiceDomain());

		SingleDdraMapping delete = oracle.get("/delete", HttpRequestMethod.DELETE);
		assertEquals(DeleteResource.T, delete.getRequestType());
		assertNull(delete.getServiceDomain());
	}

	@Test
	public void deleteMappingPrototypeDeletesResourceAndSource() {
		StandardWebApiMappingOracle oracle = new StandardWebApiMappingOracle();
		PlatformWebApiMappings.register(oracle);

		SingleDdraMapping delete = oracle.get("/delete", HttpRequestMethod.DELETE);

		DeleteResource first = (DeleteResource) delete.createRequestPrototype();
		assertEquals(DeletionScope.resource, first.getDeletionScope());

		// every call gets its own instance, so a call cannot change the prototype of the next one
		DeleteResource second = (DeleteResource) delete.createRequestPrototype();
		assertNotSame(first, second);
	}

	@Test
	public void requestPrototypeIsSuppliedPerCall() {
		StandardWebApiMappingOracle oracle = new StandardWebApiMappingOracle();
		oracle.mapping("/test-prototype", HttpRequestMethod.POST, Logout.T) //
				.requestPrototype(() -> {
					Logout logout = Logout.T.create();
					logout.setSessionId("from-prototype");
					return logout;
				}) //
				.register();

		ServiceRequest prototype = oracle.get("/test-prototype", HttpRequestMethod.POST).createRequestPrototype();
		assertTrue(prototype instanceof Logout);
		assertEquals("from-prototype", ((Logout) prototype).getSessionId());
	}

	private RequestPathPrefix prefix(String value) {
		RequestPathPrefix result = RequestPathPrefix.T.create();
		result.setPrefix(value);
		return result;
	}
}
