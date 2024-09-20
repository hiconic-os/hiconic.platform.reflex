package hiconic.rx.web.rest.servlet.handlers;

import java.io.IOException;

import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.ddra.endpoints.v2.DdraErrorEndpoint;
import com.braintribe.model.service.api.result.Unsatisfied;

import hiconic.rx.web.rest.servlet.RestV2EndpointContext;

public class PathErrorHandler extends AbstractRestV2Handler<DdraErrorEndpoint> {
	private Maybe<?> maybe;
	
	public PathErrorHandler(MarshallerRegistry marshallerRegistry, Maybe<?> maybe) {
		super();
		this.maybe = maybe;
		this.marshallerRegistry = marshallerRegistry;
	}

	@Override
	protected DdraErrorEndpoint createEndpoint() {
		return DdraErrorEndpoint.T.create();
	}
	
	@Override
	public void handle(RestV2EndpointContext<DdraErrorEndpoint> context) throws IOException {
		
		decode(context);
		Unsatisfied unsatisfied = Unsatisfied.from(maybe);
		context.setForceResponseCode(400);
		writeResponse(context, unsatisfied, true);
	}
}
