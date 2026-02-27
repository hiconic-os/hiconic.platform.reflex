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
package hiconic.platform.reflex.security.processor;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.SessionNotFound;
import com.braintribe.gm.model.usersession.PersistenceUserSession;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSessionType;

public class StandardUserSessionService extends AbstractUserSessionService {
	private static final Logger logger = System.getLogger(StandardUserSessionService.class.getName());

	private Map<String, PersistenceUserSession> userSessionsById = new ConcurrentHashMap<>();
	private Map<String, PersistenceUserSession> userSessionsByAcquirationKey = new ConcurrentHashMap<>();
	
	@Override
	public void touchUserSession(String sessionId, Date lastAccessDate, Date expiryDate) {
		PersistenceUserSession session = userSessionsById.computeIfPresent(sessionId, (k, s) -> {
			s.setLastAccessedDate(lastAccessDate);
			s.setExpiryDate(expiryDate);
			return s;
		});
		
		if (session == null) 
			logger.log(Level.ERROR, "Could not touch missing PersistenceUserSession with id: " + sessionId);
		
	}

	@Override
	protected void deletePersistenceUserSession(PersistenceUserSession pUserSession) {
		deletePersistenceUserSession((String)pUserSession.getId());
	}

	@Override
	protected void deletePersistenceUserSession(String sessionId) {
		userSessionsById.remove(sessionId);
	}

	@Override
	protected void closePersistenceUserSession(String sessionId) {
		PersistenceUserSession session = userSessionsById.computeIfPresent(sessionId, (k, s) -> {
			s.setExpiryDate(new Date());
			return s;
		});
		
		if (session == null) 
			logger.log(Level.ERROR, "Could not close missing PersistenceUserSession with id: " + sessionId);
	}

	@Override
	protected void closePersistenceUserSession(PersistenceUserSession userSession) {
		closePersistenceUserSession((String)userSession.getId());
	}

	@Override
	protected Maybe<PersistenceUserSession> findPersistenceUserSession(String sessionId) {
		PersistenceUserSession pUserSession = userSessionsById.get(sessionId);
		
		if (pUserSession == null)
			return Reasons.build(SessionNotFound.T).text("User session '" + sessionId + "' not found").toMaybe();
		
		return Maybe.complete(pUserSession);
	}

	@Override
	protected Maybe<PersistenceUserSession> findPersistenceUserSessionByAcquirationKey(String acquirationKey) {
		PersistenceUserSession pUserSession = userSessionsByAcquirationKey.get(acquirationKey);
		
		if (pUserSession == null)
			return Reasons.build(SessionNotFound.T).text("User session with acquiration key '" + acquirationKey + "' not found").toMaybe();
		
		return Maybe.complete(pUserSession);
	}

	@Override
	protected PersistenceUserSession createPersistenceUserSession(User user, Set<String> additionalRoles, UserSessionType type, TimeSpan maxIdleTime,
			TimeSpan maxAge, Date fixedExpiryDate, String internetAddress, Map<String, String> properties,
			String acquirationKey, boolean blocksAuthenticationAfterLogout) {
		UserSessionType userSessionType = type != null ? type : this.defaultUserSessionType;
		Date now = new Date();
		
		PersistenceUserSession userSession = initPersistenceUserSession(PersistenceUserSession.T.create(), user, additionalRoles, maxIdleTime, maxAge,
				fixedExpiryDate, internetAddress, properties, acquirationKey, blocksAuthenticationAfterLogout, userSessionType, now);

		userSessionsById.put(userSession.getId(), userSession);
		
		if (acquirationKey != null) {
			userSessionsByAcquirationKey.put(acquirationKey, userSession);
		}
		
		return userSession;
	}

}
