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
package hiconic.rx.demo.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.demo.model.api.ReverseText;

public class ReverseTextProcessor implements ReasonedServiceProcessor<ReverseText, String> {
	@Override
	public Maybe<? extends String> processReasoned(ServiceRequestContext context, ReverseText request) {
		String text = request.getText();
		
		if ("forbidden".equals(text))
			return Reasons.build(InvalidArgument.T).text("ReverseText.text must not be 'forbidden'").toMaybe();
		
		if (text == null)
			return Maybe.complete(null);
		
		String result = new StringBuilder(text).reverse().toString();
		
		return Maybe.complete(result);
	}
}
