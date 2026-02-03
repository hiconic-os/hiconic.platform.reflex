package hiconic.rx.db.check.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.check.api.CheckContract;
import hiconic.rx.check.model.aspect.CheckCoverage;
import hiconic.rx.check.model.aspect.CheckLatency;
import hiconic.rx.db.check.processing.DbConnectionCheckProcessor;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Registers the {@link DbConnectionCheckProcessor}.
 */
@Managed
public class DbConnectionCheckRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private CheckContract check;

	@Import
	private DatabaseContract database;

	@Override
	public void onDeploy() {
		check.checkProcessorRegistry().registerProcessor(DbConnectionCheckProcessor.symbol, //
				dbConnectionCheckProcessor(), CheckCoverage.connectivity, CheckLatency.moderate, "db");
	}

	private DbConnectionCheckProcessor dbConnectionCheckProcessor() {
		DbConnectionCheckProcessor bean = new DbConnectionCheckProcessor();
		bean.setDatabaseContract(database);

		return bean;
	}

}
