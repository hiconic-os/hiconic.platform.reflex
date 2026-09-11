package hiconic.rx.browser.acceptance.wire.space;

import javax.sql.DataSource;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import hiconic.rx.browser.acceptance.model.api.*;
import hiconic.rx.browser.acceptance.model.configuration.BrowserAcceptanceConfiguration;
import hiconic.rx.browser.acceptance.processing.*;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.SecurityExtensionContract;
import hiconic.rx.security.api.SecurityServiceDomain;
import hiconic.rx.security.web.api.WebSecurityExtensionContract;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import hiconic.rx.webapi.model.meta.RequestMapping;

@Managed public class BrowserAcceptanceRxModuleSpace implements RxModuleContract {
	@Import private RxPlatformContract platform; @Import private DatabaseContract database; @Import private SecurityExtensionContract security; @Import private WebSecurityExtensionContract webSecurity;
	private boolean registered;
	@Override public void configureServiceDomains(ServiceDomainConfigurations configurations){
		BrowserAcceptanceConfiguration c=config(); if(!c.getEnabled())return; registerExtensions();
		ServiceDomainConfiguration domain=configurations.byId(SecurityServiceDomain.security); domain.bindRequest(BrowserAcceptanceRequest.T,this::processor);
		domain.configureModel(e->{map(e,ListBrowserAcceptances.T,"browser-acceptances",HttpRequestMethod.GET);map(e,ApproveBrowserAcceptance.T,"browser-acceptances/approve",HttpRequestMethod.POST);map(e,RejectBrowserAcceptance.T,"browser-acceptances/reject",HttpRequestMethod.POST);map(e,RevokeBrowserAcceptance.T,"browser-acceptances/revoke",HttpRequestMethod.POST);});
	}
	private void registerExtensions(){if(registered)return;registered=true;security.registerUserSessionOpeningVerificationExpert(verifier());security.registerUserSessionAccessVerificationExpert(verifier());webSecurity.registerRequestContextContributor(cookieContributor());}
	private <T extends com.braintribe.model.generic.GenericEntity> void map(ModelMetaDataEditor e,com.braintribe.model.generic.reflection.EntityType<T> type,String path,HttpRequestMethod method){RequestMapping m=RequestMapping.T.create();m.setPath(path);m.setMethod(method);m.setResponseMimeType("application/json");e.onEntityType(type).addMetaData(m);}
	@Managed private BrowserAcceptanceConfiguration config(){return UnsatisfiedMaybeTunneling.getOrTunnel(platform.configuration().readConfig(BrowserAcceptanceConfiguration.T));}
	@Managed private BrowserAcceptanceStore store(){BrowserAcceptanceConfiguration c=config();DataSource ds=database.findDataSource(c.getDataSource());if(ds==null)throw new IllegalStateException("Browser acceptance is enabled but data source is missing: "+c.getDataSource());return new BrowserAcceptanceStore(ds,c.getTableNamePrefix(),c.getPendingLifetimeSeconds(),c.getAcceptanceLifetimeSeconds());}
	@Managed private BrowserAcceptancePolicyMatcher matcher(){return new BrowserAcceptancePolicyMatcher(config().getPolicies());}
	@Managed private BrowserAcceptanceVerifier verifier(){return new BrowserAcceptanceVerifier(matcher(),store());}
	@Managed private BrowserContextCookieContributor cookieContributor(){BrowserAcceptanceConfiguration c=config();return new BrowserContextCookieContributor(c.getCookieName(),c.getCookieMaxAgeSeconds());}
	@Managed private BrowserAcceptanceProcessor processor(){return new BrowserAcceptanceProcessor(store(),config().getApproverRoles());}
}
