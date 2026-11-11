package hiconic.rx.log.reflection.processing;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.level.LogLevelManager;
import com.braintribe.logging.level.LogLevelNames;
import com.braintribe.logging.level.LogLevelRuntimeUpdater;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.UnicastRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.Neutral;
import com.braintribe.model.service.api.result.ServiceResult;

import hiconic.rx.log.reflection.model.LogLevelProfile;
import hiconic.rx.log.reflection.model.LogLevelState;
import hiconic.rx.log.reflection.model.LogReflectionCapabilities;
import hiconic.rx.log.reflection.model.LogTarget;
import hiconic.rx.log.reflection.model.LogOrigin;
import hiconic.rx.log.reflection.model.api.ApplyEffectiveLogLevels;
import hiconic.rx.log.reflection.model.api.GetLogTopology;
import hiconic.rx.log.reflection.model.api.GetLogReflectionCapabilities;
import hiconic.rx.log.reflection.model.api.GetLocalLogLevelState;
import hiconic.rx.log.reflection.model.api.GetLogLevelState;
import hiconic.rx.log.reflection.model.api.CreateLogBundle;
import hiconic.rx.log.reflection.model.api.CreateLocalLogBundle;
import hiconic.rx.log.reflection.model.api.ListLogStreams;
import hiconic.rx.log.reflection.model.api.ListLocalLogStreams;
import hiconic.rx.log.reflection.model.api.LogRecordPage;
import hiconic.rx.log.reflection.model.api.LogReflectionRequest;
import hiconic.rx.log.reflection.model.api.LogStreams;
import hiconic.rx.log.reflection.model.api.LogTopology;
import hiconic.rx.log.reflection.model.api.QueryLocalLogRecords;
import hiconic.rx.log.reflection.model.api.QueryLogRecords;
import hiconic.rx.log.reflection.model.api.UpdateLocalLogLevelProfileCascade;
import hiconic.rx.log.reflection.model.api.UpdateLocalRuntimeLogLevels;
import hiconic.rx.log.reflection.model.api.UpdateLogLevelProfileCascade;
import hiconic.rx.log.reflection.model.api.UpdateRuntimeLogLevels;
import hiconic.rx.module.api.auth.RoleAuthorization;
import hiconic.rx.topology.api.LiveInstances;

public class LogReflectionProcessor extends AbstractDispatchingServiceProcessor<LogReflectionRequest, Object> {
	private InstanceId instanceId;
	private StructuredLiveLogCollector collector;
	private LogbackLogStreamInventory inventory;
	private LogFileReader fileReader;
	private Evaluator<ServiceRequest> systemEvaluator;
	private LiveInstances liveInstances;
	private long clusterTimeoutMillis = 10_000;
	private long bundleClusterTimeoutMillis = 120_000;
	private LogBundleWriter bundleWriter;
	private LogLevelManager logLevelManager;
	private LogLevelRuntimeUpdater logLevelRuntimeUpdater;
	private RoleAuthorization roleAuthorization;
	private Set<String> readerRoles = Set.of();
	private Set<String> managerRoles = Set.of();

	@Configurable
	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}

	@Configurable
	@Required
	public void setCollector(StructuredLiveLogCollector collector) {
		this.collector = collector;
	}

	@Configurable
	@Required
	public void setInventory(LogbackLogStreamInventory inventory) {
		this.inventory = inventory;
	}

	@Configurable
	@Required
	public void setFileReader(LogFileReader fileReader) {
		this.fileReader = fileReader;
	}

	@Configurable
	@Required
	public void setSystemEvaluator(Evaluator<ServiceRequest> systemEvaluator) {
		this.systemEvaluator = systemEvaluator;
	}

	@Configurable
	@Required
	public void setLiveInstances(LiveInstances liveInstances) {
		this.liveInstances = liveInstances;
	}

	@Configurable
	public void setClusterTimeoutMillis(long clusterTimeoutMillis) {
		if (clusterTimeoutMillis < 1)
			throw new IllegalArgumentException("Cluster timeout must be positive");
		this.clusterTimeoutMillis = clusterTimeoutMillis;
	}

	@Configurable
	public void setBundleClusterTimeoutMillis(long bundleClusterTimeoutMillis) {
		if (bundleClusterTimeoutMillis < 1)
			throw new IllegalArgumentException("Bundle cluster timeout must be positive");
		this.bundleClusterTimeoutMillis = bundleClusterTimeoutMillis;
	}

	@Configurable
	@Required
	public void setBundleWriter(LogBundleWriter bundleWriter) {
		this.bundleWriter = bundleWriter;
	}

	@Configurable
	@Required
	public void setLogLevelManager(LogLevelManager logLevelManager) {
		this.logLevelManager = logLevelManager;
	}

	@Configurable
	@Required
	public void setLogLevelRuntimeUpdater(LogLevelRuntimeUpdater logLevelRuntimeUpdater) {
		this.logLevelRuntimeUpdater = logLevelRuntimeUpdater;
	}

	@Configurable
	@Required
	public void setRoleAuthorization(RoleAuthorization roleAuthorization) {
		this.roleAuthorization = roleAuthorization;
	}

	@Configurable
	@Required
	public void setReaderRoles(Set<String> readerRoles) {
		this.readerRoles = Set.copyOf(readerRoles);
	}

	@Configurable
	@Required
	public void setManagerRoles(Set<String> managerRoles) {
		this.managerRoles = Set.copyOf(managerRoles);
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<LogReflectionRequest, Object> dispatching) {
		dispatching.register(GetLogReflectionCapabilities.T, this::capabilities);
		dispatching.register(ListLogStreams.T, this::listStreams);
		dispatching.register(GetLogTopology.T, (context, request) -> topology());
		dispatching.register(QueryLogRecords.T, this::queryRecords);
		dispatching.register(CreateLogBundle.T, this::createBundle);
		dispatching.register(CreateLocalLogBundle.T, (context, request) -> bundleWriter.create(inventory, request, instanceId));
		dispatching.register(ListLocalLogStreams.T, (context, request) -> listLocalStreams());
		dispatching.register(QueryLocalLogRecords.T, (context, request) -> queryLocalRecords(request));
		dispatching.register(GetLogLevelState.T, this::logLevelState);
		dispatching.register(GetLocalLogLevelState.T, (context, request) -> localLogLevelState());
		dispatching.register(UpdateRuntimeLogLevels.T, this::updateRuntimeLogLevels);
		dispatching.register(UpdateLocalRuntimeLogLevels.T, (context, request) -> updateLocalRuntimeLogLevels(request));
		dispatching.register(UpdateLogLevelProfileCascade.T, this::updateLogLevelProfileCascade);
		dispatching.register(UpdateLocalLogLevelProfileCascade.T, (context, request) -> updateLocalLogLevelProfileCascade(request));
		dispatching.register(ApplyEffectiveLogLevels.T, (context, request) -> applyEffectiveLogLevels());
	}

	private LogReflectionCapabilities capabilities(ServiceRequestContext context, GetLogReflectionCapabilities request) {
		LogReflectionCapabilities result = LogReflectionCapabilities.T.create();
		boolean securityActive = roleAuthorization.securityActive();
		result.setAuthenticated(!securityActive || context.findOrNull(UserSessionAspect.class) != null);
		result.setCanRead(!securityActive || roleAuthorization.hasAnyRole(readerRoles));
		result.setCanManageLevels(!securityActive || roleAuthorization.hasAnyRole(managerRoles));
		return result;
	}

	private LogLevelState logLevelState(ServiceRequestContext context, GetLogLevelState request) {
		InstanceId target = resolveApplication(request.getApplicationId());
		if (sameApplication(target, instanceId))
			return localLogLevelState();
		return unicast(context, GetLocalLogLevelState.T.create(), target, LogLevelState.class, clusterTimeoutMillis);
	}

	private LogLevelState localLogLevelState() {
		LogLevelState state = LogLevelState.T.create();
		Map<String, String> effectiveLevels = new LinkedHashMap<>(logLevelManager.getEffectiveLogLevels());
		String rootLevel = logLevelManager.getRootLogLevel();
		effectiveLevels.putIfAbsent(LogLevelNames.ROOT, rootLevel == null ? "INFO" : rootLevel);
		state.setEffectiveLevels(effectiveLevels);
		state.setRuntimeLevels(logLevelManager.getRuntimeLogLevels());
		state.setPackagedLevels(logLevelManager.getPackagedLogLevels());
		state.setKnownLoggerNames(logLevelManager.getKnownLoggerNames());
		state.setPackagedProfiles(profiles(logLevelManager.getPackagedProfiles()));
		state.setRuntimeProfiles(profiles(logLevelManager.getRuntimeProfiles()));
		state.setPackagedProfileCascade(logLevelManager.getPackagedProfileOrder());
		state.setRuntimeProfileCascade(logLevelManager.getRuntimeProfileOrder());
		state.setEffectiveProfileCascade(logLevelManager.getEffectiveProfileOrder());
		state.setProfileSupport(true);
		return state;
	}

	private Neutral updateRuntimeLogLevels(ServiceRequestContext context, UpdateRuntimeLogLevels request) {
		InstanceId target = resolveApplication(request.getApplicationId());
		UpdateLocalRuntimeLogLevels local = UpdateLocalRuntimeLogLevels.T.create();
		local.setProfile(request.getProfile());
		local.setLevels(request.getLevels());
		local.setNamesToRemove(request.getNamesToRemove());
		local.setClearAll(request.getClearAll());
		if (sameApplication(target, instanceId))
			return updateLocalRuntimeLogLevels(local);
		return unicast(context, local, target, Neutral.class, clusterTimeoutMillis);
	}

	private Neutral updateLocalRuntimeLogLevels(UpdateLocalRuntimeLogLevels request) {
		String profile = request.getProfile();
		if (Boolean.TRUE.equals(request.getClearAll())) {
			Map<String, String> current = logLevelManager.getRuntimeProfiles().get(profile);
			Set<String> namesToRemove = current == null ? Set.of() : current.keySet();
			getOrTunnel(logLevelRuntimeUpdater.updateRuntimeProfile(profile, Map.of(), namesToRemove));
		} else {
			getOrTunnel(logLevelRuntimeUpdater.updateRuntimeProfile(profile, request.getLevels(), request.getNamesToRemove()));
		}
		return Neutral.NEUTRAL;
	}

	private Neutral updateLogLevelProfileCascade(ServiceRequestContext context, UpdateLogLevelProfileCascade request) {
		InstanceId target = resolveApplication(request.getApplicationId());
		UpdateLocalLogLevelProfileCascade local = UpdateLocalLogLevelProfileCascade.T.create();
		local.setProfiles(request.getProfiles());
		local.setReset(request.getReset());
		if (sameApplication(target, instanceId))
			return updateLocalLogLevelProfileCascade(local);
		return unicast(context, local, target, Neutral.class, clusterTimeoutMillis);
	}

	private Neutral updateLocalLogLevelProfileCascade(UpdateLocalLogLevelProfileCascade request) {
		if (Boolean.TRUE.equals(request.getReset())) {
			getOrTunnel(logLevelRuntimeUpdater.mutateProfileOrder(
					com.braintribe.gm.model.logging.level.api.LogLevelProfileOrderMutation.RESET, null, null));
			return Neutral.NEUTRAL;
		}

		getOrTunnel(logLevelRuntimeUpdater.replaceProfileOrder(request.getProfiles()));
		return Neutral.NEUTRAL;
	}

	private Neutral applyEffectiveLogLevels() {
		logLevelManager.applyEffectiveLogLevels();
		return Neutral.NEUTRAL;
	}

	private static List<LogLevelProfile> profiles(Map<String, Map<String, String>> source) {
		List<LogLevelProfile> profiles = new ArrayList<>();
		for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
			LogLevelProfile profile = LogLevelProfile.T.create();
			profile.setName(entry.getKey());
			profile.setLevels(entry.getValue());
			profiles.add(profile);
		}
		return profiles;
	}

	private InstanceId resolveApplication(String requestedApplicationId) {
		String applicationId = normalize(requestedApplicationId);
		if (applicationId == null || applicationId.equals(instanceId.getApplicationId()))
			return instanceId;

		Set<String> candidates = liveInstances.liveInstances(InstanceId.of(null, applicationId));
		if (candidates == null || candidates.isEmpty())
			throw new IllegalArgumentException("No live instance found for application: " + applicationId);
		return InstanceId.parse(candidates.iterator().next());
	}

	private static boolean sameApplication(InstanceId left, InstanceId right) {
		return left != null && right != null && java.util.Objects.equals(left.getApplicationId(), right.getApplicationId());
	}

	private LogTopology topology() {
		Set<String> visible = liveInstances.liveInstances();
		ArrayList<InstanceId> instances = new ArrayList<>();
		if (visible != null)
			visible.stream().map(InstanceId::parse).forEach(instances::add);
		if (instances.stream().noneMatch(instanceId::equals))
			instances.add(instanceId);
		instances.sort(Comparator.comparing(InstanceId::getApplicationId, Comparator.nullsFirst(Comparator.naturalOrder()))
				.thenComparing(InstanceId::getNodeId, Comparator.nullsFirst(Comparator.naturalOrder())));

		LogTopology topology = LogTopology.T.create();
		topology.setInstances(instances.stream().map(LogReflectionModelTools::origin).toList());
		topology.setLocalInstance(LogReflectionModelTools.origin(instanceId));
		return topology;
	}

	private LogStreams listStreams(ServiceRequestContext context, ListLogStreams request) {
		if (request.getTarget() == null)
			return listLocalStreams();

		ResolvedTarget target = resolve(request.getTarget());
		ListLocalLogStreams localRequest = ListLocalLogStreams.T.create();
		if (target.nodeId != null && !target.allApplications()) {
			if (target.is(instanceId))
				return listLocalStreams();
			return unicast(context, localRequest, target.instanceId(), LogStreams.class, clusterTimeoutMillis);
		}

		MulticastResponse multicast = multicast(context, localRequest, target.addressee(), clusterTimeoutMillis);
		Map<String, String> errors = new LinkedHashMap<>();
		Map<String, LogStreams> responses = successfulResponses(multicast, LogStreams.class, errors);
		return LogClusterSupport.mergeStreams(responses, errors);
	}

	private LogStreams listLocalStreams() {
		LogStreams response = LogStreams.T.create();
		response.setStreams(inventory.streams());
		response.setErrors(new LinkedHashMap<>());
		return response;
	}

	private LogRecordPage queryRecords(ServiceRequestContext context, QueryLogRecords request) {
		if (request.getTarget() == null)
			return queryLocalRecords(localRequest(request, instanceId.stringify(), request.getCursor(), request.getBeforeCursor()));

		ResolvedTarget target = resolve(request.getTarget());
		if (target.nodeId != null && !target.allApplications()) {
			String localCursor = LogClusterSupport.localCursor(request.getCursor(), target.instanceId().stringify());
			String localBeforeCursor = LogClusterSupport.localCursor(request.getBeforeCursor(), target.instanceId().stringify());
			QueryLocalLogRecords localRequest = localRequest(request, target.instanceId().stringify(), localCursor, localBeforeCursor);
			if (target.is(instanceId))
				return queryLocalRecords(localRequest);
			return unicast(context, localRequest, target.instanceId(), LogRecordPage.class, timeout(request.getWaitMillis()));
		}

		Map<String, String> cursors = LogClusterSupport.decodeCursor(request.getCursor());
		Map<String, String> beforeCursors = request.getBeforeCursor() == null
				? null
				: LogClusterSupport.decodeCursor(request.getBeforeCursor());
		QueryLocalLogRecords localRequest = localRequest(request, cursors, beforeCursors);
		MulticastResponse multicast = multicast(context, localRequest, target.addressee(), timeout(request.getWaitMillis()));
		Map<String, String> errors = new LinkedHashMap<>();
		Map<String, LogRecordPage> responses = successfulResponses(multicast, LogRecordPage.class, errors);
		return LogClusterSupport.mergePages(responses, cursors, errors);
	}

	private LogRecordPage queryLocalRecords(QueryLocalLogRecords request) {
		String streamId = request.getStreamId();
		if (streamId == null)
			streamId = StructuredLiveLogCollector.STREAM_ID;
		Map<String, String> cursors = request.getCursors();
		String cursor = cursors == null ? null : cursors.get(instanceId.stringify());
		Map<String, String> beforeCursors = request.getBeforeCursors();
		String beforeCursor = beforeCursors == null ? null : beforeCursors.get(instanceId.stringify());
		if (request.getLoadBefore() && beforeCursor == null)
			return emptyPage();
		LogRecordPage response;
		if (StructuredLiveLogCollector.STREAM_ID.equals(streamId))
			response = collector.query(request.getFilter(), cursor, beforeCursor, request.getLimit(), request.getWaitMillis());
		else
			response = fileReader.query(inventory.fileStream(streamId), request.getFilter(), cursor, beforeCursor,
					request.getLimit(), request.getWaitMillis(), request.getIncludeRotated());
		response.setErrors(new LinkedHashMap<>());
		return response;
	}

	private static LogRecordPage emptyPage() {
		LogRecordPage response = LogRecordPage.T.create();
		response.setRecords(java.util.List.of());
		response.setObservedProperties(java.util.Set.of());
		response.setErrors(new LinkedHashMap<>());
		return response;
	}

	private QueryLocalLogRecords localRequest(QueryLogRecords source, String instance, String cursor, String beforeCursor) {
		Map<String, String> cursors = new LinkedHashMap<>();
		if (cursor != null)
			cursors.put(instance, cursor);
		Map<String, String> beforeCursors = null;
		if (source.getBeforeCursor() != null) {
			beforeCursors = new LinkedHashMap<>();
			if (beforeCursor != null)
				beforeCursors.put(instance, beforeCursor);
		}
		return localRequest(source, cursors, beforeCursors);
	}

	private QueryLocalLogRecords localRequest(QueryLogRecords source, Map<String, String> cursors,
			Map<String, String> beforeCursors) {
		QueryLocalLogRecords target = QueryLocalLogRecords.T.create();
		target.setStreamId(source.getStreamId());
		target.setFilter(source.getFilter());
		target.setCursors(new LinkedHashMap<>(cursors));
		target.setLoadBefore(beforeCursors != null);
		target.setBeforeCursors(beforeCursors == null ? null : new LinkedHashMap<>(beforeCursors));
		target.setLimit(source.getLimit());
		target.setWaitMillis(source.getWaitMillis());
		target.setIncludeRotated(source.getIncludeRotated());
		return target;
	}

	private com.braintribe.model.resource.Resource createBundle(ServiceRequestContext context, CreateLogBundle request) {
		ResolvedTarget target = request.getTarget() == null
				? new ResolvedTarget(instanceId.getApplicationId(), instanceId.getNodeId())
				: resolve(request.getTarget());
		CreateLocalLogBundle localRequest = localBundleRequest(request);
		if (target.nodeId != null && !target.allApplications()) {
			if (target.is(instanceId))
				return bundleWriter.create(inventory, request, instanceId);
			com.braintribe.model.resource.Resource contribution = unicast(context, localRequest, target.instanceId(),
					com.braintribe.model.resource.Resource.class, bundleClusterTimeoutMillis);
			return bundleWriter.combine(Map.of(target.instanceId().toString(), contribution), Map.of(), target.applicationId);
		}

		MulticastResponse multicast = multicast(context, localRequest, target.addressee(), bundleClusterTimeoutMillis);
		Map<String, String> errors = new LinkedHashMap<>();
		Map<String, com.braintribe.model.resource.Resource> contributions = successfulResponses(multicast,
				com.braintribe.model.resource.Resource.class, errors);
		if (!errors.isEmpty()) {
			release(contributions.values());
			throw new IllegalStateException("Could not create a complete log bundle; node failures: " + errors);
		}
		return bundleWriter.combine(contributions, errors, target.applicationId);
	}

	private static void release(Iterable<com.braintribe.model.resource.Resource> resources) {
		for (com.braintribe.model.resource.Resource resource : resources) {
			try (var ignored = resource.openStream()) {
				// Closing a transient contribution stream releases its file-backed pipe.
			} catch (Exception e) {
				// The original cluster failure remains the actionable error.
			}
		}
	}

	private static CreateLocalLogBundle localBundleRequest(CreateLogBundle source) {
		CreateLocalLogBundle target = CreateLocalLogBundle.T.create();
		target.setStreamIds(source.getStreamIds() == null ? Set.of() : Set.copyOf(source.getStreamIds()));
		target.setScope(source.getScope());
		target.setLayouts(source.getLayouts() == null ? Set.of() : Set.copyOf(source.getLayouts()));
		target.setFormats(source.getFormats() == null ? Set.of() : Set.copyOf(source.getFormats()));
		target.setFrom(source.getFrom());
		target.setTo(source.getTo());
		target.setIncludeRotated(source.getIncludeRotated());
		target.setFormat(source.getFormat());
		target.setFilter(source.getFilter());
		return target;
	}

	private ResolvedTarget resolve(LogTarget target) {
		String applicationId = normalize(target.getApplicationId());
		if (applicationId == null)
			applicationId = instanceId.getApplicationId();
		if (applicationId == null)
			throw new IllegalStateException("Log reflection cluster routing requires a local or explicit application id");
		return new ResolvedTarget(applicationId, normalize(target.getNodeId()));
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private long timeout(long waitMillis) {
		return Math.max(clusterTimeoutMillis, Math.min(waitMillis, 30_000) + 5_000);
	}

	private <T> T unicast(ServiceRequestContext context, LogReflectionRequest payload, InstanceId addressee, Class<T> responseType,
			long timeout) {
		UnicastRequest request = UnicastRequest.T.create();
		request.setServiceRequest(payload);
		request.setAddressee(addressee);
		request.setTimeout(timeout);
		Object response = request.eval(systemEvaluator).get();
		if (!responseType.isInstance(response))
			throw new IllegalStateException("Unexpected log reflection response from " + addressee + ": " + response);
		return responseType.cast(response);
	}

	private MulticastResponse multicast(ServiceRequestContext context, LogReflectionRequest payload, InstanceId addressee, long timeout) {
		MulticastRequest request = MulticastRequest.T.create();
		request.setServiceRequest(payload);
		request.setAddressee(addressee);
		request.setTimeout(timeout);
		request.setAsynchronous(false);
		return request.eval(systemEvaluator).get();
	}

	private static <T> Map<String, T> successfulResponses(MulticastResponse multicast, Class<T> responseType,
			Map<String, String> errors) {
		Map<String, T> responses = new LinkedHashMap<>();
		for (Map.Entry<InstanceId, ServiceResult> entry : multicast.getResponses().entrySet()) {
			String origin = entry.getKey().stringify();
			ServiceResult result = entry.getValue();
			if (result.asResponse() != null) {
				Object value = result.asResponse().getResult();
				if (responseType.isInstance(value))
					responses.put(origin, responseType.cast(value));
				else
					errors.put(origin, "Unexpected response: " + value);
			} else {
				errors.put(origin, error(result));
			}
		}
		return responses;
	}

	private static String error(ServiceResult result) {
		Failure failure = result.asFailure();
		if (failure != null) {
			String message = failure.getMessage() != null ? failure.getMessage() : failure.getDetails();
			return failure.getType() + (message == null ? "" : ": " + message);
		}
		if (result.asUnsatisfied() != null && result.asUnsatisfied().getWhy() != null)
			return result.asUnsatisfied().getWhy().asString();
		return "Unexpected cluster result: " + result.resultType();
	}

	private record ResolvedTarget(String applicationId, String nodeId) {
		private boolean allApplications() {
			return "*".equals(applicationId);
		}

		private InstanceId instanceId() {
			return InstanceId.of(nodeId, applicationId);
		}

		private InstanceId addressee() {
			return InstanceId.of(nodeId, applicationId);
		}

		private boolean is(InstanceId other) {
			return applicationId.equals(other.getApplicationId()) && nodeId.equals(other.getNodeId());
		}
	}
}
