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
package hiconic.rx.hibernate.test.common;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

import hiconic.rx.access.hibernate.model.configuration.HibernateAccess;
import hiconic.rx.access.model.configuration.AccessConfiguration;
import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.platform.RxPlatform;

/**
 * Schema-preserving database reset for RX application tests.
 * <p>
 * The reset derives the relevant logical databases from all configured {@link HibernateAccess}
 * instances, resolves their pooled data sources through the platform, and resets every physical
 * H2 database/schema only once. It therefore does not duplicate access ids or JDBC settings in
 * test code.
 * <p>
 * Call this immediately after booting the platform and before opening application persistence
 * sessions. Hibernate accesses are deployed lazily, which keeps their session-level state out of
 * the reset operation.
 */
public final class HibernateAccessTestDatabaseReset {

	private static final String H2_PRODUCT_NAME = "H2";
	private static final String H2_SYSTEM_SCHEMA = "INFORMATION_SCHEMA";

	private HibernateAccessTestDatabaseReset() {
	}

	public static ResetReport resetH2Databases(RxPlatform platform) throws SQLException {
		Objects.requireNonNull(platform, "platform");

		AccessConfiguration accessConfiguration = platform.getContract().configuration().readConfig(AccessConfiguration.T).get();
		DatabaseContract databases = platform.getWireContext().contract(DatabaseContract.class);
		Map<String, Database> databasesByName = indexDatabases(databases.allDatabases());
		Set<String> databaseNames = referencedHibernateDatabaseNames(accessConfiguration);

		List<DatabaseReset> resets = new ArrayList<>();
		Set<PhysicalDatabase> resetDatabases = new HashSet<>();
		for (String databaseName : databaseNames) {
			Database database = databasesByName.get(databaseName);
			if (database == null)
				throw new IllegalStateException("HibernateAccess references unknown database '" + databaseName + "'");

			DataSource dataSource = databases.dataSource(databaseName).get();
			try (Connection connection = dataSource.getConnection()) {
				PhysicalDatabase physicalDatabase = physicalDatabase(connection, database.getSchema());
				if (resetDatabases.add(physicalDatabase))
					resets.add(resetH2Database(databaseName, database.getSchema(), connection));
			}
		}

		return new ResetReport(resets);
	}

	private static Set<String> referencedHibernateDatabaseNames(AccessConfiguration configuration) {
		Set<String> result = new LinkedHashSet<>();
		configuration.getAccesses().stream()
				.filter(HibernateAccess.T::isInstance)
				.map(HibernateAccess.class::cast)
				.map(HibernateAccess::getDatabaseName)
				.filter(Objects::nonNull)
				.forEach(result::add);
		return result;
	}

	private static Map<String, Database> indexDatabases(List<Database> databases) {
		Map<String, Database> result = new HashMap<>();
		for (Database database : databases)
			result.put(database.getName(), database);
		return result;
	}

	private static PhysicalDatabase physicalDatabase(Connection connection, String configuredSchema) throws SQLException {
		DatabaseMetaData md = connection.getMetaData();
		return new PhysicalDatabase(md.getURL(), md.getUserName(), connection.getCatalog(), configuredSchema);
	}

	private static DatabaseReset resetH2Database(String databaseName, String configuredSchema, Connection connection) throws SQLException {
		DatabaseMetaData md = connection.getMetaData();
		if (!H2_PRODUCT_NAME.equalsIgnoreCase(md.getDatabaseProductName()))
			throw new IllegalArgumentException("Database '" + databaseName + "' is " + md.getDatabaseProductName()
					+ "; the schema-preserving reset currently supports H2 only");

		connection.setAutoCommit(true);
		List<Table> tables = tables(md, connection.getCatalog(), configuredSchema);
		String quote = md.getIdentifierQuoteString();
		SQLException failure = null;
		boolean integrityDisabled = false;
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
			integrityDisabled = true;
			for (Table table : tables)
				statement.execute("TRUNCATE TABLE " + qualifiedName(table, quote) + " RESTART IDENTITY");
		} catch (SQLException e) {
			failure = e;
			throw e;
		} finally {
			if (integrityDisabled) {
				try (Statement statement = connection.createStatement()) {
					statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
				} catch (SQLException integrityFailure) {
					if (failure != null)
						failure.addSuppressed(integrityFailure);
					else
						throw integrityFailure;
				}
			}
		}

		return new DatabaseReset(databaseName, md.getURL(), tables.stream().map(Table::qualifiedName).toList());
	}

	private static List<Table> tables(DatabaseMetaData md, String catalog, String configuredSchema) throws SQLException {
		List<Table> result = new ArrayList<>();
		String schemaPattern = configuredSchema == null || configuredSchema.isBlank() ? null : configuredSchema;
		try (ResultSet rs = md.getTables(catalog, schemaPattern, "%", null)) {
			while (rs.next()) {
				String type = rs.getString("TABLE_TYPE");
				String schema = rs.getString("TABLE_SCHEM");
				if (("TABLE".equalsIgnoreCase(type) || "BASE TABLE".equalsIgnoreCase(type)) && !H2_SYSTEM_SCHEMA.equalsIgnoreCase(schema))
					result.add(new Table(schema, rs.getString("TABLE_NAME")));
			}
		}
		return result;
	}

	private static String qualifiedName(Table table, String quote) {
		return quoted(table.schema(), quote) + "." + quoted(table.name(), quote);
	}

	private static String quoted(String identifier, String quote) {
		String effectiveQuote = quote == null || quote.isBlank() ? "\"" : quote;
		return effectiveQuote + identifier.replace(effectiveQuote, effectiveQuote + effectiveQuote) + effectiveQuote;
	}

	public record ResetReport(List<DatabaseReset> databases) {
		public ResetReport {
			databases = List.copyOf(databases);
		}

		public int resetTableCount() {
			return databases.stream().mapToInt(database -> database.tables().size()).sum();
		}
	}

	public record DatabaseReset(String configuredName, String jdbcUrl, List<String> tables) {
		public DatabaseReset {
			tables = List.copyOf(tables);
		}
	}

	private record PhysicalDatabase(String jdbcUrl, String user, String catalog, String schema) {
	}

	private record Table(String schema, String name) {
		private String qualifiedName() {
			return schema + "." + name;
		}
	}
}
