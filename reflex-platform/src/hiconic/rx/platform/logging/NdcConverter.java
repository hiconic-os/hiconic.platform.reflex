package hiconic.rx.platform.logging;

import java.util.Deque;
import java.util.stream.Collectors;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import com.braintribe.logging.ndc.mbean.NestedDiagnosticContext;

public class NdcConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        Deque<String> ndc = NestedDiagnosticContext.getNdc();
        if (ndc == null || ndc.isEmpty()) {
            return "";
        }

        return ndc.stream()
                .filter(value -> value != null && !value.isEmpty())
                .collect(Collectors.joining(","));
    }
}
