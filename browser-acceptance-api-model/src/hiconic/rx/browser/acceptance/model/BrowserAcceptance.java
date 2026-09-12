package hiconic.rx.browser.acceptance.model;

import java.util.Date;
import java.util.List;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Modeled security state. The persisted token hash is deliberately not part of this administrative view. */
public interface BrowserAcceptance extends GenericEntity {
	EntityType<BrowserAcceptance> T = EntityTypes.T(BrowserAcceptance.class);
	String getBrowserContextId(); void setBrowserContextId(String browserContextId);
	String getUserId(); void setUserId(String userId);
	String getUserName(); void setUserName(String userName);
	String getEntryPoint(); void setEntryPoint(String entryPoint);
	BrowserAcceptanceState getState(); void setState(BrowserAcceptanceState state);
	Date getCreatedAt(); void setCreatedAt(Date createdAt);
	Date getLastSeenAt(); void setLastSeenAt(Date lastSeenAt);
	Date getDecidedAt(); void setDecidedAt(Date decidedAt);
	Date getExpiresAt(); void setExpiresAt(Date expiresAt);
	String getDecidedBy(); void setDecidedBy(String decidedBy);
	/** Resolved client address observed when this browser context was first requested. */
	String getRequestorAddress(); void setRequestorAddress(String requestorAddress);
	String getLastRequestorAddress(); void setLastRequestorAddress(String lastRequestorAddress);
	/** Direct network peer, typically the last reverse proxy. */
	String getDirectRequestorAddress(); void setDirectRequestorAddress(String directRequestorAddress);
	String getLastDirectRequestorAddress(); void setLastDirectRequestorAddress(String lastDirectRequestorAddress);
	String getUserAgent(); void setUserAgent(String userAgent);
	String getClientHintsUserAgent(); void setClientHintsUserAgent(String clientHintsUserAgent);
	String getClientHintsPlatform(); void setClientHintsPlatform(String clientHintsPlatform);
	String getClientHintsMobile(); void setClientHintsMobile(String clientHintsMobile);
	/** Chronological, append-only lifecycle history. Populated only for privileged administrative listings. */
	List<BrowserAcceptanceEvent> getEvents(); void setEvents(List<BrowserAcceptanceEvent> events);
}
