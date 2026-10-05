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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.model.accessdeployment.hibernate.meta.DbUpdateStatement;
import com.braintribe.utils.logging.LogLevels;

/**
 * Executes the SQL of {@link DbUpdateStatement} meta data, as resolved by the mapping generator, before or after the Hibernate schema update.
 * <p>
 * Same semantics as the CX {@code DbUpdateStatementExecutor}: each statement runs on its own; a failing statement stops the startup only if
 * {@link DbUpdateStatement#getStopOnError() stopOnError} is set, otherwise it is logged with {@link DbUpdateStatement#getLogLevelOnError()
 * logLevelOnError}.
 */
/* package */ class DbUpdateStatementRunner {

	private static final Logger log = Logger.getLogger(DbUpdateStatementRunner.class);

	private final DataSource dataSource;
	private final String contextDescription;

	public DbUpdateStatementRunner(DataSource dataSource, String contextDescription) {
		this.dataSource = dataSource;
		this.contextDescription = contextDescription;
	}

	public void run(List<DbUpdateStatement> statements, String phase) {
		if (statements.isEmpty())
			return;

		log.info("Executing " + statements.size() + " DbUpdateStatement(s) " + phase + " the schema update for [" + contextDescription + "]");

		for (DbUpdateStatement statement : statements)
			run(statement);
	}

	private void run(DbUpdateStatement statement) {
		String sql = statement.getExpression();
		log.debug(() -> "Executing DbUpdateStatement '" + sql + "' for [" + contextDescription + "]");

		try (Connection connection = dataSource.getConnection(); Statement dbStatement = connection.createStatement()) {
			dbStatement.execute(sql);
			if (!connection.getAutoCommit())
				connection.commit();

		} catch (SQLException | RuntimeException e) {
			if (statement.getStopOnError())
				throw Exceptions.unchecked(e, "Error while executing DbUpdateStatement '" + sql + "' for [" + contextDescription + "]");

			log.log(LogLevels.convert(statement.getLogLevelOnError(), Logger.LogLevel.WARN),
					"Error while executing DbUpdateStatement '" + sql + "' for [" + contextDescription + "]", e);
		}
	}

}
