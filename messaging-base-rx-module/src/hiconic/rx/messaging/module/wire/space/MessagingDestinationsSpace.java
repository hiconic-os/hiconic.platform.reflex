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
package hiconic.rx.messaging.module.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.messaging.api.MessagingDestinationsContract;


@Managed
@Deprecated
public class MessagingDestinationsSpace implements MessagingDestinationsContract {

	private static final String DEFAULT_MESSAGING_TOPIC_MULTICAST_REQUEST = "tf.topic.multicastRequest";
	private static final String DEFAULT_MESSAGING_TOPIC_MULTICAST_RESPONSE = "tf.topic.multicastResponse";
	private static final String DEFAULT_MESSAGING_QUEUE_TRUSTED_REQUEST = "tf.queue.trustedRequest";
	private static final String DEFAULT_MESSAGING_TOPIC_TRUSTED_RESPONSE = "tf.topic.trustedResponse";
	private static final String DEFAULT_MESSAGING_TOPIC_HEARTBEAT = "tf.topic.heartbeat";
	private static final String DEFAULT_MESSAGING_TOPIC_UNLOCK = "tf.topic.unlock";
	private static final String DEFAULT_MESSAGING_TOPIC_DBL_BROADCAST = "tf.topic.dblBroadcast";
	private static final String DEFAULT_MESSAGING_TOPIC_DBL_REMOTE = "tf.topic.remoteToDbl";
	private static final String DEFAULT_MESSAGING_QUEUE_DBL_REMOTE = "tf.queue.remoteToDbl";

	@Override
	public String multicastRequestTopicName() {
		return DEFAULT_MESSAGING_TOPIC_MULTICAST_REQUEST;
	}

	@Override
	public String multicastResponseTopicName() {
		return DEFAULT_MESSAGING_TOPIC_MULTICAST_RESPONSE;
	}

	@Override
	public String trustedRequestQueueName() {
		return DEFAULT_MESSAGING_QUEUE_TRUSTED_REQUEST;
	}

	@Override
	public String trustedResponseTopicName() {
		return DEFAULT_MESSAGING_TOPIC_TRUSTED_RESPONSE;
	}

	@Override
	public String heartbeatTopicName() {
		return DEFAULT_MESSAGING_TOPIC_HEARTBEAT;
	}

	@Override
	public String unlockTopicName() {
		return DEFAULT_MESSAGING_TOPIC_UNLOCK;
	}

	@Override
	public String dblBroadcastTopicName() {
		return DEFAULT_MESSAGING_TOPIC_DBL_BROADCAST;
	}

	@Override
	public String remoteToDblTopicName() {
		return DEFAULT_MESSAGING_TOPIC_DBL_REMOTE;
	}

	@Override
	public String remoteToDblQueueName() {
		return DEFAULT_MESSAGING_QUEUE_DBL_REMOTE;
	}

	@Override
	public String prefixName(String name) {
		return name;
	}
}
