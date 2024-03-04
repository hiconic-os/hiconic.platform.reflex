package hiconic.rx.db.model.configuration;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Enum constants corresponding to values in {@link java.sql.Connection}.
 * 
 * @author peter.gazdik
 */
public enum JdbcTransactionIsolationLevel implements EnumBase {

	NONE,
	READ_UNCOMMITTED,
	READ_COMMITTED,
	REPEATABLE_READ,
	SERIALIZABLE;

	public static final EnumType T = EnumTypes.T(JdbcTransactionIsolationLevel.class);
	
	@Override
	public EnumType type() {
		return T;
	}	
}
