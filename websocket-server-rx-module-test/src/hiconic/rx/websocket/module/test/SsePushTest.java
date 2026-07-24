// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
// ============================================================================
package hiconic.rx.websocket.module.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.model.service.api.PushRequest;
import com.braintribe.model.service.api.result.PushResponse;

import hiconic.rx.demo.model.api.ReverseText;
import hiconic.rx.test.common.AbstractRxTest;
import hiconic.rx.web.server.api.WebServerContract;

public class SsePushTest extends AbstractRxTest {

	@Test
	public void receivesPushViaSse() throws Exception {
		WebServerContract webServer = platform.getWireContext().contract(WebServerContract.class);
		URI uri = URI.create("http://localhost:" + webServer.getEffectiveServerPort()
				+ "/push/sse?clientId=sse-test&accept=application/json");

		HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
		HttpResponse<InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
		Assertions.assertThat(response.statusCode()).isEqualTo(200);
		Assertions.assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(v -> Assertions.assertThat(v)
				.startsWith("text/event-stream"));

		try (InputStream stream = response.body();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String registration = readEvent(reader);
			Assertions.assertThat(registration).contains("event: channel", "data: ");

			ReverseText payload = ReverseText.T.create();
			payload.setText("SSE payload");

			PushRequest push = PushRequest.T.create();
			push.setClientIdPattern("sse-test");
			push.setServiceRequest(payload);

			PushResponse pushResponse = push.eval(platformContract.serviceProcessing().systemEvaluator()).get();
			Assertions.assertThat(pushResponse.getResponseMessages()).singleElement().satisfies(message -> {
				Assertions.assertThat(message.getSuccessful()).isTrue();
				Assertions.assertThat(message.getClientIdentification()).isEqualTo("sse-test");
			});

			Assertions.assertThat(readEvent(reader)).contains("ReverseText", "SSE payload");
		}
	}

	private String readEvent(BufferedReader reader) throws Exception {
		StringBuilder result = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty())
				return result.toString();
			result.append(line).append('\n');
		}
		throw new AssertionError("SSE stream ended before an event was completed");
	}
}
