// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.explorer.processing.check.jdbc;

import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;
import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.execution.generic.ContextualizedFuture;
import com.braintribe.execution.virtual.VirtualThreadExecutorBuilder;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.Logger;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.thread.impl.ThreadContextScopingImpl;
import com.braintribe.thread.impl.ThreadRenamingContextScope;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;
import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.db.module.api.DatabaseContract;

public abstract class ParallelDatabaseConnectionCheckProcessor implements LifecycleAware {

	private static Logger logger = Logger.getLogger(ParallelDatabaseConnectionCheckProcessor.class);

	protected ExecutorService service;
	protected boolean createdService = false;
	protected long timeWarnThreshold = 1000L;
	protected ThreadRenamer threadRenamer = new ThreadRenamer(true);
	protected long checkTimeoutInSec = 300;

	protected DatabaseContract databaseContract;

	@Required
	public void setDatabaseContract(DatabaseContract databaseContract) {
		this.databaseContract = databaseContract;
	}

	public void setService(ExecutorService service) {
		this.service = service;
	}
	public void setTimeWarnThreshold(Long timeWarnThreshold) {
		if (timeWarnThreshold != null) {
			this.timeWarnThreshold = timeWarnThreshold.longValue();
		}
	}
	public void setCheckTimeoutInSec(Long checkTimeoutInSec) {
		if (checkTimeoutInSec != null) {
			this.checkTimeoutInSec = checkTimeoutInSec;
		}
	}

	protected CheckResult performCheck(List<Database> databases) {
		if (isEmpty(databases))
			return emptyResult();
		else
			return performCheckFor(databases);
	}

	private CheckResult performCheckFor(List<Database> databases) {
		List<ContextualizedFuture<ConnectionCheckResult, String>> futures = submitCheckWorkers(databases);
		return awaitCheckResults(futures);
	}

	private List<ContextualizedFuture<ConnectionCheckResult, String>> submitCheckWorkers(List<Database> databases) {
		List<ContextualizedFuture<ConnectionCheckResult, String>> futures = newList();

		for (Database db : databases) {
			String name = db.getName();

			Future<ConnectionCheckResult> f;

			Maybe<DataSource> dataSourceMaybe = databaseContract.dataSource(name);
			if (!dataSourceMaybe.isSatisfied()) {
				ConnectionCheckResult ccr = new ConnectionCheckResult(false, 0,
						"Data source not found for " + name + ". Reason: " + dataSourceMaybe.whyUnsatisfied().asString());
				f = CompletableFuture.completedFuture(ccr);

			} else {

				DataSource dataSource = dataSourceMaybe.get();

				ConnectionCheckWorker worker = new ConnectionCheckWorker(dataSource, name);

				ThreadContextScopingImpl threadContextScoping = new ThreadContextScopingImpl();
				ThreadRenamingContextScope threadRenamingContextScope = new ThreadRenamingContextScope(threadRenamer, () -> name);
				threadContextScoping.setScopeSuppliers(Collections.singletonList(() -> threadRenamingContextScope));

				f = service.submit(threadContextScoping.bindContext(worker));
			}

			futures.add(new ContextualizedFuture<>(f, name));
		}

		return futures;
	}

	private CheckResult awaitCheckResults(List<ContextualizedFuture<ConnectionCheckResult, String>> futures) {
		CheckResult result = CheckResult.T.create();
		StringBuilder sb = new StringBuilder();
		CheckStatus overallStatus = CheckStatus.ok;

		for (ContextualizedFuture<ConnectionCheckResult, String> f : futures) {
			CheckResultEntry cre = CheckResultEntry.T.create();
			result.getEntries().add(cre);

			String message;
			Long time = -1L;
			try {
				ConnectionCheckResult checkResult = f.get(checkTimeoutInSec, TimeUnit.SECONDS);
				time = checkResult.getElapsedTime();
				if (checkResult.isConnectionValid()) {

					if (time > timeWarnThreshold) {
						message = "Connection " + f.getContext() + " is valid, but the check took " + time + " ms. (expected < " + timeWarnThreshold
								+ "ms)";
						cre.setCheckStatus(CheckStatus.warn);
						if (overallStatus != CheckStatus.fail) {
							overallStatus = CheckStatus.warn;
						}
					} else {
						message = "Connection " + f.getContext() + " is valid, the check took " + time + " ms.";
						cre.setCheckStatus(CheckStatus.ok);
					}

				} else {
					message = "Connection " + f.getContext() + " is invalid.";
					cre.setCheckStatus(CheckStatus.fail);
					overallStatus = CheckStatus.fail;
				}

				// Append detailed message from checkResult if available
				String checkDetails = checkResult.getDetails();
				if (!StringTools.isEmpty(checkDetails)) {
					message += " (" + checkDetails + ")";
				}

			} catch (TimeoutException e) {
				logger.debug(() -> "The thread timed out.");
				message = "Connection check timed out after " + checkTimeoutInSec + " seconds.";
				cre.setCheckStatus(CheckStatus.fail);
				overallStatus = CheckStatus.fail;

				f.cancel(true);
			} catch (InterruptedException e) {
				logger.debug(() -> "The thread got interrupted. Stopping the execution and return null.");
				return null;
			} catch (Exception e) {
				logger.error("Error while waiting for result of check " + f.getContext(), e);
				message = ExceptionUtil.getLastMessage(e);
				cre.setCheckStatus(CheckStatus.fail);
				overallStatus = CheckStatus.fail;
			}
			cre.setMessage(message);
			cre.setName(f.getContext());
			cre.setDetails("Checks the connection " + f.getContext());
			sb.append(message + "\n");
		}

		return result;
	}

	private CheckResult emptyResult() {
		CheckResultEntry cre = CheckResultEntry.T.create();
		cre.setCheckStatus(CheckStatus.ok);
		cre.setMessage("No database connections found.");

		CheckResult result = CheckResult.T.create();
		result.getEntries().add(cre);

		return result;
	}

	@Override
	public void postConstruct() {
		if (service == null) {
			createdService = true;
			service = VirtualThreadExecutorBuilder.newPool().concurrency(10).threadNamePrefix("database-connection-check-")
					.description("Database Connection Check").build();
		}
	}

	@Override
	public void preDestroy() {
		if (createdService && service != null) {
			try {
				service.shutdown();
			} catch (Exception e) {
				logger.error("Error while trying to shutdown executor service,", e);
			}
		}
	}

}
