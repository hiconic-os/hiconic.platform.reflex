package hiconic.rx.browser.acceptance.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum BrowserAcceptanceState implements EnumBase<BrowserAcceptanceState> {
	PENDING, APPROVED, REJECTED, REVOKED, EXPIRED, FORGOTTEN;
	public static final EnumType<BrowserAcceptanceState> T = EnumTypes.T(BrowserAcceptanceState.class);
	@Override public EnumType<BrowserAcceptanceState> type() { return T; }
}
