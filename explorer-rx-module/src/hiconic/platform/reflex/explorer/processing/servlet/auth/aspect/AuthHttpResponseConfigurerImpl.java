package hiconic.platform.reflex.explorer.processing.servlet.auth.aspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletResponse;

public class AuthHttpResponseConfigurerImpl implements AuthHttpResponseConfigurer {
	Map<Object, Consumer<HttpServletResponse>> registry = new HashMap<>();
	List<Consumer<HttpServletResponse>> registryForAll = new ArrayList<>();

	public AuthHttpResponseConfigurerImpl() {

	}

	@Override
	public void applyFor(Object response, Consumer<HttpServletResponse> consumer) {
		registry.put(response, consumer);
	}

	public void consume(Object serviceResponse, HttpServletResponse htttpResponse) {
		registry.getOrDefault(serviceResponse, r -> {
			/* noop */}).accept(htttpResponse);
		registryForAll.forEach(c -> c.accept(htttpResponse));
	}

	@Override
	public void applyForAll(Consumer<HttpServletResponse> consumer) {
		registryForAll.add(consumer);
	}
}
