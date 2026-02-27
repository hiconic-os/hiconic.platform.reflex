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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.gm.model.usersession.PersistenceUserSession;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.session.exception.GmSessionException;
import com.braintribe.model.processing.securityservice.api.DeletedSessionInfo;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.processing.securityservice.impl.Roles;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.provider.Hub;

public abstract class AbstractUserSessionService implements UserSessionService, LifecycleAware {

	static final Logger log = Logger.getLogger(AbstractUserSessionService.class);

	protected Function<UserSessionType, String> sessionIdProvider;
	protected String nodeId;

	protected UserSessionType defaultUserSessionType = UserSessionType.normal;
	protected TimeSpan defaultUserSessionMaxIdleTime;

	protected List<Hub<UserSession>> internalUserSessionHolders;

	public AbstractUserSessionService() {
		super();
	}

	protected abstract void deletePersistenceUserSession(PersistenceUserSession pUserSession);
	protected abstract void deletePersistenceUserSession(String sessionId);
	protected abstract void closePersistenceUserSession(String sessionId);
	protected abstract void closePersistenceUserSession(PersistenceUserSession userSession);

	protected abstract Maybe<PersistenceUserSession> findPersistenceUserSession(String sessionId);

	protected abstract Maybe<PersistenceUserSession> findPersistenceUserSessionByAcquirationKey(String acquirationKey);

	protected abstract PersistenceUserSession createPersistenceUserSession(User user, Set<String> additionalRoles, UserSessionType type, TimeSpan maxIdleTime, TimeSpan maxAge,
			Date fixedExpiryDate, String internetAddress, Map<String, String> properties, String acquirationKey,
			boolean blocksAuthenticationAfterLogout);

	@Required
	public void setSessionIdProvider(Function<UserSessionType, String> sessionIdProvider) {
		this.sessionIdProvider = sessionIdProvider;
	}

	@Required
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @param defaultUserSessionMaxIdleTime
	 *            Defaults to null - no max idle time limit.
	 */
	@Configurable
	public void setDefaultUserSessionMaxIdleTime(TimeSpan defaultUserSessionMaxIdleTime) {
		this.defaultUserSessionMaxIdleTime = defaultUserSessionMaxIdleTime;
	}

	/**
	 * @param defaultUserSessionType
	 *            Defaults to 'normal'.
	 */
	@Configurable
	public void setDefaultUserSessionType(UserSessionType defaultUserSessionType) {
		this.defaultUserSessionType = defaultUserSessionType;
	}

	@Configurable
	public void setInternalUserSessionHolders(List<Hub<UserSession>> internalUserSessionHolders) {
		this.internalUserSessionHolders = internalUserSessionHolders;
	}

	@Override
	public void postConstruct() {

		try {
			createInternalUserSessions();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to ensure the persistence of internal user sessions", e);
		}
	}
	
	private String encodeSet(Set<String> set) {
		StringBuilder builder = new StringBuilder();
		for (String el: set) {
			if (builder.length() != 0)
				builder.append(',');
			
			builder.append(URLEncoder.encode(el, StandardCharsets.UTF_8));
		}
		
		return builder.toString();
	}
	
	private Set<String> decodeSet(String encoded) {
		
		Set<String> set = new LinkedHashSet<>();
		StringTokenizer tokenizer = new StringTokenizer(encoded, ",");
		
		while (tokenizer.hasMoreTokens()) {
			String el = tokenizer.nextToken();
			set.add(URLDecoder.decode(el, StandardCharsets.UTF_8));
		}
		
		return set;
	}
	
	private String encodeMap(Map<String, String> map) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> entry: map.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (builder.length() != 0)
				builder.append('&');
			
			builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
			builder.append('=');
			
			if (value != null)
				builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		}
		
		return builder.toString();
	}
	
	private Map<String, String> decodeMap(String encoded) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		StringTokenizer tokenizer = new StringTokenizer(encoded, "&");
		
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			
			int index = token.indexOf('=');
			
			if (index != -1) {
				String key = URLDecoder.decode(token.substring(0, index), StandardCharsets.UTF_8); 
				String value = URLDecoder.decode(token.substring(index + 1), StandardCharsets.UTF_8); 
				map.put(key, value);
			}
			else {
				String key = URLDecoder.decode(token, StandardCharsets.UTF_8);
				map.put(key, null);
			}
		}
		
		return map;
	}

	@Override
	public void preDestroy() {
		try {
			deleteInternalUserSessions();
		} catch (Exception e) {
			log.error(() -> "Failed to cleanup the internal user sessions", e);
		}
	}

	@Override
	public Maybe<UserSession> createUserSession(User user, Set<String> additionalRoles, UserSessionType type, TimeSpan maxIdleTime, TimeSpan maxAge, Date fixedExpiryDate,
			String internetAddress, Map<String, String> properties, String acquirationKey, boolean blocksAuthenticationAfterLogout) {
		if (user == null || user.getId() == null) {
			return Reasons.build(InvalidArgument.T).text("User and user id cannot be null").toMaybe();
		}
		
		log.debug(() -> "Creating a user session for user '" + user.getName() + "' connected from '" + internetAddress + "'");
		PersistenceUserSession pUserSession = createPersistenceUserSession(user, additionalRoles, type, maxIdleTime, maxAge, fixedExpiryDate, internetAddress,
				properties, acquirationKey, blocksAuthenticationAfterLogout);
		return Maybe.complete(mapToUserSession(pUserSession));
	}

	@Override
	public Maybe<UserSession> findUserSession(String sessionId) {
		log.trace(() -> "Fetching user session '" + sessionId + "'");
		Maybe<PersistenceUserSession> pUserSessionMaybe = findPersistenceUserSession(sessionId);

		if (pUserSessionMaybe.isUnsatisfied()) {
			return Maybe.empty(pUserSessionMaybe.whyUnsatisfied());
		}

		PersistenceUserSession pUserSession = pUserSessionMaybe.get();

		UserSession userSession = mapToUserSession(pUserSession);
		log.trace(() -> "Found user session '" + sessionId + "'; Returning: " + userSession);

		return Maybe.complete(userSession);
	}

	@Override
	public Maybe<UserSession> findUserSessionByAcquirationKey(String acquirationKey) {
		log.trace(() -> "Fetching user session by acquiration key '" + acquirationKey + "'");
		Maybe<PersistenceUserSession> pUserSessionMaybe = findPersistenceUserSessionByAcquirationKey(acquirationKey);

		if (pUserSessionMaybe.isUnsatisfied()) {
			return Maybe.empty(pUserSessionMaybe.whyUnsatisfied());
		}

		PersistenceUserSession pUserSession = pUserSessionMaybe.get();

		if (pUserSession.getClosed() && pUserSession.getBlocksAuthenticationAfterLogout())
			return Reasons.build(InvalidCredentials.T).text("Credentials where already logged out").toMaybe();

		UserSession userSession = mapToUserSession(pUserSession);

		log.trace(() -> "Found user session by acquirationKey '" + acquirationKey + "'; Returning: " + userSession);

		return Maybe.complete(userSession);
	}

	@Override
	public Maybe<DeletedSessionInfo> deleteUserSession(String sessionId) {
		log.debug(() -> "Deleting user session '" + sessionId + "'");

		Maybe<PersistenceUserSession> pUserSessionMaybe = findPersistenceUserSession(sessionId);

		if (pUserSessionMaybe.isUnsatisfied()) {
			return pUserSessionMaybe.whyUnsatisfied().asMaybe();
		}

		PersistenceUserSession pUserSession = pUserSessionMaybe.get();

		String acquirationKey = pUserSession.getAcquirationKey();

		if (acquirationKey != null && pUserSession.getBlocksAuthenticationAfterLogout()) {
			closePersistenceUserSession(pUserSession);
		} else {
			deletePersistenceUserSession(pUserSession);
		}

		DeletedSessionInfo info = new DeletedSessionInfo() {
			UserSession userSession = null;

			@Override
			public UserSession userSession() {
				if (userSession == null)
					userSession = mapToUserSession(pUserSession);

				return userSession;
			}

			@Override
			public String acquirationKey() {
				return pUserSession.getAcquirationKey();
			}
		};

		return Maybe.complete(info);
	}

	protected void createInternalUserSessions() {
		if (internalUserSessionHolders == null || internalUserSessionHolders.isEmpty()) {
			log.warn(() -> "Skipping internal user sessions persistence; Internal user session holder list was not configured or is empty");
			return;
		}
		for (Hub<UserSession> userSessionHolder : internalUserSessionHolders) {
			UserSession userSession = createInternalUserSession(userSessionHolder);
			userSessionHolder.accept(userSession);
		}
	}

	private UserSession createInternalUserSession(Hub<UserSession> userSessionHolder) throws RuntimeException, GmSessionException {
		UserSession userSession = userSessionHolder.get();

		PersistenceUserSession pUserSession = createPersistenceUserSession(userSession.getUser(), Collections.emptySet(), UserSessionType.internal, null, null, null,
				userSession.getCreationInternetAddress(), userSession.getProperties(), null, false);

		return mapToUserSession(pUserSession);
	}

	protected void deleteInternalUserSessions() {
		if (this.internalUserSessionHolders == null || this.internalUserSessionHolders.isEmpty()) {
			log.warn(() -> "Skipping internal user sessions cleanup; Internal user session holder list was not configured or is empty");
			return;
		}
		for (Hub<UserSession> userSessionHolder : this.internalUserSessionHolders) {
			deleteInternalUserSession(userSessionHolder);
		}
	}

	private void deleteInternalUserSession(Hub<UserSession> userSessionHolder) {
		UserSession userSession = userSessionHolder.get();
		if (userSession == null) {
			return;
		}
		deletePersistenceUserSession(userSession.getSessionId());
	}

	protected String generateSessionId(UserSessionType userSessionType) {
		String sessionId = null;
		try {
			sessionId = sessionIdProvider.apply(userSessionType);
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to obtain the session id from the session id provider");
		}
		if (sessionId == null) {
			throw new IllegalStateException("null session id was returned from the session id provider");
		}
		return sessionId;
	}

	protected void touchPersistenceUserSessionLocally(PersistenceUserSession pUserSession) {
		pUserSession.setLastAccessedDate(new Date());
		pUserSession.setExpiryDate(calculateExpiryDate(pUserSession.getLastAccessedDate(), pUserSession.getMaxIdleTime()));
		if (pUserSession.getExpiryDate() != null) {
			if (pUserSession.getFixedExpiryDate() != null && pUserSession.getFixedExpiryDate().before(pUserSession.getExpiryDate())) {
				pUserSession.setExpiryDate(pUserSession.getFixedExpiryDate());
			}
		} else {
			pUserSession.setExpiryDate(pUserSession.getFixedExpiryDate());
		}
	}

	private UserSession mapToUserSession(PersistenceUserSession pUserSession) {
		UserSession userSession = UserSession.T.create();
		userSession.setSessionId(pUserSession.getId());
		userSession.setCreationDate(pUserSession.getCreationDate());
		userSession.setFixedExpiryDate(pUserSession.getFixedExpiryDate());
		userSession.setExpiryDate(pUserSession.getExpiryDate());
		userSession.setLastAccessedDate(pUserSession.getLastAccessedDate());
		userSession.setEffectiveRoles(decodeSet(pUserSession.getEffectiveRoles()));
		if (pUserSession.getSessionType() != null) {
			userSession.setType(UserSessionType.valueOf(pUserSession.getSessionType()));
		}
		userSession.setCreationInternetAddress(pUserSession.getCreationInternetAddress());
		userSession.setCreationNodeId(pUserSession.getCreationNodeId());
		userSession.setProperties(decodeMap(pUserSession.getProperties()));
		if (pUserSession.getMaxIdleTime() != null) {
			userSession.setMaxIdleTime(TimeSpan.fromMillies(pUserSession.getMaxIdleTime()));
		}
		User user = User.T.create();
		user.setId(pUserSession.getUserName());
		user.setName(pUserSession.getUserName());
		user.setFirstName(pUserSession.getUserFirstName());
		user.setLastName(pUserSession.getUserLastName());
		user.setEmail(pUserSession.getUserEmail());
		user.getRoles().addAll(userSession.getEffectiveRoles().stream().map(r -> {
			Role role = Role.T.create();
			role.setName(r);
			return role;
		}).toList());
		userSession.setUser(user);

		return userSession;
	}

	protected Date calculateExpiryDate(Date pivot, TimeSpan ts) {
		if (ts == null) {
			return null;
		}
		return new Date(pivot.getTime() + ts.toLongMillies());
	}

	protected Date calculateExpiryDate(Date pivot, Long millis) {
		if (millis == null) {
			return null;
		}
		return new Date(pivot.getTime() + millis);
	}

	protected PersistenceUserSession initPersistenceUserSession(PersistenceUserSession pUserSession, User user, Set<String> additionalRoles, TimeSpan maxIdleTime, TimeSpan maxAge,
			Date fixedExpiryDate, String internetAddress, Map<String, String> properties, String acquirationKey,
			boolean blocksAuthenticationAfterLogout, UserSessionType userSessionType, Date now) {
		pUserSession.setId(generateSessionId(userSessionType));
		pUserSession.setAcquirationKey(acquirationKey);
		pUserSession.setBlocksAuthenticationAfterLogout(blocksAuthenticationAfterLogout);
		pUserSession.setCreationDate(now);
		pUserSession.setFixedExpiryDate(fixedExpiryDate);
		Set<String> effectiveRoles = Roles.userEffectiveRoles(user);
		effectiveRoles.addAll(additionalRoles);
		pUserSession.setEffectiveRoles(encodeSet(effectiveRoles));
		pUserSession.setSessionType(userSessionType.toString());
		pUserSession.setCreationInternetAddress(internetAddress);
		pUserSession.setCreationNodeId(nodeId);
		pUserSession.setProperties(encodeMap(properties));
		if (maxAge != null && pUserSession.getFixedExpiryDate() == null) {
			pUserSession.setFixedExpiryDate(calculateExpiryDate(now, maxAge));
		}
		if (maxIdleTime != null) {
			pUserSession.setMaxIdleTime((long)maxIdleTime.toMillies());
		} else {
			if (!userSessionType.equals(UserSessionType.internal)) {
				pUserSession.setMaxIdleTime(this.defaultUserSessionMaxIdleTime.toLongMillies());
			}
		}
		touchPersistenceUserSessionLocally(pUserSession);

		pUserSession.setUserName(user.getName());
		pUserSession.setUserFirstName(user.getFirstName());
		pUserSession.setUserLastName(user.getLastName());
		pUserSession.setUserEmail(user.getEmail());
		return pUserSession;
	}

}