package hiconic.rx.security.web.wire.space;

import com.braintribe.utils.StringTools;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.model.configuration.SecurityConfiguration;
import hiconic.rx.security.web.api.AuthFilters;
import hiconic.rx.security.web.api.CookieHandler;
import hiconic.rx.security.web.api.WebSecurityContract;
import hiconic.rx.security.web.model.configuration.WebSecurityConfiguration;
import hiconic.rx.security.web.processing.credentials.extractor.BasicAuthCredentialsProvider;
import hiconic.rx.security.web.processing.credentials.extractor.ExistingSessionFromCookieProvider;
import hiconic.rx.security.web.processing.credentials.extractor.ExistingSessionFromHeaderParameterProvider;
import hiconic.rx.security.web.processing.credentials.extractor.ExistingSessionFromRequestParameterProvider;
import hiconic.rx.security.web.processing.credentials.extractor.JwtCredentialsProvider;
import hiconic.rx.security.web.processing.servlet.AuthRxFilter;
import hiconic.rx.security.web.processing.servlet.AuthRxServlet;
import hiconic.rx.security.web.processing.servlet.LoginRxServlet;
import hiconic.rx.security.web.processing.servlet.LogoutRxServlet;
import hiconic.rx.web.server.api.WebServerContract;
import jakarta.servlet.DispatcherType;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WebSecurityRxModuleSpace implements RxModuleContract, WebSecurityContract {

	// @formatter:off
	@Import private RxPlatformContract platform;

	@Import private WebServerContract webServer;

	@Import private HttpRxSpace http;
	// @formatter:on

	@Override
	public void onDeploy() {
		webServer.addFilter(AuthFilters.strictAuthFilter, authFilterStrict());
		webServer.addFilter(AuthFilters.lenientAuthFilter, authFilterLenient());
		webServer.addFilter(AuthFilters.strictAdminAuthFilter, authFilterAdminStrict());

		webServer.addServlet("login-servlet", "login", loginServlet());
		webServer.addServlet("login-auth-servlet", "login/auth", loginAuthServlet());

		webServer.addServlet("logout-servlet", "logout", loginServlet());
		// TODO explain why lenient filter?
		webServer.addFilterMapping(AuthFilters.lenientAuthFilter, "/logout/*", DispatcherType.REQUEST);
	}

	@Managed
	private LoginRxServlet loginServlet() {
		LoginRxServlet bean = new LoginRxServlet();
		return bean;
	}

	@Managed
	private LogoutRxServlet logoutServlet() {
		LogoutRxServlet bean = new LogoutRxServlet();
		bean.setRequestEvaluator(platform.evaluator());

		String loginPath = loginPath();
		if (!StringTools.isBlank(loginPath))
			bean.setRelativeLoginPath(loginPath);

		return bean;
	}

	@Managed
	private AuthRxServlet loginAuthServlet() {
		AuthRxServlet bean = new AuthRxServlet();
		bean.setCookieHandler(http.cookieHandler());
		bean.setRemoteAddressResolver(webServer.remoteAddressResolver());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setRequestEvaluator(platform.systemEvaluator());
		bean.setEntryPointProvider(http.openUserSessionConfigurationProvider()::findEntryPointName);

		return bean;
	}

	// Auth filters

	@Managed
	private AuthRxFilter authFilterStrict() {
		AuthRxFilter bean = new AuthRxFilter();
		bean.setStrict(true);
		configureAuthFilter(bean);
		return bean;
	}

	@Managed
	private AuthRxFilter authFilterAdminStrict() {
		// TODO move to security contract
		SecurityConfiguration securityConfig = platform.readConfig(SecurityConfiguration.T).get();

		AuthRxFilter bean = new AuthRxFilter();
		bean.setStrict(true);
		bean.setGrantedRoles(securityConfig.getAdminRoles());
		configureAuthFilter(bean);
		return bean;
	}

	@Managed
	private AuthRxFilter authFilterLenient() {
		AuthRxFilter bean = new AuthRxFilter();
		bean.setStrict(false);
		configureAuthFilter(bean);
		return bean;
	}

	private void configureAuthFilter(AuthRxFilter authFilter) {
		authFilter.setRequestEvaluator(platform.systemEvaluator());
		// TODO threadRenamer
		// if (logger.isDebugEnabled()) {
		// authFilter.setThreadRenamer(runtime.threadRenamer());
		// }
		authFilter.setThrowExceptionOnAuthFailure(true);
		authFilter.setEntryPointProvider(http.openUserSessionConfigurationProvider()::findEntryPoint);

		authFilter.addWebCredentialProvider("cookie", existingSessionFromCookieProvider());
		authFilter.addWebCredentialProvider("request-parameter", existingSessionFromRequestParameterProvider());
		authFilter.addWebCredentialProvider("basic", basicAuthCredentialsProvider());
		authFilter.addWebCredentialProvider("jwt", jwtCredentialsProvider());
		authFilter.addWebCredentialProvider("header-parameter", existingSessionFromHeaderParameterProvider());

		String loginPath = loginPath();
		if (!StringTools.isBlank(loginPath))
			authFilter.setRelativeLoginPath(loginPath);
	}

	@Managed
	private ExistingSessionFromCookieProvider existingSessionFromCookieProvider() {
		ExistingSessionFromCookieProvider bean = new ExistingSessionFromCookieProvider();
		bean.setSessionCookieNameProvider(http.sessionCookieNameSupplier());
		return bean;
	}

	@Managed
	private ExistingSessionFromHeaderParameterProvider existingSessionFromHeaderParameterProvider() {
		return new ExistingSessionFromHeaderParameterProvider();
	}

	@Managed
	private ExistingSessionFromRequestParameterProvider existingSessionFromRequestParameterProvider() {
		return new ExistingSessionFromRequestParameterProvider();
	}

	@Managed
	private BasicAuthCredentialsProvider basicAuthCredentialsProvider() {
		return new BasicAuthCredentialsProvider();
	}

	@Managed
	private JwtCredentialsProvider jwtCredentialsProvider() {
		return new JwtCredentialsProvider();
	}

	private String loginPath() {
		WebSecurityConfiguration webSecurityConfig = platform.readConfig(WebSecurityConfiguration.T).get();
		return webSecurityConfig.getLoginPath();
	}

	@Override
	public CookieHandler cookieHandler() {
		return http.cookieHandler();
	}

}