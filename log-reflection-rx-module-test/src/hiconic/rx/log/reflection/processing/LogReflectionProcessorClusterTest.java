package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

import com.braintribe.model.generic.eval.AbstractEvalContext;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.EvalException;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.commons.StandardServiceRequestContext;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.UnicastRequest;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ResponseEnvelope;
import com.braintribe.processing.async.api.AsyncCallback;

import hiconic.rx.log.reflection.model.LogOrigin;
import hiconic.rx.log.reflection.model.LogBundleFormat;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.LogTarget;
import hiconic.rx.log.reflection.model.api.CreateLocalLogBundle;
import hiconic.rx.log.reflection.model.api.CreateLogBundle;
import hiconic.rx.log.reflection.model.api.ListLocalLogStreams;
import hiconic.rx.log.reflection.model.api.ListLogStreams;
import hiconic.rx.log.reflection.model.api.LogRecordPage;
import hiconic.rx.log.reflection.model.api.LogStreams;
import hiconic.rx.log.reflection.model.api.QueryLocalLogRecords;
import hiconic.rx.log.reflection.model.api.QueryLogRecords;

public class LogReflectionProcessorClusterTest {
	@Test
	public void usesUnicastWithLocalPayloadForAnExactRemoteNode() {
		AtomicReference<ServiceRequest> dispatched = new AtomicReference<>();
		LogStreams expected = LogStreams.T.create();
		expected.setStreams(List.of());

		Evaluator<ServiceRequest> evaluator = evaluator(request -> {
			dispatched.set(request);
			return expected;
		});

		ListLogStreams request = ListLogStreams.T.create();
		request.setTarget(target("logs-app", "node-2"));

		LogStreams actual = (LogStreams) processor(evaluator).process(new StandardServiceRequestContext(failingEvaluator()), request);

		assertThat(actual).isSameAs(expected);
		assertThat(dispatched.get()).isInstanceOf(UnicastRequest.class);
		UnicastRequest unicast = (UnicastRequest) dispatched.get();
		assertThat(unicast.getAddressee().stringify()).isEqualTo("logs-app@node-2");
		assertThat(unicast.getServiceRequest()).isInstanceOf(ListLocalLogStreams.class);
	}

	@Test
	public void multicastsLocalQueryAndMergesNodeResults() {
		AtomicReference<MulticastRequest> dispatched = new AtomicReference<>();
		MulticastResponse multicastResponse = MulticastResponse.T.create();
		multicastResponse.getResponses().put(InstanceId.of("node-1", "logs-app"), envelope(page("logs-app", "node-1", 200, "later", "c1")));
		multicastResponse.getResponses().put(InstanceId.of("node-2", "logs-app"), envelope(page("logs-app", "node-2", 100, "earlier", "c2")));

		Evaluator<ServiceRequest> evaluator = evaluator(request -> {
			dispatched.set((MulticastRequest) request);
			return multicastResponse;
		});

		QueryLogRecords request = QueryLogRecords.T.create();
		request.setTarget(target("logs-app", null));
		request.setStreamId("structured-live");
		request.setLimit(20);

		LogRecordPage result = (LogRecordPage) processor(evaluator).process(new StandardServiceRequestContext(failingEvaluator()), request);

		assertThat(dispatched.get().getAddressee().stringify()).isEqualTo("logs-app@<undefined>");
		assertThat(dispatched.get().getServiceRequest()).isInstanceOf(QueryLocalLogRecords.class);
		assertThat(result.getRecords()).extracting(LogRecord::getMessage).containsExactly("earlier", "later");
		assertThat(LogClusterSupport.decodeCursor(result.getNextCursor()))
				.containsEntry("logs-app@node-1", "c1")
				.containsEntry("logs-app@node-2", "c2");
	}

	@Test
	public void multicastsAcrossApplicationsWhenApplicationIsWildcarded() {
		AtomicReference<MulticastRequest> dispatched = new AtomicReference<>();
		MulticastResponse multicastResponse = MulticastResponse.T.create();

		Evaluator<ServiceRequest> evaluator = evaluator(request -> {
			dispatched.set((MulticastRequest) request);
			return multicastResponse;
		});

		ListLogStreams request = ListLogStreams.T.create();
		request.setTarget(target("*", "node-2"));

		processor(evaluator).process(new StandardServiceRequestContext(failingEvaluator()), request);

		assertThat(dispatched.get().getAddressee().stringify()).isEqualTo("*@node-2");
		assertThat(dispatched.get().getServiceRequest()).isInstanceOf(ListLocalLogStreams.class);
	}

	@Test
	public void multicastsLocalRawBundlesAndCombinesTheirResources() throws Exception {
		AtomicReference<MulticastRequest> dispatched = new AtomicReference<>();
		MulticastResponse multicastResponse = MulticastResponse.T.create();
		multicastResponse.getResponses().put(InstanceId.of("node-1", "logs-app"),
				envelope(zipResource("application/current.log", "node one")));
		multicastResponse.getResponses().put(InstanceId.of("node-2", "logs-app"),
				envelope(zipResource("application/current.log", "node two")));

		Evaluator<ServiceRequest> evaluator = evaluator(request -> {
			dispatched.set((MulticastRequest) request);
			return multicastResponse;
		});

		CreateLogBundle request = CreateLogBundle.T.create();
		request.setTarget(target("logs-app", null));
		request.setStreamIds(Set.of("appender:application"));
		request.setFormat(LogBundleFormat.RAW_FILES);

		Resource result = (Resource) processor(evaluator).process(new StandardServiceRequestContext(failingEvaluator()), request);
		List<String> entries = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(result.openStream())) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				entries.add(entry.getName());
		}

		assertThat(dispatched.get().getAddressee().stringify()).isEqualTo("logs-app@<undefined>");
		assertThat(dispatched.get().getServiceRequest()).isInstanceOf(CreateLocalLogBundle.class);
		CreateLocalLogBundle local = (CreateLocalLogBundle) dispatched.get().getServiceRequest();
		assertThat(local.getStreamIds()).containsExactly("appender:application");
		assertThat(entries).contains(
				"logs-app_node-1/application/current.log",
				"logs-app_node-2/application/current.log",
				"manifest.json");
	}

	private static LogReflectionProcessor processor(Evaluator<ServiceRequest> systemEvaluator) {
		LogReflectionProcessor processor = new LogReflectionProcessor();
		processor.setInstanceId(InstanceId.of("node-1", "logs-app"));
		processor.setSystemEvaluator(systemEvaluator);
		processor.setBundleWriter(new LogBundleWriter());
		return processor;
	}

	private static Resource zipResource(String name, String content) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (ZipOutputStream zip = new ZipOutputStream(output)) {
				zip.putNextEntry(new ZipEntry(name));
				zip.write(content.getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
			byte[] bytes = output.toByteArray();
			Resource resource = Resource.createTransient(() -> new ByteArrayInputStream(bytes));
			resource.setName("node.zip");
			resource.setMimeType("application/zip");
			return resource;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static Evaluator<ServiceRequest> failingEvaluator() {
		return evaluator(request -> {
			throw new AssertionError("Cluster envelopes must be evaluated with the system evaluator");
		});
	}

	private static LogTarget target(String applicationId, String nodeId) {
		LogTarget target = LogTarget.T.create();
		target.setApplicationId(applicationId);
		target.setNodeId(nodeId);
		return target;
	}

	private static LogRecordPage page(String applicationId, String nodeId, long timestamp, String message, String cursor) {
		LogOrigin origin = LogOrigin.T.create();
		origin.setApplicationId(applicationId);
		origin.setNodeId(nodeId);
		LogRecord record = LogRecord.T.create();
		record.setOrigin(origin);
		record.setTimestamp(new Date(timestamp));
		record.setMessage(message);

		LogRecordPage page = LogRecordPage.T.create();
		page.setRecords(List.of(record));
		page.setNextCursor(cursor);
		page.setObservedProperties(Set.of());
		return page;
	}

	private static ResponseEnvelope envelope(Object value) {
		ResponseEnvelope envelope = ResponseEnvelope.T.create();
		envelope.setResult(value);
		return envelope;
	}

	private static Evaluator<ServiceRequest> evaluator(Function<ServiceRequest, Object> responder) {
		return new Evaluator<>() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> EvalContext<T> eval(ServiceRequest request) {
				return result(() -> (T) responder.apply(request));
			}
		};
	}

	private static <T> EvalContext<T> result(ResultSupplier<T> supplier) {
		return new AbstractEvalContext<>() {
			@Override
			public T get() throws EvalException {
				return supplier.get();
			}

			@Override
			public void get(AsyncCallback<? super T> callback) {
				try {
					callback.onSuccess(get());
				} catch (Throwable e) {
					callback.onFailure(e);
				}
			}
		};
	}

	@FunctionalInterface
	private interface ResultSupplier<T> {
		T get();
	}
}
