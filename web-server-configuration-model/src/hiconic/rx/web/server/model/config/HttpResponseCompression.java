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
package hiconic.rx.web.server.model.config;

import java.util.Set;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Optional overrides for RX's negotiated, streaming HTTP response compression. */
public interface HttpResponseCompression extends GenericEntity {
    EntityType<HttpResponseCompression> T = EntityTypes.T(HttpResponseCompression.class);

    @Description("Defaults to true, also when no compression configuration is supplied. False disables response compression.")
    Boolean getEnabled();
    void setEnabled(Boolean enabled);

    @Description("Minimum known uncompressed Content-Length in bytes. Defaults to 2048. Unknown-length eligible responses stream compressed without prebuffering. Zero also compresses small eligible responses.")
    Integer getMinimumResponseBytes();
    void setMinimumResponseBytes(Integer bytes);

    @Description("Gzip level from 0 to 9. Defaults to 1 for throughput and low CPU overhead.")
    Integer getLevel();
    void setLevel(Integer level);

    @Description("Optional replacement MIME allowlist. Null/empty uses text/*, JSON/XML suffixes, JavaScript, YAML, gm/* and RPC multipart types. Supports type/* and type/*+suffix. Already encoded responses, ranges, no-transform and SSE remain excluded.")
    Set<String> getMimeTypes();
    void setMimeTypes(Set<String> mimeTypes);
}
