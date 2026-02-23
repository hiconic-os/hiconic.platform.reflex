package hiconic.rx.platform.loading;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.imageio.stream.FileImageInputStream;

import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.utils.FileTools;

public class RxPropertiesLoader {
	public static Maybe<Map<String,String>> load(File file, Marshaller marshaller) {
		MapType stringToStringMapType = GMF.getTypeReflection().getMapType(EssentialTypes.TYPE_STRING, EssentialTypes.TYPE_STRING);
		GmDeserializationOptions options = GmDeserializationOptions.deriveDefaults().setInferredRootType(stringToStringMapType).build();
		
		try {
			Maybe<Map<String, String>> maybe = FileTools.read(file).fromInputStream(in -> {
				return marshaller.unmarshallReasoned(in, options).cast();
			});
			
			if (maybe.isUnsatisfied()) {
				return Reasons.build(ConfigurationError.T) //
						.text("Error while reading rx properties from: " + file.getAbsolutePath()) //
						.cause(maybe.whyUnsatisfied()).toMaybe();
			}
			
			return maybe;
		} catch (UncheckedIOException e) {
			return Reasons.build(ConfigurationError.T) //
					.text("Error while reading rx properties from: " + file.getAbsolutePath()) //
					.cause(IoError.create(e.getMessage())) //
					.toMaybe();
		}
	}
}
