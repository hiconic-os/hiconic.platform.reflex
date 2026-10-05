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
import org.junit.Test;

import com.braintribe.model.accessdeployment.hibernate.meta.DbUpdateStatement;

/** Tests for {@link DbUpdateStatementRunner} */
public class DbUpdateStatementRunnerTest {

	private final DataSource dataSource = dataSource();
	private final DbUpdateStatementRunner runner = new DbUpdateStatementRunner(dataSource, "test");

	@Test
	public void statementsRunInGivenOrder() throws Exception {
		runner.run(List.of( //
				statement("CREATE TABLE PROBE (VAL VARCHAR(50))", true), //
				statement("INSERT INTO PROBE VALUES ('first')", true), //
				statement("INSERT INTO PROBE VALUES ('second')", true)), "after");

		assertThat(probeValues()).containsExactly("first", "second");
	}

	@Test
	public void failingStatementIsSkippedWithoutStopOnError() throws Exception {
		runner.run(List.of( //
				statement("CREATE TABLE PROBE (VAL VARCHAR(50))", true), //
				statement("INSERT INTO NOT_EXISTING VALUES ('x')", false), //
				statement("INSERT INTO PROBE VALUES ('after-failure')", true)), "after");

		assertThat(probeValues()).containsExactly("after-failure");
	}

	@Test
	public void failingStatementWithStopOnErrorThrows() throws Exception {
		List<DbUpdateStatement> statements = List.of( //
				statement("CREATE TABLE PROBE (VAL VARCHAR(50))", true), //
				statement("INSERT INTO NOT_EXISTING VALUES ('x')", true), //
				statement("INSERT INTO PROBE VALUES ('never')", true));

		assertThatThrownBy(() -> runner.run(statements, "after")) //
				.hasMessageContaining("INSERT INTO NOT_EXISTING");

		assertThat(probeValues()).isEmpty();
	}

	@Test
	public void emptyListRunsNothing() {
		runner.run(List.of(), "before");
	}

	private static DbUpdateStatement statement(String sql, boolean stopOnError) {
		DbUpdateStatement result = DbUpdateStatement.T.create();
		result.setExpression(sql);
		result.setStopOnError(stopOnError);
		return result;
	}

	private List<String> probeValues() throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT VAL FROM PROBE ORDER BY VAL")) {
			List<String> values = new ArrayList<>();
			while (result.next())
				values.add(result.getString(1));
			return values;
		}
	}

	private static DataSource dataSource() {
		JdbcDataSource result = new JdbcDataSource();
		result.setURL("jdbc:h2:mem:db-update-statement-runner-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		return result;
	}
}
