package hiconic.rx.hibernate.processing;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import com.braintribe.cfg.Configurable;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.processing.deployment.hibernate.mapping.HbmXmlGeneratingService;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.stream.ReaderInputStream;

public class HibernateModelSessionFactoryBuilder {
	private CmdResolver cmdResolver;
	private DataSource dataSource;
	private File ormDebugOutputFolder;
	private DialectAutoSense dialectAutoSense;

	public HibernateModelSessionFactoryBuilder(CmdResolver cmdResolver, DataSource dataSource) {
		super();
		this.cmdResolver = cmdResolver;
		this.dataSource = dataSource;
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
		properties.put(Environment.HBM2DDL_AUTO, "update");

		if (dialectAutoSense != null)
			properties.put(Environment.DIALECT, dialectAutoSense.senseDialect(dataSource));
		
		generateMappings(configuration);
		
		return configuration.buildSessionFactory();
    }

	private void generateMappings(Configuration configuration) {
		
		new HbmXmlGeneratingService() //
				.cmdResolverAndModel(cmdResolver) //
				//.generateJpaOrm() //
				.entityMappingConsumer(sd -> {
					if (ormDebugOutputFolder != null) {
						File outputFile = new File(ormDebugOutputFolder, sd.sourceRelativePath);
						
						outputFile.getParentFile().mkdirs();
						
						FileTools.write(outputFile) //
							.withCharset(StandardCharsets.UTF_8) //
							.string(sd.sourceCode);
					}
						
					try (ReaderInputStream in = new ReaderInputStream(new StringReader(sd.sourceCode))) {
						configuration.addInputStream(in);
					} catch (IOException e) {
						throw new UncheckedIOException("Error while applying " + sd.sourceRelativePath + " as InputStream hibernate configuration", e);
					}
				}).renderMappings();
	}
}
