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
package hiconic.rx.hibernate.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.junit.Test;

import com.braintribe.model.accessdeployment.hibernate.meta.DbUpdateStatement;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.data.constraint.TypeSpecification;
import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.lock.impl.SimpleCdlLocking;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;

import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;
import hiconic.rx.hibernate.model.test.Person;

/**
 * Tests that {@link HibernateModelSessionFactoryBuilder} executes the {@link DbUpdateStatement} meta data of the model: "before" statements before
 * the schema update, "after" statements after it, and only together with a schema update, i.e. when the mapping fingerprint (which includes the
 * statements) changed.
 */
public class HibernateModelSessionFactoryBuilderDbUpdateStatementTest {

	private static final String CREATE_PROBE = "CREATE TABLE IF NOT EXISTS PROBE (VAL VARCHAR(20))";
	private static final String INSERT_AFTER = "INSERT INTO PROBE VALUES ('after')";
	private static final String CREATE_NAME_INDEX = "CREATE INDEX IF NOT EXISTS IDX_TEST_PERSON_NAME ON ${TABLE} (${TABLE/name})";

	private final DataSource dataSource = dataSource();
	private final Locking locking = new SimpleCdlLocking();

	@Test
	public void statementsRunAroundSchemaUpdate() throws Exception {
		// INSERT_AFTER fails (stopOnError) unless CREATE_PROBE was executed before it
		build(model(before(CREATE_PROBE), after(INSERT_AFTER), after(CREATE_NAME_INDEX)), true);

		assertThat(probeValues()).containsExactly("after");
		assertThat(indexNames()).contains("IDX_TEST_PERSON_NAME");
	}

	@Test
	public void unchangedFingerprintSkipsStatements() throws Exception {
		build(model(before(CREATE_PROBE), after(INSERT_AFTER)), true);
		build(model(before(CREATE_PROBE), after(INSERT_AFTER)), true);

		assertThat(probeValues()).containsExactly("after");
	}

	@Test
	public void changedStatementTriggersSchemaUpdate() throws Exception {
		build(model(before(CREATE_PROBE), after(INSERT_AFTER)), true);
		build(model(before(CREATE_PROBE), after(INSERT_AFTER), after("INSERT INTO PROBE VALUES ('changed')")), true);

		assertThat(probeValues()).containsExactly("after", "after", "changed");
	}

	@Test
	public void statementsRunOnEveryBuildWithoutLocking() throws Exception {
		build(model(before(CREATE_PROBE), after(INSERT_AFTER)), false);
		build(model(before(CREATE_PROBE), after(INSERT_AFTER)), false);

		assertThat(probeValues()).containsExactly("after", "after");
	}

	@Test
	public void failingStatementWithStopOnErrorFailsBuild() {
		GmMetaModel model = model(after("INSERT INTO NOT_EXISTING VALUES ('x')"));

		assertThatThrownBy(() -> build(model, true)).hasMessageContaining("INSERT INTO NOT_EXISTING");
	}

	@Test
	public void failingStatementWithoutStopOnErrorIsIgnored() throws Exception {
		DbUpdateStatement failing = after("INSERT INTO NOT_EXISTING VALUES ('x')");
		failing.setStopOnError(false);

		build(model(before(CREATE_PROBE), failing, after(INSERT_AFTER)), true);

		assertThat(probeValues()).containsExactly("after");
	}

	// -----------------------------------------------------------------------
	// HELPERS
	// -----------------------------------------------------------------------

	private void build(GmMetaModel model, boolean withLocking) {
		HibernatePersistenceConfiguration configuration = HibernatePersistenceConfiguration.T.create();
		configuration.getProperties().put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");

		HibernateModelSessionFactoryBuilder builder = new HibernateModelSessionFactoryBuilder(
				new SessionFactoryKey(configuration, cmdResolver(model), dataSource));
		builder.setInstanceId("test-node");
		if (withLocking)
			builder.setLockingSupplier(() -> locking);

		SessionFactory sessionFactory = builder.build();
		sessionFactory.close();
	}

	/** A new model (same name each time, thus the same schema identity) with {@link Person} and the given statements on it. */
	private static GmMetaModel model(DbUpdateStatement... statements) {
		GmMetaModel model = GmMetaModel.T.create();
		model.setName("test:db-update-statement-model");
		model.getDependencies().add(Person.T.getModel().getMetaModel());

		BasicModelOracle oracle = new BasicModelOracle(model);
		ModelMetaDataEditor editor = new BasicModelMetaDataEditor(model);

		TypeSpecification idType = TypeSpecification.T.create();
		idType.setType(oracle.getGmStringType());
		editor.onEntityType(GenericEntity.T).addPropertyMetaData(GenericEntity.id, idType);

		editor.onEntityType(Person.T).addMetaData(statements);

		return model;
	}

	private static CmdResolver cmdResolver(GmMetaModel model) {
		return CmdResolverImpl.create(new BasicModelOracle(model)).done();
	}

	private static DbUpdateStatement before(String sql) {
		DbUpdateStatement result = after(sql);
		result.setBefore(true);
		return result;
	}

	private static DbUpdateStatement after(String sql) {
		DbUpdateStatement result = DbUpdateStatement.T.create();
		result.setExpression(sql);
		result.setStopOnError(true);
		return result;
	}

	private List<String> probeValues() throws Exception {
		return query("SELECT VAL FROM PROBE ORDER BY VAL");
	}

	private List<String> indexNames() throws Exception {
		return query("SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES");
	}

	private List<String> query(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			List<String> values = new ArrayList<>();
			while (result.next())
				values.add(result.getString(1));
			return values;
		}
	}

	private static DataSource dataSource() {
		JdbcDataSource result = new JdbcDataSource();
		result.setURL("jdbc:h2:mem:db-update-statement-builder-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		return result;
	}
}
