package hiconic.rx.browser.acceptance.model;

import java.util.Date;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Append-only audit event for a browser acceptance lifecycle. */
public interface BrowserAcceptanceEvent extends GenericEntity {
	EntityType<BrowserAcceptanceEvent> T = EntityTypes.T(BrowserAcceptanceEvent.class);
	String getAcceptanceId(); void setAcceptanceId(String acceptanceId);
	BrowserAcceptanceEventType getType(); void setType(BrowserAcceptanceEventType type);
	Date getTimestamp(); void setTimestamp(Date timestamp);
	String getActorUserId(); void setActorUserId(String actorUserId);
	String getRequestorAddress(); void setRequestorAddress(String requestorAddress);
	String getDetails(); void setDetails(String details);
}
