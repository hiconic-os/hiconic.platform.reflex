package hiconic.rx.log.reflection.model.api;

import java.util.Date;
import java.util.Set;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.LogBundleFormat;
import hiconic.rx.log.reflection.model.LogBundleLayout;
import hiconic.rx.log.reflection.model.LogBundleScope;
import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogTarget;

/**
 * Creates a streamed ZIP bundle. The returned resource has a transient source and is transported by Web-RPC as
 * multipart data rather than being materialized in the response model.
 */
public interface CreateLogBundle extends AuthorizedLogReflectionRequest {
	EntityType<CreateLogBundle> T = EntityTypes.T(CreateLogBundle.class);

	LogTarget getTarget();
	void setTarget(LogTarget target);

	Set<String> getStreamIds();
	void setStreamIds(Set<String> streamIds);

	/** Remote record scope. If absent, {@link #getIncludeRotated()} supplies the legacy scope. */
	LogBundleScope getScope();
	void setScope(LogBundleScope scope);

	/** Non-exclusive physical layouts. Empty means legacy behavior derived from {@link #getFormat()}. */
	Set<LogBundleLayout> getLayouts();
	void setLayouts(Set<LogBundleLayout> layouts);

	/** Non-exclusive representations. Empty means legacy behavior derived from {@link #getFormat()}. */
	Set<LogBundleFormat> getFormats();
	void setFormats(Set<LogBundleFormat> formats);

	/** Inclusive lower boundary used by {@link LogBundleScope#CUSTOM_RANGE}. */
	Date getFrom();
	void setFrom(Date from);

	/** Inclusive upper boundary used by {@link LogBundleScope#CUSTOM_RANGE}. */
	Date getTo();
	void setTo(Date to);

	@Initializer("false")
	@Deprecated
	boolean getIncludeRotated();
	void setIncludeRotated(boolean includeRotated);

	@Initializer("enum(hiconic.rx.log.reflection.model.LogBundleFormat,RAW_FILES)")
	@Deprecated
	LogBundleFormat getFormat();
	void setFormat(LogBundleFormat format);

	/** Retained for wire compatibility; support bundles deliberately ignore interactive UI filters. */
	LogFilter getFilter();
	void setFilter(LogFilter filter);

	@Override
	EvalContext<Resource> eval(Evaluator<ServiceRequest> evaluator);
}
