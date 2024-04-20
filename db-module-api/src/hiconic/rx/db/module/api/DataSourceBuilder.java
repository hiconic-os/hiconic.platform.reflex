package hiconic.rx.db.module.api;

import hiconic.rx.db.model.configuration.Database;

public interface DataSourceBuilder {
	DataSourceBuilder configurationName(String name);
	DataSourceBuilder configuration(Database database);
}
