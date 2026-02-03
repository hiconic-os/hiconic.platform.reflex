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
package hiconic.rx.hibernate.processing;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.jboss.logging.Logger;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.DestructionAware;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.Lazy;

import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.hibernate.service.api.PersistenceDispatching;
import hiconic.rx.hibernate.service.api.PersistenceProcessor;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;
import hiconic.rx.hibernate.service.api.QueryProcessorArg;
import hiconic.rx.hibernate.service.impl.ConfigurableDispatchingPersistenceServiceProcessor;

public class HibernatePersistences implements DestructionAware {
	private static Logger logger = Logger.getLogger(HibernatePersistences.class);

	private final Map<SessionFactoryKey, Lazy<HibernatePersistenceImpl>> cache = new ConcurrentHashMap<>();

	private File debugOrmOutputFolder;
	private DialectAutoSense dialectAutoSense;

	@Configurable
	public void setDebugOrmOutputFolder(File debugOrmOutputFolder) {
		this.debugOrmOutputFolder = debugOrmOutputFolder;
	}

	@Configurable
	public void setDialectAutoSense(DialectAutoSense dialectAutoSense) {
		this.dialectAutoSense = dialectAutoSense;
	}

	public HibernatePersistence acquirePersistence(HibernatePersistenceConfiguration configuration, CmdResolver resolver, DataSource dataSource) {
		return cache.computeIfAbsent(new SessionFactoryKey(configuration, resolver, dataSource), this::buildPersistence).get();
	}

	private Lazy<HibernatePersistenceImpl> buildPersistence(SessionFactoryKey key) {
		return new Lazy<>(() -> new HibernatePersistenceImpl(key));
	}

	@Override
	public void preDestroy() {
		for (Entry<SessionFactoryKey, Lazy<HibernatePersistenceImpl>> entry : cache.entrySet()) {
			Lazy<HibernatePersistenceImpl> lazyPersistence = entry.getValue();

			if (!lazyPersistence.isInitialized())
				continue;

			try {
				lazyPersistence.get().sessionFactory().close();

			} catch (Exception e) {
				String modelName = entry.getKey().resolver().getModelOracle().getGmMetaModel().getName();
				logger.error("Error while closing session factory for model: " + modelName, e);
			}
		}
	}

	class HibernatePersistenceImpl implements HibernatePersistence {

		private final Lazy<SessionFactory> lazySessionFactory = new Lazy<>(this::buildSessionFactory);
		private final SessionFactoryKey key;

		public HibernatePersistenceImpl(SessionFactoryKey key) {
			this.key = key;
		}

		private SessionFactory buildSessionFactory() {
			var builder = new HibernateModelSessionFactoryBuilder(key);
			builder.setOrmDebugOutputFolder(debugOrmOutputFolder);
			builder.setDialectAutoSense(dialectAutoSense);
			return builder.build();
		}

		@Override
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(PersistenceProcessor<P, R> processor) {
			return asServiceProcessor(PersistenceProcessorDispatching.create(processor));
		}

		@Override
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(PersistenceDispatching<P, R> dispatching) {
			var dispatcher = new ConfigurableDispatchingPersistenceServiceProcessor<>(dispatching);
			return new PersistenceAdapterServiceProcessor<>(lazySessionFactory, dispatcher);
		}

		public <P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(PersistenceServiceProcessor<P, R> processor) {
			return new PersistenceAdapterServiceProcessor<>(lazySessionFactory, processor);
		}

		@Override
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> queryProcessor(EntityType<P> requestType, String queryString,
				QueryProcessorArg... args) {
			return asServiceProcessor(new QueryProcessor<>(queryString, args));
		}

		@Override
		public SessionFactory sessionFactory() {
			return lazySessionFactory.get();
		}
	}
}

record SessionFactoryKey( //
		HibernatePersistenceConfiguration configuration, //
		CmdResolver resolver, //
		DataSource dataSource) {
	// empty
}