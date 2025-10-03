// ============================================================================
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
package hiconic.rx.check.model.result;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Result for a single check.
 * 
 * @see CheckResult
 */
public interface CheckResultEntry extends GenericEntity {

	final EntityType<CheckResultEntry> T = EntityTypes.T(CheckResultEntry.class);
	
	void setName(String name);
	String getName();
	
	void setDetails(String details);
	String getDetails();
	
	boolean getDetailsAsMarkdown();
	void setDetailsAsMarkdown(boolean detailsAsMarkdown);

	void setCheckStatus(CheckStatus cs);
	CheckStatus getCheckStatus();

	void setMessage(String msg);
	String getMessage();

}
