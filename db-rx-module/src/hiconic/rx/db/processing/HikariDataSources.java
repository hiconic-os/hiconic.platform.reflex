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
package hiconic.rx.db.processing;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.logging.Logger;
import com.braintribe.utils.lcd.CollectionTools;
import com.braintribe.utils.lcd.Lazy;
import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.dropwizard.CodahaleMetricsTrackerFactory;

import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.db.model.configuration.DatabaseConfiguration;
import hiconic.rx.db.model.configuration.JdbcTransactionIsolationLevel;

/**
 * @author peter.gazdik
 */
public class HikariDataSources {

	private static final Logger log = Logger.getLogger(HikariDataSources.class);

	private final ConcurrentHashMap<String, DataSource> createdDataSources = new ConcurrentHashMap<>();

	private final MetricRegistry metricRegistry = new MetricRegistry();
	
	private DatabaseConfiguration databaseConfiguration;
	
	private final Lazy<Map<String, Database>> pools = new Lazy<>(this::indexPools);
	
	@Required
	public void setDatabaseConfiguration(DatabaseConfiguration databaseConfiguration) {
		this.databaseConfiguration = databaseConfiguration;
	}
	
	private Map<String, Database> indexPools() {
		Map<String, Database> pools = new LinkedHashMap<String, Database>();
		for (Database pool: databaseConfiguration.getDatabases()) {
			pools.put(pool.getName(), pool);
		}
		return pools;
	}
	
	public HikariDataSource findDataSource(String name) {
		Database databaseConnectionPool = pools.get().get(name);
		if (databaseConnectionPool == null)
			return null;
		
		return dataSource(databaseConnectionPool);
	}
	
	public Maybe<HikariDataSource> dataSource(String name) {
		HikariDataSource ds = findDataSource(name);
		if (ds == null)
			return Reasons.build(NotFound.T).text("DataSource with name " + name + " not configured").toMaybe();
		
		return Maybe.complete(ds);
	}
	
	/**
	 * @param connectionPool
	 *            The {@link Database} providing the necessary configuration for creating a HikariCP {@link HikariDataSource}.
	 * 
	 * @return A HikariCP {@link HikariDataSource} created based on the given {@link Database}
	 */
	public synchronized HikariDataSource dataSource(Database connectionPool) {

		String key = connectionPool.getName();
		Objects.requireNonNull(key, "The connection pool " + connectionPool.stringify() + " does not have a name.");

		HikariDataSource existingDataSource = (HikariDataSource) createdDataSources.get(key);
		if (existingDataSource != null) {
			if (existingDataSource.isClosed()) {
				createdDataSources.remove(key);
			} else {
				return existingDataSource;
			}
		}

		String url = connectionPool.getUrl();

		StringBuilder sb = new StringBuilder("HikariCP:'");
		if (url != null) {
			sb.append(url);
		}
		sb.append('\'');
		log.pushContext(sb.toString());

		try {

			HikariDataSource result = new HikariDataSource();

			result.setDriverClassName(connectionPool.getDriver());
			result.setJdbcUrl(connectionPool.getUrl());
			result.setUsername(connectionPool.getUser());
			result.setPassword(connectionPool.getPassword());

			configurePooledJdbcConnectorSettings(connectionPool, result);
			configureExtendedConnectionPoolSettings(connectionPool, result);
			configureInitStatements(connectionPool, result);

			createdDataSources.put(key, result);

			return result;

		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid config: " + connectionPool, e);
		} finally {
			log.popContext();
		}

	}

	private static void configurePooledJdbcConnectorSettings(Database connectionPool, HikariDataSource result) {
		boolean debug = log.isDebugEnabled();

		Long connectionTimeout = connectionPool.getConnectionTimeout();
		if (connectionTimeout != null) {
			if (debug)
				log.debug("Connection timeout: " + connectionTimeout + " ms");
			result.setConnectionTimeout(connectionTimeout);
		}

		Integer maxPoolSize = connectionPool.getMaxPoolSize();
		if (maxPoolSize != null && maxPoolSize > 0) {
			if (debug)
				log.debug("Maximum poolsize: " + maxPoolSize);
			result.setMaximumPoolSize(maxPoolSize);
		}

		Integer minPoolSize = connectionPool.getMinPoolSize();
		if (minPoolSize != null && minPoolSize >= 0) {
			if (debug)
				log.debug("Minimum poolsize: " + minPoolSize);
			result.setMinimumIdle(minPoolSize);
		}

		if ((maxPoolSize != null && maxPoolSize > 0) && (minPoolSize == null || minPoolSize == 0)) {
			log.info("MaxPoolSize is set to " + maxPoolSize
					+ " while no minPoolSize is defined. Please be aware that this will immediately open (and keep) " + maxPoolSize
					+ " connections.");
		}

		String preferredTestQuery = connectionPool.getPreferredTestQuery();
		if (preferredTestQuery != null) {
			if (debug)
				log.debug("Test query: " + preferredTestQuery);
			result.setConnectionTestQuery(preferredTestQuery);
		}

		Integer maxStatements = connectionPool.getMaxStatements();
		if (maxStatements != null) {
			if (debug)
				log.debug("Maximum statements cached: " + maxStatements);
			result.addDataSourceProperty("cachePrepStmts", "true");
			result.addDataSourceProperty("prepStmtCacheSize", "" + maxStatements);
		}

		Long idleTimeout = connectionPool.getIdleTimeout();
		if (idleTimeout != null) {
			if (debug)
				log.debug("Idle timeout: " + idleTimeout + " ms");
			result.setIdleTimeout(idleTimeout);
		}

		JdbcTransactionIsolationLevel isolationLevel = connectionPool.getTransactionIsolationLevel();
		if (isolationLevel != null)
			result.setTransactionIsolation("TRANSACTION_" + isolationLevel.name());
	}

	private void configureExtendedConnectionPoolSettings(Database connectionPool, HikariDataSource result) throws SQLException {

		boolean debug = log.isDebugEnabled();

		Integer loginTimeout = connectionPool.getLoginTimeout();
		if (loginTimeout != null) {
			if (debug)
				log.debug("Login timeout: " + loginTimeout + " ms");
			result.setLoginTimeout(loginTimeout);

		}

		Integer initializationFailTimeout = connectionPool.getInitializationFailTimeout();
		if (initializationFailTimeout != null) {
			if (debug)
				log.debug("Initialization timeout: " + initializationFailTimeout + " ms");
			result.setInitializationFailTimeout(initializationFailTimeout);
		}

		Integer validationTimeout = connectionPool.getValidationTimeout();
		if (validationTimeout != null) {
			if (debug)
				log.debug("Maximum validation timeout: " + validationTimeout + " ms");
			result.setValidationTimeout(validationTimeout);
		}

		String dataSourceName = connectionPool.getDataSourceName();
		if (dataSourceName != null) {
			if (debug)
				log.debug("Datasource name: " + dataSourceName);
			result.setPoolName(dataSourceName);

		} else {
			String extId = connectionPool.getName();
			if (extId != null) {
				extId = extId.replace(':', '/'); // ':' is forbidden in pool names (at least sometimes)

				if (debug)
					log.debug("Datasource name (from external Id): " + extId);
				result.setPoolName(extId);
			}
		}

		String schema = connectionPool.getSchema();
		if (schema != null) {
			if (debug)
				log.debug("Schema: " + schema);
			result.setSchema(schema);
		}

		boolean enableJmx = connectionPool.getEnableJmx();
		if (enableJmx) {
			result.setRegisterMbeans(true);
		}

		boolean enableMetrics = connectionPool.getEnableMetrics();
		if (enableMetrics) {
			try {
				MetricsTrackerFactory metricsTrackerFactory = new CodahaleMetricsTrackerFactory(metricRegistry);
				result.setMetricsTrackerFactory(metricsTrackerFactory);
			} catch (Exception e) {
				log.warn(() -> "Could not enable metrics for connection pool " + connectionPool, e);
			}
		}
	}

	private static void configureInitStatements(Database connectionPool, HikariDataSource result) {
		if (CollectionTools.isEmpty(connectionPool.getInitialStatements())) {
			return;
		}

		List<String> initialStatements = connectionPool.getInitialStatements();
		StringBuilder sb = new StringBuilder();
		for (String st : initialStatements) {
			sb.append(st);
			sb.append(";\n");
		}

		if (log.isDebugEnabled()) {
			log.debug("Connection initialization statement: " + sb.toString());
		}
		result.setConnectionInitSql(sb.toString());
	}

	public MetricRegistry metricRegistry() {
		return metricRegistry;
	}

}
