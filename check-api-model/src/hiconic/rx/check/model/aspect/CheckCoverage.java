// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.check.model.aspect;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Classifies checks based on their purpose.
 * 
 * @see CheckLatency
 */
public enum CheckCoverage implements EnumBase<CheckCoverage> {

	/**
	 * Checks for the initial bootstrapping of the node was completed.
	 * <p>
	 * <b>Failure implication:</b> The node is not ready yet, or there is an issue preventing it from starting properly.
	 */
	readiness,

	/**
	 * Checks for the ability to process functionality and not the functionality itself.
	 * <p>
	 * These checks must strictly be focused on the checked instance, not e.g. connectivity to remote components, as their purpose is to help the
	 * cluster with managing the nodes.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>Memory
	 * <li>CPU load
	 * <li>Deadlock Detection (to a degree)
	 * <li>Resources
	 * <ul>
	 * <li>Threads
	 * <li>Accepted HTTP Connections
	 * </ul>
	 * </ul>
	 * <p>
	 * <b>Failure implication:</b> Problem with the resources allocated to the node, or insufficient scaling of the cluster.
	 */
	vitality,

	/**
	 * Checks for the availability of remote services like databases, hosts, queues,...
	 * <p>
	 * Covers external dependencies like DBs, hosts, queues, and caches. Might involve a small test of the remote service functionality, but one of
	 * general nature, not directly related to business logic implemented by the server (e.g. a query to a systable or ping).
	 * <p>
	 * <b>Failure implication:</b> Problem with a remote service.
	 */
	connectivity,

	/**
	 * Checks the actual functionality.
	 * <p>
	 * If all other checks pass, this should also pass, otherwise gives a more detailed overview of which functionality is affected by <i>vitality</i>
	 * or <i>connectivity</i> problems.
	 * <p>
	 * <b>Failure implication:</b> Underlying <i>vitality</i> or <i>connectivity</i> issues, or a bug.
	 */
	functional;

	public static final EnumType<CheckCoverage> T = EnumTypes.T(CheckCoverage.class);

	@Override
	public EnumType<CheckCoverage> type() {
		return T;
	}
}
