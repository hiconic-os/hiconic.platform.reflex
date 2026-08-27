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

public abstract class StateChangeProcessors {
	private static StateChangeProcessorCapabilities defaultCapabilities = capabilities(true, true, true);
	private static StateChangeProcessorCapabilities processOnlyCapabilities = capabilities(false, false, true);
	private static StateChangeProcessorCapabilities beforeOnlyCapabilities = capabilities(true, false, false);
	private static StateChangeProcessorCapabilities afterOnlyCapabilities = capabilities(false, true, false);
	
	public static StateChangeProcessorCapabilities defaultCapabilities() {
		return defaultCapabilities;
	}
	public static StateChangeProcessorCapabilities afterOnlyCapabilities() {
		return afterOnlyCapabilities;
	}
	
	public static StateChangeProcessorCapabilities beforeOnlyCapabilities() {
		return beforeOnlyCapabilities;
	}
	
	public static StateChangeProcessorCapabilities processOnlyCapabilities() {
		return processOnlyCapabilities;
	}
	
	public static StateChangeProcessorCapabilities capabilities(boolean before, boolean after, boolean process) {
		StateChangeProcessorCapabilities capabilities = StateChangeProcessorCapabilities.T.create();
		capabilities.setBefore(before);
		capabilities.setAfter(after);
		capabilities.setProcess(process);
		
		return capabilities;
	}
}
