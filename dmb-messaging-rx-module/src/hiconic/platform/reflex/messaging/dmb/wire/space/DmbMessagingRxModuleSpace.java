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
package hiconic.platform.reflex.messaging.dmb.wire.space;

import com.braintribe.transport.messaging.api.MessagingSessionProvider;
import com.braintribe.transport.messaging.bq.BlockingQueueMessagingConnectionProvider;
import com.braintribe.transport.messaging.impl.StandardMessagingSessionProvider;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.messaging.api.MessagingBaseContract;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Messaging based on {@link BlockingQueueMessagingConnectionProvider}
 */
@Managed
public class DmbMessagingRxModuleSpace implements RxModuleContract, MessagingContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private MessagingBaseContract messagingBase;

	@Override
	@Managed
	public MessagingSessionProvider sessionProvider() {
		StandardMessagingSessionProvider bean = new StandardMessagingSessionProvider();
		bean.setMessagingConnectionProvider(connectionProvider());
		return bean;
	}

	@Managed
	private BlockingQueueMessagingConnectionProvider connectionProvider() {
		BlockingQueueMessagingConnectionProvider bean = new BlockingQueueMessagingConnectionProvider();
		bean.setMessagingContext(messagingBase.context());

		return bean;
	}

}