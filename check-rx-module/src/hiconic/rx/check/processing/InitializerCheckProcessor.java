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
 * The message of the entry is the first line of the persisted message. If that has more lines, the details show all of it as a code block.
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
			case SUCCEEDED -> entry(status.taskName(), CheckStatus.ok, firstLine(status.message()));
			case FAILED -> entry(status.taskName(), CheckStatus.fail, firstLine(status.message()));
			case NEVER_RUN -> entry(status.taskName(), CheckStatus.warn, "The task is registered, but it never ran.");
			case NOT_REGISTERED -> entry(status.taskName(), CheckStatus.warn, "The task is not registered.");
		};

		String details = lastRunTable(status) + fullMessage(status.message());
		if (!details.isEmpty()) {
			result.setDetailsAsMarkdown(true);
			result.setDetails(details);
		}

		return result;
	}

	/** The message is rendered as plain text, which shows line breaks as spaces, so a multi-line text is shown in full only in the details. */
	private static String firstLine(String s) {
		return s == null ? null : s.lines().findFirst().orElse("");
	}

	/** A table with the last run, or an empty string if the task never ran. */
	private static String lastRunTable(InitializerStatus status) {
		if (status.updated() == null)
			return "";

		return "Last run | Node | Fingerprint\n" //
				+ "--- | --- | ---\n" //
				+ format(status.updated()) + " | " + orDash(status.updatedBy()) + " | " + orDash(status.fingerprint()) + "\n\n";
	}

	/** The whole message as a code block, which keeps its line breaks and indentation, or an empty string if it has fewer than two lines. */
	private static String fullMessage(String message) {
		if (message == null || message.lines().count() < 2)
			return "";

		String fence = backtickFenceFor(message);
		return fence + "\n" + message + "\n" + fence + "\n";
	}

	/** A backtick fence longer than any backtick run in the text, so the text cannot close the code block. */
	private static String backtickFenceFor(String text) {
		int longestRun = 0;
		int run = 0;
		for (char c : text.toCharArray()) {
			run = c == '`' ? run + 1 : 0;
			longestRun = Math.max(longestRun, run);
		}

		return "`".repeat(Math.max(3, longestRun + 1));
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
