package hiconic.rx.cli.processing;

import static com.braintribe.console.ConsoleOutputs.brightGreen;
import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.CharacterMarshaller;
import com.braintribe.codec.marshaller.api.CharsetOption;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.options.GmSerializationContextBuilder;
import com.braintribe.common.attribute.common.CallerEnvironment;
import com.braintribe.common.attribute.common.impl.BasicCallerEnvironment;
import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.PrintStreamConsole;
import com.braintribe.console.VoidConsole;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.cli.posix.parser.PosixCommandLineParser;
import com.braintribe.gm.cli.posix.parser.api.ParsedCommandLine;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.OutputConfigAspect;
import com.braintribe.model.processing.service.impl.BasicOutputConfig;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.platform.cli.model.api.Introduce;
import hiconic.rx.platform.cli.model.api.Options;

public class CliExecutor {
	private static final List<String> FILE_INDICATORS = Arrays.asList(".", "/", "\\", ":");
	
	private String[] args;
	private PosixCommandLineParser parser;
	private List<String> defaultDomains;
	private Options options = Options.T.create();
	private ServiceRequest request;
	private Evaluator<ServiceRequest> evaluator;
	private MarshallerRegistry marshallerRegistry;
	
	@Required
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
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
		ConsoleConfiguration.install(VoidConsole.INSTANCE);
		System.exit(retval);
	}
	
	private int _process() throws Exception {

		Reason error = loadRequestAndOptions();
		
		configureProtocolling(options);
		
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
		
		ParsedCommandLine commandLine = commandLineMaybe.get();
		
		options = commandLine.acquireInstance(Options.T);
		request = commandLine.findInstance(ServiceRequest.T).orElseGet(Introduce.T::create);
		
		return null;
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
		String protocolTo = options.getProtocol();

		if (protocolTo != null) {
			switch (protocolTo) {
				case OutputChannels.STDOUT:
					ensureCharsetAndInstallProtocolOutput(System.out);
					break;
				case OutputChannels.STDERR:
					ensureCharsetAndInstallProtocolOutput(System.err);
					break;
				case OutputChannels.NONE:
					ConsoleConfiguration.install(VoidConsole.INSTANCE);
					break;
				default:
					Reason error = checkChannelValue(protocolTo, "protocol");
					if (error != null)
						return error;

				try {
					PrintStream printStream = new PrintStream(new FileOutputStream(protocolTo), true, "UTF-8");
					ensureCharsetAndInstallProtocolOutput(printStream);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		} else {
			ConsoleConfiguration.install(VoidConsole.INSTANCE);
		}
		
		return null;
	}
	
	private static Reason checkChannelValue(String value, String parameterName) {
		if (!StringTools.containsAny(value, FILE_INDICATORS)) {
			return Reasons.build(InvalidArgument.T).text(
					"Unknown channel value for parameter -" + parameterName + ". Did you you mean a file? Then use \"." + File.separator + value
							+ "\". Channel values are: " + OutputChannels.STDOUT + ", " + OutputChannels.STDERR + ", " + OutputChannels.NONE).toReason();
		}
		
		return null;
	}


	private void ensureCharsetAndInstallProtocolOutput(PrintStream ps) {
		try {
			if (options.getProtocolCharset() != null) {
				ps = new PrintStream(ps, false, options.getProtocolCharset());
			}

			PrintStreamConsole console = new PrintStreamConsole(ps, options.getColored());
			ConsoleConfiguration.install(console);

		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static OutputProvider configureResponding(Options options) {
		String respondTo = options.getResponse();
		if (respondTo == null)
			return null;

		switch (respondTo) {
			case OutputChannels.STDOUT:
				return new PrintStreamProvider(System.out);

			case OutputChannels.STDERR:
				return new PrintStreamProvider(System.err);

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

	private void printShortErrorMessage(Exception e) {
		printErrorMessage(getErrorMessage(e));
	}
	
	private void printErrorMessage(Reason error) {
		printErrorMessage(error.stringify());
	}
	
	private void printErrorMessage(String msg) {
		println(sequence(brightRed("\nERROR: "), text(msg)));
	}
	

	private static String getErrorMessage(Throwable e) {
		String message = e.getMessage();

		if (!StringTools.isEmpty(message))
			return message;

		StackTraceElement ste = e.getStackTrace()[0];
		return e.getClass().getSimpleName() + " occurred in " + ste.getClassName() + '.' + ste.getMethodName() + " line " + ste.getLineNumber();
	}


}
