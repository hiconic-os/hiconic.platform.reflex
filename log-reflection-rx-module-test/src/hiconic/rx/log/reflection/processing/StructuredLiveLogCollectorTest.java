package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.braintribe.model.logging.LogLevel;
import com.braintribe.model.service.api.InstanceId;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.api.LogRecordPage;

public class StructuredLiveLogCollectorTest {
	private LoggerContext context;
	private StructuredLiveLogCollector collector;
	private Logger logger;

	@Before
	public void setup() {
		context = new LoggerContext();
		context.setMDCAdapter(new LogbackMDCAdapter());
		context.start();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.TRACE);

		InstanceId instanceId = InstanceId.T.create();
		instanceId.setApplicationId("test-app");
		instanceId.setNodeId("node-1");

		collector = new StructuredLiveLogCollector();
		collector.setLoggerContext(context);
		collector.setInstanceId(instanceId);
		collector.setCapacity(3);
		collector.postConstruct();

		logger = context.getLogger("hiconic.test.component");
	}

	@After
	public void cleanup() {
		collector.preDestroy();
		context.stop();
	}

	@Test
	public void capturesCanonicalRecordsAndFiltersThem() {
		logger.info("first message");
		logger.warn("important warning");

		LogFilter filter = LogFilter.T.create();
		filter.setLevels(Set.of(LogLevel.WARN));
		filter.setFulltext("important");

		LogRecordPage page = collector.query(filter, null, 10, 0);

		assertThat(page.getRecords()).hasSize(1);
		assertThat(page.getRecords().get(0).getMessage()).isEqualTo("important warning");
		assertThat(page.getRecords().get(0).getLoggerName()).isEqualTo("hiconic.test.component");
		assertThat(page.getRecords().get(0).getOrigin().getApplicationId()).isEqualTo("test-app");
		assertThat(page.getRecords().get(0).getOrigin().getNodeId()).isEqualTo("node-1");
		assertThat(page.getRecords().get(0).getStreamId()).isEqualTo(StructuredLiveLogCollector.STREAM_ID);
		assertThat(page.getNextCursor()).isNotBlank();
	}

	@Test
	public void cursorContinuesAndRingBufferIsBounded() {
		logger.info("one");
		logger.info("two");
		logger.info("three");
		LogRecordPage initial = collector.query(null, null, 2, 0);
		assertThat(initial.getRecords()).extracting(r -> r.getMessage()).containsExactly("two", "three");

		String cursor = initial.getNextCursor();
		logger.info("four");
		LogRecordPage continued = collector.query(null, cursor, 10, 0);

		assertThat(continued.getRecords()).extracting(r -> r.getMessage()).containsExactly("four");
	}

	@Test
	public void filtersLoggerByCaseInsensitiveSubstring() {
		logger.info("matching");
		context.getLogger("hiconic.other.component").info("not matching");

		LogFilter filter = LogFilter.T.create();
		filter.setLoggerNameContains("TEST.COMP");

		LogRecordPage page = collector.query(filter, null, 10, 0);

		assertThat(page.getRecords()).extracting(r -> r.getMessage()).containsExactly("matching");
	}
}
