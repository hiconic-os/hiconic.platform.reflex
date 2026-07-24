package hiconic.rx.log.reflection.model.api;

import java.util.Map;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.LogFilter;

/**
 * Internal cluster payload. Every receiving node selects its own cursor from {@link #getCursors()} and evaluates locally.
 */
public interface QueryLocalLogRecords extends LogReflectionRequest {
	EntityType<QueryLocalLogRecords> T = EntityTypes.T(QueryLocalLogRecords.class);

	String getStreamId();
	void setStreamId(String streamId);

	LogFilter getFilter();
	void setFilter(LogFilter filter);

	Map<String, String> getCursors();
	void setCursors(Map<String, String> cursors);

	@Initializer("false")
	boolean getIncludeRotated();
	void setIncludeRotated(boolean includeRotated);

	@Initializer("200")
	int getLimit();
	void setLimit(int limit);

	@Initializer("0L")
	long getWaitMillis();
	void setWaitMillis(long waitMillis);

	@Override
	EvalContext<LogRecordPage> eval(Evaluator<ServiceRequest> evaluator);
}
