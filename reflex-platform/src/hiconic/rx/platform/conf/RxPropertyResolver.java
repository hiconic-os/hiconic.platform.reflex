package hiconic.rx.platform.conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.cfg.Configurable;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.reason.essential.ParseError;
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
		
		if (maybe.isUnsatisfiedBy(NotFound.T))
			return null;
		
		return maybe.get();
	}
	
	private Maybe<String> resolveRaw(String name) {
		String rawValue = findRawValue(name);
		
		if (rawValue == null)
			return Reasons.build(NotFound.T).text("Could not find property: " + name).toMaybe();
		
		Maybe<String> maybe = evaluate(rawValue);
		
		if (maybe.isUnsatisfied())
			return Reasons.build(ConfigurationError.T).text("Could not resolve property: " + name).cause(maybe.whyUnsatisfied()).toMaybe();
		
		return maybe;
	}
	
	private Maybe<String> evaluate(String rawValue) {
		final Template template;
		try {
			template = Template.parse(rawValue);
			if (template.isStaticOnly())
				return Maybe.complete(rawValue);
		}
		catch (IllegalArgumentException e) {
			return Reasons.build(ParseError.T).text("Could not parse expression [" + rawValue + "]: " + e.getMessage()).toMaybe();
		}
		
		PlaceholderResolutionContext placeholderResolutionContext = new PlaceholderResolutionContext();

		String value = template.evaluate(placeholderResolutionContext::resolvePlaceholder);
		
		if (placeholderResolutionContext.error != null)
			return placeholderResolutionContext.error.asMaybe();
		
		return Maybe.complete(value);
	}
	
	private String findRawValue(String name) {
		String value = rawProperties.get(name);
		
		if (value != null)
			return value;
		
		value = virtualEnvironment.getProperty(name);
		
		if (value != null)
			return value;
		
		return virtualEnvironment.getEnv(name);
	}
	
	private class PlaceholderResolutionContext {
		public Reason error;

		public String resolvePlaceholder(String placeholder) {

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
									error = Reasons.build(ConfigurationError.T).text("Could not resolve decryption secret") //
											.cause(maybeSecret.whyUnsatisfied()).toReason();
									return "?";
								}
								
								return Cryptor.decrypt(maybeSecret.get(), null, null, null, param);
							}
							break;
						default:
							throw new RuntimeException("Unsupported variable function: " + method);
					}
				}
			}

			Maybe<String> maybeValue = resolveReasoned(placeholder);
			
			if (maybeValue.isUnsatisfied()) {
				error = Reasons.build(ConfigurationError.T).text("Could not resolve placeholder: " + placeholder).cause(maybeValue.whyUnsatisfied()).toReason();
				return "?";
			}
			
			return maybeValue.get();
		}
	}
}
