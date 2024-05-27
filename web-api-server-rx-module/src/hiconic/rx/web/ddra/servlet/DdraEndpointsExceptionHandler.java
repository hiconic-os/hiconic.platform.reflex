// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package hiconic.rx.web.ddra.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.exception.AuthorizationException;
import com.braintribe.exception.HttpException;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.securityservice.api.exceptions.SecurityServiceException;
import com.braintribe.model.processing.service.common.FailureConverter;
import com.braintribe.model.service.api.result.Failure;

import dev.hiconic.servlet.ddra.endpoints.api.DdraEndpointContext;
import jakarta.servlet.http.HttpServletResponse;

public class DdraEndpointsExceptionHandler {

	private static final Logger logger = Logger.getLogger(DdraEndpointsExceptionHandler.class);

	private Marshaller defaultMarshaller;

	private String defaultMimeType;

	private boolean includeDebugInformation;

	@Required
	@Configurable
	public void setDefaultMarshaller(Marshaller defaultMarshaller) {
		this.defaultMarshaller = defaultMarshaller;
	}

	@Required
	@Configurable
	public void setDefaultMimeType(String defaultMimeType) {
		this.defaultMimeType = defaultMimeType;
	}

	@Required
	@Configurable
	public void setIncludeDebugInformation(boolean includeDebugInformation) {
		this.includeDebugInformation = includeDebugInformation;
	}

	public void handleException(DdraEndpointContext<?> context, Exception e) {
		HttpServletResponse response = context.getResponse();
		try {
			if (response.isCommitted()) {
				response.setStatus(getCode(e));
				logger.debug("Response has been committed already. Dumping exception in log.");
				logger.error(e);
				return;
			}

			Failure failure = FailureConverter.INSTANCE.apply(e);
			if (!includeDebugInformation) {
				failure.setDetails(null);
				failure.setSuppressed(new ArrayList<>());
				failure.setCause(null);
			}
			response.setStatus(getCode(e));
			response.setHeader("Content-Type", context.getMarshaller() != null ? context.getMimeType() : defaultMimeType);

			try (OutputStream out = response.getOutputStream()) {
				Marshaller marshaller = context.getMarshaller() != null ? context.getMarshaller() : defaultMarshaller;
				// TODO take from request
				marshaller.marshall(out, failure, GmSerializationOptions.deriveDefaults().outputPrettiness(OutputPrettiness.high).build());
			}
		} catch (Exception e2) {
			logger.error("Error while handling REST Exception...", e2);
			try {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error.");
			} catch (IOException e1) {
				logger.debug("Error while sending error back...", e2);
			}
		}
	}

	private int getCode(Exception e) {
		if (e instanceof HttpException) {
			return ((HttpException) e).getStatusCode();
		}
		// TODO set the header, set 403 for other stuff, etc...
		if (e instanceof AuthorizationException || e instanceof SecurityServiceException) {
			// TODO discuss authentication, 401 is not OK if not providing WWW-Authenticate header
			return HttpServletResponse.SC_UNAUTHORIZED;
		}
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}
}
