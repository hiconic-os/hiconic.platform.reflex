package hiconic.platform.reflex.db.wire.contract;

import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.db.module.api.DatabaseContract;

public interface DbImportContract extends WireSpace {
	DatabaseContract database();
}
