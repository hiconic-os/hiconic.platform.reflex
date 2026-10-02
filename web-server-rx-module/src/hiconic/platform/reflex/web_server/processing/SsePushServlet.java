// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
// ============================================================================
package hiconic.platform.reflex.web_server.processing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import com.braintribe.cfg.Required;
import com.braintribe.codec.CodecException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.codec.marshaller.api.MarshallerRegistryEntry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.securityservice.ValidateUserSession;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.InternalPushRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.PushResponse;
import com.braintribe.model.service.api.result.PushResponseMessage;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.push.api.PushChannel;
import hiconic.rx.push.api.PushContract;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** An SSE transport delegate for the platform push service. */
public class SsePushServlet extends HttpServlet implements ServiceProcessor<InternalPushRequest, PushResponse> {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(SsePushServlet.class);

	private final Map<String, SseChannel> channels = new ConcurrentHashMap<>();
	private final Object eventLock = new Object();
	private final Deque<StoredPush> replayHistory = new ArrayDeque<>();
	private MarshallerRegistry marshallerRegistry;
	private Evaluator<ServiceRequest> evaluator;
	private InstanceId processingInstanceId;
	private PushContract push;
	private long heartbeatIntervalMillis = 30_000L;
	private int replayCapacity = 256;
	private ScheduledExecutorService heartbeatExecutor;

	@Required
	public void setMarshallerRegistry(MarshallerRegistry marshallerRegistry) {
		this.marshallerRegistry = marshallerRegistry;
	}

	@Required
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}

	@Required
	public void setProcessingInstanceId(InstanceId processingInstanceId) {
		this.processingInstanceId = processingInstanceId;
	}

	@Required
	public void setPushContract(PushContract push) {
		this.push = push;
	}

	public void setHeartbeatIntervalMillis(long heartbeatIntervalMillis) {
		if (heartbeatIntervalMillis < 0)
			throw new IllegalArgumentException("heartbeatIntervalMillis must not be negative");
		this.heartbeatIntervalMillis = heartbeatIntervalMillis;
	}

	public void setReplayCapacity(int replayCapacity) {
		if (replayCapacity < 0)
			throw new IllegalArgumentException("replayCapacity must not be negative");
		this.replayCapacity = replayCapacity;
	}

	@Override
	public void init() throws ServletException {
		super.init();
		if (heartbeatIntervalMillis == 0)
			return;
		heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
			Thread thread = new Thread(task, "rx-sse-heartbeat");
			thread.setDaemon(true);
			return thread;
		});
		heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, heartbeatIntervalMillis, heartbeatIntervalMillis,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void destroy() {
		ScheduledExecutorService executor = heartbeatExecutor;
		if (executor != null)
			executor.shutdownNow();
		for (SseChannel channel : List.copyOf(channels.values()))
			channel.close();
		super.destroy();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String clientId = request.getParameter("clientId");
		if (clientId != null && clientId.isBlank())
			clientId = null;

		String accept = request.getParameter("accept");
		if (accept == null || accept.isBlank())
			accept = "application/json";
		if (marshallerRegistry.getMarshaller(accept) == null) {
			log.debug("Rejected SSE connection because the requested payload format is unsupported.");
			response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Unsupported payload format: " + accept);
			return;
		}

		String requestedSessionId = request.getParameter("sessionId");
		boolean explicitSession = requestedSessionId != null && !requestedSessionId.isBlank();
		UserSession userSession = !explicitSession
				? AttributeContexts.peek().findOrNull(UserSessionAspect.class)
				: validateSession(requestedSessionId, response);
		if (explicitSession && userSession == null)
			return;
		String sessionId = userSession == null ? null : userSession.getSessionId();
		String sessionSource = explicitSession ? "explicit" : userSession == null ? "none" : "web-auth-context";

		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache, no-transform");
		response.setHeader("Connection", "keep-alive");
		response.setHeader("X-Accel-Buffering", "no");

		AsyncContext async = request.startAsync();
		async.setTimeout(0L);
		SseChannel channel = new SseChannel(UUID.randomUUID().toString(), clientId, sessionId, accept, userSession, async);
		async.addListener(channel);
		String lastEventId = request.getHeader("Last-Event-ID");
		int replayedEvents;
		synchronized (eventLock) {
			channels.put(channel.getChannelId(), channel);
			push.registerChannel(channel);
			channel.send("channel", null, channel.getChannelId());
			replayedEvents = replay(channel, lastEventId);
		}
		boolean identifiedClient = clientId != null;
		String payloadFormat = accept;
		log.debug(() -> "Opened SSE channel (identifiedClient=" + identifiedClient + ", sessionSource=" + sessionSource
				+ ", payloadFormat=" + payloadFormat + ", replayedEvents=" + replayedEvents + ", activeChannels=" + channels.size() + ")");
	}

	private UserSession validateSession(String sessionId, HttpServletResponse response) throws IOException {
		if (sessionId == null || sessionId.isBlank())
			return null;

		ValidateUserSession validate = ValidateUserSession.T.create();
		validate.setSessionId(sessionId);
		Maybe<? extends UserSession> maybe = validate.eval(evaluator).getReasoned();
		if (maybe.isSatisfied())
			return maybe.get();

		log.debug("Rejected SSE connection because the explicitly supplied session is invalid.");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The provided session ID is invalid.");
		return null;
	}

	@Override
	public PushResponse process(ServiceRequestContext context, InternalPushRequest request) {
		StoredPush storedPush;
		Set<SseChannel> recipients;
		synchronized (eventLock) {
			storedPush = remember(request);
			Predicate<SseChannel> predicate = channel -> matches(channel, request);
			if (request.getPushChannelId() == null)
				recipients = Set.copyOf(channels.values().stream().filter(predicate).toList());
			else {
				SseChannel channel = channels.get(request.getPushChannelId());
				recipients = channel != null && predicate.test(channel) ? Set.of(channel) : Set.of();
			}
		}

		PushResponse response = PushResponse.T.create();
		Map<String, String> payloads = new HashMap<>();
		for (SseChannel channel : recipients) {
			try {
				String payload = payloads.computeIfAbsent(channel.accept, mimeType -> encode(request.getServiceRequest(), mimeType));
				channel.sendPush(storedPush, payload);
				response.getResponseMessages().add(responseMessage(channel, "Pushed message to client", true));
			} catch (Exception e) {
				log.debug(() -> "Unable to push SSE message to client " + channel.clientId, e);
				channel.close();
				response.getResponseMessages().add(responseMessage(channel, "Unable to push message to client", false));
			}
		}
		return response;
	}

	private StoredPush remember(InternalPushRequest request) {
		StoredPush storedPush = new StoredPush(UUID.randomUUID().toString(), request);
		if (replayCapacity == 0)
			return storedPush;
		replayHistory.addLast(storedPush);
		while (replayHistory.size() > replayCapacity)
			replayHistory.removeFirst();
		return storedPush;
	}

	private int replay(SseChannel channel, String lastEventId) throws IOException {
		if (lastEventId == null || lastEventId.isBlank() || replayHistory.isEmpty())
			return 0;

		List<StoredPush> candidates = new ArrayList<>(replayHistory);
		int matchingIndex = -1;
		for (int i = 0; i < candidates.size(); i++)
			if (candidates.get(i).eventId().equals(lastEventId)) {
				matchingIndex = i;
				break;
			}

		int replayed = 0;
		for (int i = matchingIndex + 1; i < candidates.size(); i++) {
			StoredPush storedPush = candidates.get(i);
			if (!matches(channel, storedPush.request()))
				continue;
			channel.sendPush(storedPush, encode(storedPush.request().getServiceRequest(), channel.accept));
			replayed++;
		}
		return replayed;
	}

	private void sendHeartbeats() {
		String data = "{\"serverTimeUtc\":\"" + Instant.now() + "\"}";
		for (SseChannel channel : List.copyOf(channels.values()))
			try {
				channel.send("ping", null, data);
			} catch (Exception e) {
				log.debug(() -> "Unable to send SSE heartbeat to client " + channel.clientId, e);
				channel.close();
			}
	}

	private boolean matches(SseChannel channel, InternalPushRequest request) {
		return patternMatches(channel.clientId, request.getClientIdPattern())
				&& patternMatches(channel.sessionId, request.getSessionIdPattern())
				&& rolesMatch(channel.userSession, request.getRolePattern());
	}

	private boolean patternMatches(String value, String pattern) {
		return pattern == null || value != null && value.matches(pattern);
	}

	private boolean rolesMatch(UserSession session, String pattern) {
		if (pattern == null)
			return true;
		if (session == null || session.getEffectiveRoles() == null)
			return false;
		return session.getEffectiveRoles().stream().anyMatch(role -> role.matches(pattern));
	}

	private String encode(GenericEntity payload, String mimeType) {
		MarshallerRegistryEntry entry = marshallerRegistry.getMarshallerRegistryEntry(mimeType);
		Marshaller marshaller = entry.getMarshaller();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			marshaller.marshall(out, payload);
			return out.toString(StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new CodecException("Could not encode push payload as " + mimeType, e);
		}
	}

	private PushResponseMessage responseMessage(SseChannel channel, String text, boolean successful) {
		PushResponseMessage message = PushResponseMessage.T.create();
		message.setMessage(text);
		message.setSuccessful(successful);
		message.setClientIdentification(channel.clientId);
		message.setOriginId(processingInstanceId);
		return message;
	}

	private class SseChannel implements PushChannel, AsyncListener {
		private final String channelId;
		private final String clientId;
		private final String sessionId;
		private final String accept;
		private final UserSession userSession;
		private final AsyncContext async;
		private final AtomicBoolean closed = new AtomicBoolean();

		private SseChannel(String channelId, String clientId, String sessionId, String accept, UserSession userSession, AsyncContext async) {
			this.channelId = channelId;
			this.clientId = clientId;
			this.sessionId = sessionId;
			this.accept = accept;
			this.userSession = userSession;
			this.async = async;
		}

		private synchronized void sendPush(StoredPush storedPush, String data) throws IOException {
			send("PushRequest", storedPush.eventId(), data);
		}

		private synchronized void send(String event, String id, String data) throws IOException {
			if (closed.get())
				throw new IOException("SSE channel is closed");
			PrintWriter writer = async.getResponse().getWriter();
			if (event != null)
				writer.append("event: ").append(event).append('\n');
			if (id != null)
				writer.append("id: ").append(id).append('\n');
			for (String line : data.split("\\R", -1))
				writer.append("data: ").append(line).append('\n');
			writer.append('\n');
			writer.flush();
			if (writer.checkError())
				throw new IOException("Writing to SSE channel failed");
		}

		private void close() {
			if (!closed.compareAndSet(false, true))
				return;
			channels.remove(channelId, this);
			push.unregisterChannel(this);
			log.debug(() -> "Closed SSE channel (activeChannels=" + channels.size() + ")");
			try {
				async.complete();
			} catch (IllegalStateException ignored) {
				// already completed by the container
			}
		}

		@Override public String getChannelId() { return channelId; }
		@Override public String getClientId() { return clientId; }
		@Override public String getSessionId() { return sessionId; }
		@Override public void onComplete(AsyncEvent event) { close(); }
		@Override public void onTimeout(AsyncEvent event) { close(); }
		@Override public void onError(AsyncEvent event) { close(); }
		@Override public void onStartAsync(AsyncEvent event) { /* no redispatch */ }
	}

	private record StoredPush(String eventId, InternalPushRequest request) {
	}
}
