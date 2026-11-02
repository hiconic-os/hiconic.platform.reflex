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

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.data.constraint.TypeSpecification;
import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.lock.impl.SimpleCdlLocking;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;

import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;
import hiconic.rx.hibernate.model.configuration.SchemaUpdateMode;
import hiconic.rx.hibernate.model.test.Person;

/** Tests that {@link HibernateModelSessionFactoryBuilder} only changes the DB schema with {@link SchemaUpdateMode#update}. */
public class HibernateModelSessionFactoryBuilderSchemaUpdateModeTest {

	private final DataSource dataSource = dataSource();
	private final Locking locking = new SimpleCdlLocking();

	@Test
	public void updateCreatesTablesAndTrackingTable() throws Exception {
		build(SchemaUpdateMode.update);

		assertThat(tableNames()).hasSizeGreaterThan(1).contains(HibernateSchemaUpdateGate.TABLE_NAME);
	}

	@Test
	public void noneDoesNotChangeDb() throws Exception {
		build(SchemaUpdateMode.none);

		assertThat(tableNames()).isEmpty();
	}

	@Test
	public void validateFailsOnMissingTables() throws Exception {
		assertThatThrownBy(() -> build(SchemaUpdateMode.validate)).hasMessageContaining("Schema-validation: missing table");

		assertThat(tableNames()).isEmpty();
	}

	@Test
	public void validateSucceedsOnMatchingSchema() throws Exception {
		build(SchemaUpdateMode.update);
		List<String> tablesAfterUpdate = tableNames();

		build(SchemaUpdateMode.validate);

		assertThat(tableNames()).isEqualTo(tablesAfterUpdate);
	}

	// -----------------------------------------------------------------------
	// HELPERS
	// -----------------------------------------------------------------------

	private void build(SchemaUpdateMode schemaUpdateMode) {
		HibernatePersistenceConfiguration configuration = HibernatePersistenceConfiguration.T.create();
		configuration.getProperties().put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		configuration.setSchemaUpdateMode(schemaUpdateMode);

		HibernateModelSessionFactoryBuilder builder = new HibernateModelSessionFactoryBuilder(
				new SessionFactoryKey(configuration, CmdResolverImpl.create(new BasicModelOracle(model())).done(), dataSource));
		builder.setInstanceId("test-node");
		builder.setLockingSupplier(() -> locking);

		SessionFactory sessionFactory = builder.build();
		sessionFactory.close();
	}

	private static GmMetaModel model() {
		GmMetaModel model = GmMetaModel.T.create();
		model.setName("test:schema-update-mode-model");
		model.getDependencies().add(Person.T.getModel().getMetaModel());

		TypeSpecification idType = TypeSpecification.T.create();
		idType.setType(new BasicModelOracle(model).getGmStringType());
		new BasicModelMetaDataEditor(model).onEntityType(GenericEntity.T).addPropertyMetaData(GenericEntity.id, idType);

		return model;
	}

	private List<String> tableNames() throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' ORDER BY TABLE_NAME")) {
			List<String> values = new ArrayList<>();
			while (result.next())
				values.add(result.getString(1));
			return values;
		}
	}

	private static DataSource dataSource() {
		JdbcDataSource result = new JdbcDataSource();
		result.setURL("jdbc:h2:mem:schema-update-mode-builder-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		return result;
	}
}
