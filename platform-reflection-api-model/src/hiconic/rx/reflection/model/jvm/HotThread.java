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
package hiconic.rx.reflection.model.jvm;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;


public interface HotThread extends GenericEntity {

	EntityType<HotThread> T = EntityTypes.T(HotThread.class);

	double getPercent();
	void setPercent(double percent);
	
	long getTimeInNanoSeconds();
	void setTimeInNanoSeconds(long timeInNanoSeconds);
	
	String getThreadName();
	void setThreadName(String threadName);
	
	int getCount();
	void setCount(int count);
	
	int getMaxSimilarity();
	void setMaxSimilarity(int maxSimilarity);
	
	List<StackTraceElement> getStackTraceElements();
	void setStackTraceElements(List<StackTraceElement> stackTraceElements);
	
}
