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
package hiconic.rx.cli.processing;

import static com.braintribe.console.ConsoleOutputs.brightGreen;
import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.CharacterMarshaller;
import com.braintribe.codec.marshaller.api.CharsetOption;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.options.GmSerializationContextBuilder;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.AttributeContextBuilder;
import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.common.attribute.common.impl.BasicCallerEnvironment;
import com.braintribe.console.Console;
import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.cli.posix.parser.PosixCommandLineParser;
import com.braintribe.gm.cli.posix.parser.api.ParsedCommandLine;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.OutputConfigAspect;
import com.braintribe.model.processing.service.impl.BasicOutputConfig;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.StringTools;
import com.braintribe.utils.stream.NullOutputStream;

import hiconic.rx.module.api.endpoint.EndpointInput;
import hiconic.rx.module.api.endpoint.EndpointInputAttribute;
import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.platform.cli.model.api.Introduce;
import hiconic.rx.platform.cli.model.api.Options;

public class CliExecutor implements EndpointInput {
	private static final List<String> FILE_INDICATORS = Arrays.asList(".", "/", "\\", ":");
	private static Logger logger = System.getLogger(CliExecutor.class.getName());
	private static PrintStream stdout = System.out;
	private static PrintStream stderr = System.err;
	
	private String[] args;
	private PosixCommandLineParser parser;
	private List<String> defaultDomains;
	private Options options = Options.T.create();
	private ServiceRequest request;
	private Evaluator<ServiceRequest> evaluator;
	private MarshallerRegistry marshallerRegistry;
	private ParsedCommandLine commandLine;
	private ServiceDomains serviceDomains;
	private Function<EntityType<?>, GenericEntity> entityFactory;
	private final List<Runnable> runAtExit = new ArrayList<>();
	
	@Required
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}
	
	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}
	
	@Required
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}
	
	@Required
	public void setDefaultDomains(List<String> defaultDomains) {
		this.defaultDomains = defaultDomains;
	}
	
	@Required
	public void setParser(PosixCommandLineParser parser) {
		this.parser = parser;
	}
	
	@Required
	public void setArgs(String[] args) {
		this.args = args;
	}
	
	@Required
	public void setEntityFactory(Function<EntityType<?>, GenericEntity> entityFactory) {
		this.entityFactory = entityFactory;
	}
	
	public void process() {
		try {
			exit(_process());
		}
		catch (Throwable e) {
			printFullException(e);
			exit(1);
		}
	}
	
	private void exit(int retval) {
		runAtExit.forEach(Runnable::run);
		System.exit(retval);
	}
	
	private void closeAtExit(Closeable closeable) {
		runAtExit(() -> {
			try {
				closeable.close();
			} catch (IOException e) {
				logger.log(Level.ERROR, "Error while closing closeable", e);
			}
		});
	}
	
	private void runAtExit(Runnable runnable) {
		runAtExit.add(runnable);
	}
	
	private int _process() throws Exception {
		AttributeContextBuilder contextBuilder = AttributeContexts.derivePeek();
		
		contextBuilder.set(EndpointInputAttribute.class, this);
		
		AttributeContext attributeContext = contextBuilder.build();
		
		AttributeContexts.push(attributeContext);
		try {
			return processContextualized();
		}
		finally {
			AttributeContexts.pop();
		}
	}
	
	private int processContextualized() throws Exception {
		// preliminary console
		Reason error = loadRequestAndOptions();
		
		installConsole(new SuppliedPrintStreamConsole(() -> System.out, options.getColored(), false));
		
		if (error != null) {
			printErrorMessage(error);
			return 1;
		}

		// preliminary console
		
		error = configureProtocolling(options);

		if (error != null) {
			printErrorMessage(error);
			return 1;
		}

		executeGeneralTasks();

		error = evalAndHandleResponse();
		
		if (error != null) {
			printErrorMessage(error);
			return 1;
		}

		maybePrintDone();
		
		return 0;
	}
	
	private Reason loadRequestAndOptions() {
		Maybe<ParsedCommandLine> commandLineMaybe = parseCommand();
		
		if (commandLineMaybe.isUnsatisfied())
			return commandLineMaybe.whyUnsatisfied();
		
		commandLine = commandLineMaybe.get();
		
		options = commandLine.findInstance(Options.T).orElseGet(() -> (Options)entityFactory.apply(Options.T));
		request = commandLine.findInstance(ServiceRequest.T).orElseGet(this::determineDefaultRequest);
		
		return null;
	}
	
	private ServiceRequest determineDefaultRequest() {
		ServiceRequest defaultRequest = serviceDomains.main().defaultRequest();
		
		if (defaultRequest != null)
			return defaultRequest;
		
		return Introduce.T.create();
	}

	private Maybe<ParsedCommandLine> parseCommand() {
		try {
			return parser.parseReasoned(Arrays.asList(args), defaultDomains);
		} catch (Exception e) {
			// If an error happens here, we couldn't even have parsed the "verbose" option, so we simply print the full error
			printFullException(e);
			throw e;
		}
	}
	
	public Reason configureProtocolling(Options options) {
		String protocolTo = Optional.ofNullable(options.getProtocol()).orElse(OutputChannels.NONE);
		
		switch (protocolTo) {
			case OutputChannels.STDOUT:
				return ensureCharsetAndInstallProtocolOutput(stdout);
			case OutputChannels.STDERR:
				return ensureCharsetAndInstallProtocolOutput(stderr);
			case OutputChannels.NONE:
				return ensureCharsetAndInstallProtocolOutput(new PrintStream(NullOutputStream.getInstance()));
			default:
				Reason error = checkChannelValue(protocolTo, "protocol");
				
				if (error != null)
					return error;

				try {
					PrintStream printStream = new PrintStream(new FileOutputStream(protocolTo), true, "UTF-8");
					closeAtExit(printStream);
					return ensureCharsetAndInstallProtocolOutput(printStream);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
		}
	}
	
	private void installConsole(Console console) {
		Console previousConsole = Console.get();
		runAtExit(() -> ConsoleConfiguration.install(previousConsole));
		ConsoleConfiguration.install(console);
	}
	
	private static Reason checkChannelValue(String value, String parameterName) {
		if (!StringTools.containsAny(value, FILE_INDICATORS)) {
			return Reasons.build(InvalidArgument.T).text(
					"Unknown channel value for parameter -" + parameterName + ". Did you you mean a file? Then use \"." + File.separator + value
							+ "\". Channel values are: " + OutputChannels.STDOUT + ", " + OutputChannels.STDERR + ", " + OutputChannels.NONE).toReason();
		}
		
		return null;
	}


	private Reason ensureCharsetAndInstallProtocolOutput(PrintStream stream) {
		Reason error = null;
		
		if (options.getProtocolCharset() != null) {
			Charset charset = Charset.forName(options.getProtocolCharset(), (Charset)null);
			
			if (charset == null) {
				error = Reasons.build(NotFound.T) //
					.text("Charset configured with Options.protocolCharset not found: " + options.getProtocolCharset()) //
					.toReason();
				charset = StandardCharsets.UTF_8;
			}
			
			stream = new PrintStream(stream, false, charset);
		}
		
		System.setOut(stream);
		
		return error;
	}

	public static OutputProvider configureResponding(Options options) {
		String respondTo = options.getResponse();
		if (respondTo == null)
			return null;

		switch (respondTo) {
			case OutputChannels.STDOUT:
				return new PrintStreamProvider(stdout);

			case OutputChannels.STDERR:
				return new PrintStreamProvider(stderr);

			case OutputChannels.NONE:
				return null;

			default:
				checkChannelValue(respondTo, "response");

				return new FileOutputStreamProvider(new File(respondTo));
		}
	}
	
	private void executeGeneralTasks() {
		// print jinni version if required
		if (options.getPrintVersion()) {
			// TODO: printVersion();
		}

		if (options.getEchoCommand()) {
			// TODO: echoCommand();
		}
	}
	
	private Reason evalAndHandleResponse() throws IOException {
		Maybe<?> maybe = evalRequest();
		
		if (maybe.isUnsatisfied())
			return maybe.whyUnsatisfied();

		Object value = maybe.get();
		
		handleResponse(value);
		
		return null;
	}

	private Maybe<?> evalRequest() {
		EvalContext<?> evalContext = request.eval(evaluator);
		evalContext.setAttribute(OutputConfigAspect.class, new BasicOutputConfig(options.getVerbose()));
		evalContext.setAttribute(CallerEnvironment.class, callerEnvironment());

		// evaluate the request
		return evalContext.getReasoned();
	}
	
	@Override
	public <I extends GenericEntity> I findInput(EntityType<I> inputType) {
		if (commandLine == null)
			return null;
		return commandLine.findInstance(inputType).orElse(null);
	}
	
	@Override
	public <I extends GenericEntity> List<I> findInputs(EntityType<I> inputType) {
		if (commandLine == null)
			return Collections.emptyList();

		return commandLine.listInstances(inputType);
	}

	private CallerEnvironment callerEnvironment() {
		File currentWorkingDirectory = new File(System.getProperty("user.dir"));
		return new BasicCallerEnvironment(true, currentWorkingDirectory);
	}

	private void handleResponse(Object value) throws IOException {
		if (value == Neutral.NEUTRAL)
			return;
		
		OutputProvider outputProvider = configureResponding(options);
		if (outputProvider != null)
			outputResult(outputProvider, value);
	}
	
	private void outputResult(OutputProvider outputProvider, Object value) throws IOException {
		Mimetype mimetype = Mimetypes.parse(options.getResponseMimeType());

		Marshaller marshaller = resolveMarshallerFor(mimetype);

		String charset = mimetype.getCharset();
		GmSerializationContextBuilder highlyPretty = GmSerializationOptions.deriveDefaults().outputPrettiness(OutputPrettiness.high);

		if (marshaller instanceof CharacterMarshaller) {
			try (Writer writer = outputProvider.openOutputWriter(charset, mimetype.hasExplicitCharset())) {
				((CharacterMarshaller) marshaller).marshall(writer, value, highlyPretty.build());
			}

		} else {
			try (OutputStream responseOut = outputProvider.openOutputStream()) {
				marshaller.marshall(responseOut, value, highlyPretty.set(CharsetOption.class, charset).build());
			}
		}
	}

	private Marshaller resolveMarshallerFor(Mimetype mimetype) {
		Marshaller marshaller = marshallerRegistry.getMarshaller(mimetype.getMimeType());
		if (marshaller == null)
			throw new NoSuchElementException("No marshaller found for mimetype: " + mimetype.getMimeType());

		return marshaller;
	}
	
	private void maybePrintDone() {
		if (!Boolean.TRUE.toString().equals(System.getProperty("jinni.suppressDone")))
			println(brightGreen("\nDONE"));
	}

	private static void printFullException(Throwable e) {
		println(brightRed("\nERROR:\n"));
		println(Exceptions.stringify(e));
	}

	private void printErrorMessage(Reason error) {
		printErrorMessage(error.stringify());
	}
	
	private void printErrorMessage(String msg) {
		println(sequence(brightRed("\nERROR: "), text(msg)));
	}
	

	// TODO these are unused. Delete them?

	@SuppressWarnings("unused")
	private void printShortErrorMessage(Exception e) {
		printErrorMessage(getErrorMessage(e));
	}
	
	private static String getErrorMessage(Throwable e) {
		String message = e.getMessage();

		if (!StringTools.isEmpty(message))
			return message;

		StackTraceElement ste = e.getStackTrace()[0];
		return e.getClass().getSimpleName() + " occurred in " + ste.getClassName() + '.' + ste.getMethodName() + " line " + ste.getLineNumber();
	}


}
