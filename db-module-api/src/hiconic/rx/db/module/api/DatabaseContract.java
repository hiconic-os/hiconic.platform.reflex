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
package hiconic.rx.db.module.api;

import javax.sql.DataSource;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.NotFound;

import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.module.api.wire.RxExportContract;

/**
 * A contract to access pooled {@link DataSource DataSources} either by name or by an explicit {@link Database} instance
 */
public interface DatabaseContract extends RxExportContract {
	/**
	 * Returns a pooled {@link DataSource} configured in DatabaseConfiguration with the name "main-db".
	 * 
	 * Reasons: {@link NotFound}
	 */
	Maybe<DataSource> mainDataSource();
	
	/**
	 * Returns a pooled {@link DataSource} configured in DatabaseConfiguration with the given name or null if not present.
	 */
	DataSource findDataSource(String name);
	
	/**
	 * Returns a pooled {@link DataSource} configured in DatabaseConfiguration with the given name.
	 * 
	 * Reasons: {@link NotFound}
	 */
	Maybe<DataSource> dataSource(String name);
	
	/**
	 * Deploys and returns a pooled {@link DataSource} configured with the given {@link Database}.
	 * If the same {@link Database} instance is deployed another time, the same DataSource is returned.
	 */
	DataSource deploy(Database database);
}
