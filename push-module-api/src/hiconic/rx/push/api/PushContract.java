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
package hiconic.rx.push.api;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.InternalPushRequest;
import com.braintribe.model.service.api.result.PushResponse;

import hiconic.rx.module.api.wire.RxExportContract;

public interface PushContract extends RxExportContract {
	PushChannelLifecyclePublisher channelLifecyclePublisher();

	/** Adds a transport delegate receiving the node-local form of a push. */
	void addHandler(ServiceProcessor<? super InternalPushRequest, PushResponse> handler);

	void registerChannel(PushChannel channel);
	void unregisterChannel(PushChannel channel);
}
