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
package hiconic.rx.security.web.processing.servlet.aspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletResponse;

public class AuthHttpResponseConfigurerImpl implements AuthHttpResponseConfigurer {
	Map<Object, Consumer<HttpServletResponse>> registry = new HashMap<>();
	List<Consumer<HttpServletResponse>> registryForAll = new ArrayList<>();

	public AuthHttpResponseConfigurerImpl() {

	}

	@Override
	public void applyFor(Object response, Consumer<HttpServletResponse> consumer) {
		registry.put(response, consumer);
	}

	public void consume(Object serviceResponse, HttpServletResponse htttpResponse) {
		registry.getOrDefault(serviceResponse, r -> {
			/* noop */}).accept(htttpResponse);
		registryForAll.forEach(c -> c.accept(htttpResponse));
	}

	@Override
	public void applyForAll(Consumer<HttpServletResponse> consumer) {
		registryForAll.add(consumer);
	}
}
