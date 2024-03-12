package hiconic.rx.platform.conf;

import java.util.function.Function;

public interface RxProperties {
	static Function<String, String> overrideLookup(Function<String, String> base, Function<String, String> override) {
		return n -> {
			var v = override.apply(n);
			if (v == null)
				v = base.apply(n);
			return v;
		};
	}
}
