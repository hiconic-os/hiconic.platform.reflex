// ============================================================================
package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;
import java.util.Objects;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;
import com.braintribe.logging.ThreadRenamer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class ThreadRenamerFilter implements Filter {

	private static final Logger logger = Logger.getLogger(ThreadRenamerFilter.class);

	private ThreadRenamer threadRenamer = ThreadRenamer.NO_OP;
	private boolean addFullUrl;

	@Configurable
	public void setThreadRenamer(ThreadRenamer threadRenamer) {
		Objects.requireNonNull(threadRenamer, "threadRenamer cannot be set to null");
		this.threadRenamer = threadRenamer;
	}

	@Configurable
	public void setAddFullUrl(boolean addFullUrl) {
		this.addFullUrl = addFullUrl;
	}

	@Override
	public void destroy() {
		/* Intentionally left empty */
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		threadRenamer.push(() -> name(request));
		try {
			filterChain.doFilter(request, response);
		} finally {
			threadRenamer.pop();
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		/* Intentionally left empty */
	}

	private String name(ServletRequest request) {
		return "from(" + url(request) + ")";
	}

	private String url(ServletRequest request) {
		try {
			if (addFullUrl) {
				return ((HttpServletRequest) request).getRequestURL().toString();
			}
			return ((HttpServletRequest) request).getRequestURI();
		} catch (Throwable t) {
			logger.error("Failed to obtain an URL from " + request, t);
			return "<unknown>";
		}
	}

}
