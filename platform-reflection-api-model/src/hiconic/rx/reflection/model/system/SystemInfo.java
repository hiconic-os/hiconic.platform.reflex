// ============================================================================
package hiconic.rx.reflection.model.system;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.reflection.model.api.PlatformReflectionResponse;
import hiconic.rx.reflection.model.application.Concurrency;
import hiconic.rx.reflection.model.application.Messaging;
import hiconic.rx.reflection.model.check.cpu.Cpu;
import hiconic.rx.reflection.model.check.io.IoMeasurements;
import hiconic.rx.reflection.model.check.java.JavaEnvironment;
import hiconic.rx.reflection.model.check.power.PowerSource;
import hiconic.rx.reflection.model.db.DatabaseInformation;
import hiconic.rx.reflection.model.system.disk.DiskInfo;
import hiconic.rx.reflection.model.system.disk.FileSystemDetailInfo;
import hiconic.rx.reflection.model.system.disk.FileSystemInfo;
import hiconic.rx.reflection.model.system.hardware.ComputerSystem;
import hiconic.rx.reflection.model.system.memory.Memory;
import hiconic.rx.reflection.model.system.network.NetworkInterface;
import hiconic.rx.reflection.model.system.network.NetworkParams;
import hiconic.rx.reflection.model.system.os.OperatingSystem;
import hiconic.rx.reflection.model.system.os.Process;
import hiconic.rx.reflection.model.system.threadpools.ThreadPools;

public interface SystemInfo extends PlatformReflectionResponse {

	EntityType<SystemInfo> T = EntityTypes.T(SystemInfo.class);

	List<DiskInfo> getDisks();
	void setDisks(List<DiskInfo> disks);

	ComputerSystem getComputerSystem();
	void setComputerSystem(ComputerSystem computerSystem);

	List<FileSystemInfo> getFileSystems();
	void setFileSystems(List<FileSystemInfo> fileSystems);

	FileSystemDetailInfo getFileSystemDetailInfo();
	void setFileSystemDetailInfo(FileSystemDetailInfo fileSystemDetailInfo);

	OperatingSystem getOperatingSystem();
	void setOperatingSystem(OperatingSystem operatingSystem);

	Memory getMemory();
	void setMemory(Memory memory);

	Cpu getCpu();
	void setCpu(Cpu cpu);

	List<NetworkInterface> getNetworkInterfaces();
	void setNetworkInterfaces(List<NetworkInterface> networkInterfaces);

	NetworkParams getNetworkParams();
	void setNetworkParams(NetworkParams networkPaar);

	List<PowerSource> getPowerSources();
	void setPowerSources(List<PowerSource> powerSources);

	List<Process> getJavaProcesses();
	void setJavaProcesses(List<Process> javaProcesses);

	JavaEnvironment getJavaEnvironment();
	void setJavaEnvironment(JavaEnvironment javaEnvironment);

	IoMeasurements getIoMeasurements();
	void setIoMeasurements(IoMeasurements ioMeasurements);

	DatabaseInformation getDatabaseInformation();
	void setDatabaseInformation(DatabaseInformation databaseInformation);

	ThreadPools getThreadPools();
	void setThreadPools(ThreadPools threadPools);

	List<String> getFontFamilies();
	void setFontFamilies(List<String> fontFamilies);

	Messaging getMessaging();
	void setMessaging(Messaging messaging);

	Concurrency getConcurrency();
	void setConcurrency(Concurrency concurrency);

}
