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
package hiconic.rx.websocket.module.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import com.braintribe.model.service.api.PushRequest;
import com.braintribe.processing.async.impl.HubPromise;

import hiconic.rx.demo.model.api.ReverseText;
import hiconic.rx.push.api.PushChannel;
import hiconic.rx.push.api.PushChannelLifecycleListener;
import hiconic.rx.push.api.PushChannelLifecyclePublisher;
import hiconic.rx.push.api.PushContract;
import hiconic.rx.test.common.AbstractRxTest;
import hiconic.rx.web.server.api.WebServerContract;

public class WebsocketTest extends AbstractRxTest {

	@BeforeClass
	public static void onBeforeClass() {
	}

	private int getPort() {
		WebServerContract webServer = platform.getWireContext().contract(WebServerContract.class);
		return webServer.getEffectiveServerPort();
	}

	@Test
	public void testGet() throws Exception {
		String url = "http://localhost:" + getPort() + "/app/index.html";

		PushChannelLifecyclePublisher publisher = platform.getWireContext().contract(PushContract.class).channelLifecyclePublisher();
		
		HubPromise<Boolean> promise = new HubPromise<>();

		List<String> data = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(3);
		
		class Listener implements PushChannelLifecycleListener {
			String openedChannelId;
			String closedChannelId;
			
			@Override
			public void onConnectionEstablished(PushChannel channel) {
				openedChannelId = channel.getChannelId();
			}
			
			@Override
			public void onConnectionClosed(PushChannel channel) {
				closedChannelId = channel.getChannelId();
				latch.countDown();
			}
		}
		
		Listener listener = new Listener();
		
		publisher.addListener(listener);

		try (var client = new WebSocketTestClient(getPort(), d -> {
			data.add(d);
			latch.countDown();
		}, promise);) {

			Assertions.assertThat(promise.get()).as("failed connection").isTrue();

			ReverseText reverseText = ReverseText.T.create();
			reverseText.setText("Egal");

			PushRequest push = PushRequest.T.create();
			push.setClientIdPattern("test");
			push.setServiceRequest(reverseText);

			evaluator.eval(push).get();
			
			
		}
		
		latch.await(3, TimeUnit.SECONDS);
		
		Assertions.assertThat(data.get(1)).as("did not get expected message").contains("ReverseText");
		Assertions.assertThat(listener.openedChannelId).isEqualTo(data.get(0));
		Assertions.assertThat(listener.closedChannelId).isEqualTo(data.get(0));
	}
}
