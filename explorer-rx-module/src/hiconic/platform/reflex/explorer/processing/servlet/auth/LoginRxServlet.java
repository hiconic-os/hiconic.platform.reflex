// ============================================================================
package hiconic.platform.reflex.explorer.processing.servlet.auth;


import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.InitializationAware;

import hiconic.platform.reflex.explorer.processing.servlet.common.BasicTemplateBasedServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginRxServlet extends BasicTemplateBasedServlet implements InitializationAware {

	private static final long serialVersionUID = -3371378397236984055L;
	private static final String signinPageTemplateLocation = "hiconic/platform/reflex/explorer/processing/servlet/auth/templates/login.html.vm";
		
	@Override
	public void postConstruct() {
		setTemplateLocation(signinPageTemplateLocation);
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request,HttpServletResponse repsonse) {
		VelocityContext context = new VelocityContext();
		context.put("continue", request.getParameter(Constants.REQUEST_PARAM_CONTINUE));
		context.put("message", request.getParameter(Constants.REQUEST_PARAM_MESSAGE));
		context.put("messageStatus", request.getParameter(Constants.REQUEST_PARAM_MESSAGESTATUS));
		// context.put("tribefireRuntime", TribefireRuntime.class);
		
		if (AuthRxServlet.offerStaySigned()) {
			context.put("offerStaySigned", Boolean.TRUE);
		} else {
			context.put("offerStaySigned", Boolean.FALSE);
		}
		
		return context;
	}

}
