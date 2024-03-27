// ============================================================================
package hiconic.rx.web.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.model.DdraEndpoint;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.utils.lcd.StopWatch;

/**
 * REST Endpoints in this project all have a context. A new context is created for each incoming request and should store all
 * the data relevant to treat this request.
 * 
 * Since this class is the only class that contains data for a single call, and an instance of this class is created for
 * each incoming call, the Servlets that use this are de-facto thread-safe.
 */
public class DdraEndpointContext<E extends DdraEndpoint> {

	private final HttpServletRequest request;

	private final HttpServletResponse response;

	private E endpoint;

	private Marshaller marshaller;

	private String mimeType;
	
	private Integer forceResponseCode;
	
	private final StopWatch stopWatch;

	public DdraEndpointContext(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		this.stopWatch = new StopWatch();
	}
	
	public void setForceResponseCode(Integer forceResponseCode) {
		this.forceResponseCode = forceResponseCode;
	}
	
	public Integer getForceResponseCode() {
		return forceResponseCode;
	}
	
	public OutputStream openResponseOutputStream() throws IOException {
		HttpServletResponse response = getResponse();
		
		if (forceResponseCode != null)
			response.setStatus(forceResponseCode);
		
		response.setHeader("Content-Type", getMimeType());
		
		return response.getOutputStream();
	}

	public StopWatch getStopWatch() {
		return this.stopWatch;
	}
	
	public Marshaller getMarshaller() {
		return marshaller;
	}

	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public E getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(E endpoint) {
		this.endpoint = endpoint;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}
	
	public Consumer<GenericEntity> getMarshallingVisitor(){
		return e -> {};
	}
	
}
