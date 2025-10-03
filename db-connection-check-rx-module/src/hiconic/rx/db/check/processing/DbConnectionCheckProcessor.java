// ============================================================================
package hiconic.rx.db.check.processing;

import java.sql.Connection;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.sql.DataSource;

import com.braintribe.cfg.Required;
import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.date.NanoClock;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.api.CheckProcessorSymbol;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;
import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.db.module.api.DatabaseContract;

/**
 * @author peter.gazdik
 */
public class DbConnectionCheckProcessor implements CheckProcessor {

	public static final CheckProcessorSymbol symbol = () -> "dbConnectionCheckProcessor";

	private static final Logger log = Logger.getLogger(DbConnectionCheckProcessor.class);

	private DatabaseContract databaseContract;

	@Required
	public void setDatabaseContract(DatabaseContract databaseContract) {
		this.databaseContract = databaseContract;
	}

	@Override
	public CheckResult check(ServiceRequestContext requestContext) {
		return new DbConnectionCheckTask().run();
	}

	private class DbConnectionCheckTask {

		private Database currentDb;
		private CheckResultEntry currentEntry;
		private DataSource currentDataSource;

		public CheckResult run() {
			CheckResult result = CheckResult.T.create();

			List<CheckResultEntry> resultEntries = result.getEntries();

			for (Database db : databaseContract.allDatabases())
				resultEntries.add(checkDb(db));

			return result;
		}

		private CheckResultEntry checkDb(Database db) {
			currentDb = db;

			currentEntry = CheckResultEntry.T.create();
			currentEntry.setName(currentDb.getName());

			if (resolveDataSource())
				checkDataSource();

			return currentEntry;
		}

		private boolean resolveDataSource() {
			try {
				currentDataSource = databaseContract.dataSource(currentDb.getName()).get();
				return true;

			} catch (Exception e) {
				markFailure(e);
				return false;
			}
		}

		private void checkDataSource() {
			currentEntry.setCheckStatus(CheckStatus.ok);
			currentEntry.setDetailsAsMarkdown(true);

			StringBuilder sb = new StringBuilder();
			sb.append("Valid | Check duration\n");
			sb.append("--- | ---\n");

			Connection c = null;
			String duration = "";
			boolean valid = false;
			try {
				c = currentDataSource.getConnection();
				Instant start = NanoClock.INSTANCE.instant();
				valid = c.isValid(10);
				duration = StringTools.prettyPrintDuration(start, true, ChronoUnit.MILLIS);

				if (!valid)
					currentEntry.setCheckStatus(CheckStatus.fail);

			} catch (Exception e) {
				markFailure(e);

			} finally {
				IOTools.closeCloseable(c, log);
			}
			sb.append(valid + " | " + duration + "\n");

			currentEntry.setDetails(sb.toString());
		}

		private void markFailure(Exception e) {
			log.error("Error while trying to check DatabaseConnectionPool " + currentDb, e);
			currentEntry.setCheckStatus(CheckStatus.fail);
			currentEntry.setMessage(Exceptions.getRootCause(e).getMessage());
		}

	}

}
