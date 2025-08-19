package hiconic.platform.reflex.explorer.processing.servlet.auth.aspect;

import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletResponse;

public interface AuthHttpResponseConfigurer {

	void applyFor(Object response, Consumer<HttpServletResponse> consumer);

	void applyForAll(Consumer<HttpServletResponse> consumer);

}
