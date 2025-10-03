// ============================================================================
package hiconic.rx.explorer.processing.check.hw;

import java.util.List;

import com.braintribe.cfg.Configurable;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.utils.StringTools;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.VirtualMemory;

public class MemoryCheckRxProcessor implements CheckProcessor {

	private static Logger logger = Logger.getLogger(MemoryCheckRxProcessor.class);

	protected String globalMemoryAvailableWarnThreshold = "10%";
	protected String globalMemoryAvailableFailThreshold = "5%";
	protected String swapAvailableWarnThreshold = "10%";
	protected String swapAvailableFailThreshold = "5%";
	protected String javaMemoryAvailableWarnThreshold = "10%";
	protected String javaMemoryAvailableFailThreshold = "5%";

	@Override
	public CheckResult check(ServiceRequestContext context) {
		CheckResult result = CheckResult.T.create();
		List<CheckResultEntry> entries = result.getEntries();

		collectGlobalMemoryResults(entries);
		collectJavaMemoryResults(entries);

		return result;
	}

	protected void collectJavaMemoryResults(List<CheckResultEntry> entries) {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long maxMemory = runtime.maxMemory();
		long usedMemory = totalMemory - runtime.freeMemory();
		long freeMemory = maxMemory - usedMemory;

		checkMemoryLimits(entries, maxMemory, freeMemory, javaMemoryAvailableWarnThreshold, javaMemoryAvailableFailThreshold, "Java");
	}

	protected void collectGlobalMemoryResults(List<CheckResultEntry> entries) {
		SystemInfo si = new SystemInfo();
		GlobalMemory memory = si.getHardware().getMemory();

		long available = memory.getAvailable();
		long total = memory.getTotal();
		VirtualMemory virtualMemory = memory.getVirtualMemory();
		long swapTotal = virtualMemory.getSwapTotal();
		long swapUsed = virtualMemory.getSwapUsed();
		long swapAvailable = swapTotal - swapUsed;

		checkMemoryLimits(entries, total, available, globalMemoryAvailableWarnThreshold, globalMemoryAvailableFailThreshold, "Global");
		if (swapTotal > 0)
			checkMemoryLimits(entries, swapTotal, swapAvailable, swapAvailableWarnThreshold, swapAvailableFailThreshold, "Swap");
	}

	protected static void checkMemoryLimits(List<CheckResultEntry> entries, //
			long total, long available, //
			String configuredWarnThreshold, String configuredFailThreshold, //
			String descriptionPrefix) {

		CheckStatus jvmStatus = CheckStatus.ok;
		if (total <= 0) {
			jvmStatus = CheckStatus.fail;
		}

		String warnThresholdString = choose(configuredWarnThreshold, Numbers.MEGABYTE * 50);
		String failThresholdString = choose(configuredFailThreshold, Numbers.MEGABYTE * 5);

		long warnThreshold = getMemoryValue(configuredWarnThreshold, total, Numbers.MEGABYTE * 50);
		long errorThreshold = getMemoryValue(configuredFailThreshold, total, Numbers.MEGABYTE * 5);

		entries.add(createEntry(descriptionPrefix.toLowerCase() + "MemoryTotal", descriptionPrefix + " Memory Total", "" + total, jvmStatus));

		CheckStatus availableStatus = CheckStatus.ok;
		if (available < warnThreshold) {
			availableStatus = CheckStatus.warn;
		}
		if (available < errorThreshold) {
			availableStatus = CheckStatus.fail;
		}
		entries.add(createEntry(descriptionPrefix.toLowerCase() + "MemoryAvailable",
				descriptionPrefix + " Memory Available (warning threshold: " + warnThresholdString + ", fail threshold: " + failThresholdString + ")",
				"" + available, availableStatus));
	}

	protected static String choose(String configuredThreshold, long defaultValue) {
		if (StringTools.isEmpty(configuredThreshold))
			return StringTools.prettyPrintBytes(defaultValue);
		else
			return configuredThreshold;
	}

	protected static long getMemoryValue(String valueString, long total, long defaultValue) {
		if (valueString == null) {
			return defaultValue;
		}
		valueString = valueString.trim();
		if (valueString.length() == 0) {
			return defaultValue;
		}
		if (valueString.endsWith("%")) {
			String percentageString = valueString.substring(0, valueString.length() - 1);
			try {
				double percentage = Double.parseDouble(percentageString);
				percentage = percentage / 100;
				return (long) (total * percentage);
			} catch (Exception e) {
				logger.error("Could not parse percentage " + percentageString, e);
				return defaultValue;
			}
		} else {
			try {
				long result = StringTools.parseBytesString(valueString);
				return result;
			} catch (Exception e) {
				logger.error("Could not parse value " + valueString, e);
				return defaultValue;
			}
		}
	}

	protected static CheckResultEntry createEntry(String name, String details, String msg, CheckStatus cs) {
		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName(name);
		entry.setDetails(details);
		entry.setMessage(msg);
		entry.setCheckStatus(cs);
		return entry;
	}

	@Configurable
	public void setGlobalMemoryAvailableWarnThreshold(String globalMemoryAvailableWarnThreshold) {
		if (globalMemoryAvailableWarnThreshold != null) {
			this.globalMemoryAvailableWarnThreshold = globalMemoryAvailableWarnThreshold;
		}
	}
	@Configurable
	public void setGlobalMemoryAvailableFailThreshold(String globalMemoryAvailableFailThreshold) {
		if (globalMemoryAvailableFailThreshold != null) {
			this.globalMemoryAvailableFailThreshold = globalMemoryAvailableFailThreshold;
		}
	}
	@Configurable
	public void setSwapAvailableWarnThreshold(String swapAvailableWarnThreshold) {
		if (swapAvailableWarnThreshold != null) {
			this.swapAvailableWarnThreshold = swapAvailableWarnThreshold;
		}
	}
	@Configurable
	public void setSwapAvailableFailThreshold(String swapAvailableFailThreshold) {
		if (swapAvailableFailThreshold != null) {
			this.swapAvailableFailThreshold = swapAvailableFailThreshold;
		}
	}
	@Configurable
	public void setJavaMemoryAvailableWarnThreshold(String javaMemoryAvailableWarnThreshold) {
		if (javaMemoryAvailableWarnThreshold != null) {
			this.javaMemoryAvailableWarnThreshold = javaMemoryAvailableWarnThreshold;
		}
	}
	@Configurable
	public void setJavaMemoryAvailableFailThreshold(String javaMemoryAvailableFailThreshold) {
		if (javaMemoryAvailableFailThreshold != null) {
			this.javaMemoryAvailableFailThreshold = javaMemoryAvailableFailThreshold;
		}
	}

}
