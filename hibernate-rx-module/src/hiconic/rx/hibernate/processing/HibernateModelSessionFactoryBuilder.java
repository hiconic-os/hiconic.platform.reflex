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
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import com.braintribe.cfg.Configurable;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.processing.deployment.hibernate.mapping.HbmXmlGeneratingService;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.utils.CommonTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.stream.ReaderInputStream;

import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;

/* package */ class HibernateModelSessionFactoryBuilder {

	private final HibernatePersistenceConfiguration hpConfiguration;
	private final String objectNamePrefix;

	private final CmdResolver cmdResolver;
	private final DataSource dataSource;
	private File ormDebugOutputFolder;
	private DialectAutoSense dialectAutoSense;

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
		Configuration configuration = new Configuration();

		Properties properties = configuration.getProperties();
		properties.put(Environment.DATASOURCE, dataSource);
		properties.put(Environment.INTERCEPTOR, new GmAdaptionInterceptor());
		properties.put(Environment.TC_CLASSLOADER, itwOrModuleClassLoader());
		// TODO review - bring model change detection from cortex
		properties.put(Environment.HBM2DDL_AUTO, "update");
		if (hpConfiguration.getShowSql())
			properties.put(Environment.SHOW_SQL, "true");

		if (dialectAutoSense != null)
			properties.put(Environment.DIALECT, dialectAutoSense.senseDialect(dataSource));

		addConfigureadProperties(properties, hpConfiguration.getProperties());

		generateMappings(configuration);

		return configuration.buildSessionFactory();
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

	private void generateMappings(Configuration configuration) {
		new HbmXmlGeneratingService() //
				.mappingVersion(hpConfiguration.getMappingVersion()) //
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

					if (!sd.sourceRelativePath.endsWith(".hbm.xml"))
						return;

					try (ReaderInputStream in = new ReaderInputStream(new StringReader(sd.sourceCode))) {
						configuration.addInputStream(in);
					} catch (IOException e) {
						throw new UncheckedIOException("Error while applying " + sd.sourceRelativePath + " as hibernate configuration", e);
					}
				}).renderMappings();
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
