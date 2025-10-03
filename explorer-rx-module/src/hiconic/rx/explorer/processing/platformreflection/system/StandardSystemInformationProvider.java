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
package hiconic.rx.explorer.processing.platformreflection.system;

import static java.util.Collections.emptyMap;

import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.nio.file.FileStore;
import oshi.software.os.FileSystem;
import java.nio.file.FileSystems;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.exception.Exceptions;
import com.braintribe.execution.monitoring.ThreadPoolMonitoring;
import com.braintribe.execution.monitoring.ThreadPoolStatistics;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.bootstrapping.TribefireRuntime;
import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.transport.messaging.api.MessagingSessionProvider;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.MathTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.CollectionTools2;

import hiconic.rx.explorer.processing.platformreflection.db.DatabaseInformationProvider;
import hiconic.rx.explorer.processing.platformreflection.db.StandardDatabaseInformationProvider;
import hiconic.rx.reflection.model.application.Concurrency;
import hiconic.rx.reflection.model.application.Messaging;
import hiconic.rx.reflection.model.check.cpu.Cpu;
import hiconic.rx.reflection.model.check.cpu.CpuLoad;
import hiconic.rx.reflection.model.check.io.IoMeasurement;
import hiconic.rx.reflection.model.check.io.IoMeasurements;
import hiconic.rx.reflection.model.check.io.MeasurementType;
import hiconic.rx.reflection.model.check.java.JavaEnvironment;
import hiconic.rx.reflection.model.check.power.PowerSource;
import hiconic.rx.reflection.model.db.DatabaseInformation;
import hiconic.rx.reflection.model.system.SystemInfo;
import hiconic.rx.reflection.model.system.disk.DiskInfo;
import hiconic.rx.reflection.model.system.disk.FileSystemDetailInfo;
import hiconic.rx.reflection.model.system.disk.FileSystemDetailStore;
import hiconic.rx.reflection.model.system.disk.FileSystemInfo;
import hiconic.rx.reflection.model.system.disk.Partition;
import hiconic.rx.reflection.model.system.hardware.Baseboard;
import hiconic.rx.reflection.model.system.hardware.ComputerSystem;
import hiconic.rx.reflection.model.system.hardware.Firmware;
import hiconic.rx.reflection.model.system.memory.Memory;
import hiconic.rx.reflection.model.system.network.NetworkInterface;
import hiconic.rx.reflection.model.system.network.NetworkParams;
import hiconic.rx.reflection.model.system.os.OperatingSystem;
import hiconic.rx.reflection.model.system.os.Process;
import hiconic.rx.reflection.model.system.threadpools.ThreadPool;
import hiconic.rx.reflection.model.system.threadpools.ThreadPools;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.Sensors;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.util.FormatUtil;
import tribefire.cortex.leadership.api.LeadershipManager;

public class StandardSystemInformationProvider implements SystemInformationProvider {

	private static Logger logger = Logger.getLogger(StandardSystemInformationProvider.class);

	private static DateTimeFormatter oshiDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.US);

	private DatabaseInformationProvider databaseInformationProvider;

	private static boolean ignoreSensors = false;

	private Supplier<MessagingSessionProvider> messagingSessionProviderSupplier;
	private Locking locking;
	private LeadershipManager leadershipManager;

	private class SystemInfoContext {
		long[] prevTicks;
		long prevTicksTimestamp;
		long[][] prevProcTicks;

		private SystemInfoContext(oshi.SystemInfo si) {
			CentralProcessor processor = si.getHardware().getProcessor();
			this.prevTicks = processor.getSystemCpuLoadTicks();
			this.prevTicksTimestamp = System.currentTimeMillis();
			this.prevProcTicks = processor.getProcessorCpuLoadTicks();
		}
	}

	@Override
	public SystemInfo get() throws RuntimeException {

		logger.debug(() -> "Compiling system information.");

		try {
			oshi.SystemInfo si = new oshi.SystemInfo();

			SystemInfoContext context = new SystemInfoContext(si);

			SystemInfo system = SystemInfo.T.create();

			exec(() -> system.setDisks(this.getDiskInfos(si)), "Disk Info");
			exec(() -> system.setComputerSystem(this.getComputerSystem(si)), "Computer");
			exec(() -> system.setFileSystems(this.getFileSystemInfo()), "File System");

			exec(() -> system.setFileSystemDetailInfo(this.getFileSystemDetailInfo(si)), "File System Details");

			exec(() -> system.setOperatingSystem(this.getOperatingSystemInfo(si)), "OS");
			exec(() -> system.setMemory(this.getMemoryInfo(si)), "Memory");
			exec(() -> system.setNetworkInterfaces(this.getNetworkInfo(si)), "Network");
			exec(() -> system.setNetworkParams(this.getNetworkParams(si)), "Network Params");
			exec(() -> system.setPowerSources(this.getPowerInfo(si)), "Power");
			exec(() -> system.setJavaProcesses(this.getJavaProcesses(si)), "Java Processes");
			exec(() -> system.setJavaEnvironment(this.getJavaEnvironment(si)), "Java Env");
			exec(() -> system.setIoMeasurements(this.getIoMeasurements()), "I/O");
			exec(() -> system.setDatabaseInformation(this.getDatabaseInformation()), "Database");
			exec(() -> system.setThreadPools(this.getThreadPools()), "Threadpools");
			exec(() -> system.setFontFamilies(this.getFontFamilies()), "Fonts");
			exec(() -> system.setMessaging(this.getMessagingInformation()), "Messaging");
			exec(() -> system.setConcurrency(this.getConcurrency()), "Concurrency");
			exec(() -> system.setCpu(this.getCpuInfo(si, context)), "CPU");

			return system;
		} finally {
			logger.debug(() -> "Done with compiling system information.");
		}
	}

	private static void exec(Runnable r, String context) {
		try {
			r.run();
		} catch (Throwable t) {
			logger.warn(() -> "Error while trying to get system information: " + context, t);
		}
	}

	private Concurrency getConcurrency() {

		Concurrency c = Concurrency.T.create();

		if (locking != null) {
			c.setLockManagerDescription(locking.getClass().getSimpleName());
		} else {
			c.setLockManagerDescription("Undefined");
		}

		if (leadershipManager != null) {
			c.setLeadershipManagerDescription(this.leadershipManager.description());
		} else {
			c.setLeadershipManagerDescription("Undefined");
		}
		return c;
	}

	private Messaging getMessagingInformation() {
		Messaging messaging = Messaging.T.create();

		if (messagingSessionProviderSupplier == null) {
			messaging.setMessagingDescription("Undefined");
		} else {
			MessagingSessionProvider messagingSessionProvider = messagingSessionProviderSupplier.get();
			if (messagingSessionProvider == null) {
				messaging.setMessagingDescription("Undefined");
			} else {
				messaging.setMessagingDescription(messagingSessionProvider.description());
			}
		}

		return messaging;
	}

	private List<String> getFontFamilies() {
		List<String> fontList = new ArrayList<>();
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			if (ge != null) {
				String fonts[] = ge.getAvailableFontFamilyNames();
				if (fonts != null && fonts.length > 0) {
					fontList = CollectionTools2.asList(fonts);
				}
			}
		} catch (Exception e) {
			logger.debug(() -> "Could not determine installed font families.", e);
		}
		return fontList;
	}

	private ThreadPools getThreadPools() {

		ThreadPools tps = ThreadPools.T.create();

		List<ThreadPoolStatistics> statistics = ThreadPoolMonitoring.getStatistics();

		TreeMap<String, List<ThreadPool>> sortedStatistics = new TreeMap<>();

		for (ThreadPoolStatistics stats : statistics) {

			String name = stats.getDescription();
			if (name == null) {
				name = "n/a";
			}

			ThreadPool threadPool = ThreadPool.T.create();
			threadPool.setName(name);
			threadPool.setActiveThreads(stats.currentlyRunning());
			threadPool.setAverageRunningTimeMs(stats.averageRunningTimeInMs());
			threadPool.setCorePoolSize(stats.getCorePoolSize());
			threadPool.setMaxPoolSize(stats.getMaximumPoolSize());
			threadPool.setPendingTasksInQueue(stats.getPendingTasksInQueue());
			threadPool.setTotalExecutions(stats.totalExecutions());
			threadPool.setPoolSize(stats.getPoolSize());
			threadPool.setTimeSinceLastExecutionMs(stats.timeSinceLastExecutionInMs());
			threadPool.setAveragePendingTimeInMs(getDoubleSafely(stats.getAverageEnqueuedTimeInMs()));
			threadPool.setMinimumEnqueuedTimeInMs(stats.getMinimumEnqueuedTimeInMs());
			threadPool.setMaximumEnqueuedTimeInMs(stats.getMaximumEnqueuedTimeInMs());

			List<ThreadPool> list = sortedStatistics.computeIfAbsent(name, n -> new ArrayList<>());
			list.add(threadPool);
		}

		sortedStatistics.values().forEach(l -> tps.getThreadPools().addAll(l));

		return tps;
	}

	private Double getDoubleSafely(Double value) {
		if (value == null) {
			return -1d;
		} else if (value.isNaN()) {
			return -1d;
		} else {
			return value;
		}
	}

	private DatabaseInformation getDatabaseInformation() {
		return databaseInformationProvider.get();
	}

	protected IoMeasurements getIoMeasurements() {

		logger.debug(() -> "Getting I/O measurements.");
		try {
			IoMeasurements result = null;

			String diskForTesting = BackgroundIoPerformanceMeasurement.diskForTesting;

			Double diskReadSpeedGBPerSecond = BackgroundIoPerformanceMeasurement.diskReadSpeedGBPerSecond;
			if (diskReadSpeedGBPerSecond != null && diskForTesting != null) {

				result = IoMeasurements.T.create();

				IoMeasurement diskRead = IoMeasurement.T.create();
				diskRead.setMeasurementType(MeasurementType.disk);
				diskRead.setSpeed(String.format("%.1f GB/s", diskReadSpeedGBPerSecond));
				diskRead.setDescription("Reading " + BackgroundIoPerformanceMeasurement.SIZE_GB + " GB from '" + diskForTesting + "'");
				result.getMeasurements().add(diskRead);

			}

			Double diskWriteSpeedGBPerSecond = BackgroundIoPerformanceMeasurement.diskWriteSpeedGBPerSecond;
			if (diskWriteSpeedGBPerSecond != null && diskForTesting != null) {

				if (result == null) {
					result = IoMeasurements.T.create();
				}

				IoMeasurement diskWrite = IoMeasurement.T.create();
				diskWrite.setMeasurementType(MeasurementType.disk);
				diskWrite.setSpeed(String.format("%.1f GB/s", diskWriteSpeedGBPerSecond));
				diskWrite.setDescription("Writing " + BackgroundIoPerformanceMeasurement.SIZE_GB + " GB to '" + diskForTesting + "'");
				result.getMeasurements().add(diskWrite);

			}

			Long httpDownloadSize = BackgroundIoPerformanceMeasurement.httpDownloadSize;
			Double httpDownloadSpeedKBPerSecond = BackgroundIoPerformanceMeasurement.httpDownloadSpeedKBPerSecond;
			if (httpDownloadSpeedKBPerSecond != null && httpDownloadSize != null) {

				if (result == null) {
					result = IoMeasurements.T.create();
				}

				IoMeasurement httpDownload = IoMeasurement.T.create();
				httpDownload.setMeasurementType(MeasurementType.internet);
				httpDownload.setSpeed(String.format("%.1f kB/s", httpDownloadSpeedKBPerSecond));
				httpDownload.setDescription("Downloading " + StringTools.prettyPrintBytesDecimal(httpDownloadSize));
				result.getMeasurements().add(httpDownload);

			}

			return result;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get I/O measurement information.", t);
		} finally {
			logger.debug(() -> "Done with getting I/O measurements.");
		}

		return null;
	}

	protected List<Process> getJavaProcesses(oshi.SystemInfo si) {
		try {
			return ProcessesProvider.getProcesses(si, true);
		} catch (Throwable t) {
			logger.debug(() -> "Could not get Java process information.", t);
		}
		return null;
	}

	protected JavaEnvironment getJavaEnvironment(oshi.SystemInfo si) {

		logger.debug(() -> "Getting Java environment information.");

		try {
			JavaEnvironment je = JavaEnvironment.T.create();

			MemoryMXBean mmx = ManagementFactory.getMemoryMXBean();
			MemoryUsage nonHeapMemoryUsage = mmx.getNonHeapMemoryUsage();
			MemoryUsage heapMemoryUsage = mmx.getHeapMemoryUsage();

			String localProcessName = ManagementFactory.getRuntimeMXBean().getName();
			int idx = localProcessName.indexOf("@");
			int pid = -1;
			if (idx > 0) {
				String pidString = localProcessName.substring(0, idx);
				try {
					pid = Integer.parseInt(pidString);
				} catch (Exception e) {
					logger.debug(() -> "Could not parse pid from " + localProcessName);
				}
			}

			Runtime runtime = Runtime.getRuntime();
			long totalMemory = runtime.totalMemory();
			long maxMemory = runtime.maxMemory();
			long usedMemory = totalMemory - runtime.freeMemory();
			long freeMemory = maxMemory - usedMemory;
			long initMemory = heapMemoryUsage.getInit();

			if (pid != -1) {
				OSProcess process = si.getOperatingSystem().getProcess(pid);
				long residentSetSize = process.getResidentSetSize();
				long virtualSize = process.getVirtualSize();

				je.setResidentSetSize(residentSetSize);
				je.setResidentSetSizeInGb(MathTools.getNumberInG(residentSetSize, true, 2));
				je.setVirtualSize(virtualSize);
				je.setVirtualSizeInGb(MathTools.getNumberInG(virtualSize, true, 2));
			} else {
				je.setResidentSetSize(-1L);
				je.setResidentSetSizeInGb(-1.0d);
				je.setVirtualSize(-1L);
				je.setVirtualSizeInGb(-1.0d);
			}

			long totalMemoryNonHeap = nonHeapMemoryUsage.getCommitted();
			long maxMemoryNonHeap = nonHeapMemoryUsage.getMax();
			long usedMemoryNonHeap = nonHeapMemoryUsage.getUsed();
			long initMemoryNonHeap = nonHeapMemoryUsage.getInit();

			je.setTotalMemory(totalMemory);
			je.setTotalMemoryInGb(MathTools.getNumberInG(totalMemory, true, 2));
			je.setFreeMemory(freeMemory);
			je.setFreeMemoryInGb(MathTools.getNumberInG(freeMemory, true, 2));
			je.setUsedMemory(usedMemory);
			je.setUsedMemoryInGb(MathTools.getNumberInG(usedMemory, true, 2));
			je.setMaxMemory(maxMemory);
			je.setMaxMemoryInGb(MathTools.getNumberInG(maxMemory, true, 2));
			je.setInitMemory(initMemory);
			if (initMemory == -1) {
				je.setInitMemoryInGb(-1.0);
			} else {
				je.setInitMemoryInGb(MathTools.getNumberInG(initMemory, true, 4));
			}

			je.setTotalMemoryNonHeap(totalMemoryNonHeap);
			je.setTotalMemoryNonHeapInGb(MathTools.getNumberInG(totalMemoryNonHeap, true, 2));
			je.setUsedMemoryNonHeap(usedMemoryNonHeap);
			je.setUsedMemoryNonHeapInGb(MathTools.getNumberInG(usedMemoryNonHeap, true, 2));
			je.setMaxMemoryNonHeap(maxMemoryNonHeap);
			if (maxMemoryNonHeap == -1) {
				je.setMaxMemoryNonHeapInGb(-1.0);
			} else {
				je.setMaxMemoryNonHeapInGb(MathTools.getNumberInG(maxMemoryNonHeap, true, 2));
			}
			je.setInitMemoryNonHeap(initMemoryNonHeap);
			if (initMemoryNonHeap == -1) {
				je.setInitMemoryNonHeapInGb(-1.0);
			} else {
				je.setInitMemoryNonHeapInGb(MathTools.getNumberInG(initMemoryNonHeap, true, 4));
			}

			je.setAvailableProcessors(runtime.availableProcessors());

			oshi.software.os.OperatingSystem operatingSystem = si.getOperatingSystem();
			je.setElevatedPrivileges(operatingSystem.isElevated());

			RuntimeMXBean rmx = ManagementFactory.getRuntimeMXBean();
			Properties sysProps = System.getProperties();

			try {
				je.setBootClassPath(rmx.getBootClassPath());
			} catch (UnsupportedOperationException uoe) {
				// Not supported in Java 10 (maybe 9 as well?)
			}
			je.setClassPath(rmx.getClassPath());
			je.setInputArguments(rmx.getInputArguments());
			je.setLibraryPath(rmx.getLibraryPath());
			je.setManagementSpecVersion(rmx.getManagementSpecVersion());
			je.setName(rmx.getName());
			je.setSpecName(rmx.getSpecName());
			je.setSpecVendor(rmx.getSpecVendor());
			je.setSpecVersion(rmx.getSpecVersion());
			long startTimeLong = rmx.getStartTime();
			if (startTimeLong > 0) {
				Date startTime = new Date(startTimeLong);
				je.setStartTime(startTime);
			}
			je.setSystemProperties(filterSystemProperties(rmx));
			je.setEnvironmentVariables(getFilteredEnvironmentVariables());
			long uptime = rmx.getUptime();
			je.setUptime(uptime);
			if (uptime > 0) {
				je.setUptimeDisplay(StringTools.prettyPrintMilliseconds(uptime, true));
			}
			je.setVmName(rmx.getVmName());
			je.setVmVendor(rmx.getVmVendor());
			je.setVmVersion(rmx.getVmVersion());
			je.setBootClassPathSupported(rmx.isBootClassPathSupported());
			je.setJavaVersion(sysProps.getProperty("java.version"));
			je.setJavaVendor(sysProps.getProperty("java.vendor"));
			je.setJavaVendorUrl(sysProps.getProperty("java.vendor.url"));
			je.setJavaHome(sysProps.getProperty("java.home"));
			je.setJavaClassVersion(sysProps.getProperty("java.class.version"));
			je.setTmpDir(sysProps.getProperty("java.io.tmpdir"));
			je.setJavaCompiler(sysProps.getProperty("java.compiler"));
			je.setUsername(sysProps.getProperty("user.name"));
			je.setUserHome(sysProps.getProperty("user.home"));
			je.setWorkingDir(sysProps.getProperty("user.dir"));

			try {
				int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
				je.setThreadCount(threadCount);
			} catch (Exception e) {
				logger.trace(() -> "Could not get thread count.", e);
			}

			return je;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get Java environment information.", t);
		} finally {
			logger.debug(() -> "Done with getting Java environment information.");
		}
		return null;
	}

	private Map<String, String> filterSystemProperties(RuntimeMXBean rmx) {
		return getFilteredMap(rmx.getSystemProperties());
	}

	private Map<String, String> getFilteredEnvironmentVariables() {
		return getFilteredMap(System.getenv());
	}

	public static Map<String, String> getFilteredMap(Map<String, String> sourceMap) {
		if (sourceMap == null)
			return emptyMap();

		Map<String, String> filteredMap = new HashMap<>();

		for (Entry<String, String> e : sourceMap.entrySet()) {
			String name = e.getKey();
			String value = e.getValue();

			if (name == null)
				continue;

			if (needsObfuscation(name))
				value = StringTools.simpleObfuscatePassword(value == null ? "null" : value);

			filteredMap.put(name, value);
		}

		return filteredMap;
	}

	private static boolean needsObfuscation(String name) {
		String lcName = name.toLowerCase();

		return TribefireRuntime.isPropertyPrivate(name) || //
				lcName.endsWith("password") || //
				lcName.endsWith("pwd") || //
				lcName.endsWith("key") || //
				lcName.endsWith("secret") || //
				lcName.endsWith("token") || //
				lcName.endsWith("credential") || //
				lcName.endsWith("credentials");
	}

	protected List<PowerSource> getPowerInfo(oshi.SystemInfo si) {

		logger.debug(() -> "Getting power information.");

		try {
			List<PowerSource> powerSourceList = new ArrayList<PowerSource>();

			List<oshi.hardware.PowerSource> powerSources = si.getHardware().getPowerSources();
			if (powerSources != null) {
				for (oshi.hardware.PowerSource powerSource : powerSources) {

					double timeRemaining = powerSource.getTimeRemainingEstimated();

					PowerSource ps = PowerSource.T.create();

					ps.setName(powerSource.getName());
					double remainingCapacityPercent = powerSource.getRemainingCapacityPercent();
					ps.setRemainingCapacityInPercent((int) remainingCapacityPercent);
					ps.setTimeRemainingInSeconds(timeRemaining);

					if (timeRemaining > 0) {
						GregorianCalendar gc = new GregorianCalendar();
						gc.add(Calendar.SECOND, (int) timeRemaining);
						Date shutdownDate = gc.getTime();
						ps.setShutdownTime(shutdownDate);
					}
					powerSourceList.add(ps);
				}
			}

			return powerSourceList;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get power information.", t);
		} finally {
			logger.debug(() -> "Done with getting power information.");
		}
		return null;
	}

	protected List<NetworkInterface> getNetworkInfo(oshi.SystemInfo si) {

		logger.debug(() -> "Getting network interfaces information.");

		try {
			List<NetworkInterface> networkInterfaces = new ArrayList<NetworkInterface>();

			List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs();
			if (networkIFs != null) {
				for (NetworkIF networkIF : networkIFs) {
					// This will re-validate the information within the NetworkIF
					networkIF.queryNetworkInterface();
					long speed = networkIF.getSpeed();

					NetworkInterface ni = NetworkInterface.T.create();
					ni.setDisplayName(networkIF.getDisplayName());
					ni.setMacAddress(networkIF.getMacaddr());
					ni.setMtu(networkIF.getMTU());
					ni.setName(networkIF.getName());
					ni.setSpeed(speed);
					ni.setIPv4Addresses(Arrays.asList(networkIF.getIPv4addr()));
					ni.setIPv6Addresses(Arrays.asList(networkIF.getIPv6addr()));
					ni.setSpeedDisplay(StringTools.humanReadableByteCount(speed, true) + "/s");

					long bytesRecv = networkIF.getBytesRecv();
					ni.setBytesRecv(bytesRecv);
					ni.setBytesRecvInGb(MathTools.getNumberInG(bytesRecv, true, 2));
					long bytesSent = networkIF.getBytesSent();
					ni.setBytesSent(bytesSent);
					ni.setBytesSentInGb(MathTools.getNumberInG(bytesSent, true, 2));
					ni.setPacketsRecv(networkIF.getPacketsRecv());
					ni.setPacketsSent(networkIF.getPacketsSent());

					networkInterfaces.add(ni);
				}
			}

			return networkInterfaces;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get network information.", t);
		} finally {
			logger.debug(() -> "Done with getting network interfaces information.");
		}
		return null;
	}

	protected NetworkParams getNetworkParams(oshi.SystemInfo si) {

		logger.debug(() -> "Getting network parameters information.");

		oshi.software.os.NetworkParams np = si.getOperatingSystem().getNetworkParams();

		NetworkParams networkParams = NetworkParams.T.create();

		// TODO: the lookup takes some seconds - at least on MacOS
		networkParams.setHostName(np.getHostName());
		networkParams.setDomainName(np.getDomainName());
		networkParams.setDnsServers(Arrays.asList(np.getDnsServers()));
		networkParams.setIpv4DefaultGateway(np.getIpv4DefaultGateway());
		networkParams.setIpv6DefaultGateway(np.getIpv6DefaultGateway());

		logger.debug(() -> "Done with getting network parameters information.");

		return networkParams;
	}

	protected Cpu getCpuInfo(oshi.SystemInfo si, SystemInfoContext context) {

		logger.debug(() -> "Getting CPU information.");

		try {
			CentralProcessor processor = si.getHardware().getProcessor();

			ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

			Cpu cpu = Cpu.T.create();
			cpu.setCpu64bit(processorIdentifier.isCpu64bit());
			cpu.setFamily(processorIdentifier.getFamily());
			cpu.setIdentifier(processorIdentifier.getIdentifier());
			cpu.setLogicalProcessorCount(processor.getLogicalProcessorCount());
			cpu.setModel(processorIdentifier.getModel());
			cpu.setName(processorIdentifier.getName());
			cpu.setPhysicalProcessorCount(processor.getPhysicalProcessorCount());
			long systemUptime = si.getOperatingSystem().getSystemUptime();
			if (systemUptime > 0) {
				GregorianCalendar cal = new GregorianCalendar();
				cal.add(Calendar.SECOND, (int) -systemUptime);
				Date bootDate = cal.getTime();
				cpu.setSystemBootTime(bootDate);
			}

			CpuLoad cpuLoad = getCpuLoad(processor, context);
			cpu.setCpuLoad(cpuLoad);

			long[] freqs = processor.getCurrentFreq();
			if (freqs[0] > 0) {
				for (int i = 0; i < freqs.length; i++) {
					if (i > 0) {
						cpu.getCurrentFrequencies().add(FormatUtil.formatHertz(freqs[i]));
					}
				}
			}

			oshi.hardware.ComputerSystem cs = si.getHardware().getComputerSystem();
			cpu.setSystemSerialNumber(cs.getSerialNumber());
			cpu.setVendor(processorIdentifier.getVendor());
			cpu.setVendorFreq(processorIdentifier.getVendorFreq());
			cpu.setVendorFreqInGh(MathTools.getNumberInG(processorIdentifier.getVendorFreq(), true, 2));
			cpu.setMaxFreq(processor.getMaxFreq());
			cpu.setMaxFreqInGh(MathTools.getNumberInG(processor.getMaxFreq(), true, 2));
			cpu.setProcessorId(processorIdentifier.getProcessorID());

			if (!ignoreSensors) {
				Sensors sensors = si.getHardware().getSensors();
				if (sensors != null) {
					double cpuTemperature = sensors.getCpuTemperature();
					cpu.setCpuTemperature(cpuTemperature);
					double cpuVoltage = sensors.getCpuVoltage();
					cpu.setCpuVoltage(cpuVoltage);
					int[] fanSpeeds = sensors.getFanSpeeds();
					if (fanSpeeds != null) {
						List<Integer> intList = new ArrayList<Integer>();
						for (int fanSpeed : fanSpeeds) {
							intList.add(fanSpeed);
						}
						cpu.setFanSpeeds(intList);
					}

					if (cpuTemperature == 0d && cpuVoltage == 0d && (fanSpeeds == null || fanSpeeds.length == 0)) {
						ignoreSensors = true;
					}
				}
			}

			return cpu;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get CPU information.", t);
		} finally {
			logger.debug(() -> "Done with getting CPU information.");
		}
		return null;
	}

	private CpuLoad getCpuLoad(CentralProcessor processor, SystemInfoContext context) {

		CpuLoad cpuLoad = CpuLoad.T.create();

		cpuLoad.setContextSwitches(processor.getContextSwitches());
		cpuLoad.setInterrupts(processor.getInterrupts());

		long[] prevTicks = context.prevTicks;
		long[][] prevProcTicks = context.prevProcTicks;
		long timeSpentSoFar = System.currentTimeMillis() - context.prevTicksTimestamp;
		long remainWaitTime = 1000l - timeSpentSoFar;
		if (remainWaitTime > 0) {
			try {
				Thread.sleep(remainWaitTime);
			} catch (InterruptedException ie) {
				throw Exceptions.unchecked(ie, "Got interrupted while waiting for CPU ticks");
			}
		}

		long[] ticks = processor.getSystemCpuLoadTicks();

		long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
		long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
		long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
		long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
		long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
		long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
		long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
		long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
		long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

		cpuLoad.setUser(MathTools.roundToDecimals(100d * user / totalCpu, 2));
		cpuLoad.setNice(MathTools.roundToDecimals(100d * nice / totalCpu, 2));
		cpuLoad.setSys(MathTools.roundToDecimals(100d * sys / totalCpu, 2));
		cpuLoad.setIdle(MathTools.roundToDecimals(100d * idle / totalCpu, 2));
		cpuLoad.setIoWait(MathTools.roundToDecimals(100d * iowait / totalCpu, 2));
		cpuLoad.setIrq(MathTools.roundToDecimals(100d * irq / totalCpu, 2));
		cpuLoad.setSoftIrq(MathTools.roundToDecimals(100d * softirq / totalCpu, 2));
		cpuLoad.setSteal(MathTools.roundToDecimals(100d * steal / totalCpu, 2));

		double cpuLoadBetweenTicks = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100d;

		cpuLoad.setTotalCpu(MathTools.roundToDecimals(cpuLoadBetweenTicks, 2));

		double[] systemLoadAverage;
		systemLoadAverage = processor.getSystemLoadAverage(3);
		if (systemLoadAverage != null && systemLoadAverage.length >= 3) {
			cpuLoad.setSystemLoadAverage1Minute(MathTools.roundToDecimals(systemLoadAverage[0], 2));
			cpuLoad.setSystemLoadAverage5Minutes(MathTools.roundToDecimals(systemLoadAverage[1], 2));
			cpuLoad.setSystemLoadAverage15Minutes(MathTools.roundToDecimals(systemLoadAverage[2], 2));
		}

		// per core CPU
		double[] loadPerProcessorArray = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
		if (loadPerProcessorArray != null) {
			for (double d : loadPerProcessorArray) {
				cpuLoad.getSystemLoadPerProcessor().add(MathTools.roundToDecimals(d * 100, 2));
			}
		}

		return cpuLoad;
	}

	protected Memory getMemoryInfo(oshi.SystemInfo si) {

		logger.debug(() -> "Getting memory information.");
		try {
			GlobalMemory memory = si.getHardware().getMemory();
			if (memory != null) {
				Memory m = Memory.T.create();

				VirtualMemory virtualMemory = memory.getVirtualMemory();

				long available = memory.getAvailable();
				long total = memory.getTotal();
				long swapTotal = virtualMemory.getSwapTotal();
				long swapUsed = virtualMemory.getSwapUsed();

				m.setAvailable(available);
				m.setSwapTotal(swapTotal);
				m.setSwapUsed(swapUsed);
				m.setTotal(total);

				m.setAvailableInGb(MathTools.getNumberInG(available, true, 2));
				m.setTotalInGb(MathTools.getNumberInG(total, true, 2));
				m.setSwapTotalInGb(MathTools.getNumberInG(swapTotal, true, 2));
				m.setSwapUsedInGb(MathTools.getNumberInG(swapUsed, true, 2));

				List<PhysicalMemory> physicalMemory = memory.getPhysicalMemory();
				if (physicalMemory != null) {
					for (PhysicalMemory pm : physicalMemory) {
						String info = pm.toString();
						if (!StringTools.isBlank(info)) {
							m.getMemoryBanksInformation().add(info);
						}
					}
				}

				return m;
			} else {
				return null;
			}

		} catch (Throwable t) {
			logger.debug(() -> "Could not get memory information.", t);
		} finally {
			logger.debug(() -> "Done with getting memory information.");
		}

		return null;
	}

	protected OperatingSystem getOperatingSystemInfo(oshi.SystemInfo si) {

		logger.debug(() -> "Getting OS information.");
		try {
			oshi.software.os.OperatingSystem siOs = si.getOperatingSystem();

			OperatingSystem os = OperatingSystem.T.create();
			os.setFamily(siOs.getFamily());
			os.setManufacturer(siOs.getManufacturer());
			os.setProcessCount(siOs.getProcessCount());
			os.setThreadCount(siOs.getThreadCount());
			os.setBitness(siOs.getBitness());

			String vmIdentifier = new VirtualMachineDetector().identifyVM(si.getHardware());
			os.setHostSystem(vmIdentifier);

			OSVersionInfo version = siOs.getVersionInfo();
			if (version != null) {
				os.setVersion(version.getVersion());
				String codeName = version.getCodeName();
				if (codeName != null && codeName.trim().length() > 0) {
					os.setCodeName(codeName);
				}
				os.setBuildNumber(version.getBuildNumber());
			}
			os.setArchitecture(System.getProperty("os.arch"));

			os.setSystemTime(new Date());
			os.setSystemTimeAsString(DateTools.encode(new Date(), DateTools.ISO8601_DATE_WITH_MS_FORMAT));

			Locale defaultLocale = Locale.getDefault();
			if (defaultLocale != null) {
				hiconic.rx.reflection.model.system.os.Locale locale = hiconic.rx.reflection.model.system.os.Locale.T.create();
				os.setDefaultLocale(locale);
				locale.setName(defaultLocale.toString());
				locale.setDisplayName(defaultLocale.getDisplayName());
				locale.setCountry(defaultLocale.getCountry());
				locale.setDisplayCountry(defaultLocale.getDisplayCountry());
				locale.setLanguage(defaultLocale.getLanguage());
				locale.setDisplayLanguage(defaultLocale.getDisplayLanguage());
			}
			os.setNumberOfAvailableLocales(Locale.getAvailableLocales().length);

			return os;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get OS information.", t);
		} finally {
			logger.debug(() -> "Done with getting OS information.");
		}
		return null;
	}

	protected List<DiskInfo> getDiskInfos(oshi.SystemInfo si) {

		logger.debug(() -> "Getting disk information.");

		List<DiskInfo> diskInfos = new ArrayList<DiskInfo>();
		try {
			HardwareAbstractionLayer hardware = si.getHardware();
			List<HWDiskStore> diskStores = hardware.getDiskStores();
			if (diskStores != null) {
				for (HWDiskStore diskStore : diskStores) {

					DiskInfo di = DiskInfo.T.create();

					di.setModel(diskStore.getModel());
					di.setDiskName(diskStore.getName());
					di.setSerial(diskStore.getSerial());

					long readBytes = diskStore.getReadBytes();
					di.setReadBytes(readBytes);
					di.setReadBytesInGb(MathTools.getNumberInG(readBytes, true, 2));
					di.setReads(diskStore.getReads());
					di.setTimeStamp(diskStore.getTimeStamp());
					di.setTransferTime(diskStore.getTransferTime());
					long writeBytes = diskStore.getWriteBytes();
					di.setWriteBytes(writeBytes);
					di.setWriteBytesInGb(MathTools.getNumberInG(writeBytes, true, 2));
					di.setWrites(diskStore.getWrites());

					long size = diskStore.getSize();
					di.setCapacityInBytes(size);
					di.setCapacityInGb(MathTools.getNumberInG(size, true, 2));

					List<HWPartition> partitions = diskStore.getPartitions();
					if (partitions != null) {
						List<Partition> partitionList = new ArrayList<Partition>();
						for (HWPartition partition : partitions) {

							Partition p = Partition.T.create();
							p.setIdentification(partition.getIdentification());
							p.setMountPoint(partition.getMountPoint());
							p.setName(partition.getName());
							long pSize = partition.getSize();
							p.setSize(pSize);
							p.setSizeInGb(MathTools.getNumberInG(pSize, true, 2));
							p.setType(partition.getType());
							p.setUuid(partition.getUuid());
							partitionList.add(p);

							p.setMajor(partition.getMajor());
							p.setMinor(partition.getMinor());
						}
						di.setPartitions(partitionList);
					}

					diskInfos.add(di);
				}
			}
		} catch (Throwable e) {
			logger.debug(() -> "Could not get disk information from OSHI tool. Falling back to Java tools.", e);
		} finally {
			logger.debug(() -> "Done with getting disk information.");
		}

		return diskInfos;
	}

	private static String trim(String t) {
		if (t == null) {
			return null;
		}
		if (t.endsWith("\0")) {
			t = t.substring(0, t.length() - 1);
		}
		return t.trim();
	}

	protected ComputerSystem getComputerSystem(oshi.SystemInfo si) {

		logger.debug(() -> "Getting computer system information.");

		try {
			oshi.hardware.ComputerSystem cs = si.getHardware().getComputerSystem();

			oshi.hardware.Firmware fw = cs.getFirmware();
			oshi.hardware.Baseboard bb = cs.getBaseboard();

			ComputerSystem computerSystem = ComputerSystem.T.create();
			Firmware firmware = Firmware.T.create();
			Baseboard baseboard = Baseboard.T.create();

			firmware.setManufacturer(trim(fw.getManufacturer()));
			firmware.setName(trim(fw.getName()));
			firmware.setDescription(trim(fw.getDescription()));
			firmware.setVersion(trim(fw.getVersion()));
			Date releaseDate = null;
			try {
				String oshiDate = trim(fw.getReleaseDate());
				if (oshiDate != null) {
					releaseDate = DateTools.decodeDate(oshiDate, oshiDateFormat);
				}
			} catch (Exception ignore) {
				// ignore
			}
			firmware.setReleaseDate(releaseDate);

			baseboard.setManufacturer(trim(bb.getManufacturer()));
			baseboard.setModel(trim(bb.getModel()));
			baseboard.setVersion(trim(bb.getVersion()));
			baseboard.setSerialNumber(trim(bb.getSerialNumber()));

			computerSystem.setManufacturer(trim(cs.getManufacturer()));
			computerSystem.setModel(trim(cs.getModel()));
			computerSystem.setSerialNumber(trim(cs.getSerialNumber()));
			computerSystem.setFirmware(firmware);
			computerSystem.setBaseboard(baseboard);

			return computerSystem;

		} catch (Throwable t) {
			logger.debug(() -> "Could not get computer information from OSHI tool.", t);
		} finally {
			logger.debug(() -> "Done with getting computer system information.");
		}

		return null;
	}

	protected List<FileSystemInfo> getFileSystemInfo() {

		logger.debug(() -> "Getting file system information.");

		try {
			List<FileSystemInfo> fileSystemInfoList = new ArrayList<FileSystemInfo>();

			for (FileStore store : FileSystems.getDefault().getFileStores()) {
				try {
					long totalSpace = store.getTotalSpace();
					long usableSpace = store.getUsableSpace();

					if (totalSpace != 0) {
						FileSystemInfo fsi = FileSystemInfo.T.create();
						String systemDisplayName = store.toString();
						if (systemDisplayName != null && systemDisplayName.trim().length() > 0) {
							fsi.setDisplayName(systemDisplayName);
						}
						fsi.setRoot(store.name());

						fsi.setCapacityInBytes(totalSpace);
						fsi.setFreeSpaceInBytes(usableSpace);

						fsi.setCapacityInGb(MathTools.getNumberInG(totalSpace, true, 2));
						fsi.setFreeSpaceInGb(MathTools.getNumberInG(usableSpace, true, 2));

						fileSystemInfoList.add(fsi);
					}
				} catch (Exception e) {
					logger.debug("Error while trying to get information about store: " + store, e);
				}
			}
			return fileSystemInfoList;
		} catch (Throwable t) {
			logger.debug(() -> "Could not get file system information.", t);
		} finally {
			logger.debug(() -> "Done with getting file system information.");
		}

		return null;
	}

	protected FileSystemDetailInfo getFileSystemDetailInfo(oshi.SystemInfo si) {

		logger.debug(() -> "Getting file system details.");

		try {
			FileSystem fs = si.getOperatingSystem().getFileSystem();

			FileSystemDetailInfo fileSystemDetailInfo = FileSystemDetailInfo.T.create();

			List<FileSystemDetailStore> fileSystemDetailStores = new ArrayList<>();
			for (OSFileStore fileStore : fs.getFileStores()) {
				FileSystemDetailStore systemDetailStore = FileSystemDetailStore.T.create();
				fileSystemDetailStores.add(systemDetailStore);
				systemDetailStore.setName(fileStore.getName());
				systemDetailStore.setVolume(fileStore.getVolume());
				systemDetailStore.setMount(fileStore.getMount());
				systemDetailStore.setDescription(fileStore.getDescription());
				systemDetailStore.setType(fileStore.getType());
				systemDetailStore.setUuid(fileStore.getUUID());
				long usableSpace = fileStore.getUsableSpace();
				systemDetailStore.setUsableSpace(usableSpace);
				systemDetailStore.setUsableSpaceInGb(MathTools.getNumberInG(usableSpace, true, 2));
				long totalSpace = fileStore.getTotalSpace();
				systemDetailStore.setTotalSpace(totalSpace);
				systemDetailStore.setTotalSpaceInGb(MathTools.getNumberInG(totalSpace, true, 2));
				systemDetailStore.setFreeInodes(fileStore.getFreeInodes());
				systemDetailStore.setTotalInodes(fileStore.getTotalInodes());
			}

			fileSystemDetailInfo.setFileSystemDetailStores(fileSystemDetailStores);
			fileSystemDetailInfo.setOpenFileDescriptors(fs.getOpenFileDescriptors());
			fileSystemDetailInfo.setMaxFileDescriptors(fs.getMaxFileDescriptors());

			return fileSystemDetailInfo;
		} catch (Throwable t) {
			logger.debug(() -> "Could not get file system details.", t);
		} finally {
			logger.debug(() -> "Done with getting file system details.");
		}

		return null;
	}

	@Required
	@Configurable
	public void setMessagingSessionProviderSupplier(Supplier<MessagingSessionProvider> messagingSessionProvider) {
		this.messagingSessionProviderSupplier = messagingSessionProvider;
	}

	@Required
	@Configurable
	public void setDatabaseInformationProvider(StandardDatabaseInformationProvider databaseInformationProvider2) {
		this.databaseInformationProvider = databaseInformationProvider2;
	}

	@Required
	@Configurable
	public void setLocking(Locking locking) {
		this.locking = locking;
	}

	@Required
	@Configurable
	public void setLeadershipManager(LeadershipManager leadershipManager) {
		this.leadershipManager = leadershipManager;
	}

}
