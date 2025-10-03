package hiconic.rx.platform.processing.lifez;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.essential.InternalError;

import hiconic.rx.module.api.state.RxApplicationLivenessChecker;

public class DeadlockChecker implements RxApplicationLivenessChecker {
	@Override
	public Reason checkLiveness() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

		if (deadlockedThreads == null || deadlockedThreads.length == 0)
			return null;

		Set<Long> threadIds = Arrays.stream(deadlockedThreads).boxed().collect(Collectors.toSet());

		List<ThreadInfo> deadlockedThreadInfos = Stream.of(threadMXBean.dumpAllThreads(true, true)) //
				.filter(ti -> threadIds.contains(ti.getThreadId())).toList();

		return InternalError.create("Deadlock detected in the following threads: " //
				+ formatThreadInfo(deadlockedThreadInfos));
	}

	private static String formatThreadInfo(Iterable<ThreadInfo> infos) {
		StringBuilder sb = new StringBuilder();

		for (var i : infos) {
			sb.append("\n");
			formatThreadInfo(sb, i);
		}

		return sb.toString();
	}

	private static void formatThreadInfo(StringBuilder sb, ThreadInfo info) {

		sb.append('"').append(info.getThreadName()).append('"').append(" Id=").append(info.getThreadId()).append(" ").append(info.getThreadState());

		if (info.getLockName() != null) {
			sb.append(" on ").append(info.getLockName());
		}
		if (info.getLockOwnerName() != null) {
			sb.append(" owned by \"").append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
		}
		sb.append('\n');

		for (StackTraceElement ste : info.getStackTrace()) {
			sb.append("\tat ").append(ste).append('\n');
		}

		if (info.getLockedMonitors() != null && info.getLockedMonitors().length > 0) {
			sb.append("\tLocked monitors:\n");
			for (MonitorInfo mi : info.getLockedMonitors()) {
				sb.append("\t\t- ").append(mi).append('\n');
			}
		}

		if (info.getLockedSynchronizers() != null && info.getLockedSynchronizers().length > 0) {
			sb.append("\tLocked synchronizers:\n");
			for (LockInfo li : info.getLockedSynchronizers()) {
				sb.append("\t\t- ").append(li).append('\n');
			}
		}

		sb.append('\n');
	}
}
