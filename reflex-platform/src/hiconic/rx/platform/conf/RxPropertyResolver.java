package hiconic.rx.platform.conf;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.braintribe.cfg.Configurable;
import com.braintribe.gm.config.yaml.PropertyResolutions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonAggregator;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.config.PropertyNotFound;
import com.braintribe.gm.model.reason.config.UnresolvedPlaceholder;
import com.braintribe.gm.model.reason.config.UnresolvedProperty;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.template.Template;
import com.braintribe.utils.encryption.Cryptor;
import com.braintribe.ve.api.VirtualEnvironment;
import com.braintribe.ve.impl.StandardEnvironment;

import hiconic.rx.module.api.common.RxPlatform;

public class RxPropertyResolver {
	private Map<String, String> rawProperties = Collections.emptyMap();
	private Map<String, Maybe<String>> resolvedProperties = new ConcurrentHashMap<>();
	private VirtualEnvironment virtualEnvironment = StandardEnvironment.INSTANCE;

	@Configurable
	public void setRawProperties(Map<String, String> rawProperties) {
		this.rawProperties = rawProperties;
	}
	
	@Configurable
	public void setVirtualEnvironment(VirtualEnvironment virtualEnvironment) {
		this.virtualEnvironment = virtualEnvironment;
	}
	
	public Maybe<String> resolveReasoned(String name) {
		return resolvedProperties.computeIfAbsent(name, this::resolveRaw);
	}
	
	public String resolve(String name) {
		Maybe<String> maybe = resolveReasoned(name);
		
		if (maybe.isUnsatisfiedBy(PropertyNotFound.T))
			return null;
		
		return maybe.get();
	}
	
	private Maybe<String> resolveRaw(String name) {
		String rawValue = findRawValue(name);
		
		if (rawValue == null)
			return PropertyNotFound.create(name).asMaybe();
		
		ReasonAggregator<UnresolvedProperty> errorAggregator = Reasons.aggregatorForceWrap(() -> UnresolvedProperty.create(name));
		
		String value = evaluate(rawValue, errorAggregator);
		
		if (errorAggregator.hasReason())
			return errorAggregator.get().asMaybe();
		
		return Maybe.complete(value);
	}
	
	private String evaluate(String rawValue, Consumer<Reason> errorConsumer) {
		final Template template;
		try {
			template = Template.parse(rawValue);
			if (template.isStaticOnly())
				return rawValue;
		}
		catch (IllegalArgumentException e) {
			var error = Reasons.build(ParseError.T) //
					.text("Could not parse expression [" + rawValue + "]: " + e.getMessage()) //
					.toReason();
			
			errorConsumer.accept(error);
			return null;
		}
		
		PlaceholderResolutionContext placeholderResolutionContext = new PlaceholderResolutionContext(errorConsumer);

		return template.evaluate(placeholderResolutionContext::resolvePlaceholder);
	}
	
	private String findRawValue(String name) {
		if (name.startsWith(PropertyResolutions.ENV_PREFIX)) {
			String envName = name.substring(PropertyResolutions.ENV_PREFIX.length());

			return virtualEnvironment.getEnv(envName);
		}
		
		String value = rawProperties.get(name);
		
		if (value != null)
			return value;
		
		value = virtualEnvironment.getProperty(name);
		
		if (value != null)
			return value;
		
		return virtualEnvironment.getEnv(name);
	}
	
	private class PlaceholderResolutionContext {
		private Consumer<Reason> errorConsumer;
		
		public PlaceholderResolutionContext(Consumer<Reason> errorConsumer) {
			this.errorConsumer = errorConsumer;
		}

		public String resolvePlaceholder(String placeholder) {
			var maybe = resolvePlaceholderReasoned(placeholder);
			if (maybe.isSatisfied())
				return maybe.get();
			
			var error = UnresolvedPlaceholder.create(placeholder);
			error.getReasons().add(maybe.whyUnsatisfied());
			errorConsumer.accept(error);
			return "?";
		}
		
		private Maybe<String> resolvePlaceholderReasoned(String placeholder) {
			if (placeholder.contains("(") && placeholder.endsWith(")")) {
				int idx1 = placeholder.indexOf("(");
				int idx2 = placeholder.lastIndexOf(")");
				if (idx1 > 0 && idx2 > idx1) {
					String method = placeholder.substring(0, idx1);
					String param = placeholder.substring(idx1 + 1, idx2);
					switch (method) {
						case "decrypt":
							if ((param.startsWith("'") && param.endsWith("'")) || (param.startsWith("\"") && param.endsWith("\""))) {
								param = param.substring(1, param.length() - 1);
								Maybe<String> maybeSecret = resolveReasoned(RxPlatform.PROPERTY_DECRYPT_SECRET);
								
								if (maybeSecret.isUnsatisfied()) {
									return Reasons.build(ConfigurationError.T).text("Could not resolve decryption secret") //
											.cause(maybeSecret.whyUnsatisfied()).toMaybe();
								}
								
								try {
									return Maybe.complete(Cryptor.decrypt(maybeSecret.get(), null, null, null, param));
								} catch (Exception e) {
									return Reasons.build(ConfigurationError.T).text("Wrong decryption secret").toMaybe();
								}
							}
							break;
						default:
							return Reasons.build(UnsupportedOperation.T).text("Unsupported operation: " + method).toMaybe();
					}
				}
			}

			return resolveReasoned(placeholder);
		}
	}
}
