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

import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;
import com.braintribe.model.accessdeployment.hibernate.meta.MappingVersion;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.processing.deployment.hibernate.mapping.HbmXmlGeneratingService;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.deployment.hibernate.mapping.SourceDescriptor;
import com.braintribe.utils.CommonTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.stream.ReaderInputStream;

import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;

/* package */ class HibernateModelSessionFactoryBuilder {
	private static final Logger log = Logger.getLogger(HibernateModelSessionFactoryBuilder.class);

	private final HibernatePersistenceConfiguration hpConfiguration;
	private final String objectNamePrefix;

	private final CmdResolver cmdResolver;
	private final DataSource dataSource;
	
	private File ormDebugOutputFolder;
	private DialectAutoSense dialectAutoSense;
	private Integer defaultMappingVersion;
	private String instanceId;
	private Supplier<Locking> lockingSupplier;

	public HibernateModelSessionFactoryBuilder(SessionFactoryKey key) {
		this.hpConfiguration = CommonTools.getValueOrSupplyDefault(key.configuration(), HibernatePersistenceConfiguration.T::create);
		this.objectNamePrefix = hpConfiguration.getObjectNamePrefix();

		this.cmdResolver = key.resolver();
		this.dataSource = key.dataSource();
	}

	@Configurable
	public void setDialectAutoSense(DialectAutoSense dialectAutoSense) {
		this.dialectAutoSense = dialectAutoSense;
	}

	@Configurable
	public void setOrmDebugOutputFolder(File ormDebugOutputFolder) {
		this.ormDebugOutputFolder = ormDebugOutputFolder;
	}

	@Configurable
	public void setDefaultMappingVersion(Integer defaultMappingVersion) {
		this.defaultMappingVersion = defaultMappingVersion;
	}

	@Configurable
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	@Configurable
	public void setLockingSupplier(Supplier<Locking> lockingSupplier) {
		this.lockingSupplier = lockingSupplier;
	}

	private ClassLoader itwOrModuleClassLoader() {
		if (isLoadedByModule())
			return getClass().getClassLoader();
		else
			return (ClassLoader) GMF.getTypeReflection().getItwClassLoader();
	}

	private boolean isLoadedByModule() {
		return getClass().getClassLoader().getClass().getSimpleName().startsWith("ModuleClassLoader");
	}

	public SessionFactory build() {
		long started = System.nanoTime();
		Configuration configuration = new Configuration();

		Properties properties = configuration.getProperties();
		properties.put(Environment.JAKARTA_JTA_DATASOURCE, dataSource);
		properties.put(Environment.INTERCEPTOR, new GmAdaptionInterceptor());
		properties.put(Environment.TC_CLASSLOADER, itwOrModuleClassLoader());
		if (hpConfiguration.getShowSql())
			properties.put(Environment.SHOW_SQL, "true");

		if (dialectAutoSense != null)
			properties.put(Environment.DIALECT, dialectAutoSense.senseDialect(dataSource));

		Integer mappingVersion = mappingVersion();
		if (isMappingVersion1(mappingVersion))
			properties.put(Environment.ID_DB_STRUCTURE_NAMING_STRATEGY, "single");

		addConfigureadProperties(properties, hpConfiguration.getProperties());

		MappingBundle mappings = generateMappings(configuration, mappingVersion);
		long mappingsGenerated = System.nanoTime();
		Locking locking = lockingSupplier == null ? null : lockingSupplier.get();
		if (locking == null) {
			log.warn("No platform Locking is available; Hibernate schema update cannot be coordinated across nodes and will run unconditionally for ["
					+ schemaIdentity() + "]");
			properties.put(Environment.HBM2DDL_AUTO, "update");
			SessionFactory result = configuration.buildSessionFactory();
			logTimings(schemaIdentity(), true, started, mappingsGenerated);
			return result;
		}

		String schemaIdentity = schemaIdentity();
		try (HibernateSchemaUpdateGate.Decision decision = new HibernateSchemaUpdateGate(dataSource, locking, schemaIdentity, physicalSchemaIdentity(),
				mappings.fingerprint(), instanceId).decide()) {
			properties.put(Environment.HBM2DDL_AUTO, decision.updateRequired() ? "update" : "none");
			SessionFactory result = configuration.buildSessionFactory();
			try {
				decision.markSuccessful();
				logTimings(schemaIdentity, decision.updateRequired(), started, mappingsGenerated);
				return result;
			} catch (RuntimeException e) {
				result.close();
				throw e;
			}
		}
	}

	private int mappingVersion() {
		if (cmdResolver != null) {
			MappingVersion mv = cmdResolver.getMetaData().meta(MappingVersion.T).exclusive();
			if (mv != null)
				return mv.getVersion();
		}
		
		return defaultMappingVersion != null ? defaultMappingVersion : MappingVersion.MAPPING_VERSION_3;
	}

	private boolean isMappingVersion1(Integer mappingVersion) {
		return mappingVersion != null && mappingVersion == MappingVersion.MAPPING_VERSION_1;
	}

	private void addConfigureadProperties(Properties properties, Map<String, String> additionalProperties) {
		if (isEmpty(additionalProperties))
			return;

		for (Map.Entry<String, String> additionalProperty : additionalProperties.entrySet()) {
			String key = additionalProperty.getKey();
			String value = additionalProperty.getValue();
			if (key != null && value != null) 
				properties.setProperty(key, value);
		}
	}

	private MappingBundle generateMappings(Configuration configuration, Integer mappingVersion) {
		List<SourceDescriptor> mappings = new ArrayList<>();
		new HbmXmlGeneratingService() //
				.mappingVersion(mappingVersion) //
				.defaultSchema(hpConfiguration.getDefaultSchema()) //
				.defaultCatalog(hpConfiguration.getDefaultCatalog()) //
				.tablePrefix(getTableNamePrefix()) //
				.foreignKeyNamePrefix(getForeignKeyNamePrefix()) //
				.uniqueKeyNamePrefix(getUniqueKeyNamePrefix()) //
				.indexNamePrefix(getIndexNamePrefix()) //
				.cmdResolverAndModel(cmdResolver) //
				// .generateJpaOrm() //
				.entityMappingConsumer(sd -> {
					if (ormDebugOutputFolder != null) {
						File outputFile = new File(ormDebugOutputFolder, sd.sourceRelativePath);

						outputFile.getParentFile().mkdirs();

						FileTools.write(outputFile) //
								.withCharset(StandardCharsets.UTF_8) //
								.string(sd.sourceCode);
					}

					if (sd.sourceRelativePath.endsWith(".hbm.xml"))
						mappings.add(sd);
				}).renderMappings();

		for (SourceDescriptor sd : mappings) {
			try (ReaderInputStream in = new ReaderInputStream(new StringReader(sd.sourceCode))) {
				configuration.addInputStream(in);
			} catch (IOException e) {
				throw new UncheckedIOException("Error while applying " + sd.sourceRelativePath + " as hibernate configuration", e);
			}
		}

		return new MappingBundle(HibernateSchemaUpdateGate.fingerprint(mappings));
	}

	private void logTimings(String schemaIdentity, boolean schemaUpdate, long started, long mappingsGenerated) {
		long completed = System.nanoTime();
		long mappingMillis = (mappingsGenerated - started) / 1_000_000;
		long sessionFactoryMillis = (completed - mappingsGenerated) / 1_000_000;
		log.info("Built Hibernate SessionFactory for [" + schemaIdentity + "] in " + ((completed - started) / 1_000_000)
				+ " ms (mapping " + mappingMillis + " ms, schema action " + (schemaUpdate ? "update" : "none")
				+ ", SessionFactory/gate " + sessionFactoryMillis + " ms)");
	}

	private String schemaIdentity() {
		String modelName = cmdResolver.getModelOracle().getGmMetaModel().getName();
		return safe(modelName) + "|" + physicalSchemaIdentity();
	}

	private String physicalSchemaIdentity() {
		return String.join("|", safe(hpConfiguration.getDefaultCatalog()), safe(hpConfiguration.getDefaultSchema()), safe(getTableNamePrefix()));
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	private record MappingBundle(String fingerprint) {
		// empty
	}

	private String getTableNamePrefix() {
		return getPrefixOrDefault(hpConfiguration.getTableNamePrefix());
	}

	private String getForeignKeyNamePrefix() {
		return getPrefixOrDefault(hpConfiguration.getForeignKeyNamePrefix());
	}

	private String getUniqueKeyNamePrefix() {
		return getPrefixOrDefault(hpConfiguration.getUniqueKeyNamePrefix());
	}

	private String getIndexNamePrefix() {
		return getPrefixOrDefault(hpConfiguration.getIndexNamePrefix());
	}

	private String getPrefixOrDefault(String prefix) {
		return StringTools.isEmpty(prefix) ? objectNamePrefix : prefix;
	}

}
