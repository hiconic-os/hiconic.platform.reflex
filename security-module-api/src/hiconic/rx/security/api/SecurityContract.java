package hiconic.rx.security.api;

import java.util.Set;

import com.braintribe.model.processing.securityservice.api.UserSessionScoping;

import hiconic.rx.module.api.wire.RxExportContract;

/**
 * 
 */
public interface SecurityContract extends RxExportContract {

	UserSessionScoping userSessionScoping();

	Set<String> adminRoles();

	String internalRole();

	Set<String> adminAndInternalRoles();

}
