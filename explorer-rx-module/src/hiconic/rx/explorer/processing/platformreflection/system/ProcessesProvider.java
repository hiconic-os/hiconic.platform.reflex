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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.braintribe.logging.Logger;
import com.braintribe.utils.MathTools;
import com.braintribe.utils.StringTools;

import hiconic.rx.reflection.model.system.os.Process;
import oshi.software.os.OSProcess;

public class ProcessesProvider {

	private static Logger logger = Logger.getLogger(ProcessesProvider.class);

	public static List<Process> getProcesses(oshi.SystemInfo si, boolean onlyJavaProcesses) {

		logger.debug(() -> "Getting processes");

		try {
			List<Process> processList = new ArrayList<Process>();

			String localProcessName = ManagementFactory.getRuntimeMXBean().getName();

			List<OSProcess> processes = si.getOperatingSystem().getProcesses();
			if (processes != null) {
				for (OSProcess op : processes) {

					String path = op.getPath();
					if (path == null) {
						continue;
					}
					path = path.toLowerCase();

					boolean addProcessToList = true;
					if (onlyJavaProcesses) {
						if (!(path.indexOf("java") != -1 || path.indexOf("tomcat") != -1)) {
							addProcessToList = false;
						}
					}

					if (addProcessToList) {

						Process process = Process.T.create();

						int processID = op.getProcessID();

						long kernelTime = op.getKernelTime();
						process.setKernelTimeInMs(kernelTime);
						process.setKernelTimeDisplay(StringTools.prettyPrintMilliseconds(kernelTime, true));
						long userTime = op.getUserTime();
						process.setUserTimeInMs(userTime);
						process.setUserTimeDisplay(StringTools.prettyPrintMilliseconds(userTime, true));
						process.setOpenFiles(op.getOpenFiles());
						process.setCurrentWorkingDirectory(op.getCurrentWorkingDirectory());

						process.setCommandLine(replaceNulls(op.getCommandLine()));
						process.setUser(op.getUser());
						process.setUserId(op.getUserID());
						process.setGroup(op.getGroup());
						process.setGroupId(op.getGroupID());
						long residentSetSize = op.getResidentSetSize();
						process.setResidentSetSize(residentSetSize);
						process.setResidentSetSizeInGb(MathTools.getNumberInG(residentSetSize, true, 2));
						long bytesRead = op.getBytesRead();
						process.setBytesRead(bytesRead);
						process.setBytesReadInGb(MathTools.getNumberInG(bytesRead, true, 2));
						long bytesWritten = op.getBytesWritten();
						process.setBytesWritten(bytesWritten);
						process.setBytesWrittenInGb(MathTools.getNumberInG(bytesWritten, true, 2));

						process.setName(op.getName());
						process.setParentProcessId(op.getParentProcessID());
						process.setPath(op.getPath());
						process.setPriority(op.getPriority());
						process.setProcessId(processID);
						Date startTime = new Date(op.getStartTime());
						process.setStartTime(startTime);
						process.setState(op.getState().name());
						process.setThreadCount(op.getThreadCount());
						long upTime = op.getUpTime();
						process.setUptime(upTime);
						process.setUptimeDisplay(StringTools.prettyPrintMilliseconds(upTime, true));
						long virtualSize = op.getVirtualSize();
						process.setVirtualSize(virtualSize);
						process.setVirtualSizeInGb(MathTools.getNumberInG(virtualSize, true, 2));

						if (localProcessName.indexOf("" + processID) != -1) {
							process.setIsCurrentProcess(true);
						} else {
							process.setIsCurrentProcess(false);
						}

						processList.add(process);
					}
				}
			}

			return processList;
		} finally {
			logger.debug(() -> "Done with getting processes");
		}
	}

	private static String replaceNulls(String text) {
		if (text == null) {
			return null;
		}
		String cleanText = text.replace('\0', ' ');
		return cleanText.trim();
	}
}
