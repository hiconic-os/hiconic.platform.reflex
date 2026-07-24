package hiconic.rx.log.reflection.demo.processing;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;

/**
 * Produces deterministic, recognizable log events for exercising log reflection locally.
 * This class lives in an explicitly optional demo module and is never pulled into a
 * production setup unless that setup depends on the module.
 */
public class DemoLogProducer {
	private static final Logger inventoryLog = logger("demo.logref.inventory");
	private static final Logger pricingLog = logger("demo.logref.pricing");
	private static final Logger ordersLog = logger("demo.logref.orders");
	private static final Logger persistenceLog = logger("demo.logref.persistence");
	private static final Logger settlementLog = logger("demo.logref.settlement");

	private final AtomicLong sequence = new AtomicLong();

	private ScheduledExecutorService scheduler;
	private long intervalMillis = 1_000;
	private volatile ScheduledFuture<?> task;

	private static Logger logger(String name) {
		return Logger.getLogger(name, DemoLogProducer.class);
	}

	@Configurable
	@Required
	public void setScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	@Configurable
	public void setIntervalMillis(long intervalMillis) {
		if (intervalMillis < 100)
			throw new IllegalArgumentException("Demo log interval must be at least 100 ms");
		this.intervalMillis = intervalMillis;
	}

	public synchronized void start() {
		if (task != null)
			return;

		task = scheduler.scheduleWithFixedDelay(this::emitSafely, 0, intervalMillis, TimeUnit.MILLISECONDS);
	}

	public synchronized void stop() {
		if (task == null)
			return;

		task.cancel(false);
		task = null;
	}

	private void emitSafely() {
		try {
			emitNext(sequence.incrementAndGet());
		} catch (RuntimeException e) {
			settlementLog.error("The synthetic log producer itself failed", e);
		}
	}

	private void emitNext(long currentSequence) {
		String runId = "demo-" + (currentSequence / 5);

		switch ((int) ((currentSequence - 1) % 5)) {
			case 0 -> log(inventoryLog, "inventory-scan", runId, currentSequence,
					() -> inventoryLog.trace("Inspected 12 catalog partitions; no changes detected"));
			case 1 -> log(pricingLog, "price-calculation", runId, currentSequence,
					() -> pricingLog.debug("Calculated quote PV-" + currentSequence + " using 7 pricing rules"));
			case 2 -> log(ordersLog, "order-import", runId, currentSequence,
					() -> ordersLog.info("Imported synthetic order ORD-" + currentSequence + " for tenant DEMO"));
			case 3 -> log(persistenceLog, "connection-pressure", runId, currentSequence,
					() -> persistenceLog.warn("Connection pool reached 80% utilization\nSynthetic follow-up line for multiline rendering"));
			case 4 -> log(settlementLog, "settlement-failure", runId, currentSequence,
					() -> settlementLog.error("Settlement of synthetic payment PAY-" + currentSequence + " failed",
							syntheticException(currentSequence)));
			default -> throw new IllegalStateException("Unexpected demo log cycle");
		}
	}

	private static void log(Logger logger, String scenario, String runId, long currentSequence, Runnable action) {
		logger.put("demo", "true");
		logger.put("demoScenario", scenario);
		logger.put("demoRunId", runId);
		logger.put("demoSequence", Long.toString(currentSequence));
		try {
			action.run();
		} finally {
			logger.remove("demo");
			logger.remove("demoScenario");
			logger.remove("demoRunId");
			logger.remove("demoSequence");
		}
	}

	private static RuntimeException syntheticException(long currentSequence) {
		IllegalArgumentException cause = new IllegalArgumentException("Synthetic gateway rejected sequence " + currentSequence);
		return new IllegalStateException("Synthetic settlement pipeline aborted", cause);
	}
}
