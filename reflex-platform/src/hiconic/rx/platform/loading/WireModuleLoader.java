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
package hiconic.rx.platform.loading;

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

import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.RxModuleContract;

/**
 * @author peter.gazdik
 */
/* package */ class WireModuleLoader {

	public static Maybe<List<RxModule<?>>> loadWireModules() {
		Maybe<List<URL>> urlsMaybe = collectModuleUrls();
		if (urlsMaybe.isUnsatisfied())
			return urlsMaybe.cast();

		List<URL> urls = urlsMaybe.get();

		List<Maybe<RxModule<?>>> maybeWireModules = urls.stream() //
				.parallel() //
				.map(url -> loadRxModule(url)) //
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

	private static Maybe<RxModule<?>> loadRxModule(URL propertiesUrl) {
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

		Maybe<RxModule<?>> rxModule = loadRxModule(wireModule);

		if (rxModule.isUnsatisfied())
			return Reasons.build(ConfigurationError.T) //
					.text("Could load " + wireModule + " configured with property 'wire-module' in " + propertiesUrl) //
					.cause(rxModule.whyUnsatisfied()).toMaybe();

		return rxModule;
	}

	private static Maybe<RxModule<?>> loadRxModule(String wireModule) {
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

		if (!(constant instanceof RxModule))
			return Reasons.build(NotFound.T) //
					.text("Constant INSTANCE of enum class " + wireModule + " is not an RxModule") //
					.toMaybe();

		var rxModule = (RxModule<?>) constant;

		if (!RxModuleContract.class.isAssignableFrom(rxModule.contract())) {
			return Reasons.build(InvalidArgument.T) //
					.text("Constant INSTANCE of enum class " + wireModule + " is not an RxModule with a contract of type RxModuleContract") //
					.toMaybe();
		}

		return Maybe.complete(rxModule);
	}
}
