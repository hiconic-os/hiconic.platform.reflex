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
package hiconic.platform.reflex.web_server.processing.exception;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;
import com.braintribe.codec.marshaller.api.EntityRecurrenceDepth;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.api.options.GmSerializationContextBuilder;
import com.braintribe.exception.AuthorizationException;
import com.braintribe.exception.Exceptions;
import com.braintribe.exception.HasLogPreferences;
import com.braintribe.exception.HttpException;
import com.braintribe.exception.LogPreferences;
import com.braintribe.logging.Logger;
import com.braintribe.logging.Logger.LogLevel;
import com.braintribe.model.processing.service.common.FailureCodec;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.NullSafe;

import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import dev.hiconic.servlet.impl.util.ServletTools;
import hiconic.rx.web.server.model.config.ExceptionExposure;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StandardExceptionHandler implements ExceptionHandler, InitializationAware {

	private static final Logger logger = Logger.getLogger(StandardExceptionHandler.class);

	private ExceptionExposure exceptionExposure = ExceptionExposure.full;

	public static final String APPLICATION_JSON = "application/json";

	private MarshallerRegistry marshallerRegistry;

	private Map<Class<? extends Throwable>, Integer> statusCodeMap;
	private Map<Class<? extends Throwable>, LogPreferences> logPreferencesMap;

	private RemoteClientAddressResolver remoteAddressResolver;

	private boolean tracebackIdExposure = true;

	@Override
	public Boolean apply(ExceptionHandlingContext context) {

		String tracebackId = context.getTracebackId();
		Throwable t = context.getThrowable();
		ServletRequest request = context.getRequest();
		ServletResponse response = context.getResponse();

		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append("TBID:");
		errorMessage.append(tracebackId);
		errorMessage.append(": ");
		errorMessage.append(t.getMessage());
		errorMessage.append(" (");
		errorMessage.append(ServletTools.stringify(request));
		if (remoteAddressResolver != null && request instanceof HttpServletRequest) {
			String sourceIp = remoteAddressResolver.getRemoteIpLenient((HttpServletRequest) request);
			if (sourceIp != null) {
				errorMessage.append(", IP:");
				errorMessage.append(sourceIp);
			}
		}
		errorMessage.append(")");

		LogPreferences logPreferences = getLogPreferences(context.getThrowable());

		if (logPreferences == null) {
			// default logging
			logger.error(errorMessage.toString(), t);
		} else {

			LogLevel logLevel = logPreferences.getLogLevel();
			if (logLevel == null) {
				logLevel = LogLevel.ERROR;
			}

			LogLevel fullLogLevel = logPreferences.getFullLogLevel();
			boolean includeThrowable = logPreferences.isIncludeThrowable();

			boolean logPrimaryMessage = logger.isLevelEnabled(logLevel);
			// We only need to write the second log message, when
			// * the first one did not contain the throwable already
			// * the log level for the full log has been specified
			// * this particular log level is enabled
			boolean logSecondaryFullMessage = !includeThrowable && fullLogLevel != null && logger.isLevelEnabled(fullLogLevel);

			if (logPrimaryMessage) {
				if (includeThrowable) {
					logger.log(logLevel, errorMessage.toString(), t);
				} else {
					logger.log(logLevel, errorMessage.toString());
				}
			}
			if (logSecondaryFullMessage) {
				logger.log(fullLogLevel, errorMessage.toString(), t);
			}
		}

		if (!context.isOutputCommitted()) {

			if (response instanceof HttpServletResponse) {
				HttpServletResponse httpResponse = (HttpServletResponse) response;

				int statusCode = getStatusCode(context.getThrowable());

				Body body = createBody(context, statusCode);

				context.setOutputCommitted(true);

				writeException(context, body, statusCode, httpResponse);

				return Boolean.TRUE;

			} else {
				logger.warn("The request (" + request + ") or the response (" + response + ") are not of HttpServletRequest/HttpServletResponse");
			}

		}

		return Boolean.FALSE;
	}

	protected Body createBody(ExceptionHandlingContext context, int statusCode) {
		Marshaller marshaller = null;

		boolean htmlAccepted = false;
		boolean textAccepted = false;
		String actualMimeType = "text/plain";
		ServletRequest request = context.getRequest();
		HttpServletRequest httpServletRequest = null;
		if (request instanceof HttpServletRequest) {
			httpServletRequest = (HttpServletRequest) request;
		}

		boolean textPreferred = false;
		if (marshallerRegistry != null) {
			if (httpServletRequest != null) {
				List<String> acceptedMimeTypes = ServletTools.getAcceptedMimeTypes(httpServletRequest);
				if (acceptedMimeTypes != null && !acceptedMimeTypes.isEmpty()) {
					for (String acceptedMimeType : acceptedMimeTypes) {
						if (acceptedMimeType.equals("text/plain")) {
							textAccepted = true;
						}
						if (acceptedMimeType.equals("text/html")) {
							htmlAccepted = true;
						}
						marshaller = marshallerRegistry.getMarshaller(acceptedMimeType);
						if (marshaller != null) {
							if (textAccepted || htmlAccepted) {
								textPreferred = true;
							}
							actualMimeType = acceptedMimeType;
							break;
						}
					}
				} else {
					// No Accept header in the request; let's try to respond in the same format as the request
					String contentType = request.getContentType();
					if (!StringTools.isBlank(contentType)) {
						int index = contentType.indexOf(";");
						if (index != -1) {
							contentType = contentType.substring(0, index).trim();
						}
						contentType = contentType.toLowerCase();
						marshaller = marshallerRegistry.getMarshaller(contentType);
						if (marshaller != null) {
							actualMimeType = contentType;
						}
					}
				}
			}

			if (marshaller == null) {
				if (!textAccepted && !htmlAccepted) {
					marshaller = marshallerRegistry.getMarshaller(APPLICATION_JSON);
					actualMimeType = APPLICATION_JSON;
				}
			}
		}

		boolean exposeMessage = exposeMessage();
		boolean exposeException = exposeException(statusCode);

		if (marshaller != null && !textPreferred) {
			Throwable throwable = context.getThrowable();
			if (throwable instanceof HttpException) {

				HttpException httpException = (HttpException) throwable;
				Object payload = httpException.getPayload();
				if (payload != null) {

					// Use the transfered payload as body instead of creating a Failure object.

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					marshaller.marshall(baos, payload, GmSerializationOptions.deriveDefaults().setOutputPrettiness(OutputPrettiness.high).build());

					Body marshalledBody = new Body(baos.toByteArray(), actualMimeType.concat("; charset=UTF-8"));
					return marshalledBody;
				}
			}

			Failure failure = FailureCodec.INSTANCE.encode(context.getThrowable());

			if (tracebackIdExposure)
				failure.setTracebackId(context.getTracebackId());

			if (!exposeMessage) {
				failure.setMessage(null);
			} else {
				String lastMessage = getLastMessage(context.getThrowable());
				failure.setMessage(lastMessage);
			}
			if (!exposeException) {
				failure.setDetails(null);
				failure.setCause(null);
				failure.getSuppressed().clear();
				failure.setType(null);
			}

			GmSerializationContextBuilder optsBuilder = GmSerializationOptions.deriveDefaults() //
					.setOutputPrettiness(OutputPrettiness.high) //
					.set(EntityRecurrenceDepth.class, 1);

			if (!exposeException)
				optsBuilder.setInferredRootType(Failure.T) //
						.set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic);

			GmSerializationOptions serializationOpts = optsBuilder.build();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			marshaller.marshall(baos, failure, serializationOpts);

			Body marshalledBody = new Body(baos.toByteArray(), actualMimeType.concat("; charset=UTF-8"));
			return marshalledBody;

		} else {

			// TODO: if html is accepted, we might want to add a nice page here.

			StringBuilder sb = new StringBuilder();

			String url = httpServletRequest != null ? httpServletRequest.getRequestURL().toString() : "(non HTTP servlet request)";
			sb.append(statusCode);
			sb.append(": Error while invoking service " + url);
			sb.append("\nTBID: ");
			sb.append(context.getTracebackId());
			sb.append('\n');

			if (exposeMessage) {
				sb.append('\n');
				sb.append(getLastMessage(context.getThrowable()));
			}
			if (exposeException) {
				sb.append('\n');
				sb.append(ServletTools.stringify(request));
				sb.append('\n');
				String stack = Exceptions.stringify(context.getThrowable());
				sb.append(stack);
			}

			byte[] bodyBytes;
			try {
				bodyBytes = sb.toString().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.info("Unexpected exception when trying to convert the String " + sb.toString() + " to a byte array.", e);
				bodyBytes = new byte[0];
			}
			Body textBody = new Body(bodyBytes, "text/plain; charset=UTF-8");
			return textBody;

		}

	}

	private int getStatusCode(Throwable e) {
		if (e instanceof HttpException) {
			return ((HttpException) e).getStatusCode();
		}
		Throwable rootCause = Exceptions.getRootCause(e);
		Integer statusCode = findStatusCode(rootCause);
		if (statusCode != null) {
			return statusCode.intValue();
		}
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}

	private LogPreferences getLogPreferences(Throwable e) {
		if (e instanceof HasLogPreferences) {
			return ((HasLogPreferences) e).getLogPreferences();
		}
		Throwable rootCause = Exceptions.getRootCause(e);
		LogPreferences logPreferences = findLogPreferences(rootCause);
		if (logPreferences != null) {
			return logPreferences;
		}
		return null;
	}

	private Integer findStatusCode(Throwable rootCause) {
		if (statusCodeMap != null && rootCause != null) {
			//@formatter:off
			return statusCodeMap.entrySet()
				.stream()
				.filter(e -> e.getKey().isAssignableFrom(rootCause.getClass()))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
			//@formatter:on
		}
		return null;
	}

	private LogPreferences findLogPreferences(Throwable rootCause) {
		if (logPreferencesMap != null && rootCause != null) {
			//@formatter:off
			return logPreferencesMap.entrySet()
				.stream()
				.filter(e -> e.getKey().isAssignableFrom(rootCause.getClass()))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
			//@formatter:on
		}
		return null;
	}

	private static String getLastMessage(Throwable throwable) {
		if (throwable == null)
			return null;

		Set<Throwable> visitedThrowables = new HashSet<>();
		String message = throwable.getMessage();
		while ((throwable = throwable.getCause()) != null) {
			if (visitedThrowables.contains(throwable)) {
				break;
			}
			visitedThrowables.add(throwable);
			if (throwable.getMessage() != null) {
				message = throwable.getMessage();
			}
		}
		return message;
	}

	private static class Body {

		private final byte[] bodyContent;
		private final String mimeType;

		public Body(byte[] bodyContent, String mimeType) {
			super();
			this.bodyContent = bodyContent;
			this.mimeType = mimeType;
		}
		public byte[] getBody() {
			return bodyContent;
		}
		public String getMimeType() {
			return mimeType;
		}
	}

	private boolean exposeMessage() {
		switch (exceptionExposure) {
			case full:
				return true;
			case messageOnly:
				return true;
			case none:
				return false;
			default:
				break;
		}
		// default
		return false;
	}
	private boolean exposeException(int statusCode) {

		// This is a conscious decision to omit the stacktrace in case of security related status codes
		switch (statusCode) {
			case HttpServletResponse.SC_UNAUTHORIZED:
			case HttpServletResponse.SC_FORBIDDEN: {
				if (exceptionExposure != ExceptionExposure.full) {
					return false;
				}
				break;
			}
			default:
				break;
		}

		switch (exceptionExposure) {
			case full:
				return true;
			case messageOnly:
				return false;
			case none:
				return false;
			default:
				break;
		}
		// default
		return false;
	}

	private void writeException(ExceptionHandlingContext context, Body body, int statusCode, HttpServletResponse httpResponse) {
		try {
			httpResponse.setContentType(body.getMimeType());
			httpResponse.setStatus(statusCode);

			ServletOutputStream outputStream = httpResponse.getOutputStream();
			outputStream.write(body.getBody());
			outputStream.flush();

		} catch (Exception e) {
			logger.error("Unable to write exception " + context.getThrowable().getClass().getName() + " to the response due to "
					+ e.getClass().getName() + " (" + e.getMessage() + ").", e);
		}
	}

	@Configurable
	public void setExceptionExposure(ExceptionExposure exceptionExposure) {
		this.exceptionExposure = NullSafe.nonNull(exceptionExposure, "exceptionExposure");
	}
	
	@Configurable
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}
	@Configurable
	public void setStatusCodeMap(Map<Class<? extends Throwable>, Integer> statusCodeMap) {
		this.statusCodeMap = statusCodeMap;
	}
	@Configurable
	public void setLogPreferencesMap(Map<Class<? extends Throwable>, LogPreferences> logPreferencesMap) {
		this.logPreferencesMap = logPreferencesMap;
	}
	@Configurable
	public void setRemoteAddressResolver(RemoteClientAddressResolver remoteAddressResolver) {
		this.remoteAddressResolver = remoteAddressResolver;
	}

	@Override
	public void postConstruct() {
		if (statusCodeMap == null) {
			statusCodeMap = new LinkedHashMap<>();
			statusCodeMap.put(AuthorizationException.class, HttpServletResponse.SC_FORBIDDEN);
		}
	}

	@Configurable
	public void setTracebackIdExposure(boolean expose) {
		this.tracebackIdExposure = expose;
	}

}
