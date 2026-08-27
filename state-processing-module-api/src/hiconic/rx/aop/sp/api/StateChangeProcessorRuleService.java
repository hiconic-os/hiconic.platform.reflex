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
package hiconic.rx.aop.sp.api;

import java.util.List;


/**
 * the service that returns the addressing for the state change processor's cartridge 
 * 
 * @author pit, dirk
 *
 */
public interface StateChangeProcessorRuleService  {
	/**
	 * 
	 * @return {@link SelectiveStateChangeProcessorAddressing} to be used to match the processor against the manipulation 
	 */
	public List<SelectiveStateChangeProcessorAddressing> getProcessorAddressings();
	
	StateChangeProcessingResponse processStateChange(String processorId, StateChangeProcessingRequest request) throws StateChangeProcessorException;
	OnBeforeStateChangeProcessingResponse onBeforeStateChange(String processorId, OnBeforeStateChangeProcessingRequest request) throws StateChangeProcessorException;
	OnAfterStateChangeProcessingResponse onAfterChange(String processorId, OnAfterStateChangeProcessingRequest request) throws StateChangeProcessorException;
}
