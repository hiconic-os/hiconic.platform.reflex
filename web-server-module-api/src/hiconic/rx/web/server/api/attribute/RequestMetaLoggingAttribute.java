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
package hiconic.rx.web.server.api.attribute;

import com.braintribe.common.attribute.TypeSafeAttribute;

/** The request-local control shared by request processing and infrastructure loggers. */
public interface RequestMetaLoggingAttribute extends TypeSafeAttribute<RequestMetaLoggingControl> {
}
