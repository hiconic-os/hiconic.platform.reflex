package hiconic.rx.log.reflection.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.logging.LogLevel;

public interface LogFilter extends GenericEntity {
	EntityType<LogFilter> T = EntityTypes.T(LogFilter.class);

	Date getFrom();
	void setFrom(Date from);

	Date getTo();
	void setTo(Date to);

	Set<LogLevel> getLevels();
	void setLevels(Set<LogLevel> levels);

	Set<String> getLoggerNames();
	void setLoggerNames(Set<String> loggerNames);

	String getLoggerNameContains();
	void setLoggerNameContains(String loggerNameContains);

	Set<String> getThreadNames();
	void setThreadNames(Set<String> threadNames);

	String getFulltext();
	void setFulltext(String fulltext);

	List<LogPropertyFilter> getPropertyFilters();
	void setPropertyFilters(List<LogPropertyFilter> propertyFilters);
}
