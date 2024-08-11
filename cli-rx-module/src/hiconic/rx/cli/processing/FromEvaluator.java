// ============================================================================
package hiconic.rx.cli.processing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.util.NoSuchElementException;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.EntityFactory;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.options.GmDeserializationContextBuilder;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.cli.posix.parser.api.CliEntityEvaluator;
import com.braintribe.gm.config.yaml.ConfigVariableResolver;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.config.yaml.api.ConfigurationReadBuilder;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.resource.FileResource;
import com.braintribe.utils.stream.KeepAliveDelegateInputStream;
import com.braintribe.ve.api.VirtualEnvironment;
import com.braintribe.ve.impl.StandardEnvironment;

import hiconic.rx.platform.cli.model.api.From;
import hiconic.rx.platform.cli.model.api.FromFile;
import hiconic.rx.platform.cli.model.api.FromStdin;
import hiconic.rx.platform.cli.model.api.FromUrl;

/**
 * @author peter.gazdik
 */
public class FromEvaluator implements CliEntityEvaluator {

	private VirtualEnvironment virtualEnvironment = StandardEnvironment.INSTANCE;
	private MarshallerRegistry marshallerRegistry;

	@Configurable
	public void setVirtualEnvironment(VirtualEnvironment virtualEnvironment) {
		this.virtualEnvironment = virtualEnvironment;
	}
	
	@Required
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}
	
	@Override
	public boolean isEvaluable(GenericEntity entity) {
		return entity instanceof From;
	}
	
	@Override
	public Object evaluate(GenericEntity entity) {
		if (!(entity instanceof From)) 
			throw new IllegalStateException("Unexpected entity type " + entity.entityType().getTypeSignature());
		
		From from = (From)entity;
		
		String mimeType = from.getMimeType();

		Marshaller marshaller = marshallerRegistry.getMarshaller(mimeType);

		if (marshaller == null)
			throw new NoSuchElementException("No marshaller registered for mimetype: " + mimeType);
		
		String inferredTypeSignature = from.getInferredType();
		
		GenericModelType inferredType = determineType(inferredTypeSignature);
		
		if (marshaller instanceof YamlMarshaller && from instanceof FromFile && ((FromFile)from).getHasVars()) {
			FromFile fromFile = (FromFile)from;
			File file = new File(fromFile.getFile().getPath());
			
			ConfigVariableResolver variableResolver = new ConfigVariableResolver(virtualEnvironment, file);
			
			ConfigurationReadBuilder<Object> builder = YamlConfigurations.read(inferredType).placeholders(variableResolver::resolve);

			if (from.getReproduce())
				builder.noDefaulting();
			
			return builder.from(file).get();
		}
		else {
			try (InputStream in = openInputStream(from)) {
				GmDeserializationContextBuilder options = GmDeserializationOptions.deriveDefaults();
				// options
				if (!from.getReproduce())
					options = options.set(EntityFactory.class, EntityType::create);
				
				options.setInferredRootType(inferredType);

				return marshaller.unmarshall(in, options.build());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private GenericModelType determineType(String inferredTypeSignature) {
		return GMF.getTypeReflection().getType(inferredTypeSignature);
	}

	private InputStream openInputStream(From from) throws IOException {
		if (from instanceof FromStdin) {
			return new KeepAliveDelegateInputStream(System.in);
		} else if (from instanceof FromUrl) {
			FromUrl fromUrl = (FromUrl) from;
			String urlProperty = fromUrl.getUrl();

			if (urlProperty == null)
				throw new IllegalStateException("FromUrl is missing url");

			URL url = URI.create(urlProperty).toURL();
			return url.openStream();
		} else if (from instanceof FromFile) {
			FromFile fromFile = (FromFile) from;
			FileResource file = fromFile.getFile();

			if (file == null)
				throw new IllegalStateException("FromFile is missing file");

			return file.openStream();
		} else {
			throw new NoSuchElementException("No support for From type: " + from.entityType());
		}
	}
}