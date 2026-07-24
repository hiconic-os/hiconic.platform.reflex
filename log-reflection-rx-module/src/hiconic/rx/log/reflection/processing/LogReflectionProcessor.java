package hiconic.rx.log.reflection.processing;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.UnicastRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ServiceResult;

import hiconic.rx.log.reflection.model.LogTarget;
import hiconic.rx.log.reflection.model.LogOrigin;
import hiconic.rx.log.reflection.model.api.GetLogTopology;
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

	@Override
	protected void configureDispatching(DispatchConfiguration<LogReflectionRequest, Object> dispatching) {
		dispatching.register(ListLogStreams.T, this::listStreams);
		dispatching.register(GetLogTopology.T, (context, request) -> topology());
		dispatching.register(QueryLogRecords.T, this::queryRecords);
		dispatching.register(CreateLogBundle.T, this::createBundle);
		dispatching.register(CreateLocalLogBundle.T, (context, request) -> bundleWriter.create(inventory, request, instanceId));
		dispatching.register(ListLocalLogStreams.T, (context, request) -> listLocalStreams());
		dispatching.register(QueryLocalLogRecords.T, (context, request) -> queryLocalRecords(request));
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
			return queryLocalRecords(localRequest(request, instanceId.stringify(), request.getCursor()));

		ResolvedTarget target = resolve(request.getTarget());
		if (target.nodeId != null && !target.allApplications()) {
			String localCursor = LogClusterSupport.localCursor(request.getCursor(), target.instanceId().stringify());
			QueryLocalLogRecords localRequest = localRequest(request, target.instanceId().stringify(), localCursor);
			if (target.is(instanceId))
				return queryLocalRecords(localRequest);
			return unicast(context, localRequest, target.instanceId(), LogRecordPage.class, timeout(request.getWaitMillis()));
		}

		Map<String, String> cursors = LogClusterSupport.decodeCursor(request.getCursor());
		QueryLocalLogRecords localRequest = localRequest(request, cursors);
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
		LogRecordPage response;
		if (StructuredLiveLogCollector.STREAM_ID.equals(streamId))
			response = collector.query(request.getFilter(), cursor, request.getLimit(), request.getWaitMillis());
		else
			response = fileReader.query(inventory.fileStream(streamId), request.getFilter(), cursor, request.getLimit(),
					request.getWaitMillis(), request.getIncludeRotated());
		response.setErrors(new LinkedHashMap<>());
		return response;
	}

	private QueryLocalLogRecords localRequest(QueryLogRecords source, String instance, String cursor) {
		Map<String, String> cursors = new LinkedHashMap<>();
		if (cursor != null)
			cursors.put(instance, cursor);
		return localRequest(source, cursors);
	}

	private QueryLocalLogRecords localRequest(QueryLogRecords source, Map<String, String> cursors) {
		QueryLocalLogRecords target = QueryLocalLogRecords.T.create();
		target.setStreamId(source.getStreamId());
		target.setFilter(source.getFilter());
		target.setCursors(new LinkedHashMap<>(cursors));
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
				return bundleWriter.create(inventory, localRequest, instanceId);
			return unicast(context, localRequest, target.instanceId(), com.braintribe.model.resource.Resource.class,
					bundleClusterTimeoutMillis);
		}

		MulticastResponse multicast = multicast(context, localRequest, target.addressee(), bundleClusterTimeoutMillis);
		Map<String, String> errors = new LinkedHashMap<>();
		Map<String, com.braintribe.model.resource.Resource> contributions = successfulResponses(multicast,
				com.braintribe.model.resource.Resource.class, errors);
		return bundleWriter.combine(contributions, errors, target.applicationId);
	}

	private static CreateLocalLogBundle localBundleRequest(CreateLogBundle source) {
		CreateLocalLogBundle target = CreateLocalLogBundle.T.create();
		target.setStreamIds(source.getStreamIds() == null ? Set.of() : Set.copyOf(source.getStreamIds()));
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
