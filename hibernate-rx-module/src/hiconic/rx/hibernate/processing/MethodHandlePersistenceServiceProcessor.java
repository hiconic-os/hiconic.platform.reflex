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
package hiconic.rx.hibernate.processing;

import java.lang.invoke.MethodHandle;

import org.hibernate.Session;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;

public class MethodHandlePersistenceServiceProcessor<P extends ServiceRequest, R> implements PersistenceServiceProcessor<P, R> {

	private final MethodHandle methodHandle;

	public MethodHandlePersistenceServiceProcessor(MethodHandle methodHandle) {
		this.methodHandle = methodHandle;
	}

	@Override
	public Maybe<R> process(PersistenceContext context, Session session, P request) {
		try {
			return (Maybe<R>) methodHandle.invoke(context, session, request);

		} catch (RuntimeException | Error e) {
			throw e;

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
