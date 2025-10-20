// ============================================================================
package hiconic.rx.explorer.processing.platformreflection;

import java.util.Comparator;

public class ExtendedThreadInfoComparator implements Comparator<ExtendedThreadInfo> {
	
	private final String type;

	public ExtendedThreadInfoComparator(String type) {
		this.type = type;
	}

	@Override
	public int compare(ExtendedThreadInfo o1, ExtendedThreadInfo o2) {
		 if ("cpu".equals(type)) {
             return (int) (o2.cpuTimeInNanos - o1.cpuTimeInNanos);
         } else if ("wait".equals(type)) {
             return (int) (o2.waitedTimeInMs - o1.waitedTimeInMs);
         } else if ("block".equals(type)) {
             return (int) (o2.blockedTimeInMs - o1.blockedTimeInMs);
         }
         throw new IllegalArgumentException("expected thread type to be either 'cpu', 'wait', or 'block', but was " + type);
	}

}
