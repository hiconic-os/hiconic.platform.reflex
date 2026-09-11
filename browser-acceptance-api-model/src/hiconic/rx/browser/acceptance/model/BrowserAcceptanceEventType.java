package hiconic.rx.browser.acceptance.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum BrowserAcceptanceEventType implements EnumBase<BrowserAcceptanceEventType> {
	REQUESTED, APPROVED, REJECTED, REVOKED, EXPIRED, ACCESSED;
	public static final EnumType<BrowserAcceptanceEventType> T = EnumTypes.T(BrowserAcceptanceEventType.class);
	@Override public EnumType<BrowserAcceptanceEventType> type() { return T; }
}
