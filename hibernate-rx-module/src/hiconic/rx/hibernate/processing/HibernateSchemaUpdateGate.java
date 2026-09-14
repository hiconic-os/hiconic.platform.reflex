// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.hibernate.processing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.sql.DataSource;

import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.deployment.hibernate.mapping.SourceDescriptor;
import com.braintribe.model.processing.lock.api.Locking;

/**
 * Serializes Hibernate schema updates across application nodes and remembers the fingerprint of the last successfully applied mapping bundle.
 * <p>
 * The technical table name and layout deliberately remain compatible with the CX schema-update mechanism. RX uses a hash of the configured model
 * and physical schema identity as {@code ACCESS_ID}; the readable identity is retained in {@code CONTEXT} for diagnostics.
 */
final class HibernateSchemaUpdateGate {

	private static final Logger log = Logger.getLogger(HibernateSchemaUpdateGate.class);

	static final String TABLE_NAME = "TF_SCHEMA_UPDATE_TMP";
	private static final String NOT_INITIALIZED = "notInitialized";
	private static final String LOCK_NAMESPACE = "rx-hibernate-schema-update";

	private final DataSource dataSource;
	private final Locking locking;
	private final String schemaIdentity;
	private final String schemaKey;
	private final String physicalSchemaKey;
	private final String mappingFingerprint;
	private final String instanceId;

	HibernateSchemaUpdateGate(DataSource dataSource, Locking locking, String schemaIdentity, String physicalSchemaIdentity,
			String mappingFingerprint, String instanceId) {
		this.dataSource = dataSource;
		this.locking = locking;
		this.schemaIdentity = schemaIdentity;
		this.schemaKey = sha256(schemaIdentity);
		this.physicalSchemaKey = sha256(physicalSchemaIdentity);
		this.mappingFingerprint = mappingFingerprint;
		this.instanceId = instanceId == null ? "unknown" : instanceId;
	}

	Decision decide() {
		ensureTable();

		Lock lock = locking.forIdentifier(LOCK_NAMESPACE, "schema:" + physicalSchemaKey).writeLock();
		lock.lock();
		try {
			StoredState state = readOrCreateState();
			boolean updateRequired = !mappingFingerprint.equals(state.fingerprint());
			if (!updateRequired) {
				lock.unlock();
				log.info("Skipping Hibernate schema update for [" + schemaIdentity + "]; mapping fingerprint is unchanged: " + mappingFingerprint);
				return Decision.noUpdate();
			}

			log.info("Hibernate schema update required for [" + schemaIdentity + "]; stored fingerprint [" + state.fingerprint()
					+ "], current fingerprint [" + mappingFingerprint + "]");
			return new Decision(this, lock);
		} catch (RuntimeException e) {
			lock.unlock();
			throw e;
		}
	}

	private void ensureTable() {
		Lock lock = locking.forIdentifier(LOCK_NAMESPACE, "table:" + TABLE_NAME).writeLock();
		lock.lock();
		try (Connection connection = dataSource.getConnection()) {
			if (tableExists(connection))
				return;

			connection.setAutoCommit(false);
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("CREATE TABLE " + TABLE_NAME + " ("
						+ "ACCESS_ID varchar(255) primary key,"
						+ "HASH varchar(255) not null,"
						+ "ERROR_COUNT integer not null,"
						+ "INSTANCE_ID varchar(255) not null,"
						+ "CONTEXT varchar(255) not null)");
			}
			connection.commit();
		} catch (SQLException e) {
			throw Exceptions.unchecked(e, "Could not ensure Hibernate schema-update table " + TABLE_NAME);
		} finally {
			lock.unlock();
		}
	}

	private boolean tableExists(Connection connection) throws SQLException {
		try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[] { "TABLE" })) {
			while (tables.next())
				if (TABLE_NAME.equalsIgnoreCase(tables.getString("TABLE_NAME")))
					return true;
		}
		return false;
	}

	private StoredState readOrCreateState() {
		try (Connection connection = dataSource.getConnection()) {
			StoredState state = readState(connection);
			if (state != null)
				return state;

			connection.setAutoCommit(false);
			try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + TABLE_NAME
					+ " (ACCESS_ID, HASH, ERROR_COUNT, INSTANCE_ID, CONTEXT) VALUES (?, ?, ?, ?, ?)")) {
				insert.setString(1, schemaKey);
				insert.setString(2, NOT_INITIALIZED);
				insert.setInt(3, 0);
				insert.setString(4, clipped(instanceId));
				insert.setString(5, clipped(schemaIdentity));
				insert.executeUpdate();
			}
			connection.commit();
			return new StoredState(NOT_INITIALIZED, 0);
		} catch (SQLException e) {
			throw Exceptions.unchecked(e, "Could not read or create Hibernate schema-update state for " + schemaIdentity);
		}
	}

	private StoredState readState(Connection connection) throws SQLException {
		try (PreparedStatement query = connection.prepareStatement(
				"SELECT HASH, ERROR_COUNT FROM " + TABLE_NAME + " WHERE ACCESS_ID=?")) {
			query.setString(1, schemaKey);
			try (ResultSet result = query.executeQuery()) {
				if (!result.next())
					return null;
				StoredState state = new StoredState(result.getString(1), result.getInt(2));
				if (result.next())
					throw new IllegalStateException("Multiple Hibernate schema-update rows found for key " + schemaKey);
				return state;
			}
		}
	}

	private void storeSuccess() {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement update = connection.prepareStatement("UPDATE " + TABLE_NAME
					+ " SET HASH=?, ERROR_COUNT=0, INSTANCE_ID=?, CONTEXT=? WHERE ACCESS_ID=?")) {
				update.setString(1, mappingFingerprint);
				update.setString(2, clipped(instanceId));
				update.setString(3, clipped(schemaIdentity));
				update.setString(4, schemaKey);
				if (update.executeUpdate() != 1)
					throw new IllegalStateException("Hibernate schema-update state disappeared for key " + schemaKey);
			}
			connection.commit();
		} catch (SQLException e) {
			throw Exceptions.unchecked(e, "Could not store successful Hibernate schema-update state for " + schemaIdentity);
		}
	}

	private void recordFailure() {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement update = connection.prepareStatement("UPDATE " + TABLE_NAME
					+ " SET ERROR_COUNT=ERROR_COUNT+1, INSTANCE_ID=?, CONTEXT=? WHERE ACCESS_ID=?")) {
				update.setString(1, clipped(instanceId));
				update.setString(2, clipped(schemaIdentity));
				update.setString(3, schemaKey);
				update.executeUpdate();
			}
			connection.commit();
		} catch (SQLException e) {
			log.error("Could not record failed Hibernate schema update for [" + schemaIdentity + "]", e);
		}
	}

	static String fingerprint(List<SourceDescriptor> mappings) {
		MessageDigest digest = messageDigest();
		for (SourceDescriptor mapping : mappings.stream().sorted((a, b) -> a.sourceRelativePath.compareTo(b.sourceRelativePath)).toList()) {
			update(digest, mapping.sourceRelativePath);
			update(digest, mapping.sourceCode);
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void update(MessageDigest digest, String value) {
		digest.update(value.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
	}

	private static String sha256(String value) {
		MessageDigest digest = messageDigest();
		digest.update(value.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest.digest());
	}

	private static MessageDigest messageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	private static String clipped(String value) {
		return value.length() <= 255 ? value : value.substring(0, 255);
	}

	private record StoredState(String fingerprint, int errorCount) {
		// errorCount is retained for CX-compatible diagnostics.
	}

	static final class Decision implements AutoCloseable {
		private final HibernateSchemaUpdateGate gate;
		private final Lock lock;
		private boolean successful;
		private boolean closed;

		private Decision(HibernateSchemaUpdateGate gate, Lock lock) {
			this.gate = gate;
			this.lock = lock;
		}

		private static Decision noUpdate() {
			Decision result = new Decision(null, null);
			result.successful = true;
			return result;
		}

		boolean updateRequired() {
			return gate != null;
		}

		void markSuccessful() {
			if (gate == null)
				return;
			gate.storeSuccess();
			successful = true;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			if (gate != null) {
				try {
					if (!successful)
						gate.recordFailure();
				} finally {
					lock.unlock();
				}
			}
		}
	}
}
