// ============================================================================
package hiconic.rx.explorer.processing.platformreflection;

import java.lang.management.ThreadInfo;

public class ExtendedThreadInfo {
	  long cpuTimeInNanos;
      long blockedCount;
      long blockedTimeInMs;
      long waitedCount;
      long waitedTimeInMs;
      boolean deltaDone;
      ThreadInfo info;

      public ExtendedThreadInfo(long cpuTimeInNanos, ThreadInfo info) {
          blockedCount = info.getBlockedCount();
          blockedTimeInMs = info.getBlockedTime();
          waitedCount = info.getWaitedCount();
          waitedTimeInMs = info.getWaitedTime();
          this.cpuTimeInNanos = cpuTimeInNanos;
          this.info = info;
      }

      void setDelta(long cpuTime, ThreadInfo info) {
          if (deltaDone) throw new IllegalStateException("setDelta already called once");
          blockedCount = info.getBlockedCount() - blockedCount;
          blockedTimeInMs = info.getBlockedTime() - blockedTimeInMs;
          waitedCount = info.getWaitedCount() - waitedCount;
          waitedTimeInMs = info.getWaitedTime() - waitedTimeInMs;
          this.cpuTimeInNanos = cpuTime - this.cpuTimeInNanos;
          deltaDone = true;
          this.info = info;
      }
}
