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
package hiconic.rx.reflection.model.check.cpu;

import java.util.Date;
import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Cpu extends GenericEntity {

	EntityType<Cpu> T = EntityTypes.T(Cpu.class);

	String getFamily();
	void setFamily(String family);

	String getIdentifier();
	void setIdentifier(String identifier);

	String getModel();
	void setModel(String model);

	String getName();
	void setName(String name);

	int getLogicalProcessorCount();
	void setLogicalProcessorCount(int logicalProcessorCount);

	int getPhysicalProcessorCount();
	void setPhysicalProcessorCount(int physicalProcessorCount);

	CpuLoad getCpuLoad();
	void setCpuLoad(CpuLoad cpuLoad);
	
	String getSystemSerialNumber();
	void setSystemSerialNumber(String systemSerialNumber);

	Date getSystemBootTime();
	void setSystemBootTime(Date systemBootTime);

	String getVendor();
	void setVendor(String vendor);

	long getVendorFreq();
	void setVendorFreq(long vendorFreq);

	Double getVendorFreqInGh();
	void setVendorFreqInGh(Double vendorFreqInGh);

	long getMaxFreq();
	void setMaxFreq(long maxFreq);

	Double getMaxFreqInGh();
	void setMaxFreqInGh(Double maxFreqInGh);

	List<String> getCurrentFrequencies();
	void setCurrentFrequencies(List<String> currentFrequencies);

	String getProcessorId();
	void setProcessorId(String processorId);

	boolean getCpu64bit();
	void setCpu64bit(boolean cpu64bit);

	double getCpuTemperature();
	void setCpuTemperature(double cpuTemperature);

	double getCpuVoltage();
	void setCpuVoltage(double cpuVoltage);

	List<Integer> getFanSpeeds();
	void setFanSpeeds(List<Integer> fanSpeeds);
}
