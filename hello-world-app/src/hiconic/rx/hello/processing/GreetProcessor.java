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
package hiconic.rx.hello.processing;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.hello.model.api.Greet;

public class GreetProcessor implements ReasonedServiceProcessor<Greet, String> {
	
	private Logger logger = System.getLogger("foobar");
	
	@Override
	public Maybe<? extends String> processReasoned(ServiceRequestContext context, Greet request) {
		String name = request.getName();
		
		logger.log(Level.ERROR, "Hallo");
		
		if (name == null)
			name = "nobody";

		String greeting = "Hello " + name + "!";
		
		return Maybe.complete(greeting);
	}
}
