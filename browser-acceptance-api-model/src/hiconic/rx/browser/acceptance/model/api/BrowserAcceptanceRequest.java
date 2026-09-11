package hiconic.rx.browser.acceptance.model.api;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;
@Abstract public interface BrowserAcceptanceRequest extends AuthorizedRequest { EntityType<BrowserAcceptanceRequest> T = EntityTypes.T(BrowserAcceptanceRequest.class); }
