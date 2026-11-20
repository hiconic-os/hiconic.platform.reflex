package hiconic.rx.webapi.client.processing;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.ConfigurationError;
import com.braintribe.gm.model.reason.essential.InternalError;

import hiconic.rx.webapi.client.model.meta.HttpDateEncoding;
import hiconic.rx.webapi.client.model.meta.HttpDateFormatting;

/** Scalar date encoding for HTTP parameters, independent of body marshalling options. */
final class HttpParameterDateEncoder {

	private HttpParameterDateEncoder() { }

	static String encode(Date value, HttpDateEncoding parameter, HttpDateFormatting legacy) {
		String format = parameter != null ? parameter.getDateFormat() : null;
		String zone = parameter != null ? parameter.getDateZone() : null;
		Locale locale = Locale.ROOT;
		if (isBlank(format)) {
			if (!isBlank(zone))
				throw new UnsatisfiedMaybeTunneling(Reasons.build(ConfigurationError.T)
						.text("HTTP parameter dateZone requires dateFormat.").toMaybe());
			if (legacy == null)
				return Long.toString(value.getTime());
			format = legacy.getDateFormat();
			zone = legacy.getDefaultZone();
			locale = isBlank(legacy.getDefaultLocale()) ? Locale.getDefault()
					: Locale.forLanguageTag(legacy.getDefaultLocale().replace('_', '-'));
		}

		try {
			if (isBlank(format))
				throw new IllegalArgumentException("Date format must not be blank.");
			ZoneId zoneId = isBlank(zone) ? ZoneId.systemDefault() : ZoneId.of(zone);
			return DateTimeFormatter.ofPattern(format, locale).withZone(zoneId).format(value.toInstant());
		} catch (DateTimeException | IllegalArgumentException e) {
			throw new UnsatisfiedMaybeTunneling(Reasons.build(ConfigurationError.T)
					.text("Invalid HTTP parameter date encoding: format=" + format + ", zone=" + zone)
					.cause(InternalError.from(e)).toMaybe());
		}
	}

	private static boolean isBlank(String value) { return value == null || value.isBlank(); }
}
