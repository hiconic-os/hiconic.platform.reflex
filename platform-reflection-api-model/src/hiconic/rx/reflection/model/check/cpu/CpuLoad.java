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

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface CpuLoad extends GenericEntity {

	EntityType<CpuLoad> T = EntityTypes.T(CpuLoad.class);

	Long getContextSwitches();
	void setContextSwitches(Long contextSwitches);
	
	Long getInterrupts();
	void setInterrupts(Long interrupts);
	
	Double getSystemCpuLoad();
	void setSystemCpuLoad(Double systemCpuLoad);

	Double getSystemLoadAverage1Minute();
	void setSystemLoadAverage1Minute(Double systemLoadAverage1Minute);

	Double getSystemLoadAverage5Minutes();
	void setSystemLoadAverage5Minutes(Double systemLoadAverage5Minutes);

	Double getSystemLoadAverage15Minutes();
	void setSystemLoadAverage15Minutes(Double systemLoadAverage15Minutes);

	List<Double> getSystemLoadPerProcessor();
	void setSystemLoadPerProcessor(List<Double> systemLoadPerProcessor);
	
	Double getUser();
	void setUser(Double user);

	Double getNice();
	void setNice(Double nice);

	Double getSys();
	void setSys(Double sys);

	Double getIdle();
	void setIdle(Double idle);

	Double getIoWait();
	void setIoWait(Double ioWait);

	Double getIrq();
	void setIrq(Double irq);

	Double getSoftIrq();
	void setSoftIrq(Double softIrq);

	Double getSteal();
	void setSteal(Double steal);

	Double getTotalCpu();
	void setTotalCpu(Double totalCpu);

}
