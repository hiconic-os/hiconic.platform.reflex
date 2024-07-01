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
package hiconic.rx.demo.model.api;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.MaxLength;
import com.braintribe.model.generic.annotation.meta.Pattern;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@PositionalArguments("text")
@Description("Reverses the order of letters in the passed text")
public interface ReverseText extends ServiceRequest {
	EntityType<ReverseText> T = EntityTypes.T(ReverseText.class);
	
	String text = "text";
	
	@Description("The text that should be reversed")
	@Mandatory
	@Pattern("^(?!etah$).*$")
	@MaxLength(10)
	String getText();
	void setText(String text);
	
	@Override
	EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
