package hiconic.rx.platform.logging;

import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.braintribe.logging.ndc.mbean.NestedDiagnosticContext;

public class MdcConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = NestedDiagnosticContext.getMdc();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }

        return mdc.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .filter(value -> value != null && !value.isEmpty())
                .collect(Collectors.joining(","));
    }
}
