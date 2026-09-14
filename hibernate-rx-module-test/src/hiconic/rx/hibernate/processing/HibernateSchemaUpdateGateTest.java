// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.hibernate.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.lock.impl.SimpleCdlLocking;

public class HibernateSchemaUpdateGateTest {

	private final DataSource dataSource = dataSource();
	private final Locking locking = new SimpleCdlLocking();

	@Test
	public void skipsAnAlreadyAppliedMappingFingerprint() {
		try (HibernateSchemaUpdateGate.Decision first = gate("mapping-a").decide()) {
			assertThat(first.updateRequired()).isTrue();
			first.markSuccessful();
		}

		try (HibernateSchemaUpdateGate.Decision second = gate("mapping-a").decide()) {
			assertThat(second.updateRequired()).isFalse();
		}
	}

	@Test
	public void changedMappingRequiresAnotherUpdate() {
		try (HibernateSchemaUpdateGate.Decision first = gate("mapping-a").decide()) {
			first.markSuccessful();
		}

		try (HibernateSchemaUpdateGate.Decision changed = gate("mapping-b").decide()) {
			assertThat(changed.updateRequired()).isTrue();
			changed.markSuccessful();
		}
	}

	@Test
	public void failedUpdateDoesNotAdvanceFingerprint() throws Exception {
		try (HibernateSchemaUpdateGate.Decision failed = gate("mapping-a").decide()) {
			assertThat(failed.updateRequired()).isTrue();
		}

		try (HibernateSchemaUpdateGate.Decision retry = gate("mapping-a").decide()) {
			assertThat(retry.updateRequired()).isTrue();
			retry.markSuccessful();
		}

		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT ERROR_COUNT FROM " + HibernateSchemaUpdateGate.TABLE_NAME)) {
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isZero();
		}
	}

	@Test
	public void concurrentNodeWaitsAndThenObservesSuccessfulUpdate() throws Exception {
		CountDownLatch firstHasLock = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);

		try (var executor = Executors.newFixedThreadPool(2)) {
			Future<Boolean> first = executor.submit(() -> {
				try (HibernateSchemaUpdateGate.Decision decision = gate("mapping-a").decide()) {
					firstHasLock.countDown();
					releaseFirst.await(5, TimeUnit.SECONDS);
					decision.markSuccessful();
					return decision.updateRequired();
				}
			});

			assertThat(firstHasLock.await(5, TimeUnit.SECONDS)).isTrue();
			Future<Boolean> second = executor.submit(() -> {
				try (HibernateSchemaUpdateGate.Decision decision = gate("mapping-a").decide()) {
					return decision.updateRequired();
				}
			});

			Thread.sleep(100);
			assertThat(second.isDone()).isFalse();
			releaseFirst.countDown();

			assertThat(first.get(5, TimeUnit.SECONDS)).isTrue();
			assertThat(second.get(5, TimeUnit.SECONDS)).isFalse();
		}
	}

	private HibernateSchemaUpdateGate gate(String fingerprint) {
		return new HibernateSchemaUpdateGate(dataSource, locking, "configured-model|catalog|schema|prefix", "catalog|schema|prefix", fingerprint,
				"test-node");
	}

	private static DataSource dataSource() {
		JdbcDataSource result = new JdbcDataSource();
		result.setURL("jdbc:h2:mem:hibernate-schema-gate-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		return result;
	}
}
