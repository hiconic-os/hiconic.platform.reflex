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
package hiconic.platform.reflex.websocket_server.processing;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import hiconic.rx.push.api.PushChannel;
import jakarta.websocket.Session;


/**
 * A class used by the {@link WsServer} that helps maintaining (registering, removing, finding) 
 * open websocket session along with according Client informations.
 */
public class WsRegistry {

	private Map<String, WsRegistrationEntry> registry = new ConcurrentHashMap<>();
			
	/**
	 * Registers an entry for the given websocket session along with passed clientInfo.
	 */
	public WsRegistrationEntry register (WsClientInfo info, Session session) {
		WsRegistrationEntry entry = new WsRegistrationEntry(info, session);
		registry.put(session.getId(), entry);
		return entry;
	}
	
	/**
	 * Removes the registered entry registered for passed websocket session.
	 * @return the removed entry or null if no entry found for passed session.
	 */
	public WsRegistrationEntry remove(Session session) {
		WsRegistrationEntry e = findEntry(session);
		if (e == null) {
			return null;
		}
		registry.remove(e.getSession().getId());
		return e;
	}
	/**
	 * Removes all registered entries matching the given Predicate.
	 * @return the removed entries.
	 */
	public Set<WsRegistrationEntry> remove(Predicate<WsRegistrationEntry> predicate) {
		Set<WsRegistrationEntry> entries = findEntries(predicate);
		for (WsRegistrationEntry e : entries) {
			remove(e.getSession());
		}
		return entries;
	}

	/**
	 * Returns the entry registered for the given websocket session. 
	 * If no entry is registered this method returns null.
	 */
	public WsRegistrationEntry findEntry(Session session) {
		return registry.get(session.getId());
	}
	
	public WsRegistrationEntry findEntry(String pushChannelId) {
		return registry.get(pushChannelId);
	}

	/**
	 * Returns a collection of registered entries matching the given Predicate. 
	 */
	public Set<WsRegistrationEntry> findEntries(Predicate<WsRegistrationEntry> predicate) {
		return findEntries(predicate, null);
	}
	
	public Set<WsRegistrationEntry> findEntries(Predicate<WsRegistrationEntry> predicate, String pushChannelId) {
		if (pushChannelId != null) {
			WsRegistrationEntry entry = findEntry(pushChannelId);
			
			if (entry != null)
				return Collections.singleton(entry);
			
			return Collections.emptySet();
		}
		
		return registry
				.values()
				.stream()
				.filter((e) -> predicate.test(e))
				.collect(Collectors.toSet());
	}

	/**
	 * The entry object holding all necessary informations associated to a websocket session.
	 */
	public class WsRegistrationEntry implements PushChannel {
		
		private WsClientInfo clientInfo;
		private Session session;
		
		public WsRegistrationEntry(WsClientInfo clientInfo, Session session) {
			this.clientInfo = clientInfo;
			this.session = session;		
		}
		
		public WsClientInfo getClientInfo() {
			return clientInfo;
		}
		
		public Session getSession() {
			return session;
		}
		
		@Override
		public String getChannelId() {
			return session.getId();
		}
		
		@Override
		public String getClientId() {
			return clientInfo.getClientId();
		}
		
		@Override
		public String getSessionId() {
			return clientInfo.getSessionId();
		}
		
	}
	
}
