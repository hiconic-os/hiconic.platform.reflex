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
package hiconic.rx.web.endpoints.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Test;

import hiconic.rx.test.common.AbstractRxTest;
import hiconic.rx.web.server.api.WebServerContract;

public class WebEndpointParityTest extends AbstractRxTest {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	public void exposesAggregatedHealthChecks() throws Exception {
		HttpResponse<String> response = get("/healthz");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(value -> assertThat(value).startsWith("application/json"));
		assertThat(response.body()).contains("nodeId");
	}

	@Test
	public void acceptsUnknownPropertiesForLenientMappings() throws Exception {
		HttpResponse<String> response = postJson("/api/lenient", "{\"text\":\"abc\",\"isTrueid\":true}");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("cba");
	}

	@Test
	public void rejectsUnknownPropertiesForStrictMappings() throws Exception {
		HttpResponse<String> response = postJson("/api/strict", "{\"text\":\"abc\",\"isTrueid\":true}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("isTrueid");
	}

	@Test
	public void supportsContractBasedInternalForwards() throws Exception {
		HttpResponse<String> response = postJson("/contract-forward", "{\"text\":\"abc\"}");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("cba");
	}

	@Test
	public void supportsConfiguredInternalForwards() throws Exception {
		HttpResponse<String> response = get("/configured-forward?text=abc");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("cba");
	}

	@Test
	public void supportsConfiguredRedirects() throws Exception {
		HttpResponse<String> response = get("/configured-redirect");

		assertThat(response.statusCode()).isEqualTo(302);
		assertThat(response.headers().firstValue("location")).contains("/healthz");
	}

    @Test
    public void defaultCompressionAppliesToRealWebApiResponses() throws Exception {
        String text = "structured-response-".repeat(1000);
        var request = HttpRequest.newBuilder(uri("/api/lenient")).header("Content-Type", "application/json")
                .header("Accept-Encoding", "gzip").POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"" + text + "\"}")).build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Encoding")).contains("gzip");
        try (var input = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(response.body()))) {
            assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains(new StringBuilder(text).reverse().toString());
        }
    }

	private HttpResponse<String> get(String path) throws Exception {
		return httpClient.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> postJson(String path, String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri(path)) //
				.header("Content-Type", "application/json") //
				.POST(HttpRequest.BodyPublishers.ofString(body)) //
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		WebServerContract webServer = platform.getWireContext().contract(WebServerContract.class);
		return URI.create("http://localhost:" + webServer.getEffectiveServerPort() + path);
	}
}
