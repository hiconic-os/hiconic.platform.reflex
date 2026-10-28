package hiconic.rx.check.processing;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.cfg.Required;
import com.braintribe.gm.initializer.api.InitializerRegistry;
import com.braintribe.gm.initializer.api.InitializerStatus;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.api.CheckProcessorSymbol;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;

/**
 * Reports the {@link InitializerStatus status} of every task of an {@link InitializerRegistry}, one {@link CheckResultEntry} per task, named after
 * the task:
 * <ul>
 * <li>succeeded - {@link CheckStatus#ok ok}
 * <li>failed - {@link CheckStatus#fail fail}, with the reason or the stack trace as message
 * <li>never run - {@link CheckStatus#warn warn}
 * </ul>
 * The status is the persisted outcome of the last run of the task, on whichever node it ran. A task that was skipped because its fingerprint did not
 * change thus reports the run that produced that fingerprint.
 * <p>
 * If the statuses cannot be read, e.g. before the initializers ran, the result is a single failed entry.
 */
public class InitializerCheckProcessor implements CheckProcessor {

	public static final CheckProcessorSymbol symbol = () -> "initializerCheckProcessor";

	private static final Logger log = Logger.getLogger(InitializerCheckProcessor.class);

	/** Name of the single entry that reports that the statuses could not be read. */
	public static final String STATUSES_ENTRY_NAME = "Initializer statuses";

	private InitializerRegistry registry;

	@Required
	public void setRegistry(InitializerRegistry registry) {
		this.registry = registry;
	}

	@Override
	public CheckResult check(ServiceRequestContext context) {
		CheckResult result = CheckResult.T.create();
		List<CheckResultEntry> entries = result.getEntries();

		try {
			Set<String> initializerNames = registry.initializerNames();
			Map<String, InitializerStatus> allStatuses = registry.statusesOf(initializerNames);

			for (InitializerStatus status : allStatuses.values())
				entries.add(entryFor(status));

		} catch (Exception e) {
			log.error("Error while reading the initializer statuses", e);
			entries.clear();
			entries.add(entry(STATUSES_ENTRY_NAME, CheckStatus.fail, "Cannot read the initializer statuses: " + e.getMessage()));
		}

		return result;
	}

	private CheckResultEntry entryFor(InitializerStatus status) {
		CheckResultEntry result = switch (status.state()) {
			case SUCCEEDED -> entry(status.taskName(), CheckStatus.ok, status.message());
			case FAILED -> entry(status.taskName(), CheckStatus.fail, status.message());
			case NEVER_RUN -> entry(status.taskName(), CheckStatus.warn, "The task is registered, but it never ran.");
			case NOT_REGISTERED -> entry(status.taskName(), CheckStatus.warn, "The task is not registered.");
		};

		if (status.updated() != null) {
			result.setDetailsAsMarkdown(true);
			result.setDetails(details(status));
		}

		return result;
	}

	private static String details(InitializerStatus status) {
		return "Last run | Node | Fingerprint\n" //
				+ "--- | --- | ---\n" //
				+ format(status.updated()) + " | " + orDash(status.updatedBy()) + " | " + orDash(status.fingerprint()) + "\n";
	}

	private static String format(Date date) {
		return DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
	}

	private static String orDash(String s) {
		return s == null ? "-" : s;
	}

	private static CheckResultEntry entry(String name, CheckStatus checkStatus, String message) {
		CheckResultEntry result = CheckResultEntry.T.create();
		result.setName(name);
		result.setCheckStatus(checkStatus);
		result.setMessage(message);
		return result;
	}

}
