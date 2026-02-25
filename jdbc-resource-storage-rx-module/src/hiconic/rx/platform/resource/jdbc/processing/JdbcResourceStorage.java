package hiconic.rx.platform.resource.jdbc.processing;

import static com.braintribe.util.jdbc.JdbcTools.thoroughlyCloseJdbcConnection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.function.CheckedBiFunction;
import com.braintribe.common.lcd.function.CheckedConsumer;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resource.source.SqlSource;
import com.braintribe.model.resourceapi.stream.range.StreamRange;
import com.braintribe.util.jdbc.JdbcTools;
import com.braintribe.util.jdbc.dialect.JdbcDialect;
import com.braintribe.utils.lcd.LazyInitialization;
import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.utils.stream.RangeInputStream;

import hiconic.rx.module.api.resource.AbstractResourceStorage;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.ExistingResourcePayloadRequest;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;

/**
 * JDBC-based resource storage that persists binary data as BLOBs in a database table.
 * <p>
 * Migrated from {@code JdbcSqlBinaryProcessor} to the new resource storage framework.
 *
 * @see SqlSource
 *
 * @author peter.gazdik
 */
public class JdbcResourceStorage extends AbstractResourceStorage<String> implements InitializationAware {

	// configurable
	private DataSource dataSource;
	private String tableName = "hc_resources";
	private String idColumnName = "id";
	private String blobColumnName = "data";

	// computed
	private String insertSql;
	private String selectSql;
	private String deleteSql;

	// Marks that getBinaryStream(position, length) isn't supported by JDBC driver; only set if
	// SQLFeatureNotSupportedException is thrown
	private volatile Boolean rangedBinaryStreamNotSupported;

	// @formatter:off
	@Required public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }

	@Configurable public void setTableName(String tableName) { this.tableName = NullSafe.get(tableName, this.tableName); }
	@Configurable public void setIdColumnName(String idColumnName) { this.idColumnName = NullSafe.get(idColumnName, this.idColumnName); }
	@Configurable public void setBlobColumnName(String blobColumnName) { this.blobColumnName = NullSafe.get(blobColumnName, this.blobColumnName); }
	// @formatter:on

	// ######################################################
	// ## . . . . . . . . Initialization . . . . . . . . . ##
	// ######################################################

	@Override
	public void postConstruct() {
		insertSql = "insert into " + tableName + " (" + idColumnName + ", " + blobColumnName + ") values (?,?)";
		selectSql = "select " + blobColumnName + " from " + tableName + " where " + idColumnName + " = ?";
		deleteSql = "delete from " + tableName + " where " + idColumnName + " = ?";
	}

	private final LazyInitialization ensureTable = new LazyInitialization(this::ensureTableExists);

	private void ensureTableExists() {
		JdbcDialect dialect = JdbcDialect.detectDialect(dataSource);
		log.debug(() -> logPrefix() + "Detected dialect: " + dialect.hibernateDialect());

		withInitializedConnection(this::doThis, c -> ensureTableExists(c, dialect), "ensuring binary persistence table");
	}

	private Object doThis(Connection c, CheckedConsumer<Connection, Exception> code) throws Exception {
		code.accept(c);
		return null;
	}

	private void ensureTableExists(Connection connection, JdbcDialect dialect) throws Exception {
		if (resourcesTableExists(connection))
			return;

		String createSql = "create table " + tableName + " (" + //
				idColumnName + " varchar(255) primary key not null, " + //
				blobColumnName + " " + dialect.blobType() + //
				")";

		log.debug(() -> logPrefix() + "Creating resource storage table: " + createSql);

		try (Statement st = connection.createStatement()) {
			log.debug(() -> "Creating table with statement: " + createSql);
			st.executeUpdate(createSql);
			log.debug(() -> "Successfully created table " + tableName);

		} catch (SQLException e) {
			if (!resourcesTableExists(connection))
				throw e;
		}

		connection.commit();
		log.debug(() -> logPrefix() + "Successfully created table: " + tableName);
	}

	private boolean resourcesTableExists(Connection connection) {
		return JdbcTools.tableExists(connection, tableName) != null;
	}

	// ######################################################
	// ## . . . . . . . Resolve Payload . . . . . . . . . .##
	// ######################################################

	@Override
	protected Maybe<String> resolvePayload(GetResourcePayload  request) {
		return resolvePayloadInternal(request);
	}

	private Maybe<String> resolvePayloadInternal(ExistingResourcePayloadRequest  request) {
		ResourceSource source = request.getResourceSource();
		if (!(source instanceof SqlSource))
			return error(InvalidArgument.T, "Resource source is not a SqlSource, but: " + source.entityType().getTypeSignature());

		String blobId = ((SqlSource) source).getBlobId();
		if (blobId == null)
			return error(InvalidArgument.T, "BlobId is null for resource source: " + source);

		return Maybe.complete(blobId);
	}

	// ######################################################
	// ## . . . . . . . . Get Payload . . . . . . . . . . .##
	// ######################################################

	@Override
	protected Maybe<GetResourcePayloadResponse> getPayload(String blobId, GetResourcePayload request, GetResourcePayloadResponse response) {
		return withConnection(connection -> {
			String id = blobId;
			Blob blob = queryBlob(connection, id);
			if (blob == null)
				return error(NotFound.T, "No resource found for id: " + id);

			StreamRange range = resolveRange(request, blob);

			Supplier<InputStream> newConnectionCreatingIss = () -> openDbConnectionAndGetInputStream(id, range);

			// set response and cut stream
			addRangeDataToResponseIfNeeded(range, response, blob);

			Resource responseResource = Resource.createTransient(newConnectionCreatingIss::get);
			responseResource.setFileSize(streamLength(range));
			response.setResource(responseResource);

			return Maybe.complete(response);

		}, "get resource");

	}
	private Long streamLength(StreamRange range) {
		// Note the cast to Long is important!
		return range != null ? (Long) (range.getEnd() - range.getStart() + 1) : null;
	}

	private StreamRange resolveRange(GetResourcePayload request, Blob blob) {
		StreamRange range = request.getRange();
		if (range == null)
			return null;

		Long start = range.getStart();
		if (start == null)
			return null;

		Long end = range.getEnd();
		try {
			if (end == null || end < start || end >= blob.length())
				end = blob.length() - 1;
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not get the length of BLOB " + blob);
		}

		return StreamRange.create(start, end);
	}

	private InputStream openDbConnectionAndGetInputStream(String id, StreamRange range) {
		Connection c = null;

		try {
			Connection connection = c = dataSource.getConnection();
			connection.setAutoCommit(false);

			try {
				Blob blob = queryBlob(connection, id);
				if (blob == null)
					throw new IllegalStateException("SQL Resource no longer exists: " + id);

				try {
					return new BufferedInputStream(resolveBlobStream(range, blob)) {
						@Override
						public void close() throws IOException {
							try {
								super.close();
							} finally {
								closeConnection(connection, id, true); // happy path, we commit
								log.trace(() -> logPrefix() + "Closed connection on InputStream.close(), resource: " + id);
							}
						}
					};

				} catch (Exception e) {
					closeConnection(connection, id, false); // error happened, no commit
					throw Exceptions.unchecked(e, logPrefix() + "Failed to open blob data input stream of resource: " + id);
				}

			} catch (Exception e) {
				connection.rollback();
				throw e;
			}

		} catch (SQLException e) {
			closeConnection(c, id, false);
			throw Exceptions.unchecked(e, logPrefix() + "Error while opening input stream for resource: " + id);
		}
	}

	private void closeConnection(Connection connection, String id, boolean commit) {
		thoroughlyCloseJdbcConnection(connection, commit, () -> logPrefix() + "Associated with resource: " + id);
	}

	private void addRangeDataToResponseIfNeeded(StreamRange range, GetResourcePayloadResponse response, Blob blob) {
		if (range != null) {
			try {
				response.setRanged(true);
				response.setRangeStart(range.getStart());
				response.setRangeEnd(range.getEnd());
				response.setSize(blob.length());
			} catch (Exception e) {
				throw new RuntimeException(logPrefix() + "Could not rangify stream according to " + range.getStart() + "-" + range.getEnd(), e);
			}
		}
	}

	private InputStream resolveBlobStream(StreamRange range, Blob blob) throws SQLException {
		if (range == null || isFullRange(range, blob))
			return blob.getBinaryStream();

		if (rangedBinaryStreamNotSupported == Boolean.TRUE)
			return resolveRangeBlobStream(blob, range);

		try {
			long len = range.getEnd() - range.getStart() + 1;
			return blob.getBinaryStream(range.getStart() + 1, len);

		} catch (SQLFeatureNotSupportedException e) {
			rangedBinaryStreamNotSupported = Boolean.TRUE;
			return resolveRangeBlobStream(blob, range);
		}
	}

	private boolean isFullRange(StreamRange range, Blob blob) throws SQLException {
		return range.getStart() == 0 && range.getEnd() == blob.length() - 1;
	}

	private InputStream resolveRangeBlobStream(Blob blob, StreamRange range) throws SQLException {
		try {
			return new RangeInputStream(blob.getBinaryStream(), range.getStart(), range.getEnd() + 1);
		} catch (IOException e) {
			throw new UncheckedIOException("This JDBC driver does not support getBinaryStream(pos, lenght) and an workaround failed too.", e);
		}
	}

	// ######################################################
	// ## . . . . . . . Store Payload . . . . . . . . . . .##
	// ######################################################

	@Override
	protected Maybe<StoreResourcePayloadResponse> storePayload(StoreResourcePayload request) {
		return withConnection(this::store, request, "store resource");

	}

	private Maybe<StoreResourcePayloadResponse> store(Connection connection, StoreResourcePayload request) throws Exception {
		Resource resource = request.getData();

		String id = insertToDb(connection, resource);

		SqlSource resourceSource = SqlSource.T.create();
		resourceSource.setBlobId(id);
		resourceSource.setUseCase(request.getUseCase());

		StoreResourcePayloadResponse response = StoreResourcePayloadResponse.T.create();
		response.setResourceSource(resourceSource);

		return Maybe.complete(response);
	}

	private String insertToDb(Connection connection, Resource resource) throws Exception {
		try (InputStream inputStream = resource.openStream()) {
			String id = UUID.randomUUID().toString();

			try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
				ps.setString(1, id);
				ps.setBlob(2, inputStream);

				int updated = ps.executeUpdate();
				if (updated != 1)
					throw new Exception(
							"The execution of '" + insertSql + "' did not change anything in the DB (result is " + updated + ", expected 1)");
			}

			return id;
		}
	}

	// ######################################################
	// ## . . . . . . . Delete Payload . . . . . . . . . . ##
	// ######################################################

	@Override
	protected Maybe<DeleteResourcePayloadResponse> deletePayload(DeleteResourcePayload request) {
		return withConnection(this::delete, request, "delete");
	}

	private Maybe<DeleteResourcePayloadResponse> delete(Connection connection, DeleteResourcePayload request) throws SQLException {
		Maybe<String> blobIdMaybe = resolvePayloadInternal(request);
		if (blobIdMaybe.isUnsatisfied())
			return blobIdMaybe.propagateReason();

		String blobId = blobIdMaybe.get();
		boolean deleted = false;

		try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
			ps.setString(1, blobId);

			int count = ps.executeUpdate();
			connection.commit();

			deleted = count > 0;

			if (count == 1)
				log.trace(() -> logPrefix() + "Deleted resource: " + blobId);
			else
				log.debug(() -> logPrefix() + "Attempt to delete resource '" + blobId + "' did not delete one entry, but: " + count);
		}

		DeleteResourcePayloadResponse response = DeleteResourcePayloadResponse.T.create();
		response.setDeleted(deleted);
		return Maybe.complete(response);
	}

	// ######################################################
	// ## . . . . . . . . . Helpers . . . . . . . . . . . .##
	// ######################################################

	// must not return null
	private Blob queryBlob(Connection connection, String id) {
		try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
			ps.setString(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;

				Blob blob = rs.getBlob(1);

				if (rs.next())
					log.warn("");

				return blob;
			}
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not query for BLOB with id " + id);
		}
	}

	private <R, ARG> R withConnection(CheckedBiFunction<Connection, ARG, R, Exception> code, ARG arg, String operation) {
		ensureTable.run();
		return withInitializedConnection(code, arg, operation);
	}

	/** Unlike regular withConnections, this one does not call ensureTables to avoid recursion and thus stack overflow. */
	private <R, ARG> R withInitializedConnection(CheckedBiFunction<Connection, ARG, R, Exception> code, ARG arg, String operation) {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);

			try {
				R result = code.apply(connection, arg);
				connection.commit();

				return result;

			} catch (SQLException e) {
				connection.rollback();
				throw e;
			}

		} catch (Exception e) {
			throw Exceptions.unchecked(e, logPrefix() + "Error while performing " + operation);
		}
	}

	private <R> R withConnection(Function<Connection, R> code, String operation) {
		ensureTable.run();

		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);

			try {
				R result = code.apply(connection);
				connection.commit();

				return result;

			} catch (SQLException e) {
				connection.rollback();
				throw e;
			}

		} catch (Exception e) {
			throw Exceptions.unchecked(e, logPrefix() + "Error while performing " + operation);
		}
	}

	private String logPrefix() {
		return "JdbcResourceStorage[" + storageId + "] ";
	}

}
