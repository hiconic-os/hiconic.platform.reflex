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

public interface OperatingSystem extends GenericEntity {

	EntityType<OperatingSystem> T = EntityTypes.T(OperatingSystem.class);

	String getFamily();
	void setFamily(String family);

	String getManufacturer();
	void setManufacturer(String manufacturer);

	Integer getProcessCount();
	void setProcessCount(Integer processCount);

	int getThreadCount();
	void setThreadCount(int threadCount);

	int getBitness();
	void setBitness(int bitness);

	String getVersion();
	void setVersion(String version);

	String getCodeName();
	void setCodeName(String codeName);

	String getBuildNumber();
	void setBuildNumber(String buildNumber);

	String getArchitecture();
	void setArchitecture(String architecture);

	String getHostSystem();
	void setHostSystem(String hostSystem);

	Date getSystemTime();
	void setSystemTime(Date systemTime);

	String getSystemTimeAsString();
	void setSystemTimeAsString(String systemTimeAsString);

	Locale getDefaultLocale();
	void setDefaultLocale(Locale defaultLocale);

	int getNumberOfAvailableLocales();
	void setNumberOfAvailableLocales(int numberOfAvailableLocales);
}
