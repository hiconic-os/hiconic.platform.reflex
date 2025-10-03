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
package hiconic.rx.explorer.processing.platformreflection.db;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.net.URI;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.utils.StringTools;

import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.reflection.model.db.DatabaseConnectionInfo;
import hiconic.rx.reflection.model.db.DatabaseConnectionPoolMetrics;
import hiconic.rx.reflection.model.db.DatabaseInformation;

public class StandardDatabaseInformationProvider implements DatabaseInformationProvider {

	private static Logger logger = Logger.getLogger(StandardDatabaseInformationProvider.class);

	private List<Database> databases;

	@Required
	public void setDatabases(List<Database> databases) {
		this.databases = databases;
	}

	@Override
	public DatabaseInformation get() {
		logger.debug(() -> "Compiling database information.");

		try {
			DatabaseInformation result = DatabaseInformation.T.create();
			List<DatabaseConnectionInfo> cps = collectHikariCpConnectionPools();
			result.getConnectionPools().addAll(cps);

			List<DatabaseConnectionInfo> combined = combineConnectionPoolInfos(cps);
			result.getCombinedConnectionInfo().addAll(combined);

			return result;

		} finally {
			logger.debug(() -> "Done with database information.");
		}
	}

	private List<DatabaseConnectionInfo> collectHikariCpConnectionPools() {
		return databases.stream() //
				.map(this::toDbConnectionInfo) //
				.collect(Collectors.toList());
	}

	private DatabaseConnectionInfo toDbConnectionInfo(Database db) {
		DatabaseConnectionInfo result = DatabaseConnectionInfo.T.create();
		result.setName(db.getName());
		result.setUrl(db.getUrl());
		result.setConnectionTimeout(db.getConnectionTimeout());
		result.setIdleTimeout(db.getIdleTimeout());
		result.setMinimumPoolSize(db.getMinPoolSize());
		result.setMaximumPoolSize(db.getMaxPoolSize());

		return result;
	}

	private List<DatabaseConnectionInfo> combineConnectionPoolInfos(List<DatabaseConnectionInfo> cps) {
		TreeMap<String, DatabaseConnectionInfo> map = new TreeMap<>();

		for (DatabaseConnectionInfo dcp : cps) {
			String key = createMapKeyFromConnectionPool(dcp);
			DatabaseConnectionInfo entry = map.get(key);
			if (entry == null) {
				entry = DatabaseConnectionInfo.T.create();
				entry.setName(key);
				entry.setUrl(dcp.getUrl());
				entry.setMaximumPoolSize(dcp.getMaximumPoolSize());
				entry.setMinimumPoolSize(dcp.getMinimumPoolSize());
				DatabaseConnectionPoolMetrics entryMetrics = DatabaseConnectionPoolMetrics.T.create();
				entry.setMetrics(entryMetrics);

				DatabaseConnectionPoolMetrics dcpMetrics = dcp.getMetrics();
				if (dcpMetrics != null) {
					entryMetrics.setActiveConnections(dcpMetrics.getActiveConnections());
					entryMetrics.setIdleConnections(dcpMetrics.getIdleConnections());
					entryMetrics.setLeaseCount(dcpMetrics.getLeaseCount());
					entryMetrics.setThreadsAwaitingConnections(dcpMetrics.getThreadsAwaitingConnections());
					entryMetrics.setTotalConnections(dcpMetrics.getTotalConnections());
				}

				map.put(key, entry);

			} else {
				entry.setMaximumPoolSize(addIntegers(dcp.getMaximumPoolSize(), entry.getMaximumPoolSize()));
				entry.setMinimumPoolSize(addIntegers(entry.getMinimumPoolSize(), entry.getMinimumPoolSize()));
				DatabaseConnectionPoolMetrics dcpMetrics = dcp.getMetrics();
				if (dcpMetrics != null) {
					DatabaseConnectionPoolMetrics entryMetrics = entry.getMetrics();

					entryMetrics.setActiveConnections(addIntegers(dcpMetrics.getActiveConnections(), entryMetrics.getActiveConnections()));
					entryMetrics.setIdleConnections(addIntegers(dcpMetrics.getIdleConnections(), entryMetrics.getIdleConnections()));
					entryMetrics.setLeaseCount(addLongs(dcpMetrics.getLeaseCount(), entryMetrics.getLeaseCount()));
					entryMetrics.setThreadsAwaitingConnections(
							addIntegers(dcpMetrics.getThreadsAwaitingConnections(), entryMetrics.getThreadsAwaitingConnections()));
					entryMetrics.setTotalConnections(addIntegers(dcpMetrics.getTotalConnections(), entryMetrics.getTotalConnections()));
				}
			}
		}

		return newList(map.values());
	}

	private Integer addIntegers(Integer i1, Integer i2) {
		if (i1 == null)
			return i2;
		if (i2 == null)
			return i1;
		return i1 + i2;
	}

	private Long addLongs(Long l1, Long l2) {
		if (l1 == null)
			return l2;
		if (l2 == null)
			return l1;
		return l1 + l2;
	}

	private static String createMapKeyFromConnectionPool(DatabaseConnectionInfo dcp) {
		String key = dcp.getName();
		String url = dcp.getUrl();
		if (!StringTools.isBlank(url) && url.startsWith("jdbc:")) {
			try {
				String cleanURI = url.substring(5);

				URI uri = URI.create(cleanURI);

				StringBuilder sb = new StringBuilder();
				sb.append(uri.getHost());
				int port = uri.getPort();
				if (port != -1) {
					sb.append(":");
					sb.append(port);
				}
				key = sb.toString();
			} catch (Exception e) {
				logger.debug(() -> "Could not parse JDBC URL " + url, e);
			}
		}
		return key;
	}
}
