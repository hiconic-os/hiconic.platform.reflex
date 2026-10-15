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
package hiconic.rx.webapi.model.meta;

import com.braintribe.model.cache.CacheType;
import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/** A null-safe cache-type override used by complete mappings. */
public enum CacheTypeOverride implements EnumBase<CacheTypeOverride> {
	INHERIT,
	NO_CACHE,
	NO_STORE,
	PUBLIC_CACHE,
	PRIVATE_CACHE;

	public static final EnumType<CacheTypeOverride> T = EnumTypes.T(CacheTypeOverride.class);

	@Override
	public EnumType<CacheTypeOverride> type() {
		return T;
	}

	public CacheType cacheType() {
		return switch (this) {
			case INHERIT -> null;
			case NO_CACHE -> CacheType.noCache;
			case NO_STORE -> CacheType.noStore;
			case PUBLIC_CACHE -> CacheType.publicCache;
			case PRIVATE_CACHE -> CacheType.privateCache;
		};
	}
}
