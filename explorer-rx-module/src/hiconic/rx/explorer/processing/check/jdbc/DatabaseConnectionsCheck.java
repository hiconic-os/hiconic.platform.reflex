// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.explorer.processing.check.jdbc;

import java.util.List;

import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.db.model.configuration.Database;

public class DatabaseConnectionsCheck extends ParallelDatabaseConnectionCheckProcessor implements CheckProcessor {

	private static Logger logger = Logger.getLogger(DatabaseConnectionsCheck.class);

	@Override
	public CheckResult check(ServiceRequestContext requestContext) {
		List<Database> dbs = databaseContract.allDatabases();
		logger.debug(() -> "Found " + (dbs != null ? dbs.size() : "0") + " DB connection pools to check.");

		return super.performCheck(dbs);
	}

}
