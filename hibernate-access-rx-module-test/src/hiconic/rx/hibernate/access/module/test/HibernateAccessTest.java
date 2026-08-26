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
package hiconic.rx.hibernate.access.module.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.hibernate.test.common.HibernateAccessTestDatabaseReset;
import hiconic.rx.hibernate.test.common.HibernateAccessTestDatabaseReset.ResetReport;
import hiconic.rx.test.common.AbstractRxTest;

public class HibernateAccessTest extends AbstractRxTest {
	
	@BeforeClass
	public static void onBeforeClass() {
		System.setProperty("rx.hibernate.module.ormDebugOutputFolder", new File("res/out").getAbsolutePath());
	}
	
	@Test
	public void testHibernateAccess() {
		PersistenceGmSession session = newSession();
		
		Object id;
		String name = "Hans";
		String lastName = "Wurst";
		
		Person p = session.create(Person.T);
		p.setName(name);
		p.setLastName(lastName);
		session.commit();
		
		id = p.getId();
		
		session = newSession(); 
		
		p = session.query().entity(Person.T, id).require();
		
		Assertions.assertThat(p).isNotNull();
		Assertions.assertThat(p.getName()).isEqualTo(name);
		Assertions.assertThat(p.getLastName()).isEqualTo(lastName);
	}

	@Test
	public void resetDiscoversHibernateDatabaseAndPreservesSchema() throws SQLException {
		PersistenceGmSession session = newSession();
		Person person = session.create(Person.T);
		person.setName("Transient");
		person.setLastName("Fixture");
		session.commit();

		ensureSchemaUpdateSentinel();
		int schemaUpdateRowsBefore = schemaUpdateRowCount();
		ResetReport report = HibernateAccessTestDatabaseReset.resetH2Databases(platform);

		Assertions.assertThat(report.databases()).hasSize(1);
		Assertions.assertThat(report.resetTableCount()).isPositive();
		Assertions.assertThat(report.databases()).flatExtracting(database -> database.tables())
				.noneMatch(table -> table.equalsIgnoreCase("PUBLIC.TF_SCHEMA_UPDATE_TMP"));
		Assertions.assertThat(schemaUpdateRowsBefore).isPositive();
		Assertions.assertThat(schemaUpdateRowCount()).isEqualTo(schemaUpdateRowsBefore);
		Assertions.assertThat(newSession().query().entities(EntityQueryBuilder.from(Person.T).done()).list()).isEmpty();
	}

	@Test
	public void resetClosedPlatformDatabaseThroughJdbcUrl() throws SQLException {
		String jdbcUrl = "jdbc:h2:mem:closed-platform-reset-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
		try (Connection connection = DriverManager.getConnection(jdbcUrl); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE TF_SCHEMA_UPDATE_TMP (ID integer primary key)");
			statement.execute("CREATE TABLE APPLICATION_DATA (ID integer primary key)");
			statement.execute("INSERT INTO TF_SCHEMA_UPDATE_TMP VALUES (1)");
			statement.execute("INSERT INTO APPLICATION_DATA VALUES (1)");
		}

		HibernateAccessTestDatabaseReset.resetH2Database("closed-platform-test", jdbcUrl);

		try (Connection connection = DriverManager.getConnection(jdbcUrl); Statement statement = connection.createStatement()) {
			Assertions.assertThat(rowCount(statement, "TF_SCHEMA_UPDATE_TMP")).isEqualTo(1);
			Assertions.assertThat(rowCount(statement, "APPLICATION_DATA")).isZero();
		}
	}

	private int rowCount(Statement statement, String table) throws SQLException {
		try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
			result.next();
			return result.getInt(1);
		}
	}

	private void ensureSchemaUpdateSentinel() throws SQLException {
		DatabaseContract databases = resolveExportContract(DatabaseContract.class);
		try (Connection connection = databases.dataSource("main-db").get().getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE IF NOT EXISTS TF_SCHEMA_UPDATE_TMP ("
					+ "ACCESS_ID varchar(255) primary key, HASH varchar(255) not null, ERROR_COUNT integer not null, "
					+ "INSTANCE_ID varchar(255) not null, CONTEXT varchar(255) not null)");
			statement.executeUpdate("MERGE INTO TF_SCHEMA_UPDATE_TMP KEY(ACCESS_ID) "
					+ "VALUES ('reset-test', 'hash', 0, 'test', 'test')");
		}
	}

	private int schemaUpdateRowCount() throws SQLException {
		DatabaseContract databases = resolveExportContract(DatabaseContract.class);
		try (Connection connection = databases.dataSource("main-db").get().getConnection();
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM TF_SCHEMA_UPDATE_TMP")) {
			result.next();
			return result.getInt(1);
		}
	}
	
	private PersistenceGmSession newSession() {
		AccessContract contract = resolveExportContract(AccessContract.class);
		return contract.systemSessionFactory().newSession("main-access");
	}
}
