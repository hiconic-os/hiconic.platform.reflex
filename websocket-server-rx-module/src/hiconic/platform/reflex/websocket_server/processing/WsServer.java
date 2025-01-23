// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.platform.reflex.websocket_server.processing;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;
import com.braintribe.cfg.Required;
import com.braintribe.codec.CodecException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.MarshallerRegistryEntry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.InvalidSession;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.ScalarType;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.ValidateUserSession;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.InternalPushRequest;
import com.braintribe.model.service.api.PushRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.PushResponse;
import com.braintribe.model.service.api.result.PushResponseMessage;
import com.braintribe.model.usersession.UserSession;

import hiconic.platform.reflex.websocket_server.processing.WsRegistry.WsRegistrationEntry;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

/**
 * 
 * This class establishes a WebSocket based push channel support <br/>
 * for registered web-clients. </br>
 * </br>
 * It establishes a WebSocket endpoint by extending the {@link Endpoint} provided by javax.websocket API.<br/>
 * On the registered URL (usually <code>ws://host:port/tribefire-services/websocket</code>) clients can register them self by passing a clientId and a
 * valid tribefire sessionId in order to setup a WebSocket push channel. </br>
 * </br>
 * 
 * @author gunther.schenk
 *
 */
public class WsServer extends Endpoint
		implements ServiceProcessor<PushRequest, PushResponse>, MessageHandler.Whole<String>, InitializationAware {

	private static final Logger logger = Logger.getLogger(WsServer.class);

	// ############################## Configurable members ##############################

	private MarshallerRegistry marshallerRegistry;
	private Evaluator<ServiceRequest> evaluator;
	private InstanceId processingInstanceId;

	private String defaultMarshallerMimeType = "application/json";
	private String targetApplicationId = "reflex";

	// ############################## Internally used members ##############################

	private WsRegistry sessionRegistry = new WsRegistry();
	private InstanceId targetInstanceId;

	private PushChannelLifecycleHub channelLifecyclePublisher;

	// ############################## Setters ##############################

	@Required
	public void setPushChannelLifecycleHub(PushChannelLifecycleHub channelLifecyclePublisher) {
		this.channelLifecyclePublisher = channelLifecyclePublisher;
	}
	
	@Configurable
	@Required
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}

	@Configurable
	@Required
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}

	@Configurable
	@Required
	public void setProcessingInstanceId(InstanceId processingInstanceId) {
		this.processingInstanceId = processingInstanceId;
	}

	@Configurable
	public void setDefaultMarshallerMimeType(String defaultMarshallerMimeType) {
		this.defaultMarshallerMimeType = defaultMarshallerMimeType;
	}

	@Configurable
	public void setTargetApplicationId(String targetApplicationId) {
		this.targetApplicationId = targetApplicationId;
	}

	@Configurable
	public void setSessionRegistry(WsRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	// ############################## Initializing ##############################

	@Override
	public void postConstruct() {
		targetInstanceId = InstanceId.T.create();
		targetInstanceId.setApplicationId(targetApplicationId);
	}

	// ############################## WebSocket Endpoint ##############################

	/**
	 * Called by clients to open a WebSocket session. The passed sessionId will be verified and if its a valid tribefire session the WebSocket session
	 * will be registered along with the clientId. <br/>
	 * If the passed client info (sessionId, clientId, accept) can't be successfully verified the established WebSocket session will be closed
	 * providing a {@value CloseCodes#CANNOT_ACCEPT} status code.
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		WsClientInfo info = decodeClientInfo(session.getRequestParameterMap());
		try {
			String channelId = session.getId();
			verifyClientInfo(info);
			info.setPushChannelId(channelId);
			
			 // Send the connection ID to the client
	        try {
	        	if (info.getSendChannelId())
	        		session.getBasicRemote().sendText(channelId);
	        	
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		} catch (WsVerificationException ve) {
			close(session, new CloseReason(CloseCodes.CANNOT_ACCEPT, ve.getMessage()));
			logger.trace(() -> "An attempt to establish a websocket connection was denied because of invalid client information: " + ve.getMessage(),
					ve);
			return;

		} catch (Exception e) {
			String sessionString = stringifySession(session);
			close(session, new CloseReason(CloseCodes.CANNOT_ACCEPT, e.getMessage()));
			logger.debug(() -> "An attempt to establish a websocket connection was denied because of an unexpected error: " + e.getMessage() + " ("
					+ sessionString + ")", e);
			return;
		}

		WsRegistrationEntry entry = sessionRegistry.register(info, session);
		logger.trace(() -> "Opened and registered websocket session for client: " + info.getClientId() + " with session: " + info.getSessionId());

		session.addMessageHandler(this);
		
		channelLifecyclePublisher.notifyConnectionEstablished(entry);
	}

	/**
	 * Called by clients once the WebSocket session will be closed. </br>
	 * Internally the session will be closed and the according entry will be removed from the registry.
	 */
	@Override
	public void onClose(Session session, CloseReason closeReason) {
		WsRegistrationEntry entry = sessionRegistry.remove(session);
		close(session, closeReason);
		channelLifecyclePublisher.notifyConnectionClosed(entry);

		if (entry == null) {
			logger.trace(() -> "An unregistered websocket session was closed.");
			return;
		}
		WsClientInfo info = entry.getClientInfo();
		Session entrySession = entry.getSession();
		if (entrySession.isOpen()) {
			// Shouldn't be needed since we already closed the given session - but who knows what happens out there
			// :) - so just in case!!!
			close(entrySession, closeReason);
		}
		logger.trace(() -> "Closed websocket session for client: " + info.getClientId() + " with session: " + info.getSessionId());
	}

	/**
	 * Called by the client in case an error occurred. </br>
	 * Internally the error along with associated ClientInfo will be logged and the session will be closed using
	 * {@link #onClose(Session, CloseReason)}
	 */
	@Override
	public void onError(Session session, Throwable thr) {
		WsRegistrationEntry entry = sessionRegistry.findEntry(session);
		if (entry == null) {
			logger.warn("An error was reported for a notregistered websocket session. Ignored.");
			return;
		}
		WsClientInfo info = entry.getClientInfo();
		// EOFException occurs when client harshly closes the connection
		if (thr instanceof EOFException || thr instanceof IOException) {
			logger.trace(() -> "A 'java.io.EOFException' or 'java.io.IOException' occurred on websocket session for client: " + info.getClientId()
					+ " with session: " + info.getSessionId() + ". Hint: probbably caused by harsh close of connection by websocket client.");
			logger.trace(() -> "Exception details:", thr);
		} else {
			logger.warn(() -> "An error occurred on websocket session for client: " + info.getClientId() + " with session: " + info.getSessionId(),
					thr);
		}
		onClose(session, null);
	}

	/**
	 * Potentially called by clients to send messages on an established WebSocket channel. </br>
	 * Currently there's no support for that route planed, just a warning message will be logged in that case.
	 */
	@Override
	public void onMessage(String message) {
		logger.warn("Unsupported message received via websocket channel: " + message);
	}

	// ############################## DDSA ##############################

	/**
	 * Process the passed {@link InternalPushRequest} by identifying the matching WebSocket sessions and pushing the serialized payload to them using
	 * the open WebSocket channel.
	 */
	@Override
	public PushResponse process(ServiceRequestContext requestContext, PushRequest request) {
		Set<WsRegistrationEntry> entries = sessionRegistry.findEntries(new RequestPredicate(request), request.getPushChannelId());

		if (entries.isEmpty()) {
			logger.trace(() -> "No registered websocket sessions found matching push request patterns: " + request);
			return PushResponse.T.create();
		}

		ServiceRequest payload = request.getServiceRequest();

		logger.debug(() -> "Found " + entries.size() + " registered websocket sessions that should be notified with payload: " + payload);

		PushResponse response = PushResponse.T.create();
		Map<String, String> marshalledPayloadCache = new HashMap<>();
		for (WsRegistrationEntry entry : entries) {
			WsClientInfo clientInfo = entry.getClientInfo();
			Session session = entry.getSession();

			// Get the accept based encoded payload either from cache or encoded it and put it into cache.
			String marshalledPayload = marshalledPayloadCache.computeIfAbsent(clientInfo.getAccept(), (accept) -> encode(payload, accept));

			PushResponseMessage responseMessage = sendPayload(marshalledPayload, clientInfo, session);
			response.getResponseMessages().add(responseMessage);
		}

		return response;
	}
	
	// ############################## Utilities ##############################

	public static String stringifySession(Session session) {
		if (session == null) {
			return "<null>";
		} else {
			Map<String, String> map = new LinkedHashMap<>();

			mapPutNullSafe(map, "RequestURI", session.getRequestURI());
			mapPutNullSafe(map, "QueryString", session.getQueryString());
			mapPutNullSafe(map, "PathParameters", session.getPathParameters());
			mapPutNullSafe(map, "ProtocolVersion", session.getProtocolVersion());
			mapPutNullSafe(map, "RequestParameterMap", session.getRequestParameterMap());
			mapPutNullSafe(map, "UserPrincipal", session.getUserPrincipal());
			mapPutNullSafe(map, "UserProperties", session.getUserProperties());
			return map.toString();
		}
	}

	private static void mapPutNullSafe(Map<String, String> map, String key, Object value) {
		if (value != null) {
			map.put(key, value.toString());
		}
	}

	/**
	 * An internal helper to close a WebSocket session witch catches a potential error and logs the stacktrace.
	 */
	private void close(Session session, CloseReason closeReason) {
		try {
			if (closeReason != null) {
				session.close(closeReason);
			} else {
				session.close();
			}
		} catch (IOException e) {
			logger.error("Error closing session", e);

		}
	}

	/**
	 * Serializes the passed payload (ServiceRequest) instance into the passed accept format.
	 */
	private String encode(GenericEntity payload, String accept) {
		MarshallerRegistryEntry requireMarshallerRegistryEntry = requireMarshallerRegistryEntry(accept);
		Marshaller marshaller = requireMarshallerRegistryEntry.getMarshaller();

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			marshaller.marshall(os, payload);
			String marshalledPayload = os.toString("UTF-8");
			return marshalledPayload;
		} catch (Exception e) {
			throw new CodecException("Could not encode given payload: " + payload + "to: " + accept, e);
		}
	}

	/**
	 * Sends the encoded (marshalled) payload to the passed WebSocket session.
	 * 
	 * @returns a {@link PushResponseMessage} with success or failure state.
	 */
	private PushResponseMessage sendPayload(String marshalledPayload, WsClientInfo clientInfo, Session session) {
		try {
			session.getBasicRemote().sendText(marshalledPayload);
			logger.debug(() -> "Pushed message to client with identification: " + clientInfo.getClientId());
			return createResponseMessage(clientInfo, "Pushed message to client", true);
		} catch (IOException e) {
			logger.error("Unable to push message to client with identification: " + clientInfo.getClientId(), e);
			return createResponseMessage(clientInfo, "Unable to push message to client", false);
		}

	}

	/**
	 * Creates {@link PushResponseMessage} based on passed information.
	 */
	private PushResponseMessage createResponseMessage(WsClientInfo clientInfo, String msg, boolean success) {
		PushResponseMessage responseMessage = PushResponseMessage.T.create();
		responseMessage.setMessage(msg);
		responseMessage.setSuccessful(success);
		responseMessage.setClientIdentification(clientInfo.getClientId());
		responseMessage.setOriginId(processingInstanceId);
		return responseMessage;
	}

	/**
	 * Creates a {@link WsClientInfo} instance based on the provided parameters map.
	 */
	private WsClientInfo decodeClientInfo(Map<String, List<String>> parameters) {
		EntityType<WsClientOpenInfo> infoType = WsClientOpenInfo.T;
		WsClientInfo info = WsClientInfo.T.create();

		for (Map.Entry<String, List<String>> parameter : parameters.entrySet()) {

			Property property = infoType.findProperty(parameter.getKey());
			if (property != null && property.getType().isScalar()) {
				ScalarType scalarType = (ScalarType) property.getType();
				List<String> values = parameter.getValue();
				if (!values.isEmpty()) {
					String valueAsStr = values.get(0);
					Object value = scalarType.instanceFromString(valueAsStr);
					property.set(info, value);
				}
			} else {
				logger.warn("Ignore unsupported parameter: " + parameter.getKey() + " sent by client.");
			}
		}

		return info;
	}

	/**
	 * Returns the {@link MarshallerRegistryEntry} registered for the passed requestMimeType.
	 * 
	 * @throws WsException
	 *             in case no entry could be found for the passed requestMimeType.
	 */
	private MarshallerRegistryEntry requireMarshallerRegistryEntry(String requestMimeType) {
		MarshallerRegistryEntry marshallerRegistryEntry = marshallerRegistry.getMarshallerRegistryEntry(requestMimeType);
		if (marshallerRegistryEntry == null) {
			throw new WsException("no marshaller for mime type '" + requestMimeType + "' is configured in the registry");
		}
		return marshallerRegistryEntry;
	}

	/**
	 * Validates the passed client info and ensures that <br>
	 * 
	 * <li>a clientId is passed.</li>
	 * <li>a sessionId is passed.</li>
	 * <li>a valid tribefire UserSession is associated with that sessionId.</li>
	 * 
	 * @throws WsException
	 *             in case either sessionId or clientId is not passed or an unsupported accept mimeType is requested.
	 */
	private void verifyClientInfo(WsClientInfo info) {
		String sessionId = info.getSessionId();
		if (sessionId == null || sessionId.isEmpty()) {
			if (requiresAuth(info))
				throw new WsVerificationException("A valid sessionId is required to open a websocket connection.");
		}
		if (info.getClientId() == null || info.getClientId().isEmpty()) {
			throw new WsVerificationException("A clientId is required to open a websocket connection.");
		}

		if (sessionId != null) {
			ValidateUserSession validateUserSession = ValidateUserSession.T.create();
			validateUserSession.setSessionId(info.getSessionId());

			Maybe<? extends UserSession> userSessionMaybe = validateUserSession.eval(evaluator).getReasoned();
			
			if (userSessionMaybe.isUnsatisfiedBy(InvalidSession.T)) {
				InvalidSession invalidSession = userSessionMaybe.whyUnsatisfied();
				throw new WsVerificationException("The provided session ID " + info.getSessionId() + " was invalid: " + invalidSession.stringify());
			}
			
			info.setUserSession(userSessionMaybe.get());

		}
		
		if (info.getAccept() == null || info.getAccept().isEmpty()) {
			info.setAccept(defaultMarshallerMimeType);
		} else {
			if (this.marshallerRegistry.getMarshaller(info.getAccept()) == null) {
				throw new WsException(
						"No supported marshaller found for requested accept: " + info.getAccept() + " of client: " + info.getClientId());
			}
		}
	}

	private boolean requiresAuth(WsClientInfo info) {
		// TOOD: implement ServiceDomain defined auth requirement
		return false;
	}

	/**
	 * A {@link Predicate} implementation that compares a {@link WsRegistrationEntry} with patterns passed via a {@link PushRequest}
	 */
	private class RequestPredicate implements Predicate<WsRegistrationEntry> {
		private final PushRequest request;

		public RequestPredicate(PushRequest request) {
			this.request = request;
		}
		
		@Override
		public boolean test(WsRegistrationEntry entry) {
			WsClientInfo clientInfo = entry.getClientInfo();

			// @formatter:off
			return 
				patternMatches(clientInfo.getClientId(), request.getClientIdPattern()) && 
				patternMatches(clientInfo.getSessionId(), request.getSessionIdPattern()) &&
				pushChannelMatches(clientInfo.getPushChannelId(), request.getPushChannelId()) &&
				rolesMatches(entry, request.getRolePattern());
			// @formatter:on
		}
	}

	/**
	 * Returns true if the passed string matches the passed pattern. This method returns true if no pattern is passed and false if no compares string
	 * is passed.
	 */
	private boolean patternMatches(String compare, String pattern) {
		if (pattern == null) {
			return true;
		}
		if (compare == null) {
			return false;
		}
		return compare.matches(pattern);
	}

	private boolean pushChannelMatches(String entryId, String requestId) {
		return requestId == null || requestId.equals(entryId);
	}

	/**
	 * Returns true if any of the effective roles from the userSession of the passed entry matches the passed pattern.<br/>
	 * This method return true if no pattern is passed and false if no entry is passed.
	 */
	private boolean rolesMatches(WsRegistrationEntry entry, String pattern) {
		if (entry == null) {
			return false;
		}
		if (pattern == null) {
			return true;
		}
		UserSession userSession = entry.getClientInfo().getUserSession();
		Set<String> roles = userSession.getEffectiveRoles();
		for (String role : roles) {
			if (patternMatches(role, pattern)) {
				return true;
			}
		}
		return false;
	}

}
