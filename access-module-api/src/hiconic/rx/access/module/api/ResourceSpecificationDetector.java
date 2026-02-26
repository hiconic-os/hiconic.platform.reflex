// ============================================================================
package hiconic.rx.access.module.api;

import java.io.IOException;
import java.io.InputStream;

import com.braintribe.model.generic.session.GmSession;
import com.braintribe.model.resource.specification.ResourceSpecification;

public interface ResourceSpecificationDetector<T extends ResourceSpecification> {
	
	T getSpecification(InputStream in, String mimeType, GmSession session) throws IOException;
	
	
}
