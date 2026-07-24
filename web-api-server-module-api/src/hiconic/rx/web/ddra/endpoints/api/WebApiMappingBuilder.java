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
package hiconic.rx.web.ddra.endpoints.api;

import java.util.Set;

import hiconic.rx.webapi.endpoints.OutputPrettiness;
import hiconic.rx.webapi.endpoints.TypeExplicitness;

/** Fluent configuration of an explicit Web API mapping. */
public interface WebApiMappingBuilder {

	/**
	 * Binds the mapping to a fixed service domain. If omitted, a {@code DomainRequest}'s {@code domainId} selects the domain at request time.
	 */
	WebApiMappingBuilder serviceDomain(String serviceDomain);

	WebApiMappingBuilder responseProjection(String propertyPath);
	WebApiMappingBuilder responseMimeType(String mimeType);
	WebApiMappingBuilder downloadResource(boolean downloadResource);
	WebApiMappingBuilder saveLocally(boolean saveLocally);
	WebApiMappingBuilder responseFilename(String filename);
	WebApiMappingBuilder responseContentType(String mimeType);
	WebApiMappingBuilder depth(String depth);
	WebApiMappingBuilder entityRecurrenceDepth(int depth);
	WebApiMappingBuilder prettiness(OutputPrettiness prettiness);
	WebApiMappingBuilder typeExplicitness(TypeExplicitness typeExplicitness);
	WebApiMappingBuilder writeEmptyProperties(boolean writeEmptyProperties);
	WebApiMappingBuilder writeAbsenceInformation(boolean writeAbsenceInformation);
	WebApiMappingBuilder stabilizeOrder(boolean stabilizeOrder);
	WebApiMappingBuilder useSessionEvaluation(boolean useSessionEvaluation);
	WebApiMappingBuilder preserveTransportPayload(boolean preserveTransportPayload);
	WebApiMappingBuilder decodingLenience(boolean decodingLenience);
	WebApiMappingBuilder tags(Set<String> tags);

	/** Freezes and registers this mapping. */
	void register();
}
