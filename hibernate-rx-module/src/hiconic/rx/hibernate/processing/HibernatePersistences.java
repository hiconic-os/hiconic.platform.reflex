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
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.hibernate.service.api.PersistenceDispatching;
import hiconic.rx.hibernate.service.api.PersistenceProcessor;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;
import hiconic.rx.hibernate.service.api.QueryProcessorArg;
import hiconic.rx.hibernate.service.impl.ConfigurableDispatchingPersistenceServiceProcessor;

public class HibernatePersistences implements DestructionAware {
	private static Logger logger = Logger.getLogger(HibernatePersistences.class);
	record SessionFactoryKey(CmdResolver resolver, DataSource dataSource) {};
	
	private Map<SessionFactoryKey, LazyInitialized<HibernatePersistenceImpl>> cache = new ConcurrentHashMap<>();
	
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
	
	public HibernatePersistence acquirePersistence(CmdResolver resolver, DataSource dataSource) {
		return cache.computeIfAbsent(new SessionFactoryKey(resolver, dataSource), this::buildPersistence).get();
	}
	
	private LazyInitialized<HibernatePersistenceImpl> buildPersistence(SessionFactoryKey key) {
		return new LazyInitialized<>(() -> new HibernatePersistenceImpl(key));
	}
	
	@Override
	public void preDestroy() {
		for (Entry<SessionFactoryKey, LazyInitialized<HibernatePersistenceImpl>> entry: cache.entrySet()) {
			LazyInitialized<HibernatePersistenceImpl> lazy = entry.getValue();
			if (lazy.isInitialized()) {
				HibernatePersistenceImpl persistence = lazy.get();
				try {
					persistence.sessionFactory().close();
				} catch (Exception e) {
					String modelName = entry.getKey().resolver().getModelOracle().getGmMetaModel().getName();
					logger.error("Error while closing session factory for model: " + modelName, e);
				}
			}
		}
	}
	

	class HibernatePersistenceImpl implements HibernatePersistence {
		
		private LazyInitialized<SessionFactory> lazySessionFactory = new LazyInitialized<>(this::buildSessionFactory);
		private SessionFactoryKey key;

		public HibernatePersistenceImpl(SessionFactoryKey key) {
			this.key = key;
		}
		
		private SessionFactory buildSessionFactory() {
			var builder = new HibernateModelSessionFactoryBuilder(key.resolver(), key.dataSource());
			builder.setOrmDebugOutputFolder(debugOrmOutputFolder);
			builder.setDialectAutoSense(dialectAutoSense);
			return builder.build();
		}

		@Override
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(
				PersistenceDispatching<P, R> dispatching) {
			ConfigurableDispatchingPersistenceServiceProcessor<P, R> dispatcher = new ConfigurableDispatchingPersistenceServiceProcessor<>(dispatching);
			return new PersistenceAdapterServiceProcessor<>(lazySessionFactory, dispatcher);
		}
		
		@Override
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(
				PersistenceProcessor<P, R> processor) {
			return asServiceProcessor(PersistenceProcessorDispatching.create(processor));
		}
		
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(PersistenceServiceProcessor<P, R> processor) {
			return new PersistenceAdapterServiceProcessor<>(lazySessionFactory, processor);
		}
		
		@Override
		public <P extends ServiceRequest, R> ServiceProcessor<P, R> queryProcessor(EntityType<P> requestType,
				String queryString, QueryProcessorArg... args) {
			return asServiceProcessor(new QueryProcessor<>(queryString, args));
		}
		
		@Override
		public SessionFactory sessionFactory() {
			return lazySessionFactory.get();
		}
	}
}
