// ============================================================================
package hiconic.rx.explorer.processing.platformreflection;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.braintribe.logging.Logger;

import hiconic.rx.reflection.model.jvm.HotThread;
import hiconic.rx.reflection.model.jvm.HotThreads;

public class HotThreadsProvider {

	private static Logger logger = Logger.getLogger(HotThreadsProvider.class);

	private static final StackTraceElement[] EMPTY = new StackTraceElement[0];

	public static HotThreads detect() throws Exception {
		return detect(500L, 3, true, "cpu", 10, 10L);
	}

	public static HotThreads detectWithDefaults(Long intervalInMs, Integer noOfBusiestThreads, Boolean ignoreIdleThreads, String type, Integer threadElementsSnapshotCount, Long threadElementsSnapshotDelayInMs) throws Exception {
		if (intervalInMs == null) {
			intervalInMs = 500L;
		}
		if (noOfBusiestThreads == null) {
			noOfBusiestThreads = 3;
		}
		if (ignoreIdleThreads == null) {
			ignoreIdleThreads = Boolean.TRUE;
		}
		if (type == null) {
			type = "cpu";
		}
		if (threadElementsSnapshotCount == null) {
			threadElementsSnapshotCount = 10;
		}
		if (threadElementsSnapshotDelayInMs == null) {
			threadElementsSnapshotDelayInMs = 10L;
		}
		return detect(intervalInMs, noOfBusiestThreads, ignoreIdleThreads, type, threadElementsSnapshotCount, threadElementsSnapshotDelayInMs);
	}

	public static HotThreads detect(long intervalInMs, int noOfBusiestThreads, boolean ignoreIdleThreads, String type, int threadElementsSnapshotCount, long threadElementsSnapshotDelayInMs) throws Exception {

		logger.debug(() -> "Detecting hot threads.");

		try {
			ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
			if (threadBean.isThreadCpuTimeSupported() == false) {
				throw new Exception("thread CPU time is not supported on this JDK");
			}

			HotThreads hts = HotThreads.T.create();
			hts.setTimestamp(new Date());
			hts.setIntervalInMs(intervalInMs);
			hts.setNoOfBusiestThreads(noOfBusiestThreads);
			hts.setIgnoreIdleThreads(ignoreIdleThreads);
			hts.setThreadElementsSnapshotCount(threadElementsSnapshotCount);
			hts.setThreadElementsSnapshotDelayInMs(threadElementsSnapshotDelayInMs);
			hts.setType(type);
			List<HotThread> hotThreadList = new ArrayList<HotThread>();
			hts.setHotThreadList(hotThreadList);

			Map<Long, ExtendedThreadInfo> threadInfos = new HashMap<>();
			for (long threadId : threadBean.getAllThreadIds()) {
				// ignore our own thread...
				if (Thread.currentThread().threadId() == threadId) {
					continue;
				}
				long cpuInNanos = threadBean.getThreadCpuTime(threadId);
				if (cpuInNanos == -1) {
					continue;
				}
				ThreadInfo info = threadBean.getThreadInfo(threadId, 0);
				if (info == null) {
					continue;
				}
				threadInfos.put(threadId, new ExtendedThreadInfo(cpuInNanos, info));
			}
			Thread.sleep(intervalInMs);
			for (long threadId : threadBean.getAllThreadIds()) {
				// ignore our own thread...
				if (Thread.currentThread().threadId() == threadId) {
					continue;
				}
				long cpuInNanos = threadBean.getThreadCpuTime(threadId);
				if (cpuInNanos == -1) {
					threadInfos.remove(threadId);
					continue;
				}
				ThreadInfo info = threadBean.getThreadInfo(threadId, 0);
				if (info == null) {
					threadInfos.remove(threadId);
					continue;
				}
				ExtendedThreadInfo data = threadInfos.get(threadId);
				if (data != null) {
					data.setDelta(cpuInNanos, info);
				} else {
					threadInfos.remove(threadId);
				}
			}
			// sort by delta CPU time on thread.
			List<ExtendedThreadInfo> hotties = new ArrayList<>(threadInfos.values());
			int busiestThreads = Math.min(noOfBusiestThreads, hotties.size());
			busiestThreads = Math.max(busiestThreads, 1);
			// skip that for now
			Collections.sort(hotties, new ExtendedThreadInfoComparator(type));

			// analyse N stack traces for M busiest threads
			long[] ids = new long[busiestThreads];
			for (int i = 0; i < busiestThreads; i++) {
				ExtendedThreadInfo info = hotties.get(i);
				ids[i] = info.info.getThreadId();
			}
			ThreadInfo[][] allInfos = new ThreadInfo[threadElementsSnapshotCount][];
			for (int j = 0; j < threadElementsSnapshotCount; j++) {
				// NOTE, javadoc of getThreadInfo says: If a thread of the given ID is not alive or does not exist,
				// null will be set in the corresponding element in the returned array. A thread is alive if it has
				// been started and has not yet died.
				allInfos[j] = threadBean.getThreadInfo(ids, Integer.MAX_VALUE);
				Thread.sleep(threadElementsSnapshotDelayInMs);
			}
			for (int t = 0; t < busiestThreads; t++) {
				long time = 0;
				if ("cpu".equals(type)) {
					time = hotties.get(t).cpuTimeInNanos;
				} else if ("wait".equals(type)) {
					time = hotties.get(t).waitedTimeInMs;
				} else if ("block".equals(type)) {
					time = hotties.get(t).blockedTimeInMs;
				}
				String threadName = null;
				for (ThreadInfo[] info : allInfos) {
					if (info != null && info[t] != null) {
						if (ignoreIdleThreads && isIdleThread(info[t])) {
							info[t] = null;
							continue;
						}
						threadName = info[t].getThreadName();
						break;
					}
				}
				if (threadName == null) {
					continue; // thread is not alive yet or died before the first snapshot - ignore it!
				}
				double percent = (((double) time) / TimeUnit.MILLISECONDS.toNanos(intervalInMs)) * 100;

				HotThread ht = HotThread.T.create();
				hotThreadList.add(ht);

				ht.setPercent(percent);
				ht.setTimeInNanoSeconds(time);
				ht.setThreadName(threadName);

				// for each snapshot (2nd array index) find later snapshot for same thread with max number of
				// identical StackTraceElements (starting from end of each)
				boolean[] done = new boolean[threadElementsSnapshotCount];
				for (int i = 0; i < threadElementsSnapshotCount; i++) {
					if (done[i]) continue;
					int maxSim = 1;
					boolean[] similars = new boolean[threadElementsSnapshotCount];
					for (int j = i + 1; j < threadElementsSnapshotCount; j++) {
						if (done[j]) continue;
						int similarity = similarity(allInfos[i][t], allInfos[j][t]);
						if (similarity > maxSim) {
							maxSim = similarity;
							similars = new boolean[threadElementsSnapshotCount];
						}
						if (similarity == maxSim) similars[j] = true;
					}
					// print out trace maxSim levels of i, and mark similar ones as done
					int count = 1;
					for (int j = i + 1; j < threadElementsSnapshotCount; j++) {
						if (similars[j]) {
							done[j] = true;
							count++;
						}
					}
					if (allInfos[i][t] != null) {
						final StackTraceElement[] show = allInfos[i][t].getStackTrace();

						ht.setCount(count);
						List<hiconic.rx.reflection.model.jvm.StackTraceElement> stackTraceElementList = newList();
						ht.setStackTraceElements(stackTraceElementList);

						if (count == 1) {
							ht.setMaxSimilarity(0);
							for (int l = 0; l < show.length; l++) {
								stackTraceElementList.add(PlatformReflectionTools.convertStackTraceElement(show[l]));
							}
						} else {
							ht.setMaxSimilarity(maxSim);
							for (int l = show.length - maxSim; l < show.length; l++) {
								stackTraceElementList.add(PlatformReflectionTools.convertStackTraceElement(show[l]));
							}
						}
					}
				}
			}
			return hts;
		} finally {
			logger.debug(() -> "Done with detecting hot threads.");
		}
	}

	private static boolean isIdleThread(ThreadInfo threadInfo) {
		String threadName = threadInfo.getThreadName();

		// NOTE: these are likely JVM dependent
		if (threadName.equals("Signal Dispatcher") ||
				threadName.equals("Finalizer") ||
				threadName.equals("Reference Handler")) {
			return true;
		}

		for (StackTraceElement frame : threadInfo.getStackTrace()) {
			String className = frame.getClassName();
			String methodName = frame.getMethodName();
			if (className.equals("java.util.concurrent.ThreadPoolExecutor") &&
					methodName.equals("getTask")) {
				return true;
			}
			if (className.equals("sun.nio.ch.SelectorImpl") &&
					methodName.equals("select")) {
				return true;
			}
			if (className.equals("java.util.concurrent.LinkedTransferQueue") &&
					methodName.equals("poll")) {
				return true;
			}
		}

		return false;
	}

	private static int similarity(ThreadInfo threadInfo, ThreadInfo threadInfo0) {
		StackTraceElement[] s1 = threadInfo == null ? EMPTY : threadInfo.getStackTrace();
		StackTraceElement[] s2 = threadInfo0 == null ? EMPTY : threadInfo0.getStackTrace();
		int i = s1.length - 1;
		int j = s2.length - 1;
		int rslt = 0;
		while (i >= 0 && j >= 0 && s1[i].equals(s2[j])) {
			rslt++;
			i--;
			j--;
		}
		return rslt;
	}
}
