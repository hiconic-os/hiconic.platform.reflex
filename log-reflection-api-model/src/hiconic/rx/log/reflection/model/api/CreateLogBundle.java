package hiconic.rx.log.reflection.model.api;

import java.util.Set;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.LogBundleFormat;
import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogTarget;

/**
 * Creates a streamed ZIP bundle. The returned resource has a transient source and is transported by Web-RPC as
 * multipart data rather than being materialized in the response model.
 */
public interface CreateLogBundle extends LogReflectionRequest {
	EntityType<CreateLogBundle> T = EntityTypes.T(CreateLogBundle.class);

	LogTarget getTarget();
	void setTarget(LogTarget target);

	Set<String> getStreamIds();
	void setStreamIds(Set<String> streamIds);

	@Initializer("false")
	boolean getIncludeRotated();
	void setIncludeRotated(boolean includeRotated);

	@Initializer("enum(hiconic.rx.log.reflection.model.LogBundleFormat,RAW_FILES)")
	LogBundleFormat getFormat();
	void setFormat(LogBundleFormat format);

	LogFilter getFilter();
	void setFilter(LogFilter filter);

	@Override
	EvalContext<Resource> eval(Evaluator<ServiceRequest> evaluator);
}
