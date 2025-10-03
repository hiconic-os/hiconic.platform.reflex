// ============================================================================
package hiconic.platform.reflex.web_server.processing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.UUID;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;

import dev.hiconic.servlet.api.HttpFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class CaptureFilter implements HttpFilter {
	private static final Logger logger = Logger.getLogger(CaptureFilter.class);

	private File captureDir;

	@Required
	@Configurable
	public void setCaptureDir(File captureDir) {
		this.captureDir = captureDir;
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
	
		// boolean captureRequest = getBooleanHeader(request, "Capture-Request");
		boolean captureResponse = getBooleanHeader(request, "Capture-Response");
		
		if (!captureResponse) {
			chain.doFilter(request, response);
			return;
		}
		
		String callId = request.getHeader("Call-Id");
		
		if (callId == null)
			callId = UUID.randomUUID().toString();
		
		try (CapturedHttpServletResponse capturedHttpServletResponse = new CapturedHttpServletResponse(response, request.getRequestURI(), callId)) {
			chain.doFilter(request, capturedHttpServletResponse);
		}
	}

	private static boolean getBooleanHeader(HttpServletRequest request, String name) {
		String value = request.getHeader(name);

		if (value == null)
			return false;

		return Boolean.TRUE.toString().equals(value);
	}

	private static class CaptureOutputStream extends ServletOutputStream {
		private final OutputStream out;
		private final OutputStream cOut;

		public CaptureOutputStream(OutputStream out, OutputStream cOut) {
			super();
			this.out = out;
			this.cOut = cOut;
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
			cOut.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			cOut.write(b, off, len);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			throw new UnsupportedOperationException("Method 'CaptureFilter.CaptureOutputStream.setWriteListener' is not supported!");
		}

	}

	private class CapturedHttpServletResponse extends HttpServletResponseWrapper implements AutoCloseable {
		private CaptureOutputStream out;
		private	OutputStream cOut;
		private PrintWriter printWriter;
		private final String callId;
		private final String path;

		public CapturedHttpServletResponse(HttpServletResponse response, String path, String callId) {
			super(response);
			this.path = path;
			this.callId = callId;
		}

		private CaptureOutputStream createOutputStream() throws IOException {
			return out = new CaptureOutputStream(getResponse().getOutputStream(), openCaptureOutputStream());
		}

		private OutputStream openCaptureOutputStream() throws FileNotFoundException {
			File captureFolder = new File(captureDir, path);
			captureFolder.mkdirs();
			File captureFile = new File(captureFolder, callId);
			return cOut = new BufferedOutputStream(new FileOutputStream(captureFile));
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (this.printWriter != null)
				throw new IllegalStateException("PrintWriter obtained already - cannot get OutputStream");
			
			if (this.out != null)
				return this.out;

			return createOutputStream();
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (this.printWriter != null)
				return this.printWriter;

			if (this.out != null)
				throw new IllegalStateException("OutputStream obtained already - cannot get PrintWriter");

			return this.printWriter = new PrintWriter(
					new OutputStreamWriter(createOutputStream(), getResponse().getCharacterEncoding()));
		}
		
		@Override
		public void close() {
			try {
				if (cOut != null)
					cOut.close();
			}
			catch (IOException e) {
				logger.error("Could not close capture file output for path [" + path + "] and call id [" + callId +"]", e);
			}
		}

	}
}
