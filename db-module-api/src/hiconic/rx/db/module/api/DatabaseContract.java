package hiconic.rx.db.module.api;

import javax.sql.DataSource;

import com.braintribe.gm.model.reason.Maybe;

import hiconic.rx.module.api.wire.RxExportContract;

public interface DatabaseContract extends RxExportContract {
	DataSource findDataSource(String name);
	Maybe<DataSource> dataSource(String name);
}
