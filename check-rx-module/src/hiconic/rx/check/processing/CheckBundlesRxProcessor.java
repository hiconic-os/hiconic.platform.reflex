//============================================================================
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//============================================================================
package hiconic.rx.check.processing;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import com.braintribe.cc.lcd.CodingMap;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.Pair;
import com.braintribe.exception.Exceptions;
import com.braintribe.execution.virtual.VirtualThreadExecutorBuilder;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.HttpStatusCodeNotification;
import com.braintribe.model.processing.service.common.topology.InstanceIdHashingComparator;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.UnicastRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ResponseEnvelope;
import com.braintribe.model.service.api.result.ServiceResult;
import com.braintribe.model.service.api.result.Unsatisfied;
import com.braintribe.thread.api.ThreadContextScoping;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.xml.XmlTools;

import hiconic.rx.check.model.bundle.api.request.CheckBundlesRequest;
import hiconic.rx.check.model.bundle.api.request.HasCheckBundleFilters;
import hiconic.rx.check.model.bundle.api.request.RunAimedCheckBundles;
import hiconic.rx.check.model.bundle.api.request.RunCheckBundles;
import hiconic.rx.check.model.bundle.api.request.RunDistributedCheckBundles;
import hiconic.rx.check.model.bundle.api.request.RunHealthChecks;
import hiconic.rx.check.model.bundle.api.request.RunVitalityCheckBundles;
import hiconic.rx.check.model.bundle.api.response.CbrAggregatable;
import hiconic.rx.check.model.bundle.api.response.CbrAggregation;
import hiconic.rx.check.model.bundle.api.response.CbrAggregationKind;
import hiconic.rx.check.model.bundle.api.response.CbrContainer;
import hiconic.rx.check.model.bundle.api.response.CheckBundleResult;
import hiconic.rx.check.model.bundle.api.response.CheckBundlesResponse;
import hiconic.rx.check.model.bundle.aspect.CheckCoverage;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;
import hiconic.rx.check.processing.BasicCheckProcessorRegistry.CheckProcessorEntry;


/**
* This service processor handles {@link CheckBundlesRequest Check Bundle Requests} covering local instance checks as well as distributed checks.
* Also, vitality/health checks are treated here. These kind of low-level checks can be executed in an unauthorized way.
*/
public class CheckBundlesRxProcessor extends AbstractDispatchingServiceProcessor<CheckBundlesRequest, Object> implements LifecycleAware {
	private static Logger log = Logger.getLogger(CheckBundlesRxProcessor.class);

	private BasicCheckProcessorRegistry registry;

	private Evaluator<ServiceRequest> systemEvaluator;
	private InstanceId instanceId;

	private ThreadContextScoping threadContextScoping;
	private ExecutorService executionThreadPool;

	private static Comparator<CbrAggregatable> comparator = cbrAggregatableComparator();

	private static Comparator<CbrAggregatable> cbrAggregatableComparator() {
		Comparator<CbrAggregatable> statusComparator = Comparator.comparing(CbrAggregatable::getStatus);
		Comparator<CbrAggregatable> nameComparator = Comparator.comparing(CheckBundlesUtils::getIdentification);

		return statusComparator.reversed().thenComparing(nameComparator);
	}

	public void setRegistry(BasicCheckProcessorRegistry registry) {
		this.registry = registry;
	}

	@Configurable
	@Required
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.systemEvaluator = evaluator;
	}

	@Configurable
	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}

	@Configurable
	@Required
	public void setThreadContextScoping(ThreadContextScoping threadContextScoping) {
		this.threadContextScoping = threadContextScoping;
	}

	// Service Processor preparation

	@Override
	public void postConstruct() {
		executionThreadPool = VirtualThreadExecutorBuilder.newPool().concurrency(50).threadNamePrefix("CheckBundleExecution").build();
	}

	@Override
	public void preDestroy() {
		executionThreadPool.close();
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<CheckBundlesRequest, Object> dispatching) {
		// do require permission
		dispatching.register(RunCheckBundles.T, (c, r) -> runCheckBundles(c, r));
		dispatching.register(RunAimedCheckBundles.T, (c, r) -> runAimedCheckBundles(r));
		dispatching.register(RunDistributedCheckBundles.T, (c, r) -> runDistributedCheckBundles(r));

		// do not require permissions
		dispatching.register(RunVitalityCheckBundles.T, (c, r) -> runVitalityCheckBundles(c, r));
		dispatching.register(RunHealthChecks.T, (c, r) -> runHealthChecks(c, r));
	}

	// Requests

	/** Executes {@link RunCheckBundles} */
	private CheckBundlesResponse runCheckBundles(ServiceRequestContext context, RunCheckBundles request) {
		long t0 = System.nanoTime();

		CheckBundlesResponse response = CheckBundlesResponse.T.create();
		response.setStatus(CheckStatus.ok);
		response.setCreatedAt(new Date());

		try {
			List<CheckProcessorEntry> bundles = getFilteredCheckBundles(request);

			if (bundles.size() == 0) {
				log.trace(() -> "Nothing to do here.");

				response.setElapsedTimeInMs((System.nanoTime() - t0) / 1_000_000.0);

				return response;
			}

			List<Pair<CheckProcessorEntry, Future<CheckResult>>> contextToResultFutureList = new ArrayList<>();

			Map<CheckProcessorEntry, CheckResult> results = new LinkedHashMap<>();
			for (CheckProcessorEntry c : bundles) {
				Future<CheckResult> resultFuture = evaluateParallel(c, context);
				contextToResultFutureList.add(Pair.of(c, resultFuture));
			}
			for (Pair<CheckProcessorEntry, Future<CheckResult>> pair : contextToResultFutureList) {
				CheckProcessorEntry entry = pair.first();
				Future<CheckResult> future = pair.second();

				CheckResult r = null;
				try {
					r = future.get();

				} catch (Exception e) {
					r = CheckResult.T.create();
					r.getEntries().add(createFailureCheckResultEntry(e));
					r.setElapsedTimeInMs(0);
				}

				results.put(entry, r);
			}

			if (log.isTraceEnabled()) {
				StringBuilder sb = new StringBuilder("Health check:\n");
				for (Map.Entry<CheckProcessorEntry, CheckResult> e : results.entrySet()) {
					CheckProcessorEntry ctx = e.getKey();
					CheckResult value = e.getValue();
					sb.append("Service: " + ctx.name() + ", time: " + value.getElapsedTimeInMs() + "\n");
				}
				log.trace(StringTools.asciiBoxMessage(sb.toString()));
			}

			response = buildResponse(request.getAggregateBy(), results);

		} catch (Exception e) {
			log.error("Error while evaluating checks: ", e);
			CheckBundleResult result = createFailureCheckBundleResult(e, t0);

			response.getElements().add(result);
			response.setStatus(CheckStatus.fail);
		}

		response.setElapsedTimeInMs((System.nanoTime() - t0) / 1_000_000.0);
		return response;
	}

	/** Executes {@link RunAimedCheckBundles} via Unicast */
	private CheckBundlesResponse runAimedCheckBundles(RunAimedCheckBundles request) {
		long t0 = System.nanoTime();

		try {
			String nodeId = request.getNodeId();
			if (nodeId == null || nodeId.isEmpty())
				throw new IllegalArgumentException("nodeId must be set");

			RunCheckBundles run = RunCheckBundles.T.create();
			for (Property p : HasCheckBundleFilters.T.getProperties()) {
				if (p.getDeclaringType() != HasCheckBundleFilters.T)
					continue;

				Object v = p.get(request);
				p.set(run, v);
			}

			UnicastRequest r = UnicastRequest.T.create();
			r.setServiceRequest(run);
			r.setAddressee(InstanceId.of(nodeId, null));

			return (CheckBundlesResponse) r.eval(systemEvaluator).get();

		} catch (Exception e) {
			log.error("Error while evaluating aimed checks: ", e);
			CheckBundlesResponse response = CheckBundlesResponse.T.create();

			response.getElements().add(createFailureCheckBundleResult(e, t0));
			response.setStatus(CheckStatus.fail);
			response.setCreatedAt(new Date());
			response.setElapsedTimeInMs((System.nanoTime() - t0) / 1_000_000.0);

			return response;
		}
	}

	/** Executes {@link RunDistributedCheckBundles} via Multicast */
	private CheckBundlesResponse runDistributedCheckBundles(RunDistributedCheckBundles request) {
		long t0 = System.nanoTime();

		CheckBundlesResponse response = CheckBundlesResponse.T.create();
		response.setCreatedAt(new Date());

		List<CheckBundleResult> results = new ArrayList<>();
		try {
			MulticastRequest m = buildMulticast(request);
			Map<InstanceId, CheckBundlesResponse> responsePerNode = getDistributedCheckResponses(m);

			for (CheckBundlesResponse r : responsePerNode.values()) {
				for (CbrAggregatable a : r.getElements()) {
					results.add((CheckBundleResult) a);
				}
			}

		} catch (Exception e) {
			log.error("Error while evaluating distributed checks: ", e);
			results.add(createFailureCheckBundleResult(e, t0));
		}

		aggregate(response, results, request.getAggregateBy(), 0);

		response.setElapsedTimeInMs((System.nanoTime() - t0) / 1_000_000.0);
		return response;
	}

	/** Executes unauthorized {@link RunVitalityCheckBundles} */
	private CheckBundlesResponse runVitalityCheckBundles(ServiceRequestContext requestContext, RunVitalityCheckBundles request) {
		long t0 = System.nanoTime();
		CheckBundlesResponse response;
		Consumer<Integer> statusCodeConsumer = statusCodeConsumer(requestContext);

		try {
			RunCheckBundles run = RunCheckBundles.T.create();

			run.setAggregateBy(request.getAggregateBy());
			run.setCoverage(Collections.singleton(CheckCoverage.vitality));

			response = run.eval(systemEvaluator).get();

			switch (response.getStatus()) {
				case ok:
					statusCodeConsumer.accept(200);
					break;
				case warn:
					statusCodeConsumer.accept(request.getWarnStatusCode());
					break;
				case fail:
				default:
					statusCodeConsumer.accept(503);
					break;
			}

		} catch (Exception e) {
			log.error("Error while evaluating vitality checks: ", e);
			response = CheckBundlesResponse.T.create();
			response.getElements().add(createFailureCheckBundleResult(e, t0));
			response.setStatus(CheckStatus.fail);

			response.setCreatedAt(new Date());
			response.setElapsedTimeInMs((System.nanoTime() - t0) / 1_000_000.0);

			statusCodeConsumer.accept(503);
		}

		return response;
	}

	/** Executes {@link RunHealthChecks} */
	private Map<InstanceId, Object> runHealthChecks(ServiceRequestContext requestContext, RunHealthChecks request) {
		long t0 = System.nanoTime();
		Map<InstanceId, Object> response = new HashMap<>();
		List<CheckResult> results = new ArrayList<>();
		CheckBundlesResponse cbr;
		try {

			Integer warnStatusCode = request.getWarnStatusCode();
			if (warnStatusCode == null)
				warnStatusCode = 503;

			RunVitalityCheckBundles run = RunVitalityCheckBundles.T.create();
			run.setWarnStatusCode(warnStatusCode);

			cbr = run.eval(requestContext).get();

			// no aggregation has been defined so we get back a flat list of check bundle results
			for (CbrAggregatable a : cbr.getElements()) {
				if (a.isResult())
					results.add(((CheckBundleResult) a).getResult());
			}

		} catch (Exception e) {
			log.error("Error while evaluating health checks: ", e);
			results.add(createFailureCheckResult(e, t0));

			statusCodeConsumer(requestContext).accept(503);
		}

		response.put(instanceId, results);
		return response;
	}

	// HELPERS

	private Consumer<Integer> statusCodeConsumer(ServiceRequestContext requestContext) {
		return requestContext.getAspect(HttpStatusCodeNotification.class).orElse(s -> {/* ignore */});
	}

	
	private CheckBundleResult createFailureCheckBundleResult(Exception e, long t0) {
		CheckResult r = createFailureCheckResult(e, t0);

		CheckBundleResult cbr = CheckBundleResult.T.create();
		cbr.setResult(r);
		cbr.setNode(this.instanceId.getNodeId());
		cbr.setStatus(CheckStatus.fail);
		cbr.setName("Check Bundle Framework - Internal Error");
		// TODO properly solve this
		cbr.setCheckProcessorName("Check Framework");

		return cbr;
	}

	private CheckResult createFailureCheckResult(Exception e, long t0) {
		CheckResultEntry entry = createFailureCheckResultEntry(e);

		CheckResult r = CheckResult.T.create();
		r.getEntries().add(entry);
		r.setElapsedTimeInMs((System.nanoTime() - t0) / 1_000_000.0);

		return r;
	}

	private CheckResultEntry createFailureCheckResultEntry(Exception e) {
		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setCheckStatus(CheckStatus.fail);
		entry.setName("Check Bundle Framework");

		StringBuilder sb = new StringBuilder();
		sb.append("Evaluation of checks on node " + this.instanceId.getNodeId() + " failed with " + e.getClass().getName());

		String m = e.getMessage();
		if (m != null) {
			sb.append(": ");
			sb.append(m);
		}
		sb.append("");

		entry.setMessage(sb.toString());
		entry.setDetails(Exceptions.stringify(e));
		return entry;
	}

	private MulticastRequest buildMulticast(CheckBundlesRequest request) {
		RunCheckBundles run = RunCheckBundles.T.create();

		for (Property p : HasCheckBundleFilters.T.getProperties()) {
			if (p.getDeclaringType() == GenericEntity.T)
				continue;

			Object v = p.get(request);
			p.set(run, v);
		}
		//run.setAggregateBy(Collections.emptyList());

		MulticastRequest m = MulticastRequest.T.create();
		m.setServiceRequest(run);
		m.setAddressee(InstanceId.of(null, instanceId.getApplicationId()));

		return m;
	}

	private Map<InstanceId, CheckBundlesResponse> getDistributedCheckResponses(MulticastRequest multicastRequest) {
		Map<InstanceId, CheckBundlesResponse> responsePerNode = CodingMap.create(new ConcurrentHashMap<>(), InstanceIdHashingComparator.instance);
		MulticastResponse mcResponse = multicastRequest.eval(systemEvaluator).get();

		for (Map.Entry<InstanceId, ServiceResult> entry : mcResponse.getResponses().entrySet()) {
			ServiceResult serviceResult = entry.getValue();
			CheckBundlesResponse response;

			// can this be null?
			InstanceId instanceId = entry.getKey();

			switch (serviceResult.resultType()) {
				case success:
					ResponseEnvelope standardServiceResult = (ResponseEnvelope) serviceResult;
					response = (CheckBundlesResponse) standardServiceResult.getResult();
					break;
				case failure: {
					Failure fail = (Failure) serviceResult;
					String nodeId = instanceId.getNodeId();

					CheckResultEntry cre = CheckResultEntry.T.create();
					cre.setCheckStatus(CheckStatus.fail);
					cre.setName("Check Bundle Framework Distributed");
					cre.setMessage("Evaluation of distributed checks on node " + nodeId + " failed with " + fail.getType());
					cre.setDetails(fail.getType() + "\n" + fail.getDetails());

					CheckResult r = CheckResult.T.create();
					r.getEntries().add(cre);

					CheckBundleResult cbr = CheckBundleResult.T.create();
					cbr.setResult(r);
					cbr.setNode(nodeId);
					cbr.setStatus(CheckStatus.fail);
					cbr.setName("Check Bundle Framework - Internal Error");
					cbr.setCheckProcessorName("Check Framework");

					response = CheckBundlesResponse.T.create();
					response.getElements().add(cbr);
					response.setStatus(CheckStatus.fail);

					break;
				}
				case unsatisfied: {
					Unsatisfied unsatisfied = serviceResult.asUnsatisfied();

					CheckResultEntry cre = CheckResultEntry.T.create();
					cre.setCheckStatus(CheckStatus.fail);
					cre.setName("Check Bundle Framework Distributed");
					cre.setMessage("Evaluation of distributed checks failed with: unsatisfied");

					Reason reason = unsatisfied.getWhy();
					if (reason != null) {
						StringBuilder sb = new StringBuilder(unsatisfied.getWhy().stringify());
						if (reason instanceof InternalError ie) {
							Throwable throwable = ie.getJavaException();
							if (throwable != null) {
								sb.append("\n");
								sb.append(Exceptions.stringify(throwable));
							}
						}
						cre.setDetails(sb.toString());
					}

					CheckResult r = CheckResult.T.create();
					r.getEntries().add(cre);

					CheckBundleResult cbr = CheckBundleResult.T.create();
					cbr.setResult(r);
					cbr.setStatus(CheckStatus.fail);
					cbr.setName("Check Bundle Framework - Internal Error");
					cbr.setCheckProcessorName("Check Framework");

					response = CheckBundlesResponse.T.create();
					response.getElements().add(cbr);
					response.setStatus(CheckStatus.fail);

					break;
				}

				default:
					throw new IllegalStateException("Unexpected Multicast result type: " + serviceResult.resultType());
			}

			responsePerNode.put(instanceId, response);
		}

		return responsePerNode;
	}

	private CheckBundlesResponse buildResponse(List<CbrAggregationKind> aggregatedBy, Map<CheckProcessorEntry, CheckResult> results) {
		CheckBundlesResponse response = CheckBundlesResponse.T.create();

		// # Calculate CheckStatus: ok, fail or warn
		CheckStatus status = CheckBundlesUtils.getStatus(results.values());
		response.setStatus(status);

		// # Create results
		List<CheckBundleResult> bundleResults = new ArrayList<>();
		for (Map.Entry<CheckProcessorEntry, CheckResult> entry : results.entrySet()) {
			CheckProcessorEntry context = entry.getKey();

			CheckBundleResult cbr = CheckBundleResult.T.create();
			// Why does a result have a name???
			cbr.setName(context.name());
			cbr.setCheckProcessorName(context.name());
			cbr.setCoverage(context.coverage());
			cbr.setLatency(context.latency());
			cbr.setLabels(context.labels());
			
			CheckResult result = entry.getValue();
			cbr.setResult(result);

			cbr.setNode(instanceId.getNodeId());

			CheckStatus bundleStatus = CheckBundlesUtils.getStatus(result);
			cbr.setStatus(bundleStatus);

			bundleResults.add(cbr);
		}

		// # Aggregate
		aggregate(response, bundleResults, aggregatedBy, 0);

		return response;
	}

	private void aggregate(CbrContainer container, List<CheckBundleResult> results, List<CbrAggregationKind> aggregateBy, int aggregateByIndex) {
		List<CbrAggregatable> elements = container.getElements();

		if (aggregateByIndex < aggregateBy.size()) {
			CbrAggregationKind kind = aggregateBy.get(aggregateByIndex);

			Function<CheckBundleResult, Collection<?>> accessor = CheckBundlesUtils.getAccessor(kind);

			Map<Object, Pair<CbrAggregation, List<CheckBundleResult>>> aggregationByKindSpecificValue = new LinkedHashMap<>();

			Iterator<CheckBundleResult> iterator = results.iterator();
			while (iterator.hasNext()) {
				boolean matched = false;
				CheckBundleResult result = iterator.next();

				Collection<?> values = accessor.apply(result);
				for (Object v : values) {
					if (v == null) {
						continue;
					}
					List<CheckBundleResult> filteredResults = aggregationByKindSpecificValue
							.computeIfAbsent(v, k -> Pair.of(createAggregation(kind, v), new ArrayList<>())).second();
					filteredResults.add(result);
					matched = true;
				}

				if (matched)
					iterator.remove();

			}

			for (Pair<CbrAggregation, List<CheckBundleResult>> aggregationPair : aggregationByKindSpecificValue.values()) {
				CbrAggregation aggregation = aggregationPair.first();
				List<CheckBundleResult> filteredResults = aggregationPair.second();
				aggregate(aggregation, filteredResults, aggregateBy, aggregateByIndex + 1);
				elements.add(aggregation);
			}

			results.sort(comparator);
			elements.addAll(0, results);

		} else {
			elements.addAll(results);
			results.clear();
		}

		CheckStatus containerStatus = CheckStatus.ok;

		for (CbrAggregatable aggregatable : elements) {
			CheckStatus aggregatableStatus = aggregatable.getStatus();

			if (aggregatableStatus.ordinal() > containerStatus.ordinal())
				containerStatus = aggregatableStatus;
		}

		elements.sort(comparator);

		container.setStatus(containerStatus);
	}

	static CbrAggregation createAggregation(CbrAggregationKind kind, Object discriminator) {
		CbrAggregation newAggregation = CbrAggregation.T.create();
		newAggregation.setKind(kind);
		newAggregation.setDiscriminator(discriminator);
		return newAggregation;
	}

	private List<CheckProcessorEntry> getFilteredCheckBundles(RunCheckBundles request) {
		return registry.listMatchingProcessorEntries(request);
	}

	private Future<CheckResult> evaluateParallel(CheckProcessorEntry entry, ServiceRequestContext context) {
		Callable<CheckResult> callable = () -> evaluate(entry, context);
		Callable<CheckResult> contextualizedCallable = threadContextScoping.bindContext(callable);
		return executionThreadPool.submit(contextualizedCallable);
	}

	private CheckResult evaluate(CheckProcessorEntry check, ServiceRequestContext context) {
		try {
			log.trace(() -> "Evaluating check: " + check);
			CheckResult result = check.processor().check(context);
			log.trace(() -> "Done with evaluating check: " + check.name());

			// Validate
			ListIterator<CheckResultEntry> iterator = result.getEntries().listIterator();
			while (iterator.hasNext()) {
				CheckResultEntry entry = iterator.next();

				if (entry != null) {
					if (entry.getCheckStatus() == null) {
						entry.setCheckStatus(CheckStatus.warn);

						String name = entry.getName();
						String message = entry.getMessage();

						entry.setName("Check Bundle Framework Result Entry Validation");
						entry.setMessage("CheckResultEntry.status must not be null");

						StringBuilder builder = new StringBuilder();

						String details = entry.getDetails();

						builder.append("# Original Entry Values\n");
						builder.append("The system has kept the original values of this entry:<br><br>\n");

						builder.append("\n\nEntry Property | Original Value\n");
						builder.append("--- | ---\n");
						builder.append("Name | ");
						builder.append(XmlTools.escape(name));
						builder.append('\n');
						builder.append("Message | ");
						builder.append(XmlTools.escape(message));

						if (details != null) {
							builder.append("\n\n# Original Details\n");

							if (entry.getDetailsAsMarkdown()) {
								builder.append(details);
							} else {
								builder.append("<pre>");
								builder.append(XmlTools.escape(details));
								builder.append("</pre>");
							}
						}

						entry.setDetails(builder.toString());
					}
				} 

				iterator.set(entry);
			}
			
			return result;


		} catch (Exception e) {
			log.debug(() -> "Got an exception while evaluating check: " + check.name(), e);

			CheckResultEntry entry = createFailureCheckResultEntry(e);

			CheckResult result = CheckResult.T.create();
			result.getEntries().add(entry);
			return result;
		}

	}

}
