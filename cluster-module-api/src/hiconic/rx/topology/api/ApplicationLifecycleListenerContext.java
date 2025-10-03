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
package hiconic.rx.topology.api;

/**
 * <p>
 * The context provided upon {@link ApplicationLifecycleListener} events.
 * 
 */
public interface ApplicationLifecycleListenerContext {

	/**
	 * <p>
	 * Returns the id of the application which triggered the event.
	 * 
	 * @return The id of the application which triggered the event.
	 */
	String applicationId();

	/**
	 * <p>
	 * Returns the id of the node which triggered the event.
	 * 
	 * @return The id of the node which triggered the event.
	 */
	String nodeId();

	/**
	 * <p>
	 * Unsubscribes the calling listener from subsequent events.
	 */
	void unsubscribe();

}
