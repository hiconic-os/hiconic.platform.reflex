// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.platform.reflex.messaging.jdbc.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.braintribe.common.concurrent.ScheduledTask;
import com.braintribe.common.concurrent.TaskScheduler;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.messaging.jdbc.JdbcConnectionProvider;
import com.braintribe.transport.messaging.api.MessagingSessionProvider;
import com.braintribe.transport.messaging.impl.StandardMessagingSessionProvider;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.scope.InstanceConfiguration;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.messaging.api.MessagingBaseContract;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.messaging.jdbc.model.configuration.JdbcMessagingConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class JdbcMessagingRxModuleSpace implements RxModuleContract, MessagingContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private DatabaseContract database;

	@Import
	private MessagingBaseContract messagingBase;

	@Override
	@Managed
	public MessagingSessionProvider sessionProvider() {
		StandardMessagingSessionProvider bean = new StandardMessagingSessionProvider();
		bean.setMessagingConnectionProvider(connectionProvider());
		return bean;
	}

	@Managed
	private JdbcConnectionProvider connectionProvider() {
		JdbcMessagingConfiguration configuration = configuration();

		JdbcConnectionProvider bean = new JdbcConnectionProvider();
		bean.setName("Jdbc Messaging");
		bean.setSqlPrefix(configuration.getSqlPrefix());
		bean.setDataSource(dataSource(configuration));
		bean.setMessagingContext(messagingBase.context());

		configureExpiredMessagesDeleting(bean, InstanceConfiguration.currentInstance());

		return bean;
	}

	private DataSource dataSource(JdbcMessagingConfiguration configuration) {
		Maybe<DataSource> dataSourceMaybe = database.dataSource(configuration.getDatabaseId());
		return getOrTunnel(dataSourceMaybe);
	}

	private void configureExpiredMessagesDeleting(JdbcConnectionProvider bean, InstanceConfiguration instanceConfiguration) {
		int refreshHour = 1;

		TaskScheduler scheduler = platform.taskScheduler();
		ScheduledTask task = scheduler
				.scheduleAtFixedRate("jdbc-messagingBase-expired-deleting", bean::deleteExpiredMessages, refreshHour, refreshHour, TimeUnit.HOURS)
				.done();

		instanceConfiguration.onDestroy(() -> {
			task.cancel();
			// Await termination?
		});
	}

	@Managed
	private JdbcMessagingConfiguration configuration() {
		return getOrTunnel(platform.readConfig(JdbcMessagingConfiguration.T));
	}

}