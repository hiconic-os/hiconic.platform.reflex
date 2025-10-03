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
package hiconic.rx.reflection.model.check.java;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface JavaEnvironment extends GenericEntity {

	EntityType<JavaEnvironment> T = EntityTypes.T(JavaEnvironment.class);

	long getTotalMemory();
	void setTotalMemory(long totalMemory);

	long getFreeMemory();
	void setFreeMemory(long freeMemory);

	long getUsedMemory();
	void setUsedMemory(long usedMemory);

	long getMaxMemory();
	void setMaxMemory(long maxMemory);

	long getInitMemory();
	void setInitMemory(long initMemory);

	long getTotalMemoryNonHeap();
	void setTotalMemoryNonHeap(long totalMemoryNonHeap);

	long getUsedMemoryNonHeap();
	void setUsedMemoryNonHeap(long usedMemoryNonHeap);

	long getMaxMemoryNonHeap();
	void setMaxMemoryNonHeap(long maxMemoryNonHeap);

	long getInitMemoryNonHeap();
	void setInitMemoryNonHeap(long initMemoryNonHeap);

	long getResidentSetSize();
	void setResidentSetSize(long residentSetSize);

	Double getResidentSetSizeInGb();
	void setResidentSetSizeInGb(Double residentSetSizeInGb);

	long getVirtualSize();
	void setVirtualSize(long virtualSize);

	Double getVirtualSizeInGb();
	void setVirtualSizeInGb(Double virtualSizeInGb);

	Double getTotalMemoryInGb();
	void setTotalMemoryInGb(Double totalMemoryInGb);

	Double getFreeMemoryInGb();
	void setFreeMemoryInGb(Double freeMemoryInGb);

	Double getUsedMemoryInGb();
	void setUsedMemoryInGb(Double usedMemoryInGb);

	Double getMaxMemoryInGb();
	void setMaxMemoryInGb(Double maxMemoryInGb);

	Double getInitMemoryInGb();
	void setInitMemoryInGb(Double initMemoryInGb);

	Double getTotalMemoryNonHeapInGb();
	void setTotalMemoryNonHeapInGb(Double totalMemoryNonHeapInGb);

	Double getUsedMemoryNonHeapInGb();
	void setUsedMemoryNonHeapInGb(Double usedMemoryNonHeapInGb);

	Double getMaxMemoryNonHeapInGb();
	void setMaxMemoryNonHeapInGb(Double maxMemoryNonHeapInGb);

	Double getInitMemoryNonHeapInGb();
	void setInitMemoryNonHeapInGb(Double initMemoryNonHeapInGb);

	// --------

	int getAvailableProcessors();
	void setAvailableProcessors(int availableProcessors);

	String getBootClassPath();
	void setBootClassPath(String bootClassPath);

	String getClassPath();
	void setClassPath(String classPath);

	List<String> getInputArguments();
	void setInputArguments(List<String> inputArguments);

	String getLibraryPath();
	void setLibraryPath(String libraryPath);

	String getManagementSpecVersion();
	void setManagementSpecVersion(String managementSpecVersion);

	String getName();
	void setName(String name);

	String getSpecName();
	void setSpecName(String specName);

	String getSpecVendor();
	void setSpecVendor(String specVendor);

	String getSpecVersion();
	void setSpecVersion(String specVersion);

	Date getStartTime();
	void setStartTime(Date startTime);

	Map<String, String> getSystemProperties();
	void setSystemProperties(Map<String, String> systemProperties);

	Map<String, String> getEnvironmentVariables();
	void setEnvironmentVariables(Map<String, String> environmentVariables);

	long getUptime();
	void setUptime(long uptime);

	String getUptimeDisplay();
	void setUptimeDisplay(String uptimeDisplay);

	String getVmName();
	void setVmName(String vmName);

	String getVmVendor();
	void setVmVendor(String vmVendor);

	String getVmVersion();
	void setVmVersion(String vmVersion);

	boolean getBootClassPathSupported();
	void setBootClassPathSupported(boolean bootClassPathSupported);

	String getJavaVersion();
	void setJavaVersion(String javaVersion);

	String getJavaVendor();
	void setJavaVendor(String javaVendor);

	String getJavaVendorUrl();
	void setJavaVendorUrl(String javaVendorUrl);

	String getJavaHome();
	void setJavaHome(String javaHome);

	String getJavaClassVersion();
	void setJavaClassVersion(String javaClassVersion);

	String getTmpDir();
	void setTmpDir(String tmpDir);

	String getJavaCompiler();
	void setJavaCompiler(String javaCompiler);

	String getUsername();
	void setUsername(String username);

	String getUserHome();
	void setUserHome(String userHome);

	String getWorkingDir();
	void setWorkingDir(String workingDir);

	int getThreadCount();
	void setThreadCount(int threadCount);

	Boolean getElevatedPrivileges();
	void setElevatedPrivileges(Boolean elevatedPrivileges);
}
