// ============================================================================
package hiconic.rx.explorer.processing.platformreflection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.exception.Exceptions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.Timeout;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.gm.model.security.reason.MissingSession;
import com.braintribe.gm.model.security.reason.SecurityReason;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.processing.bootstrapping.TribefireRuntime;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.FailureCodec;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ResponseEnvelope;
import com.braintribe.model.service.api.result.ServiceResult;
import com.braintribe.model.service.api.result.StillProcessing;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.processing.async.api.AsyncCallback;
import com.braintribe.provider.Box;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.StopWatch;
import com.braintribe.utils.stream.PipeBackedZippingInputStreamProvider;
import com.braintribe.utils.stream.api.StreamPipeFactory;
import com.braintribe.utils.system.SystemTools;
import com.braintribe.utils.system.exec.CommandExecution;
import com.braintribe.utils.system.exec.RunCommandContext;
import com.braintribe.utils.system.exec.RunCommandRequest;

import hiconic.rx.check.model.bundle.api.request.RunCheckBundles;
import hiconic.rx.check.model.bundle.api.response.CheckBundlesResponse;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.CollectAccessDataFolderAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.CollectConfigurationFolderAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.CollectHealthzAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.CollectPackagingInformationAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.HeapDumpAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.HotThreadsAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.PlatformReflectionJsonAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.ProcessesJsonAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.ReflectionResponseAsyncCallback.ThreadDumpAsyncCallback;
import hiconic.rx.explorer.processing.platformreflection.system.ProcessesProvider;
import hiconic.rx.explorer.processing.platformreflection.system.StandardSystemInformationProvider;
import hiconic.rx.explorer.processing.platformreflection.system.SystemInformationProvider;
import hiconic.rx.logs.model.api.GetLogs;
import hiconic.rx.logs.model.api.Logs;
import hiconic.rx.module.api.common.RxPlatform;
import hiconic.rx.reflection.model.api.AccessDataFolder;
import hiconic.rx.reflection.model.api.CollectDiagnosticPackages;
import hiconic.rx.reflection.model.api.CollectHealthz;
import hiconic.rx.reflection.model.api.ConfigurationFolder;
import hiconic.rx.reflection.model.api.DiagnosticPackage;
import hiconic.rx.reflection.model.api.DiagnosticPackages;
import hiconic.rx.reflection.model.api.GetAccessDataFolder;
import hiconic.rx.reflection.model.api.GetConfigurationFolder;
import hiconic.rx.reflection.model.api.GetDiagnosticPackage;
import hiconic.rx.reflection.model.api.GetHeapDump;
import hiconic.rx.reflection.model.api.GetHotThreads;
import hiconic.rx.reflection.model.api.GetPackagingInformation;
import hiconic.rx.reflection.model.api.GetProcesses;
import hiconic.rx.reflection.model.api.GetProcessesJson;
import hiconic.rx.reflection.model.api.GetRxAppInformation;
import hiconic.rx.reflection.model.api.GetSystemInformation;
import hiconic.rx.reflection.model.api.GetThreadDump;
import hiconic.rx.reflection.model.api.Healthz;
import hiconic.rx.reflection.model.api.PackagingInformation;
import hiconic.rx.reflection.model.api.PlatformReflection;
import hiconic.rx.reflection.model.api.PlatformReflectionJson;
import hiconic.rx.reflection.model.api.PlatformReflectionRequest;
import hiconic.rx.reflection.model.api.PlatformReflectionResponse;
import hiconic.rx.reflection.model.api.Processes;
import hiconic.rx.reflection.model.api.ProcessesJson;
import hiconic.rx.reflection.model.api.ReflectPlatform;
import hiconic.rx.reflection.model.api.ReflectPlatformJson;
import hiconic.rx.reflection.model.application.RxAppInfo;
import hiconic.rx.reflection.model.jvm.HeapDump;
import hiconic.rx.reflection.model.jvm.HotThreads;
import hiconic.rx.reflection.model.jvm.ThreadDump;
import hiconic.rx.reflection.model.system.SystemInfo;
import hiconic.rx.reflection.model.system.os.Process;

public class PlatformReflectionProcessor extends AbstractDispatchingServiceProcessor<PlatformReflectionRequest, Object>
		implements InitializationAware {

	private static Logger logger = Logger.getLogger(PlatformReflectionProcessor.class);

	public static final DateTimeFormatter fileDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withLocale(Locale.US);

	// private static final String FILENAME_REPOSITORY_VIEW_RESOLUTION_YAML = "repository-view-resolution.yaml";

	protected GmSerializationOptions serializationOptions = GmSerializationOptions.defaultOptions.derive().setOutputPrettiness(OutputPrettiness.high)
			.useDirectPropertyAccess(true).writeEmptyProperties(true).build();

	private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
	private static volatile Object hotspotMBean;

	private Set<String> allowedRoles = Collections.emptySet();

	private SystemInformationProvider systemInformationProvider;
	private Supplier<RxAppInfo> rxAppInfoProvider;

	private CommandExecution commandExecution;
	//
	// private Supplier<Packaging> packagingProvider = null;

	private String applicationId;
	private InstanceId instanceId;

	private Marshaller jsonMarshaller;
	private Marshaller checkResultMarshaller;
	//
	private String zipPassword = "operating";

	private File confFolder = null;
	private File dataFolder;

	private StreamPipeFactory streamPipeFactory;

	// private List<ClasspathContainer> cachedClasspathContainers = null;

	@Override
	protected void configureDispatching(DispatchConfiguration<PlatformReflectionRequest, Object> dispatching) {
		dispatching.register(GetPackagingInformation.T, (c, r) -> getPackagingInformation());
		dispatching.register(ReflectPlatform.T, (c, r) -> reflectPlatform(c));
		dispatching.register(ReflectPlatformJson.T, (c, r) -> reflectPlatformJson(c, r));
		dispatching.register(GetRxAppInformation.T, (c, r) -> getRxAppInformation());
		dispatching.register(GetProcesses.T, (c, r) -> getProcesses());
		dispatching.register(GetProcessesJson.T, (c, r) -> getProcessesJson(c));
		dispatching.register(GetSystemInformation.T, (c, r) -> getSystemInformation());
		// dispatching.register(GetHostInformation.T, (c, r) -> getHostInformation(c, r));
		dispatching.register(GetHotThreads.T, (c, r) -> getHotThreads(c, r));
		dispatching.register(CollectHealthz.T, (c, r) -> collectHealthz(c));
		dispatching.register(GetDiagnosticPackage.T, (c, r) -> getDiagnosticPackage(c, r));
		dispatching.register(GetHeapDump.T, (c, r) -> getHeapDump(c, r));
		dispatching.register(GetConfigurationFolder.T, (c, r) -> getConfigurationFolder(c, r));
		dispatching.register(GetAccessDataFolder.T, (c, r) -> getAccessDataFolder(c, r));
		dispatching.register(GetThreadDump.T, (c, r) -> getThreadDump());
		dispatching.register(CollectDiagnosticPackages.T, (c, r) -> collectDiagnosticPackage(c, r));
	}

	@Override
	public void postConstruct() {
		initHotspotMBean();
	}

	@Required
	public void setRxAppInfoProvider(Supplier<RxAppInfo> rxAppInfoProvider) {
		this.rxAppInfoProvider = rxAppInfoProvider;
	}

	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, PlatformReflectionRequest request) {
		SecurityReason reason = authorize(context);
		if (reason != null)
			return reason.asMaybe();

		return super.processReasoned(context, request);
	}

	private SecurityReason authorize(ServiceRequestContext context) {
		UserSession userSession = context.findAspect(UserSessionAspect.class);
		if (userSession == null)
			return Reasons.build(MissingSession.T) //
					.text("Missing UserSession information.") //
					.toReason();

		if (!isAllowed(userSession))
			return Reasons.build(Forbidden.T) //
					.text("Access denied.") //
					.toReason();

		return null;
	}

	private boolean isAllowed(UserSession userSession) {
		for (String userRole : userSession.getEffectiveRoles())
			if (allowedRoles.contains(userRole))
				return true;

		return false;
	}

	private PackagingInformation getPackagingInformation() {
		// TODO implement getPackagingInformation
		
		PackagingInformation pi = PackagingInformation.T.create();
		return pi;
	}

	private PlatformReflection reflectPlatform(ServiceRequestContext context) {
		logger.debug("Processing a ReflectPlatform request.");

		PlatformReflection pr = PlatformReflection.T.create();

		CountDownLatch countdown = new CountDownLatch(3);

		GetSystemInformation gsi = GetSystemInformation.T.create();
		this.<SystemInfo> evalAsyncWithConsumer(gsi, context, pr::setSystemInfo, countdown);

		GetRxAppInformation grai = GetRxAppInformation.T.create();
		this.<RxAppInfo> evalAsyncWithConsumer(grai, context, pr::setRxAppInfo, countdown);

		try {
			countdown.await(20L, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			logger.info("reflectPlatform: Got interrupted while waiting for multicast responses.");
		}

		return pr;
	}

	private void extractFromServiceResult(ServiceResult serviceResult, Consumer<ResponseEnvelope> envelopeConsumer, Consumer<Failure> failureConsumer,
			Consumer<StillProcessing> spConsumer) {
		if (serviceResult == null) {
			return;
		}
		if (serviceResult instanceof ResponseEnvelope) {
			ResponseEnvelope envelope = (ResponseEnvelope) serviceResult;
			envelopeConsumer.accept(envelope);
		} else if (serviceResult instanceof Failure) {
			Failure fail = (Failure) serviceResult;
			failureConsumer.accept(fail);
		} else if (serviceResult instanceof StillProcessing) {
			StillProcessing sp = (StillProcessing) serviceResult;
			spConsumer.accept(sp);
		} else {
			logger.debug("Unsupported ServiceResult type: " + serviceResult);
		}
	}

	private void extractFromServiceResult(ServiceResult serviceResult, Consumer<ResponseEnvelope> consumer) {
		extractFromServiceResult(serviceResult, consumer, failure -> {
			Throwable throwable = FailureCodec.INSTANCE.decode(failure);
			logger.debug("Received a failure: " + failure.getDetails(), throwable);
		}, stillProcessing -> {
			logger.debug("Instead of receiving a response, we received " + stillProcessing);
		});
	}

	@SuppressWarnings("unused")
	private PlatformReflectionJson reflectPlatformJson(ServiceRequestContext context, ReflectPlatformJson request) {
		logger.debug("Processing a ReflectPlatformJson request.");
		try {
			ReflectPlatform rp = ReflectPlatform.T.create();
			PlatformReflection platformReflection = unicastRequestSync(rp, context);

			if (platformReflection != null) {
				PlatformReflectionJson pr = PlatformReflectionJson.T.create();

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				jsonMarshaller.marshall(baos, platformReflection, serializationOptions);
				String json = baos.toString(StandardCharsets.UTF_8);

				pr.setPlatformReflectionJson(json);
				return pr;
			} else {
				logger.debug("Did not get a response for the ReflectPlatform request.");
			}

			return null;

		} finally {
			logger.debug("Done with processing a ReflectPlatformJson request.");
		}
	}

	private ProcessesJson getProcessesJson(ServiceRequestContext context) {
		logger.debug("Processing a GetProcessesJson request.");
		try {
			GetProcesses gp = GetProcesses.T.create();
			Processes getProcesses = unicastRequestSync(gp, context);

			if (getProcesses != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				jsonMarshaller.marshall(baos, getProcesses, serializationOptions);
				String json = baos.toString(StandardCharsets.UTF_8);

				ProcessesJson pj = ProcessesJson.T.create();
				pj.setProcessesJson(json);

				return pj;
			} else {
				logger.debug("Did not get a response for the GetProcesses request.");
				return null;
			}

		} finally {
			logger.debug("Done with processing a GetProcessesJson request.");
		}
	}

	private Processes getProcesses() {
		logger.debug("Processing a GetProcesses request.");
		try {
			oshi.SystemInfo si = new oshi.SystemInfo();

			List<Process> processList = ProcessesProvider.getProcesses(si, false);

			processList.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

			Processes processes = Processes.T.create();
			processes.setProcesses(processList);

			return processes;

		} catch (Throwable t) {
			logger.error("Error while trying to collect process information.", t);
			return null;

		} finally {
			logger.debug("Done with processing a GetProcesses request.");
		}
	}

	private SystemInfo getSystemInformation() {
		logger.debug("Processing a GetSystemInformation request.");

		SystemInformationProvider sysInfoProvider = this.getSystemInformationProvider();
		if (sysInfoProvider != null) {
			SystemInfo system = sysInfoProvider.get();

			logger.debug("Done with processing a GetSystemInformation request.");

			return system;
		}

		logger.debug("Could not process a GetSystemInformation request as no SystemInformationProvider is set.");
		return null;
	}

	private RxAppInfo getRxAppInformation() {
		logger.debug("Processing a GetSystemInformation request.");
		return rxAppInfoProvider.get();
	}

	@SuppressWarnings("unused")
	protected HotThreads getHotThreads(ServiceRequestContext context, GetHotThreads request) {

		logger.debug("Processing a GetHotThreads request.");

		HotThreads hts;
		try {
			hts = HotThreadsProvider.detectWithDefaults(request.getInterval(), request.getThreads(), request.getIgnoreIdleThreads(),
					request.getSampleType(), request.getThreadElementsSnapshotCount(), request.getThreadElementsSnapshotDelayInMs());
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Error while trying to get hot threads.");
		}

		logger.debug("Done with processing a GetHotThreads request.");

		return hts;

	}

	private static void initHotspotMBean() {
		if (hotspotMBean == null) {
			synchronized (PlatformReflectionProcessor.class) {
				if (hotspotMBean == null) {
					hotspotMBean = getHotspotMBean();
				}
			}
		}
	}
	private static Object getHotspotMBean() {
		try {
			Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			Object bean = ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
			return bean;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	@Required
	public void setAllowedRoles(Set<String> allowedRoles) {
		this.allowedRoles = allowedRoles;
	}

	private Healthz collectHealthz(ServiceRequestContext context) {
		logger.debug("Processing a CollectHealthz request.");

		Healthz result = Healthz.T.create();

		try {
			RunCheckBundles runChecks = RunCheckBundles.T.create();
			runChecks.setCoverage(null);

			// TODO make timeout configurable?
			Maybe<CheckBundlesResponse> checkResultMaybe = evalWithTimeout(runChecks, runChecks.eval(context), 2, TimeUnit.MINUTES);

			if (checkResultMaybe.isSatisfied()) {
				CheckBundlesResponse checkResult = checkResultMaybe.get();
				String html = marshall(checkResult);
				result.setCheckBundlesResponseAsHtml(html);

			} else {
				logger.info("Error while running health checks: " + checkResultMaybe.whyUnsatisfied().stringify());
			}

		} catch (InterruptedException ie) {
			logger.info("Interrupted while waiting for check results.");
			Thread.currentThread().interrupt();
		}

		return result;
	}

	private String marshall(CheckBundlesResponse checkResult) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			checkResultMarshaller.marshall(bos, checkResult);
			return new String(bos.toByteArray());

		} catch (IOException e) {
			throw new UncheckedIOException("Error while marshalling " + checkResult.entityType().getShortName(), e);
		}
	}

	// TODO extract to some core services ...
	private static <T> Maybe<T> evalWithTimeout(ServiceRequest request, EvalContext<T> evalContext, long timeout, TimeUnit unit)
			throws InterruptedException {
		Box<Maybe<T>> resultBox = new Box<>();
		Box<Throwable> errorBox = new Box<>();

		CountDownLatch countDown = new CountDownLatch(1);
		String reqName = request.entityType().getShortName();

		evalContext.getReasoned(new AsyncCallback<Maybe<T>>() {
			// @formatter:off
			@Override public void onSuccess(Maybe<T> response) { resultBox.value = response; countDown.countDown(); }
			@Override public void onFailure(Throwable error)   { errorBox .value = error;    countDown.countDown(); }
			// @formatter:on
		});

		if (!countDown.await(timeout, unit))
			return Maybe.empty(Timeout
					.create("Evaluating request [" + reqName + "] timed out after " + timeout + " " + unit.toString() + (timeout > 1 ? "s" : "")));

		if (resultBox.value != null)
			return resultBox.value;

		throw Exceptions.unchecked(errorBox.value, "Error while evaluating request [" + reqName + "]");
	}

	private DiagnosticPackages collectDiagnosticPackage(ServiceRequestContext context, CollectDiagnosticPackages request) {
		logger.debug("collectDiagnosticPackage: Processing a CollectDiagnosticPackages request.");

		DiagnosticPackages result = DiagnosticPackages.T.create();
		try {

			GetDiagnosticPackage gp = GetDiagnosticPackage.T.create();
			gp.setIncludeHeapDump(request.getIncludeHeapDump());
			gp.setIncludeLogs(request.getIncludeLogs());
			gp.setExcludeSharedStorageBinaries(request.getExcludeSharedStorageBinaries());
			gp.setPasswordProtection(false);

			Long waitTimeoutInMs = request.getWaitTimeoutInMs();
			if (waitTimeoutInMs == null || waitTimeoutInMs <= 0) {
				waitTimeoutInMs = Numbers.MILLISECONDS_PER_MINUTE * 30l;
			}
			float allWaitTimeoutFloat = waitTimeoutInMs.floatValue();
			float individualWaitTimeoutFloat = allWaitTimeoutFloat * 0.75f;
			long individualWaitTimeout = Math.max((long) individualWaitTimeoutFloat, 0l);
			gp.setWaitTimeoutInMs(individualWaitTimeout);

			logger.debug(
					"collectDiagnosticPackage: Issuing a multicast GetDiagnosticPackage request now. Timeout will be: " + waitTimeoutInMs + " ms");

			Map<InstanceId, DiagnosticPackage> resultMap = multicastRequestSync(gp, context, waitTimeoutInMs);

			if (resultMap != null && !resultMap.isEmpty()) {

				logger.debug(() -> "collectDiagnosticPackage: Received a result map with " + resultMap.size() + " entries.");

				String now = now(fileDateTimeFormatter);
				String filename = applicationId + "tribefire-diagnostic-package-" + now + ".zip";

				List<Resource> collectedResources = new ArrayList<>();
				for (Map.Entry<InstanceId, DiagnosticPackage> entry : resultMap.entrySet()) {
					InstanceId responder = entry.getKey();
					logger.debug(() -> "collectDiagnosticPackage: Processing response from " + responder.stringify());

					DiagnosticPackage dp = entry.getValue();
					Resource dpResource = dp.getDiagnosticPackage();
					collectedResources.add(dpResource);
				}

				try {
					Resource callResource = Resource.createTransient(
							new ResourceBasedEncryptedZippingInputStreamProvider(streamPipeFactory, filename, collectedResources, zipPassword));

					callResource.setName(filename);
					callResource.setMimeType("application/zip");
					callResource.setCreated(new Date());
					callResource.setCreator(RxPlatform.currentUserName());

					result.setDiagnosticPackages(callResource);

				} catch (IOException ioe) {
					throw new RuntimeException("Could not package diagnostic package.", ioe);
				}

			} else {
				logger.debug("collectDiagnosticPackage: Did not get a single response for the GetDiagnosticPackage request.");
			}

		} finally {
			logger.debug("collectDiagnosticPackage: Done with processing a CollectDiagnosticPackages request.");
		}
		return result;

	}

	private DiagnosticPackage getDiagnosticPackage(ServiceRequestContext context, GetDiagnosticPackage request) {

		StopWatch stopWatch = new StopWatch();

		logger.debug(() -> "getDiagnosticPackage: Processing a GetDiagnosticPackage request.");

		DiagnosticPackageContext dpContext = new DiagnosticPackageContext();

		boolean includeLogs = request.getIncludeLogs() != null ? request.getIncludeLogs() : false;
		boolean includeHeapDump = request.getIncludeHeapDump() != null ? request.getIncludeHeapDump() : false;

		int count = 8;
		if (includeLogs)
			count++;
		if (includeHeapDump)
			count++;

		CountDownLatch countdown = new CountDownLatch(count);

		GetThreadDump gtd = GetThreadDump.T.create();
		multicastRequestAsync(gtd, context, new ThreadDumpAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous GetThreadDump request.");

		ReflectPlatformJson rpj = ReflectPlatformJson.T.create();
		multicastRequestAsync(rpj, context, new PlatformReflectionJsonAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous PlatformReflectionJson request.");

		GetHotThreads ght = GetHotThreads.T.create();
		ght.setThreads(10);
		multicastRequestAsync(ght, context, new HotThreadsAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous HotThreads request.");

		GetProcessesJson gpj = GetProcessesJson.T.create();
		multicastRequestAsync(gpj, context, new ProcessesJsonAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous CollectHealthz request.");

		CollectHealthz ch = CollectHealthz.T.create();
		multicastRequestAsync(ch, context, new CollectHealthzAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous GetProcessesJson request.");

		GetPackagingInformation gpi = GetPackagingInformation.T.create();
		multicastRequestAsync(gpi, context, new CollectPackagingInformationAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous GetPackagingInformation request.");

		GetConfigurationFolder gcf = GetConfigurationFolder.T.create();
		EvalContext<? extends ConfigurationFolder> configurationFolderEval = gcf.eval(context);
		configurationFolderEval.get(new CollectConfigurationFolderAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous GetConfigurationFolder request.");

		GetAccessDataFolder gad = GetAccessDataFolder.T.create();
		EvalContext<? extends AccessDataFolder> accessDataFolderEval = gad.eval(context);
		accessDataFolderEval.get(new CollectAccessDataFolderAsyncCallback(dpContext, countdown));

		logger.debug("getDiagnosticPackage: Issued an asynchronous GetSharedStorage request.");

		if (includeHeapDump) {
			GetHeapDump ghd = GetHeapDump.T.create();
			EvalContext<? extends HeapDump> heapDumpEval = ghd.eval(context);
			heapDumpEval.get(new HeapDumpAsyncCallback(dpContext, countdown));

			logger.debug("getDiagnosticPackage: Issued an asynchronous GetHeapDump request.");
		}

		if (includeLogs) {
			GetLogs gl = GetLogs.T.create();
			gl.setFilename("*");
			EvalContext<? extends Logs> logsEval = gl.eval(context);
			logsEval.get(new LogsAsyncCallback(dpContext, countdown));

			logger.debug("getDiagnosticPackage: Issued an asynchronous GetLogs request.");
		}

		stopWatch.intermediate("Triggered internal requests");

		final int totalWaitCount = count;
		logger.debug(() -> "getDiagnosticPackage: Waiting for " + totalWaitCount + " asynchronous responses.");

		Long waitTimeoutInMs = request.getWaitTimeoutInMs();
		try {
			if (waitTimeoutInMs == null || waitTimeoutInMs < 0l) {
				countdown.await();
			} else {
				if (!countdown.await(waitTimeoutInMs, TimeUnit.MILLISECONDS)) {
					String msg = "Diagnostic package may be incomplete as the timeout of " + waitTimeoutInMs + " was exceeded.";
					logger.debug(() -> msg);
					dpContext.errors.add(msg);
				}
			}
		} catch (InterruptedException e) {
			logger.info("getDiagnosticPackage: Got interrupted while waiting for the platform reflection results.");
		}

		stopWatch.intermediate("Got " + totalWaitCount + " result");

		logger.debug(() -> "getDiagnosticPackage: Done waiting for " + totalWaitCount + " asynchronous responses.");

		DiagnosticPackage dp = DiagnosticPackage.T.create();
		dp.setInstanceId(instanceId);

		String now = now(fileDateTimeFormatter);
		String nodeId = TribefireRuntime.getProperty(TribefireRuntime.ENVIRONMENT_NODE_ID);
		if (nodeId == null || nodeId.trim().length() == 0) {
			nodeId = "tribefire";
		} else {
			nodeId = FileTools.normalizeFilename(nodeId, '_');
		}

		Map<String, File> files = new LinkedHashMap<>();

		if (dpContext.healthz != null) {
			String htmlString = dpContext.healthz.getCheckBundlesResponseAsHtml();
			if (htmlString != null) {
				File processesJsonFile = writeToTempFile("healthz-" + now, htmlString);
				files.put("healthz-" + now + ".html", processesJsonFile);
			}
		}
		if (dpContext.packagingInformation != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			jsonMarshaller.marshall(baos, dpContext.packagingInformation,
					GmSerializationOptions.defaultOptions.derive().setOutputPrettiness(OutputPrettiness.high).build());
			String packagingInformationString = baos.toString(StandardCharsets.UTF_8);
			File processesJsonFile = writeToTempFile("packagingInformation-" + now, packagingInformationString);
			files.put("packagingInformation-" + now + ".json", processesJsonFile);
		}
		if (dpContext.setupDescriptorResource != null) {
			File setupDescriptorYamlFile = writeToTempFile("setupDescriptor-" + now, dpContext.setupDescriptorResource);
			files.put("setupDescriptor-" + now + ".yaml", setupDescriptorYamlFile);
		}

		File reflectionJsonFile = writeToTempFile("platformReflectionJson-" + now, dpContext.platformReflectionJson);
		File processesJsonFile = writeToTempFile("processesJson-" + now, dpContext.processesJson);
		File threadDumpFile = writeToTempFile("threaddump-" + now, dpContext.threadDump);
		File hotThreadsFile = writeToTempFile("hotThreads-" + now, dpContext.hotThreads);
		File setupAssetsFile = writeToTempFile("setupAssets-" + now, dpContext.setupAssetsAsJson);
		files.put("platformReflectionJson-" + now + ".json", reflectionJsonFile);
		files.put("processesJson-" + now + ".json", processesJsonFile);
		files.put("threaddump-" + now + ".txt", threadDumpFile);
		files.put("hotThreads-" + now + ".txt", hotThreadsFile);
		files.put("setupAssets-" + now + ".json", setupAssetsFile);

		if (includeLogs) {
			if (dpContext.logs != null) {
				files.put(dpContext.logsFilename, dpContext.logs);
			}
		}
		if (includeHeapDump) {
			if (dpContext.heapDump != null) {
				files.put(dpContext.heapDumpFilename, dpContext.heapDump);
			}
		}
		if (dpContext.configurationFolderAsZip != null) {
			files.put(dpContext.configurationFolderAsZipFilename, dpContext.configurationFolderAsZip);
		}
		if (dpContext.modulesFolderAsZip != null) {
			files.put(dpContext.modulesFolderAsZipFilename, dpContext.modulesFolderAsZip);
		}
		if (dpContext.sharedStorageAsZip != null) {
			files.put(dpContext.sharedStorageAsZipFilename, dpContext.sharedStorageAsZip);
		}
		if (dpContext.accessDataFolderAsZip != null) {
			files.put(dpContext.accessDataFolderAsZipFilename, dpContext.accessDataFolderAsZip);
		}

		if (!dpContext.errors.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			dpContext.errors.forEach(e -> sb.append(e).append("\n"));
			File errorFile = writeToTempFile("reflectionErrors-" + now, sb.toString());
			files.put("reflectionErrors-" + now + ".txt", errorFile);
		}

		stopWatch.intermediate("File Prep");

		String filename = "tribefire-diagnostic-package-" + nodeId + "-" + now + ".zip";

		logger.debug(() -> "getDiagnosticPackage: Embedding the diagnostic package directly in the response.");
		Resource callResource = null;
		try {
			String pwd = request.getPasswordProtection() ? zipPassword : null;
			callResource = Resource.createTransient(new EncryptedZippingInputStreamProvider(streamPipeFactory,
					"tribefire-diagnostic-package-" + nodeId + "-" + now, files, true, pwd));
		} catch (IOException ioe) {
			throw new RuntimeException("Could not package diagnostic package.", ioe);
		}
		callResource.setName(filename);
		callResource.setMimeType("application/zip");
		callResource.setCreated(new Date());
		callResource.setCreator(RxPlatform.currentUserName());

		dp.setDiagnosticPackage(callResource);

		logger.debug("getDiagnosticPackage: Done with processing a GetDiagnosticPackage request: " + stopWatch);

		return dp;
	}

	private File writeToTempFile(String name, Resource resource) {
		try (InputStream in = resource.openStream()) {
			String content = IOTools.slurp(in, "UTF-8");
			return writeToTempFile(name, content);
		} catch (IOException e) {
			throw Exceptions.unchecked(e, "Could not open stream of resource " + resource);
		}
	}

	private File writeToTempFile(String name, String content) {
		File tempFile = null;
		try {
			if (content == null) {
				content = "empty";
			}
			tempFile = File.createTempFile(name, ".txt");
			tempFile.delete();
			FileTools.deleteFileWhenOrphaned(tempFile);
			FileTools.writeStringToFile(tempFile, content, "UTF-8");
			return tempFile;
		} catch (Exception e) {
			logger.error("Could not write content " + content + " to file " + tempFile, e);
			return null;
		}
	}

	@SuppressWarnings("unused")
	private HeapDump getHeapDump(ServiceRequestContext context, GetHeapDump request) {

		logger.debug("Processing a GetHeapDump request.");

		try {
			HeapDump hd = HeapDump.T.create();

			String now = now(fileDateTimeFormatter);
			File headDumpFile = File.createTempFile("heapdump-" + now + "-", ".hprof");
			headDumpFile.delete();
			FileTools.deleteFileWhenOrphaned(headDumpFile);

			Boolean liveObjectOnly = request.getLiveObjectOnly();
			if (liveObjectOnly == null) {
				liveObjectOnly = true;
			}
			try {
				Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
				Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
				m.invoke(hotspotMBean, headDumpFile.getAbsolutePath(), liveObjectOnly.booleanValue());
			} catch (Exception exp) {
				throw new Exception("Error while using the hotspot bean to create a heap dump.", exp);
			}

			Map<String, File> files = new HashMap<>();
			files.put("heapdump-" + now + ".hprof", headDumpFile);
			Resource callResource = Resource
					.createTransient(new PipeBackedZippingInputStreamProvider(streamPipeFactory, "heapdump-" + now, files, true));
			callResource.setName("heapdump-" + now + ".zip");
			callResource.setMimeType("application/zip");
			callResource.setCreated(new Date());
			callResource.setCreator(RxPlatform.currentUserName());
			hd.setHeapDump(callResource);

			return hd;
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not acquire a heapdump.");
		} finally {
			logger.debug("Done with processing a GetHeapDump request.");
		}
	}

	@SuppressWarnings("unused")
	private ConfigurationFolder getConfigurationFolder(ServiceRequestContext context, GetConfigurationFolder request) {

		logger.debug("Processing a GetConfigurationFolder request.");

		try {
			ConfigurationFolder hd = ConfigurationFolder.T.create();

			if (confFolder != null) {

				String filename = "conf-" + now(fileDateTimeFormatter) + ".zip";

				List<File> list = FileTools.listFilesRecursively(confFolder);

				String basePath = confFolder.getAbsolutePath();
				logger.debug(() -> "Base configuration folder pathInfo: " + basePath);
				int basePathLength = basePath.length();
				if (!basePath.endsWith("/") && !basePath.endsWith("\\")) {
					basePathLength++;
				}
				if (logger.isDebugEnabled())
					logger.debug("Base configuration folder pathInfo length (with ending '/'): " + basePathLength);
				Map<String, File> map = new LinkedHashMap<>();
				for (File f : list) {
					String fullPath = f.getAbsolutePath();
					logger.debug(() -> "Evaluating full pathInfo: " + fullPath);
					if (fullPath.length() > basePathLength) {
						String relPath = fullPath.substring(basePathLength);
						logger.debug(() -> "Including relative pathInfo: " + fullPath);
						map.put(relPath, f);
					}
				}

				if (!map.isEmpty()) {
					Resource callResource = Resource
							.createTransient(new PipeBackedZippingInputStreamProvider(streamPipeFactory, filename, map, false));

					callResource.setName(filename);
					callResource.setMimeType("application/zip");
					callResource.setCreated(new Date());
					callResource.setCreator(RxPlatform.currentUserName());

					hd.setConfigurationFolderAsZip(callResource);
				}
			}

			return hd;
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not collect the files in the configuration folder: " + confFolder);
		} finally {
			logger.debug("Done with processing a GetConfigurationFolder request.");
		}
	}

	@SuppressWarnings("unused")
	private AccessDataFolder getAccessDataFolder(ServiceRequestContext context, GetAccessDataFolder request) {

		logger.debug("Processing a GetAccessDataFolder request.");

		try {
			AccessDataFolder hd = AccessDataFolder.T.create();

			if (dataFolder != null && dataFolder.exists()) {

				String now = now(fileDateTimeFormatter);

				String filename = instanceId.getApplicationId() + "-data-" + now + ".zip";

				List<File> list = FileTools.listFilesRecursively(dataFolder);

				String basePath = dataFolder.getAbsolutePath();
				int basePathLength = basePath.length();
				if (!basePath.endsWith("/") && !basePath.endsWith("\\")) {
					basePathLength++;
				}
				Map<String, File> map = new LinkedHashMap<>();
				for (File f : list) {
					String fullPath = f.getAbsolutePath();
					if (fullPath.length() > basePathLength) {
						String relPath = fullPath.substring(basePathLength);
						map.put(relPath, f);
					}
				}

				if (!map.isEmpty()) {
					Resource callResource = Resource
							.createTransient(new PipeBackedZippingInputStreamProvider(streamPipeFactory, filename, map, false));

					callResource.setName(filename);
					callResource.setMimeType("application/zip");
					callResource.setCreated(new Date());
					callResource.setCreator(RxPlatform.currentUserName());

					hd.setAccessDataFolderAsZip(callResource);
				}
			}

			return hd;
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "Could not collect the files in the configuration folder.");
		} finally {
			logger.debug("Done with processing a GetConfigurationFolder request.");
		}
	}
	//
	// private void writeResource(Map<String, File> map, String indexString, CsaResourceBasedOperation rbo, Resource payload) {
	// String payloadName = payload.getName();
	// if (StringTools.isBlank(payloadName)) {
	// payloadName = "payload.bin";
	// }
	//
	// String resourceFilename = indexString + "-" + rbo.operationType() + "-" + FileTools.normalizeFilename(payloadName, '_');
	//
	// final File outputResourceFile;
	// try {
	// outputResourceFile = File.createTempFile(resourceFilename, ".tmp");
	// outputResourceFile.delete();
	// } catch (IOException ioe) {
	// logger.warn(() -> "Error while trying to create a temporary file.", ioe);
	// return;
	// }
	//
	// try (InputStream in = payload.openStream(); OutputStream os = new BufferedOutputStream(new FileOutputStream(outputResourceFile))) {
	// IOTools.pump(in, os);
	// map.put(resourceFilename, outputResourceFile);
	// } catch (Exception e) {
	// logger.warn(() -> "Error while trying to stream the payload of CsaResourceBasedOperation " + rbo + " to "
	// + outputResourceFile.getAbsolutePath(), e);
	// }
	// }
	//
	// private class LazyResourceReference {
	// String indexString;
	// CsaResourceBasedOperation rbo;
	//
	// public LazyResourceReference(String indexString, CsaResourceBasedOperation rbo) {
	// this.indexString = indexString;
	// this.rbo = rbo;
	// }
	// }

	private ThreadDump getThreadDump() {

		ThreadDump td = ThreadDump.T.create();

		// First, we try to get a native thread dump. If that does not work out (e.g., no JDK is installed), we use a
		// minimal Java-internal dump

		String dump = getThreadDumpNative();
		if (dump == null || dump.trim().length() == 0) {
			dump = getThreadDumpJava();
		}
		td.setThreadDump(dump);
		return td;
	}

	private String getThreadDumpNative() {
		logger.debug("Trying to create a native thread dump.");
		try {
			RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
			String localProcessName = runtimeMXBean.getName();
			int idx = localProcessName.indexOf('@');
			int pid = -1;
			if (idx != -1) {
				try {
					pid = Integer.parseInt(localProcessName.substring(0, idx));
				} catch (Exception e) {
					logger.debug(() -> "Could not get the local PID from the process name " + localProcessName);
					return null;
				}
			}
			if (pid == -1) {
				logger.debug("The process name " + localProcessName + " does not provide the PID.");
				return null;
			}
			String javaHome = System.getProperty("java.home");
			File javaHomeDir = new File(javaHome);

			String threadDump = null;
			File jcmdExecutable = findJCmd(javaHomeDir);
			if (jcmdExecutable != null) {
				List<String> contentList = new ArrayList<>();
				File tempFile = null;
				try {
					tempFile = Files.createTempFile("threaddump-" + UUID.randomUUID().toString(), ".txt").toFile();
					if (tempFile.exists()) {
						tempFile.delete();
					}
					String content = getThreadDumpNativeWithFileOutput(jcmdExecutable, pid, tempFile, "" + pid, "Thread.dump_to_file", "-format=json",
							tempFile.getAbsolutePath());
					if (!StringTools.isBlank(content)) {
						contentList.add(content);
					}
				} catch (Exception e) {
					logger.debug(() -> "Could not execute: Thread.dump_to_file", e);
				} finally {
					if (tempFile != null) {
						FileTools.deleteFileSilently(tempFile);
					}
				}

				String content = getThreadDumpNative(jcmdExecutable, pid, "" + pid, "Thread.print");
				if (!StringTools.isBlank(content)) {
					contentList.add(content);
				}

				if (!contentList.isEmpty()) {
					threadDump = contentList.stream().collect(Collectors.joining("\n\n\n"));
				}
			}
			if (threadDump == null) {
				File jstackExecutable = findJStack(javaHomeDir);
				threadDump = getThreadDumpNative(jstackExecutable, pid, "-l", "" + pid);
			}
			return threadDump;

		} catch (Exception e) {
			logger.debug(() -> "Could not get the native thread dump.", e);
		}
		return null;
	}

	private String getThreadDumpNative(File executable, int pid, String... arguments) {
		if (executable == null) {
			return null;
		}
		logger.debug(() -> "Using executable " + executable.getAbsolutePath() + " to get a thread dump from process " + pid);

		try {
			List<String> execParts = new ArrayList<>(arguments.length + 1);
			execParts.add(executable.getAbsolutePath());
			CollectionTools.addElementsToCollection(execParts, arguments);

			RunCommandRequest request = new RunCommandRequest(execParts.toArray(new String[0]), 5000L);
			RunCommandContext context = commandExecution.runCommand(request);
			int errorCode = context.getErrorCode();
			if (errorCode == 0) {

				logger.debug("Creating a native thread dump succeeded.");

				String output = context.getOutput();
				if (StringTools.isBlank(output)) {
					return null;
				}
				return output;
			} else {
				logger.debug(() -> "Executing " + executable.getAbsolutePath() + " with PID " + pid + " resulted in: " + context.toString());
			}
		} catch (Exception e) {
			logger.debug(() -> "Could not get the native thread dump using " + executable.getAbsolutePath(), e);
		}
		return null;
	}

	private String getThreadDumpNativeWithFileOutput(File executable, int pid, File targetFile, String... arguments) {
		if (executable == null) {
			return null;
		}
		logger.debug(() -> "Using executable " + executable.getAbsolutePath() + " to get a thread dump from process with file output " + pid);

		try {
			List<String> execParts = new ArrayList<>(arguments.length + 1);
			execParts.add(executable.getAbsolutePath());
			CollectionTools.addElementsToCollection(execParts, arguments);

			RunCommandRequest request = new RunCommandRequest(execParts.toArray(new String[0]), 5000L);
			RunCommandContext context = commandExecution.runCommand(request);
			int errorCode = context.getErrorCode();
			if (errorCode == 0) {

				logger.debug("Creating a native thread dump succeeded.");

				if (targetFile.exists()) {
					logger.debug(() -> "Found target file: " + targetFile.getName() + ", length: " + targetFile.length());
					String output = IOTools.slurp(targetFile, "UTF-8");
					if (StringTools.isBlank(output)) {
						return null;
					}
					return output;
				}
				logger.debug(() -> "Supposed target file " + targetFile.getAbsolutePath() + " does not exist.");

				return null;
			} else {
				logger.debug(() -> "Executing " + executable.getAbsolutePath() + " with PID " + pid + " resulted in: " + context.toString());
			}
		} catch (Exception e) {
			logger.debug(() -> "Could not get the native thread dump using " + executable.getAbsolutePath(), e);
		}
		return null;
	}

	private File findJStack(File javaHomeDir) {
		return findJavaBinExecutable(javaHomeDir, "jstack");
	}

	private File findJCmd(File javaHomeDir) {
		return findJavaBinExecutable(javaHomeDir, "jcmd");
	}

	private File findJavaBinExecutable(File javaHomeDir, String execName) {
		if (!javaHomeDir.exists()) {
			return null;
		}
		String executable = SystemTools.isWindows() ? execName + ".exe" : execName;

		List<File> toInspectList = new ArrayList<>();
		toInspectList.add(javaHomeDir.getParentFile());
		while (!toInspectList.isEmpty()) {
			File dir = toInspectList.remove(0);
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) {
						toInspectList.add(f);
					} else {
						String name = f.getName().toLowerCase();
						if (name.equals(executable)) {
							logger.debug(() -> "Found " + execName + " at: " + f.getAbsolutePath());
							return f;
						}
					}
				}
			}
		}

		logger.debug(() -> "Could not find " + execName);

		return null;
	}

	private String getThreadDumpJava() {

		logger.debug("Trying to create a thread dump within the JVM.");

		try {
			final StringBuilder dump = new StringBuilder();
			final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
			final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);

			Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
			Map<Long, Map.Entry<Thread, StackTraceElement[]>> threadMap = new HashMap<>();
			for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
				threadMap.put(entry.getKey().threadId(), entry);
			}

			dump.append(now(DateTools.ISO8601_DATE_FORMAT));
			dump.append('\n');
			dump.append("Full thread dump");

			RuntimeMXBean rmx = ManagementFactory.getRuntimeMXBean();
			String vmName = rmx.getVmName();
			if (vmName != null) {
				dump.append(' ');
				dump.append(vmName);
				String vmVersion = rmx.getVmVersion();
				if (vmVersion != null) {
					dump.append(" (");
					dump.append(vmVersion);
					dump.append(')');
				}
			}
			dump.append("\n\n");

			for (ThreadInfo threadInfo : threadInfos) {

				String threadName = threadInfo.getThreadName();
				Map.Entry<Thread, StackTraceElement[]> entry = threadMap.get(threadInfo.getThreadId());
				if (entry != null) {

					Thread thread = entry.getKey();

					dump.append(String.format("\"%s\" %sprio=%d tid=%d nid=1 %s\n   java.lang.Thread.State: %s", threadName,
							(thread.isDaemon() ? "daemon " : ""), thread.getPriority(), thread.threadId(),
							Thread.State.WAITING.equals(thread.getState()) ? "in Object.wait()" : thread.getState().name().toLowerCase(),
							(thread.getState().equals(Thread.State.WAITING) ? "WAITING (on object monitor)" : thread.getState())));

					final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
					for (final StackTraceElement stackTraceElement : stackTraceElements) {
						dump.append("\n\tat ");
						dump.append(stackTraceElement);
					}

					dump.append("\n\n");

				}
			}

			logger.debug("Successfully created a thread dump within the JVM.");

			return dump.toString();
		} catch (Exception e) {
			logger.error("Could not create a thread dump.", e);
			return null;
		}
	}

	private <T extends PlatformReflectionResponse> void evalAsyncWithConsumer(PlatformReflectionRequest request, ServiceRequestContext context,
			Consumer<T> onSuccess, CountDownLatch countdown) {

		logger.trace(() -> "Evaluating asynchronously: " + request);

		EvalContext<? extends PlatformReflectionResponse> eval = request.eval(context);
		eval.get(new AsyncCallback<PlatformReflectionResponse>() {

			@Override
			public void onSuccess(PlatformReflectionResponse response) {
				try {
					onSuccess.accept((T) response);
				} finally {
					countdown.countDown();
				}
			}

			@Override
			public void onFailure(Throwable t) {
				try {
					logger.warn(() -> "Received a failure for request " + request, t);
				} finally {
					countdown.countDown();
				}
			}

		});
	}

	private <T extends GenericEntity> void multicastRequestAsync(ServiceRequest request, ServiceRequestContext context,
			final AsyncCallback<T> callback) {

		MulticastRequest mcR = MulticastRequest.T.create();
		mcR.setSessionId(context.getRequestorSessionId());
		mcR.setServiceRequest(request);
		mcR.setAddressee(this.instanceId);
		// mcR.setTimeout((long) Numbers.MILLISECONDS_PER_MINUTE);
		mcR.setTimeout((long) Numbers.MILLISECONDS_PER_SECOND * 20);
		EvalContext<? extends MulticastResponse> eval = mcR.eval(context);
		logger.trace(() -> "Sending " + request + " as an asynchronous multicast request.");

		eval.get(new AsyncCallback<MulticastResponse>() {

			@Override
			public void onSuccess(MulticastResponse multicastResponse) {
				// Just expecting a single response as we are sending the request to just this instance
				Map<InstanceId, ServiceResult> responses = multicastResponse.getResponses();
				if (responses.size() > 1) {
					logger.debug(() -> "Received " + responses.size() + " responses. Expected just one.");
				}
				boolean receivedResponse = false;
				for (Map.Entry<InstanceId, ServiceResult> entry : responses.entrySet()) {

					InstanceId instanceId = entry.getKey();

					logger.trace(() -> "Received a response from instance: " + instanceId);

					String nodeId = instanceId.getNodeId();
					String appId = instanceId.getApplicationId();

					if (nodeId != null && nodeId.equals(instanceId.getNodeId()) && appId != null
							&& appId.equals(instanceId.getApplicationId())) {

						logger.trace(() -> "Accepting answer from " + instanceId);
						receivedResponse = true;

						extractFromServiceResult(entry.getValue(), envelope -> {
							T result = (T) envelope.getResult();
							logger.trace(() -> "Trying to forward answer from " + instanceId);
							callback.onSuccess(result);
							logger.trace(() -> "Successfully forwarded answer from " + instanceId);
						}, failure -> {
							logger.trace(() -> "Failure received from " + instanceId);
							Throwable throwable = FailureCodec.INSTANCE.decode(failure);
							callback.onFailure(throwable);
						}, stillProcessing -> {
							logger.debug(() -> "Still processing received from " + instanceId);
						});

					}
				}

				if (!receivedResponse) {
					logger.debug(() -> "Did not receive an answer from " + instanceId);

					callback.onFailure(new Exception("No response received from the intended recipient."));
				}
			}

			@Override
			public void onFailure(Throwable t) {
				callback.onFailure(t);
			}

		});

	}

	private <T extends GenericEntity> T unicastRequestSync(ServiceRequest request, ServiceRequestContext context) {

		MulticastRequest mcR = MulticastRequest.T.create();
		mcR.setSessionId(context.getRequestorSessionId());
		mcR.setServiceRequest(request);
		mcR.setAddressee(this.instanceId);
		// mcR.setTimeout((long) Numbers.MILLISECONDS_PER_MINUTE);
		mcR.setTimeout((long) Numbers.MILLISECONDS_PER_SECOND * 60);
		EvalContext<? extends MulticastResponse> eval = mcR.eval(context);
		logger.trace(() -> "Sending " + request + " as a synchronous multicast request.");

		MulticastResponse multicastResponse = eval.get();

		// Just expecting a single response as we are sending the request to just this instance
		Map<InstanceId, ServiceResult> responses = multicastResponse.getResponses();
		if (responses.size() > 1) {
			logger.debug(() -> "Received " + responses.size() + " responses. Expected just one.");
		}
		for (Map.Entry<InstanceId, ServiceResult> entry : responses.entrySet()) {

			InstanceId instanceId = entry.getKey();

			logger.trace(() -> "Received a response from instance: " + instanceId);

			String nodeId = instanceId.getNodeId();
			String appId = instanceId.getApplicationId();

			if (nodeId != null && nodeId.equals(instanceId.getNodeId()) && appId != null && appId.equals(instanceId.getApplicationId())) {

				logger.trace(() -> "Accepting answer from " + instanceId);

				Object[] result = new Object[1];
				extractFromServiceResult(entry.getValue(), envelope -> {
					result[0] = envelope.getResult();
				});

				return (T) result[0];
			}
		}
		logger.debug(() -> "Did not receive an answer from " + instanceId);

		return null;
	}

	private <T extends GenericEntity> Map<InstanceId, T> multicastRequestSync(ServiceRequest request, ServiceRequestContext context,
			Long waitTimeoutInMs) {

		MulticastRequest mcR = MulticastRequest.T.create();
		mcR.setSessionId(context.getRequestorSessionId());
		mcR.setServiceRequest(request);
		mcR.setTimeout(waitTimeoutInMs);
		EvalContext<? extends MulticastResponse> eval = mcR.eval(context);
		logger.trace(() -> "Sending " + request + " as a synchronous multicast request.");

		Map<InstanceId, T> responseMap = new ConcurrentHashMap<>();
		MulticastResponse multicastResponse = eval.get();

		Map<InstanceId, ServiceResult> responses = multicastResponse.getResponses();

		for (Map.Entry<InstanceId, ServiceResult> entry : responses.entrySet()) {

			InstanceId instanceId = entry.getKey();

			logger.trace(() -> "Received a response from instance: " + instanceId);

			ServiceResult result = entry.getValue();
			if (result instanceof Failure) {
				Throwable throwable = FailureCodec.INSTANCE.decode(result.asFailure());
				logger.error("Received failure from " + instanceId, throwable);
			} else if (result instanceof ResponseEnvelope) {

				ResponseEnvelope envelope = (ResponseEnvelope) result;
				T resultPayload = (T) envelope.getResult();
				responseMap.put(instanceId, resultPayload);

			} else {
				logger.error("Unsupported response type: " + result);
			}

		}
		return responseMap;

	}

	private SystemInformationProvider getSystemInformationProvider() {
		if (systemInformationProvider == null) {
			systemInformationProvider = new StandardSystemInformationProvider();
		}
		return systemInformationProvider;
	}

	@Configurable
	public void setSystemInformationProvider(SystemInformationProvider systemInformationProvider) {
		this.systemInformationProvider = systemInformationProvider;
	}

	// public HostInformationProvider getHostInformationProvider() {
	// if (hostInformationProvider == null) {
	//
	// String hostIdentification = hostDetector != null ? hostDetector.hostIdentification() : null;
	// logger.debug(() -> "Identified host: " + hostIdentification);
	// if (hostIdentification != null) {
	// for (String key : this.hostInformationProviderMap.keySet()) {
	// try {
	// if (key.equalsIgnoreCase(hostIdentification) || hostIdentification.matches(key)) {
	// hostInformationProvider = this.hostInformationProviderMap.get(key);
	// logger.debug(() -> "Selected host information provider: " + hostInformationProvider);
	// break;
	// }
	// } catch (PatternSyntaxException pse) {
	// logger.trace(() -> "Could not use key " + key + " as regex.", pse);
	// }
	// }
	// }
	//
	// if (hostInformationProvider == null) {
	// logger.info(() -> "Could not detect the current host. Using the default host information provider.");
	// hostInformationProvider = new TomcatHostInformationProvider();
	// }
	// }
	// return hostInformationProvider;
	// }
	// @Configurable
	// public void setHostInformationProviderMap(Map<String, HostInformationProvider> hostInformationProviderMap) {
	// this.hostInformationProviderMap = hostInformationProviderMap;
	// }
	// @Configurable
	// public void setHostInformationProvider(HostInformationProvider hostInformationProvider) {
	// this.hostInformationProvider = hostInformationProvider;
	// }
	//
	// public TribefireInformationProvider getTribefireInformationProvider() {
	// return tribefireInformationProvider;
	// }
	// @Configurable
	// @Required
	// public void setTribefireInformationProvider(TribefireInformationProvider tribefireInformationProvider) {
	// this.tribefireInformationProvider = tribefireInformationProvider;
	// }
	//
	// @Configurable
	// public void setHostDetector(HostDetector hostDetector) {
	// this.hostDetector = hostDetector;
	// }
	//
	@Required
	public void setCommandExecution(CommandExecution commandExecution) {
		this.commandExecution = commandExecution;
	}

	// @Configurable
	// @Required
	// public void setPackagingProvider(Supplier<Packaging> packagingProvider) {
	// this.packagingProvider = packagingProvider;
	// }
	
	@Required
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}
	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}
	// @Configurable
	// @Required
	// public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
	// this.evaluator = evaluator;
	// }
	@Required
	public void setJsonMarshaller(Marshaller jsonMarshaller) {
		this.jsonMarshaller = jsonMarshaller;
	}
	@Required
	public void setCheckResultMarshaller(Marshaller checkResultMarshaller) {
		this.checkResultMarshaller = checkResultMarshaller;
	}
	@Configurable
	public void setZipPassword(String zipPassword) {
		if (!StringTools.isBlank(zipPassword))
			this.zipPassword = zipPassword;
	}
	@Configurable
	public void setConfFolder(File confFolder) {
		this.confFolder = confFolder;
	}
	@Configurable
	public void setDatabaseFolder(File databaseFolder) {
		this.dataFolder = databaseFolder;
	}
	@Required
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = streamPipeFactory;
	}

	private String now(DateTimeFormatter format) {
		return DateTools.encode(new Date(), format);
	}

}
