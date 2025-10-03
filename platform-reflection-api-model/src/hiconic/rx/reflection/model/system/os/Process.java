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
package hiconic.rx.reflection.model.system.os;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Process extends GenericEntity {

	EntityType<Process> T = EntityTypes.T(Process.class);

	String getName();
	void setName(String name);

	int getParentProcessId();
	void setParentProcessId(int parentProcessId);

	int getProcessId();
	void setProcessId(int processId);

	String getPath();
	void setPath(String path);

	int getPriority();
	void setPriority(int priority);

	String getState();
	void setState(String state);

	int getThreadCount();
	void setThreadCount(int threadCount);

	long getKernelTimeInMs();
	void setKernelTimeInMs(long kernelTimeInMs);

	String getKernelTimeDisplay();
	void setKernelTimeDisplay(String kernelTimeDisplay);

	long getUserTimeInMs();
	void setUserTimeInMs(long userTimeInMs);

	String getUserTimeDisplay();
	void setUserTimeDisplay(String userTimeDisplay);

	Date getStartTime();
	void setStartTime(Date startTime);

	long getUptime();
	void setUptime(long uptime);

	String getUptimeDisplay();
	void setUptimeDisplay(String uptimeDisplay);

	long getVirtualSize();
	void setVirtualSize(long virtualSize);

	Double getVirtualSizeInGb();
	void setVirtualSizeInGb(Double virtualSizeInGb);

	boolean getIsCurrentProcess();
	void setIsCurrentProcess(boolean isCurrentProcess);

	String getCommandLine();
	void setCommandLine(String commandLine);

	String getUser();
	void setUser(String user);

	String getUserId();
	void setUserId(String userId);

	String getGroup();
	void setGroup(String group);

	String getGroupId();
	void setGroupId(String groupId);

	long getResidentSetSize();
	void setResidentSetSize(long residentSetSize);
	
	Double getResidentSetSizeInGb();
	void setResidentSetSizeInGb(Double residentSetSizeInGb);

	long getBytesRead();
	void setBytesRead(long bytesRead);

	Double getBytesReadInGb();
	void setBytesReadInGb(Double bytesReadInGb);

	long getBytesWritten();
	void setBytesWritten(long bytesWritten);
	
	Double getBytesWrittenInGb();
	void setBytesWrittenInGb(Double bytesWrittenInGb);

	long getOpenFiles();
	void setOpenFiles(long openFiles);
	
	String getCurrentWorkingDirectory();
	void setCurrentWorkingDirectory(String currentWorkingDirectory);
}
