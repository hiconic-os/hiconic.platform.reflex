package hiconic.rx.platform.loading;

import static com.braintribe.console.ConsoleOutputs.brightBlack;
import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.IoError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.wire.api.module.WireTerminalModule;

import hiconic.rx.module.api.wire.RxModuleContract;

/**
 * @author peter.gazdik
 */
/* package */ class WireModuleLoader {

	public static Maybe<List<WireTerminalModule<RxModuleContract>>> loadWireModules() {
		Maybe<List<URL>> urlsMaybe = collectModuleUrls();
		if (urlsMaybe.isUnsatisfied())
			return urlsMaybe.cast();

		List<URL> urls = urlsMaybe.get();

		List<Maybe<WireTerminalModule<RxModuleContract>>> maybeWireModules = urls.stream() //
				.parallel() //
				.map(url -> loadWireModule(url)) //
				.collect(Collectors.toList());

		return fuseMaybes(maybeWireModules, () -> ConfigurationError.create("Error while loading rx-module configurations"));
	}

	/**
	 * Naturally converts given {@code Iterable<Maybe<T>>} into a single {@code Maybe<List<T>>}.
	 * <p>
	 * If all given {@link Maybe}s were {@link Maybe#isSatisfied() satisfied}, the resulting Maybe is also satisfied, with a list of all the
	 * individual values.
	 * <p>
	 * If some of the given Maybes were unsatisfied, the resulting maybe is also unsatisfied, with a reason created by given
	 * <code>collationReasonFactory</code>, and {@link Reason#getReasons() caused} by a list of all the reasons behind all the unsatisfied Maybes.
	 */
	private static <T> Maybe<List<T>> fuseMaybes(Iterable<Maybe<T>> maybes, Supplier<? extends Reason> collationReasonFactory) {
		Reason collationReason = null;

		List<T> values = new ArrayList<>();

		for (Maybe<T> maybe : maybes) {
			if (maybe.isSatisfied()) {
				values.add(maybe.get());
				continue;
			}

			if (collationReason == null)
				collationReason = collationReasonFactory.get();

			collationReason.getReasons().add(maybe.whyUnsatisfied());
		}

		if (collationReason != null)
			return collationReason.asMaybe();

		return Maybe.complete(values);
	}

	private static Maybe<List<URL>> collectModuleUrls() {
		try {
			Enumeration<URL> resources = loadRxModulePropertyFiles();

			List<URL> result = new ArrayList<>();

			while (resources.hasMoreElements()) {
				result.add(resources.nextElement());
			}

			return Maybe.complete(result);

		} catch (IOException e) {
			return Reasons.build(IoError.T) //
					.text("Could not enumerate classpath resources with the name 'META-INF/rx-module.properties'") //
					.cause(InternalError.from(e)) //
					.toMaybe();
		}
	}

	private static Enumeration<URL> loadRxModulePropertyFiles() throws IOException {
		return WireModuleLoader.class.getClassLoader().getResources("META-INF/rx-module.properties");
	}

	private static Maybe<WireTerminalModule<RxModuleContract>> loadWireModule(URL propertiesUrl) {
		Properties properties = new Properties();

		try (Reader reader = new InputStreamReader(propertiesUrl.openStream(), "UTF-8")) {
			properties.load(reader);

		} catch (IOException e) {
			return Reasons.build(IoError.T) //
					.text("Could not read properties from " + propertiesUrl) //
					.cause(InternalError.from(e)) //
					.toMaybe();
		}

		String wireModule = properties.getProperty("wire-module");
		if (wireModule == null)
			return Reasons.build(ConfigurationError.T) //
					.text("Missing property 'wire-module' in " + propertiesUrl) //
					.toMaybe();

		printWireModule(wireModule);

		Maybe<WireTerminalModule<RxModuleContract>> maybeTerminalModule = loadWireTerminalModule(wireModule);

		if (maybeTerminalModule.isUnsatisfied())
			return Reasons.build(ConfigurationError.T) //
					.text("Could load " + wireModule + " configured with property 'wire-module' in " + propertiesUrl) //
					.cause(maybeTerminalModule.whyUnsatisfied()).toMaybe();

		return maybeTerminalModule;
	}

	private static void printWireModule(String wireModule) {
		int index = wireModule.lastIndexOf('.');

		String pckg = wireModule.substring(0, index);
		String name = wireModule.substring(index + 1);

		println( //
				sequence(text("  - "), //
						cyan(name), //
						brightBlack(" (" + pckg + ")") //
				));
	}

	private static Maybe<WireTerminalModule<RxModuleContract>> loadWireTerminalModule(String wireModule) {
		Class<?> wireModuleClass;
		try {
			wireModuleClass = Class.forName(wireModule);

		} catch (ClassNotFoundException e) {
			return Reasons.build(NotFound.T) //
					.text("Class not found: " + wireModule) //
					.toMaybe();
		}

		if (!wireModuleClass.isEnum())
			return Reasons.build(InvalidArgument.T) //
					.text("Class is not an enum: " + wireModule) //
					.toMaybe();

		@SuppressWarnings("rawtypes")
		var enumClass = (Class<? extends Enum>) wireModuleClass;

		Enum<?> constant;
		try {
			constant = Enum.valueOf(enumClass, "INSTANCE");

		} catch (IllegalArgumentException e) {
			return Reasons.build(InvalidArgument.T) //
					.text("Enum class " + wireModule + " is missing a constant INSTANCE") //
					.toMaybe();
		}

		if (!(constant instanceof WireTerminalModule))
			return Reasons.build(NotFound.T) //
					.text("Constant INSTANCE of enum class " + wireModule + " is not a WireTerminalModule") //
					.toMaybe();

		var wireTerminalModule = (WireTerminalModule<RxModuleContract>) constant;

		if (!RxModuleContract.class.isAssignableFrom(wireTerminalModule.contract())) {
			return Reasons.build(InvalidArgument.T) //
					.text("Constant INSTANCE of enum class " + wireModule + " is not a WireTerminalModule with a contract of type RxModuleContract") //
					.toMaybe();
		}

		return Maybe.complete(wireTerminalModule);
	}
}
