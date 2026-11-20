package hiconic.rx.webapi.client.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Date;

import org.junit.Test;

import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;

import hiconic.rx.webapi.client.model.meta.HttpDateFormatting;
import hiconic.rx.webapi.client.model.meta.params.HttpPathParam;

public class HttpParameterDateEncoderTest {
	private static final Date DATE = Date.from(Instant.parse("2026-11-18T23:30:00Z"));

	@Test
	public void localFormatAndZoneOverrideTheLegacyBodyConfiguration() {
		HttpPathParam parameter = HttpPathParam.T.create();
		parameter.setDateFormat("yyyy-MM-dd"); parameter.setDateZone("Europe/Berlin");
		HttpDateFormatting legacy = HttpDateFormatting.T.create();
		legacy.setDateFormat("dd/MM/yyyy HH:mm"); legacy.setDefaultZone("UTC");
		assertEquals("2026-11-19", HttpParameterDateEncoder.encode(DATE, parameter, legacy));
		parameter.setDateZone("UTC");
		assertEquals("2026-11-18", HttpParameterDateEncoder.encode(DATE, parameter, legacy));
	}

	@Test
	public void appliesTheConfiguredZoneAcrossSummerTimeBoundaries() {
		HttpPathParam parameter = HttpPathParam.T.create();
		parameter.setDateFormat("yyyy-MM-dd HH:mm XXX"); parameter.setDateZone("Europe/Berlin");
		assertEquals("2026-03-29 01:30 +01:00", HttpParameterDateEncoder.encode(Date.from(Instant.parse("2026-03-29T00:30:00Z")), parameter, null));
		assertEquals("2026-03-29 03:30 +02:00", HttpParameterDateEncoder.encode(Date.from(Instant.parse("2026-03-29T01:30:00Z")), parameter, null));
	}

	@Test
	public void retainsEpochMillisecondsWithoutAnyFormattingMetadata() {
		assertEquals(Long.toString(DATE.getTime()), HttpParameterDateEncoder.encode(DATE, HttpPathParam.T.create(), null));
		assertEquals(Long.toString(DATE.getTime()), HttpParameterDateEncoder.encode(DATE, null, null));
	}

	@Test
	public void legacyFallbackNowHonorsItsConfiguredZoneAndLocale() {
		HttpDateFormatting legacy = HttpDateFormatting.T.create();
		legacy.setDateFormat("dd MMMM yyyy"); legacy.setDefaultZone("Europe/Berlin"); legacy.setDefaultLocale("de-DE");
		assertEquals("02 März 2026", HttpParameterDateEncoder.encode(
				Date.from(Instant.parse("2026-03-01T23:30:00Z")), null, legacy));
	}

	@Test
	public void reportsInvalidConfigurationAsReasons() {
		HttpPathParam parameter = HttpPathParam.T.create(); parameter.setDateZone("UTC");
		assertReason(parameter);
		parameter.setDateFormat("yyyy-MM-dd"); parameter.setDateZone("not/a-zone");
		assertReason(parameter);
		parameter.setDateZone("UTC"); parameter.setDateFormat("yyyy-MM-dd '");
		assertReason(parameter);
	}

	private void assertReason(HttpPathParam parameter) {
		UnsatisfiedMaybeTunneling error = assertThrows(UnsatisfiedMaybeTunneling.class,
				() -> HttpParameterDateEncoder.encode(DATE, parameter, null));
		assertTrue(error.getMaybe().isUnsatisfiedBy(com.braintribe.gm.model.reason.essential.ConfigurationError.T));
	}
}
