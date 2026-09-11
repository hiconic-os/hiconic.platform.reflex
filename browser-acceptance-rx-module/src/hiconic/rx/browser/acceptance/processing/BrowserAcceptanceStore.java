package hiconic.rx.browser.acceptance.processing;

import static com.braintribe.utils.lcd.CollectionTools2.asMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.braintribe.gm.jdbc.api.GmColumn;
import com.braintribe.gm.jdbc.api.GmDb;
import com.braintribe.gm.jdbc.api.GmIndex;
import com.braintribe.gm.jdbc.api.GmRow;
import com.braintribe.gm.jdbc.api.GmTable;
import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceEvent;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceEventType;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceState;
import hiconic.rx.browser.acceptance.model.StoredBrowserAcceptance;

/** Maps the modeled browser-acceptance state and audit events to compact JDBC tables. */
public class BrowserAcceptanceStore {
	private static final long LAST_SEEN_WRITE_INTERVAL_MILLIS = 5 * 60 * 1000L;
	private final long pendingLifetimeMillis;
	private final long acceptanceLifetimeMillis;
	private final AcceptanceTable acceptances;
	private final EventTable events;

	public BrowserAcceptanceStore(DataSource dataSource, String tableNamePrefix, int pendingLifetimeSeconds, int acceptanceLifetimeSeconds) {
		if (tableNamePrefix == null || !tableNamePrefix.matches("[A-Za-z][A-Za-z0-9_]*"))
			throw new IllegalArgumentException("Browser acceptance table name prefix must match [A-Za-z][A-Za-z0-9_]*: " + tableNamePrefix);
		GmDb db = GmDb.newDb(dataSource).done();
		pendingLifetimeMillis = pendingLifetimeSeconds * 1000L;
		acceptanceLifetimeMillis = acceptanceLifetimeSeconds * 1000L;
		acceptances = new AcceptanceTable(db, tableNamePrefix + "ACCEPTANCE");
		events = new EventTable(db, tableNamePrefix + "ACCEPTANCE_EVENT");
		acceptances.table.ensure();
		events.table.ensure();
	}

	public synchronized BrowserAcceptance findOrRequest(String rawToken, String userId, String userName, String entryPoint, String requestorAddress,
			BrowserRequestInformation requestInformation) {
		String tokenHash = hash(rawToken);
		StoredBrowserAcceptance stored = acceptances.find(tokenHash, userId, entryPoint);
		Date now = new Date();
		if (stored == null) {
			stored = StoredBrowserAcceptance.T.create();
			stored.setId(stableUuid("acceptance:" + tokenHash + ':' + userId + ':' + entryPoint));
			stored.setBrowserContextId(stableUuid("browser-context:" + tokenHash));
			stored.setTokenHash(tokenHash);
			stored.setUserId(userId);
			stored.setUserName(userName);
			stored.setEntryPoint(entryPoint);
			stored.setCreatedAt(now);
			stored.setRequestorAddress(requestorAddress);
			stored.setDirectRequestorAddress(directAddress(requestInformation));
		}
		if (stored.getState() == BrowserAcceptanceState.APPROVED) {
			if (stored.getExpiresAt() != null && stored.getExpiresAt().after(now)) {
				if (stored.getLastSeenAt() == null || now.getTime() - stored.getLastSeenAt().getTime() >= LAST_SEEN_WRITE_INTERVAL_MILLIS) {
					observe(stored, requestorAddress, requestInformation, now);
					acceptances.write(stored);
				}
				return view(stored);
			}
			stored.setState(BrowserAcceptanceState.EXPIRED);
			stored.setLastSeenAt(now);
			event(stored, BrowserAcceptanceEventType.EXPIRED, null, requestorAddress, null);
		}
		if (stored.getState() == BrowserAcceptanceState.REJECTED || stored.getState() == BrowserAcceptanceState.REVOKED) {
			if (stored.getLastSeenAt() == null || now.getTime() - stored.getLastSeenAt().getTime() >= LAST_SEEN_WRITE_INTERVAL_MILLIS) {
				observe(stored, requestorAddress, requestInformation, now);
				acceptances.write(stored);
			}
			return view(stored);
		}
		if (stored.getState() == BrowserAcceptanceState.PENDING && stored.getExpiresAt() != null && stored.getExpiresAt().after(now)) {
			if (stored.getLastSeenAt() == null || now.getTime() - stored.getLastSeenAt().getTime() >= LAST_SEEN_WRITE_INTERVAL_MILLIS) {
				observe(stored, requestorAddress, requestInformation, now);
				acceptances.write(stored);
			}
			return view(stored);
		}
		stored.setState(BrowserAcceptanceState.PENDING);
		stored.setCreatedAt(now);
		observe(stored, requestorAddress, requestInformation, now);
		stored.setExpiresAt(new Date(now.getTime() + pendingLifetimeMillis));
		stored.setDecidedAt(null);
		stored.setDecidedBy(null);
		stored.setRequestorAddress(requestorAddress);
		acceptances.write(stored);
		event(stored, BrowserAcceptanceEventType.REQUESTED, userId, requestorAddress, null);
		return view(stored);
	}

	private static void observe(StoredBrowserAcceptance stored, String requestorAddress, BrowserRequestInformation information, Date now) {
		stored.setLastSeenAt(now);
		stored.setLastRequestorAddress(requestorAddress);
		if (information == null)
			return;
		stored.setLastDirectRequestorAddress(information.directAddress());
		stored.setUserAgent(information.userAgent());
		stored.setClientHintsUserAgent(information.clientHintsUserAgent());
		stored.setClientHintsPlatform(information.clientHintsPlatform());
		stored.setClientHintsMobile(information.clientHintsMobile());
	}

	private static String directAddress(BrowserRequestInformation information) {
		return information == null ? null : information.directAddress();
	}

	public BrowserAcceptance findApproved(String rawToken, String userId, String entryPoint) {
		StoredBrowserAcceptance stored = acceptances.find(hash(rawToken), userId, entryPoint);
		if (stored == null || stored.getState() != BrowserAcceptanceState.APPROVED) return null;
		if (stored.getExpiresAt() == null || !stored.getExpiresAt().after(new Date())) return null;
		return view(stored);
	}

	public List<BrowserAcceptance> list(BrowserAcceptanceState state) { return acceptances.list(state).stream().map(BrowserAcceptanceStore::view).toList(); }

	public synchronized BrowserAcceptance change(String id, BrowserAcceptanceState state, String actor, String address) {
		StoredBrowserAcceptance stored = acceptances.findById(id);
		if (stored == null) return null;
		Date now = new Date();
		stored.setState(state);
		stored.setDecidedAt(now);
		stored.setDecidedBy(actor);
		stored.setExpiresAt(state == BrowserAcceptanceState.APPROVED ? new Date(now.getTime() + acceptanceLifetimeMillis) : stored.getExpiresAt());
		acceptances.write(stored);
		event(stored, BrowserAcceptanceEventType.valueOf(state.name()), actor, address, null);
		return view(stored);
	}

	private void event(StoredBrowserAcceptance acceptance, BrowserAcceptanceEventType type, String actor, String address, String details) {
		BrowserAcceptanceEvent event = BrowserAcceptanceEvent.T.create();
		event.setId(UUID.randomUUID().toString()); event.setAcceptanceId(acceptance.getId()); event.setType(type);
		event.setTimestamp(new Date()); event.setActorUserId(actor); event.setRequestorAddress(address); event.setDetails(details);
		events.insert(event);
	}

	private static BrowserAcceptance view(StoredBrowserAcceptance source) {
		BrowserAcceptance target = BrowserAcceptance.T.create();
		target.setId(source.getId()); target.setBrowserContextId(source.getBrowserContextId()); target.setUserId(source.getUserId());
		target.setUserName(source.getUserName()); target.setEntryPoint(source.getEntryPoint()); target.setState(source.getState());
		target.setCreatedAt(source.getCreatedAt()); target.setLastSeenAt(source.getLastSeenAt()); target.setDecidedAt(source.getDecidedAt());
		target.setExpiresAt(source.getExpiresAt()); target.setDecidedBy(source.getDecidedBy()); target.setRequestorAddress(source.getRequestorAddress());
		target.setLastRequestorAddress(source.getLastRequestorAddress()); target.setDirectRequestorAddress(source.getDirectRequestorAddress());
		target.setLastDirectRequestorAddress(source.getLastDirectRequestorAddress()); target.setUserAgent(source.getUserAgent());
		target.setClientHintsUserAgent(source.getClientHintsUserAgent()); target.setClientHintsPlatform(source.getClientHintsPlatform());
		target.setClientHintsMobile(source.getClientHintsMobile());
		return target;
	}

	private static String hash(String token) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
		catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
	}

	private static String stableUuid(String identity) {
		return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
	}

	private static class AcceptanceTable {
		final GmTable table; final GmColumn<String> id, contextId, tokenHash, userId, userName, entryPoint, state, decidedBy, address, lastAddress,
				directAddress, lastDirectAddress, userAgent, clientHintsUserAgent, clientHintsPlatform, clientHintsMobile;
		final GmColumn<Date> created, lastSeen, decided, expires;
		AcceptanceTable(GmDb db, String name) {
			id=db.shortString255("ID").primaryKey().notNull().done(); contextId=db.shortString255("CONTEXT_ID").notNull().done();
			tokenHash=db.shortString255("TOKEN_HASH").notNull().done(); userId=db.shortString255("USER_ID").notNull().done();
			userName=db.shortString255("USER_NAME").done(); entryPoint=db.shortString255("ENTRY_POINT").done(); state=db.shortString255("STATE").notNull().done();
			created=db.date("CREATED_AT").notNull().done(); lastSeen=db.date("LAST_SEEN_AT").done(); decided=db.date("DECIDED_AT").done(); expires=db.date("EXPIRES_AT").done();
			decidedBy=db.shortString255("DECIDED_BY").done(); address=db.shortString255("REQUESTOR_ADDRESS").done();
			lastAddress=db.shortString255("LAST_REQUESTOR_ADDRESS").done(); directAddress=db.shortString255("DIRECT_REQUESTOR_ADDRESS").done();
			lastDirectAddress=db.shortString255("LAST_DIRECT_REQUESTOR_ADDRESS").done(); userAgent=db.shortString("USER_AGENT",1000).done();
			clientHintsUserAgent=db.shortString("CLIENT_HINTS_USER_AGENT",1000).done(); clientHintsPlatform=db.shortString255("CLIENT_HINTS_PLATFORM").done();
			clientHintsMobile=db.shortString255("CLIENT_HINTS_MOBILE").done();
			GmIndex identity=db.index(name+"_IDENTITY_IDX", tokenHash,userId,entryPoint), pending=db.index(name+"_STATE_EXPIRES_IDX",state,expires), user=db.index(name+"_USER_IDX",userId);
			table=db.newTable(name).withColumns(id,contextId,tokenHash,userId,userName,entryPoint,state,created,lastSeen,decided,expires,decidedBy,address,
					lastAddress,directAddress,lastDirectAddress,userAgent,clientHintsUserAgent,clientHintsPlatform,clientHintsMobile).withIndices(identity,pending,user).done();
		}
		StoredBrowserAcceptance find(String hash,String uid,String ep) { String q=tokenHash.getSingleSqlColumn()+" = ? and "+userId.getSingleSqlColumn()+" = ? and "+entryPoint.getSingleSqlColumn()+(ep==null?" is null":" = ?"); Object[] p=ep==null?new Object[]{hash,uid}:new Object[]{hash,uid,ep}; List<GmRow> rows=table.select().where(q,p).limit(1).rows(); return rows.isEmpty()?null:read(rows.get(0)); }
		StoredBrowserAcceptance findById(String value) { List<GmRow> rows=table.select().whereColumn(id,value).limit(1).rows(); return rows.isEmpty()?null:read(rows.get(0)); }
		List<StoredBrowserAcceptance> list(BrowserAcceptanceState value) { var s=table.select(); if(value!=null)s=s.whereColumn(state,value.name()); return s.orderBy(created.getSingleSqlColumn()+" desc").mapRows(this::read); }
		void write(StoredBrowserAcceptance e) { if(findById(e.getId())==null)table.insert(values(e)); else table.update(values(e)).whereColumn(id,e.getId()); }
		private java.util.Map<GmColumn<?>,Object> values(StoredBrowserAcceptance e) { return asMap(id,e.getId(),contextId,e.getBrowserContextId(),tokenHash,e.getTokenHash(),userId,e.getUserId(),userName,e.getUserName(),entryPoint,e.getEntryPoint(),state,e.getState().name(),created,e.getCreatedAt(),lastSeen,e.getLastSeenAt(),decided,e.getDecidedAt(),expires,e.getExpiresAt(),decidedBy,e.getDecidedBy(),address,e.getRequestorAddress(),lastAddress,e.getLastRequestorAddress(),directAddress,e.getDirectRequestorAddress(),lastDirectAddress,e.getLastDirectRequestorAddress(),userAgent,e.getUserAgent(),clientHintsUserAgent,e.getClientHintsUserAgent(),clientHintsPlatform,e.getClientHintsPlatform(),clientHintsMobile,e.getClientHintsMobile()); }
		private StoredBrowserAcceptance read(GmRow r) { StoredBrowserAcceptance e=StoredBrowserAcceptance.T.create(); e.setId(r.getValue(id));e.setBrowserContextId(r.getValue(contextId));e.setTokenHash(r.getValue(tokenHash));e.setUserId(r.getValue(userId));e.setUserName(r.getValue(userName));e.setEntryPoint(r.getValue(entryPoint));e.setState(BrowserAcceptanceState.valueOf(r.getValue(state)));e.setCreatedAt(r.getValue(created));e.setLastSeenAt(r.getValue(lastSeen));e.setDecidedAt(r.getValue(decided));e.setExpiresAt(r.getValue(expires));e.setDecidedBy(r.getValue(decidedBy));e.setRequestorAddress(r.getValue(address));e.setLastRequestorAddress(r.getValue(lastAddress));e.setDirectRequestorAddress(r.getValue(directAddress));e.setLastDirectRequestorAddress(r.getValue(lastDirectAddress));e.setUserAgent(r.getValue(userAgent));e.setClientHintsUserAgent(r.getValue(clientHintsUserAgent));e.setClientHintsPlatform(r.getValue(clientHintsPlatform));e.setClientHintsMobile(r.getValue(clientHintsMobile));return e; }
	}

	private static class EventTable {
		final GmTable table; final GmColumn<String> id,acceptanceId,type,actor,address,details; final GmColumn<Date> timestamp;
		EventTable(GmDb db,String name){id=db.shortString255("ID").primaryKey().notNull().done();acceptanceId=db.shortString255("ACCEPTANCE_ID").notNull().done();type=db.shortString255("TYPE").notNull().done();timestamp=db.date("EVENT_AT").notNull().done();actor=db.shortString255("ACTOR_USER_ID").done();address=db.shortString255("REQUESTOR_ADDRESS").done();details=db.string("DETAILS").done();table=db.newTable(name).withColumns(id,acceptanceId,type,timestamp,actor,address,details).withIndices(db.index(name+"_ACCEPTANCE_TIME_IDX",acceptanceId,timestamp)).done();}
		void insert(BrowserAcceptanceEvent e){table.insert(id,e.getId(),acceptanceId,e.getAcceptanceId(),type,e.getType().name(),timestamp,e.getTimestamp(),actor,e.getActorUserId(),address,e.getRequestorAddress(),details,e.getDetails());}
	}
}
