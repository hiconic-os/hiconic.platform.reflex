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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import com.braintribe.cfg.Required;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.SessionNotFound;
import com.braintribe.gm.model.usersession.PersistenceUserSession;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.securityservice.api.exceptions.SecurityServiceException;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSessionType;

public class JdbcUserSessionService extends AbstractUserSessionService {

	DataSource dataSource;

	// @formatter:off
	private static final String CREATE_PERSISTENCE_USER_SESSION_STMT = //
			"INSERT INTO TF_US_PERSISTENCE_USER_SESSION (" +
				"ID, "+
				"USER_NAME, USER_FIRST_NAME, USER_LAST_NAME, USER_EMAIL, " +
				"CREATION_DATE, FIXED_EXPIRY_DATE, EXPIRY_DATE, LAST_ACCESSED_DATE, " +
				"MAX_IDLE_TIME, EFFECTIVE_ROLES, SESSION_TYPE, CREATION_INTERNET_ADDRESS, CREATION_NODE_ID, PROPERTIES, "+
				"ACQUIRATION_KEY, BLOCKS_AUTHENTICATION_AFTER_LOGOUT" +
			") VALUES ("+
				"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"+
			")";
	// @formatter:on
	private static final String FIND_PERSISTENCE_USER_SESSION_STMT = "SELECT * FROM TF_US_PERSISTENCE_USER_SESSION WHERE ID = ?";
	private static final String FIND_PERSISTENCE_USER_SESSION_BY_ACQKEY_STMT = "SELECT * FROM TF_US_PERSISTENCE_USER_SESSION WHERE ACQUIRATION_KEY = ? ORDER BY CREATION_DATE DESC";
	private static final String TOUCH_PERSISTENCE_USER_SESSION_STMT = "UPDATE TF_US_PERSISTENCE_USER_SESSION SET LAST_ACCESSED_DATE = ?, EXPIRY_DATE = ? WHERE ID = ?";
	private static final String DELETE_PERSISTENCE_USER_SESSION_STMT = "DELETE FROM TF_US_PERSISTENCE_USER_SESSION WHERE ID = ?";
	private static final String CLOSE_PERSISTENCE_USER_SESSION_STMT = "UPDATE TF_US_PERSISTENCE_USER_SESSION SET CLOSED = ?, EXPIRY_DATE = ? WHERE ID = ?";

	static final Logger log = Logger.getLogger(JdbcUserSessionService.class);

	@Required
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	protected Connection openJdbcConnection() throws SecurityServiceException {
		try {
			return dataSource.getConnection();
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to obtain the JDBC connection from the provider");
		}
	}

	@Override
	protected PersistenceUserSession createPersistenceUserSession(User user, UserSessionType type, TimeSpan maxIdleTime, TimeSpan maxAge,
			Date fixedExpiryDate, String internetAddress, Map<String, String> properties, String acquirationKey,
			boolean blocksAuthenticationAfterLogout) {
		UserSessionType userSessionType = type != null ? type : this.defaultUserSessionType;
		Date now = new Date();

		PersistenceUserSession pUserSession = initPersistenceUserSession(PersistenceUserSession.T.create(), user, maxIdleTime, maxAge,
				fixedExpiryDate, internetAddress, properties, acquirationKey, blocksAuthenticationAfterLogout, userSessionType, now);

		try (Connection conn = openJdbcConnection(); PreparedStatement stmt = conn.prepareStatement(CREATE_PERSISTENCE_USER_SESSION_STMT)) {
			stmt.setString(1, pUserSession.getId());
			stmt.setString(2, pUserSession.getUserName());
			stmt.setString(3, pUserSession.getUserFirstName());
			stmt.setString(4, pUserSession.getUserLastName());
			stmt.setString(5, pUserSession.getUserEmail());
			stmt.setTimestamp(6, new Timestamp(pUserSession.getCreationDate().getTime()));
			stmt.setTimestamp(7, pUserSession.getFixedExpiryDate() != null ? new Timestamp(pUserSession.getFixedExpiryDate().getTime()) : null);
			stmt.setTimestamp(8, pUserSession.getExpiryDate() != null ? new Timestamp(pUserSession.getExpiryDate().getTime()) : null);
			stmt.setTimestamp(9, new Timestamp(pUserSession.getLastAccessedDate().getTime()));
			if (pUserSession.getMaxIdleTime() != null) {
				stmt.setLong(10, pUserSession.getMaxIdleTime());
			} else {
				stmt.setNull(10, Types.BIGINT);
			}
			stmt.setString(11, pUserSession.getEffectiveRoles());
			stmt.setString(12, pUserSession.getSessionType());
			stmt.setString(13, pUserSession.getCreationInternetAddress());
			stmt.setString(14, pUserSession.getCreationNodeId());
			stmt.setString(15, pUserSession.getProperties());
			stmt.setString(16, pUserSession.getAcquirationKey());
			stmt.setBoolean(17, pUserSession.getBlocksAuthenticationAfterLogout());

			stmt.execute();
		} catch (Exception e) {
			log.error("Error while trying to persist session " + pUserSession + " in the DB.", e);
			throw Exceptions.unchecked(e, "Failed to create a user session for user '" + user.getName() + "'");
		}

		return pUserSession;
	}

	@Override
	protected Maybe<PersistenceUserSession> findPersistenceUserSession(String sessionId) {
		PersistenceUserSession pUserSession;
		try (Connection conn = openJdbcConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(FIND_PERSISTENCE_USER_SESSION_STMT)) {
				stmt.setString(1, sessionId);
				pUserSession = mapQueryResultToPersistenceUserSession(stmt.executeQuery());
			}

			if (pUserSession == null)
				return Reasons.build(SessionNotFound.T).text("User session '" + sessionId + "' not found").toMaybe();
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to query user session '" + sessionId + "'");
		}
		return Maybe.complete(pUserSession);
	}

	// TODO: how to select from ambiguity
	@Override
	protected Maybe<PersistenceUserSession> findPersistenceUserSessionByAcquirationKey(String acquirationKey) {
		PersistenceUserSession pUserSession;
		try (Connection conn = openJdbcConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(FIND_PERSISTENCE_USER_SESSION_BY_ACQKEY_STMT)) {
				stmt.setString(1, acquirationKey);

				try (ResultSet resultSet = stmt.executeQuery()) {
					pUserSession = mapQueryResultToPersistenceUserSession(resultSet);
				}

				if (pUserSession == null)
					return Reasons.build(SessionNotFound.T).text("User session with acquiration key '" + acquirationKey + "' not found").toMaybe();
			}
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to query user with acquiration key '" + acquirationKey + "'");
		}
		return Maybe.complete(pUserSession);
	}

	private PersistenceUserSession mapQueryResultToPersistenceUserSession(ResultSet result) throws SQLException {
		if (!result.next()) {
			return null;
		}
		PersistenceUserSession pUserSession = PersistenceUserSession.T.create();
		pUserSession.setId(result.getString("ID"));
		pUserSession.setUserName(result.getString("USER_NAME"));
		pUserSession.setUserFirstName(result.getString("USER_FIRST_NAME"));
		pUserSession.setUserLastName(result.getString("USER_LAST_NAME"));
		pUserSession.setUserEmail(result.getString("USER_EMAIL"));
		Timestamp creationDate = result.getTimestamp("CREATION_DATE");
		pUserSession.setCreationDate(creationDate != null ? new Date(creationDate.getTime()) : null);
		Timestamp fixedExpiryDate = result.getTimestamp("FIXED_EXPIRY_DATE");
		pUserSession.setFixedExpiryDate(fixedExpiryDate != null ? new Date(fixedExpiryDate.getTime()) : null);
		Timestamp expiryDate = result.getTimestamp("EXPIRY_DATE");
		pUserSession.setExpiryDate(expiryDate != null ? new Date(expiryDate.getTime()) : null);
		Timestamp lastAccessedDate = result.getTimestamp("LAST_ACCESSED_DATE");
		pUserSession.setLastAccessedDate(lastAccessedDate != null ? new Date(lastAccessedDate.getTime()) : null);
		Long maxIdleTime = result.getLong("MAX_IDLE_TIME");
		pUserSession.setMaxIdleTime(maxIdleTime == 0 ? null : maxIdleTime);
		pUserSession.setEffectiveRoles(result.getString("EFFECTIVE_ROLES"));
		pUserSession.setSessionType(result.getString("SESSION_TYPE"));
		pUserSession.setCreationInternetAddress(result.getString("CREATION_INTERNET_ADDRESS"));
		pUserSession.setCreationNodeId(result.getString("CREATION_NODE_ID"));
		pUserSession.setProperties(result.getString("PROPERTIES"));
		pUserSession.setAcquirationKey(result.getString("ACQUIRATION_KEY"));
		pUserSession.setBlocksAuthenticationAfterLogout(result.getBoolean("BLOCKS_AUTHENTICATION_AFTER_LOGOUT"));

		return pUserSession;
	}

	@Override
	public void touchUserSession(String sessionId, Date lastAccessDate, Date expiryDate) {
		try (Connection conn = openJdbcConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(TOUCH_PERSISTENCE_USER_SESSION_STMT)) {
				Timestamp expiryTimestamp = expiryDate != null ? new Timestamp(expiryDate.getTime()) : null;
				stmt.setTimestamp(1, new Timestamp(lastAccessDate.getTime()));
				stmt.setTimestamp(2, expiryTimestamp);
				stmt.setString(3, sessionId);
				stmt.executeUpdate();
			}
		} catch (Exception e) {
			log.error("Could not touch PersistenceUserSession with id: " + sessionId);
		}
	}

	@Override
	protected void deletePersistenceUserSession(PersistenceUserSession pUserSession) {
		String sessionId = pUserSession.getId();
		deletePersistenceUserSession(sessionId);
	}

	@Override
	protected void closePersistenceUserSession(PersistenceUserSession pUserSession) {
		String sessionId = pUserSession.getId();
		closePersistenceUserSession(sessionId);
	}

	@Override
	protected void closePersistenceUserSession(String sessionId) {
		try (Connection conn = openJdbcConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(CLOSE_PERSISTENCE_USER_SESSION_STMT)) {
				stmt.setBoolean(1, true);
				stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				stmt.setString(3, sessionId);
				stmt.executeUpdate();
			}
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to close user session '" + sessionId + "'");
		}
	}

	@Override
	protected void deletePersistenceUserSession(String sessionId) {
		try (Connection conn = openJdbcConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(DELETE_PERSISTENCE_USER_SESSION_STMT)) {
				stmt.setString(1, sessionId);
				stmt.executeUpdate();
			}
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Failed to delete user session '" + sessionId + "'");
		}
	}

}
