package hiconic.platform.reflex.security.processor;

import java.util.Date;

import com.braintribe.gm.jdbc.api.GmColumn;
import com.braintribe.gm.jdbc.api.GmDb;
import com.braintribe.gm.jdbc.api.GmIndex;
import com.braintribe.gm.jdbc.api.GmTable;

public class UserSessionJdbcTableDefinition {
	
	public final GmDb gmDb;
	public final GmTable gmTable;
	
	public final GmColumn<String> id;
	public final GmColumn<String> userName;
	public final GmColumn<String> userFirstName;
	public final GmColumn<String> userLastName;
	public final GmColumn<String> userEmail;
	
	public final GmColumn<Date> creationDate;
	public final GmColumn<Date> fixedExpiryDate;
	public final GmColumn<Date> expiryDate;
	public final GmColumn<Date> lastAccessedDate;
	
	public final GmColumn<Long> maxIdleTime;
	public final GmColumn<String> effectiveRoles;

	
	public final GmColumn<String> acquirationKey;

	public final GmColumn<Boolean> blocksAuthenticationAfterLogout;
	public final GmColumn<Boolean> closed;

	
	public final GmColumn<String> sessionType;
	
	public final GmColumn<String> creationInternetAddress;
	public final GmColumn<String> creationNodeId;
	public final GmColumn<String> properties;

	public UserSessionJdbcTableDefinition(GmDb gmDb, String tableName) {
		super();
		this.gmDb = gmDb;
		this.id = gmDb.shortString255("id").primaryKey().notNull().done();
		this.userName = gmDb.shortString255("USER_NAME").done();
		this.userFirstName = gmDb.shortString255("USER_FIRST_NAME").done();
		this.userLastName = gmDb.shortString255("USER_LAST_NAME").done();
		this.userEmail = gmDb.shortString255("USER_EMAIL").done();
		
		this.creationDate = gmDb.date("CREATION_DATE").done();
		this.fixedExpiryDate = gmDb.date("FIXED_EXPIRY_DATE").done();
		this.expiryDate = gmDb.date("EXPIRY_DATE").done();
		this.lastAccessedDate = gmDb.date("LAST_ACCESSED_DATE").done();
		
		this.maxIdleTime = gmDb.longCol("MAX_IDLE_TIME").done();
		this.effectiveRoles = gmDb.shortString("EFFECTIVE_ROLES", 4000).done();
		
		this.blocksAuthenticationAfterLogout = gmDb.booleanCol("BLOCKS_AUTHENTICATION_AFTER_LOGOUT").done();
		this.closed = gmDb.booleanCol("CLOSED").done();
		
		this.sessionType = gmDb.shortString255("SESSION_TYPE").done();
		
		this.creationInternetAddress = gmDb.shortString("CREATION_INTERNET_ADDRESS", 1000).done();
		this.creationNodeId = gmDb.shortString("CREATION_NODE_ID", 1000).done();
		this.properties = gmDb.shortString("PROPERTIES", 4000).done();
		
		this.acquirationKey = gmDb.shortString255("ACQUIRATION_KEY").done();
		
		GmIndex acquirationKeyIndex = gmDb.index(tableName + "_" + acquirationKey.getGmName() + "_INDEX", acquirationKey);
		GmIndex fixedExpiryDateIndex = gmDb.index(tableName + "_" + fixedExpiryDate.getGmName() + "_INDEX", fixedExpiryDate);
		GmIndex expiryDateIndex = gmDb.index(tableName + "_" + expiryDate.getGmName() + "_INDEX", expiryDate);
		
		gmTable = gmDb.newTable(tableName).withColumns(
				 id,
				 userName,
				 userFirstName,
				 userLastName,
				 userEmail,
				 creationDate,
				 fixedExpiryDate,
				 expiryDate,
				 lastAccessedDate,
				 maxIdleTime,
				 effectiveRoles,
				 acquirationKey,
				 blocksAuthenticationAfterLogout,
				 closed,
				 sessionType,
				 creationInternetAddress,
				 creationNodeId,
				 properties
		).withIndices(
				acquirationKeyIndex, 
				fixedExpiryDateIndex, 
				expiryDateIndex
		).done();
	}
	
	
	

}
