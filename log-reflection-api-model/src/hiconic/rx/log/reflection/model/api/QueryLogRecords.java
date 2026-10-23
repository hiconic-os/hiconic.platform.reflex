package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogTarget;

public interface QueryLogRecords extends AuthorizedLogReflectionRequest {
	EntityType<QueryLogRecords> T = EntityTypes.T(QueryLogRecords.class);

	LogTarget getTarget();
	void setTarget(LogTarget target);

	String getStreamId();
	void setStreamId(String streamId);

	LogFilter getFilter();
	void setFilter(LogFilter filter);

	/** Continues towards newer records; kept independently from backwards history paging. */
	String getCursor();
	void setCursor(String cursor);

	/**
	 * Loads the page immediately preceding this cursor. This is independent from {@link #getCursor()}, which continues
	 * towards newer records and is used for tailing.
	 */
	String getBeforeCursor();
	void setBeforeCursor(String beforeCursor);

	/**
	 * Includes segments discovered from the appender's rolling policy in the initial history window. Follow-up/tail
	 * requests continue on the active file and therefore do not replay archived records.
	 */
	@Initializer("false")
	boolean getIncludeRotated();
	void setIncludeRotated(boolean includeRotated);

	/**
	 * Maximum number of records read per selected node. Consequently an all-node response may contain up to
	 * {@code limit * respondingNodeCount} records without losing records between independent node cursors.
	 */
	@Initializer("200")
	int getLimit();
	void setLimit(int limit);

	@Initializer("0L")
	long getWaitMillis();
	void setWaitMillis(long waitMillis);

	@Override
	EvalContext<LogRecordPage> eval(Evaluator<ServiceRequest> evaluator);
}
