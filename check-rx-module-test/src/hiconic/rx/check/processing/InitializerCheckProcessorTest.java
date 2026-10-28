package hiconic.rx.check.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.braintribe.gm.initializer.api.InitializerFingerprintResolver;
import com.braintribe.gm.initializer.api.InitializerRegistry;
import com.braintribe.gm.initializer.api.InitializerState;
import com.braintribe.gm.initializer.api.InitializerStatus;
import com.braintribe.gm.initializer.api.InitializerTask;

import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;

/**
 * Tests for {@link InitializerCheckProcessor}
 */
public class InitializerCheckProcessorTest {

	private final Map<String, InitializerStatus> statuses = new LinkedHashMap<>();
	private RuntimeException statusesError;

	@Test
	public void noTasks_NoEntries() {
		assertThat(check()).isEmpty();
	}

	@Test
	public void oneEntryPerTask_InOrderOfRegistry() {
		addStatus("task-a", InitializerState.SUCCEEDED, "Success");
		addStatus("task-b", InitializerState.FAILED, "Failed with reason: broken");
		addStatus("task-c", InitializerState.SUCCEEDED, "Success");

		assertThat(check()).extracting(CheckResultEntry::getName).containsExactly("task-a", "task-b", "task-c");
	}

	@Test
	public void succeededTask_IsOk() {
		addStatus("task-a", InitializerState.SUCCEEDED, "Success: done");

		CheckResultEntry entry = check().get(0);
		assertThat(entry.getCheckStatus()).isEqualTo(CheckStatus.ok);
		assertThat(entry.getMessage()).isEqualTo("Success: done");
	}

	@Test
	public void failedTask_FailsWithReason() {
		addStatus("task-a", InitializerState.FAILED, "Failed with reason: broken");

		CheckResultEntry entry = check().get(0);
		assertThat(entry.getCheckStatus()).isEqualTo(CheckStatus.fail);
		assertThat(entry.getMessage()).contains("broken");
	}

	@Test
	public void neverRunTask_Warns() {
		statuses.put("task-a", InitializerStatus.neverRun("task-a"));

		CheckResultEntry entry = check().get(0);
		assertThat(entry.getCheckStatus()).isEqualTo(CheckStatus.warn);
		assertThat(entry.getDetails()).isNull();
	}

	@Test
	public void ranTask_HasDetailsOfLastRun() {
		addStatus("task-a", InitializerState.SUCCEEDED, "Success");

		CheckResultEntry entry = check().get(0);
		assertThat(entry.getDetailsAsMarkdown()).isTrue();
		assertThat(entry.getDetails()).contains("1970-01-01T00:00:01Z", "node-1", "fp-task-a");
	}

	/** E.g. the RX registry, which knows the statuses only after the initializers ran. */
	@Test
	public void unreadableStatuses_SingleFailedEntry() {
		addStatus("task-a", InitializerState.SUCCEEDED, "Success");
		statusesError = new IllegalStateException("Initializer statuses are only available once the initializers were run");

		List<CheckResultEntry> entries = check();
		assertThat(entries).hasSize(1);
		assertThat(entries.get(0).getName()).isEqualTo(InitializerCheckProcessor.STATUSES_ENTRY_NAME);
		assertThat(entries.get(0).getCheckStatus()).isEqualTo(CheckStatus.fail);
		assertThat(entries.get(0).getMessage()).contains("only available once the initializers were run");
	}

	// ###############################################
	// ## . . . . . . . . . Helpers . . . . . . . . ##
	// ###############################################

	private void addStatus(String taskName, InitializerState state, String message) {
		statuses.put(taskName, new InitializerStatus(taskName, state, "fp-" + taskName, new Date(1000), "node-1", message));
	}

	private List<CheckResultEntry> check() {
		InitializerCheckProcessor processor = new InitializerCheckProcessor();
		processor.setRegistry(new StatusesRegistry());

		return processor.check(null).getEntries();
	}

	/** Registry with the {@link #statuses} of this test, in their order. */
	private class StatusesRegistry implements InitializerRegistry {

		@Override
		public Set<String> initializerNames() {
			return statuses.keySet();
		}

		@Override
		public Map<String, InitializerStatus> statusesOf(Collection<String> taskNames) {
			if (statusesError != null)
				throw statusesError;

			Map<String, InitializerStatus> result = new LinkedHashMap<>();
			for (String taskName : taskNames)
				result.put(taskName, statuses.get(taskName));

			return result;
		}

		@Override
		public void registerInitializer(String initializerName, InitializerFingerprintResolver fingerprintResolver, InitializerTask task) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void ensureOrder(String runsFirstName, String runsLaterNamer) {
			throw new UnsupportedOperationException();
		}

	}

}
