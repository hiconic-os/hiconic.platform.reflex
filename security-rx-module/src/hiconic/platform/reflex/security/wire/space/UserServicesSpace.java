package hiconic.platform.reflex.security.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.Map;

import javax.sql.DataSource;

import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.security.processor.JdbcUserSessionService;
import hiconic.platform.reflex.security.processor.StandardUserService;
import hiconic.platform.reflex.security.processor.StandardUserSessionService;
import hiconic.platform.reflex.security.processor.UserSessionIdProvider;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.model.configuration.SecurityConfiguration;
import hiconic.rx.security.model.configuration.UsersConfiguration;

@Managed
public class UserServicesSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;
	
	@Import
	private DatabaseContract database;

	@Managed
	private SecurityConfiguration configuration() {
		return getOrTunnel(platform.readConfig(SecurityConfiguration.T));
	}
	
	@Managed
	private UsersConfiguration usersConfiguration() {
		return getOrTunnel(platform.readConfig(UsersConfiguration.T));
	}
	
	public UserSessionService userSessionService() {
		DataSource dataSource = database.findDataSource(configuration().getUserSessionDb());
		
		if (dataSource != null)
			return jdbcUserSessionService(dataSource);
		else
			return standardUserSessionService();
	}
	
	@Managed
	public StandardUserService standardUserService() {
		StandardUserService bean = new StandardUserService();
		bean.setUsers(usersConfiguration().getUsers());
		return bean;
	}
	
	@Managed 
	private StandardUserSessionService standardUserSessionService() {
		StandardUserSessionService bean = new StandardUserSessionService();
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setNodeId(platform.nodeId());
		bean.setDefaultUserSessionMaxIdleTime(TimeSpan.create(24, TimeUnit.hour));
		return bean;
	}
	
	@Managed
	private JdbcUserSessionService jdbcUserSessionService(DataSource dataSource) {
		JdbcUserSessionService bean = new JdbcUserSessionService();
		bean.setDataSource(dataSource);
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setNodeId(platform.nodeId());
		bean.setDefaultUserSessionMaxIdleTime(TimeSpan.create(24, TimeUnit.hour));
		return bean;
	}
	
	@Managed
	public UserSessionIdProvider userSessionIdFactory() {
		UserSessionIdProvider bean = new UserSessionIdProvider();
		bean.setTypePrefixes(Map.of( //
			UserSessionType.internal, "i-", //
			UserSessionType.trusted, "t-" //
		));
		return bean;
	}
}