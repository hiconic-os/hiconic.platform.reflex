package hiconic.rx.platform.resource.jdbc.processing;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.braintribe.common.lcd.function.XConsumer;
import com.braintribe.exception.Exceptions;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.resource.source.SqlSource;
import com.braintribe.util.jdbc.JdbcTools;
import com.braintribe.utils.IOTools;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.test.resource.AbstractResourceStorageRxTest;

/**
 * Tests for FsResourceStorage.
 * 
 * @see AbstractResourceStorageRxTest
 */
public class JdbcResourceStorageTest extends AbstractResourceStorageRxTest<SqlSource> {

	private DataSource dataSource;

	@Before
	public void prepareTest() {
		dataSource = resolveExportContract(DatabaseContract.class).dataSource("main-db").get();

		withConnection("deleting JDBC resources", c -> {
			// table name 'test_resources' configured in resource-storage-configuration.yaml
			if (JdbcTools.tableExists(c, "test_resources") != null)
				c.prepareStatement("DELETE FROM test_resources").executeUpdate();
		});

	}

	@Override
	protected EntityType<SqlSource> resourceSourceType() {
		return SqlSource.T;
	}

	//
	// Tests
	//

	// @formatter:off
	@Override @Test public void happyPath()             { super.happyPath(); }
	@Override @Test public void nonExistentResource()   { super.nonExistentResource(); }
	@Override @Test public void nonMatchingMd5()        { super.nonMatchingMd5(); }
	@Override @Test public void nonMatchingCreateDate() { super.nonMatchingCreateDate(); }
	// @formatter:on

	//
	// Implement abstract methods
	//

	@Override
	protected void assertStoredResourceSourceIsOk(SqlSource resourceSource, String expectedPayload) {
		withConnection("asserting stored resource source is ok", c -> {
			Blob blob = queryBlob(c, resourceSource.getBlobId());
			assertThat(blob).isNotNull();

			assertThat(blobToString(blob)).isEqualTo(expectedPayload);
		});
	}

	@Override
	protected void assertResourceSourceNotExists(SqlSource resourceSource) {
		withConnection("asserting resource not exists", c -> {
			Blob blob = queryBlob(c, resourceSource.getBlobId());
			assertThat(blob).isNull();
		});
	}

	@Override
	protected SqlSource createNonExistentResourceSource() {
		SqlSource result = SqlSource.T.create();
		result.setBlobId("non-existent-blob-Id");
		return result;
	}

	//
	// Helpers
	//

	private Blob queryBlob(Connection connection, String id) {
		try (PreparedStatement ps = connection.prepareStatement("SELECT data FROM test_resources WHERE id = ?")) {
			ps.setString(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;

				Blob blob = rs.getBlob(1);

				if (rs.next())
					throw new IllegalStateException("Expected only one result for id " + id);

				return blob;
			}
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not query for BLOB with id " + id);
		}
	}

	private String blobToString(Blob blob) throws SQLException {
		try (InputStream is = blob.getBinaryStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			IOTools.pump(is, baos);
			return baos.toString("UTF-8");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void withConnection(String details, XConsumer<Connection> task) {
		JdbcTools.withConnection(dataSource, true, () -> details, c -> {
			task.accept(c);
		});
	}

}
