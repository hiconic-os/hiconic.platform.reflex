package hiconic.rx.browser.acceptance.model.api;
import java.util.List;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
public interface BrowserAcceptances extends GenericEntity { EntityType<BrowserAcceptances> T=EntityTypes.T(BrowserAcceptances.class); List<BrowserAcceptance> getAcceptances(); void setAcceptances(List<BrowserAcceptance> value); }
