package hiconic.rx.demo.web.processing;

import java.io.IOException;

import com.braintribe.logging.Logger;

import dev.hiconic.servlet.api.HttpFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AccessLogFilter implements HttpFilter {
	private Logger logger = Logger.getLogger(AccessLogFilter.class);
	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		long start = System.nanoTime();
		
		chain.doFilter(request, response);
		long end = System.nanoTime();
		
		long delta = end - start;
		
		logger.info("Logged " + delta + " ns");
	}
}
