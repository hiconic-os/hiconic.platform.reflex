package hiconic.rx.hibernate.wire.contract;

import java.io.File;

import com.braintribe.wire.api.annotation.Name;

import hiconic.rx.module.api.wire.SystemPropertiesContract;

public interface HibernatePropertiesContract extends SystemPropertiesContract {
	@Name("rx.hibernate.module.ormDebugOutputFolder")
	File ormDebugOutputFolder();
}
