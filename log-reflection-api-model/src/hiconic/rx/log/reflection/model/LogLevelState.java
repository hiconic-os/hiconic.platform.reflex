package hiconic.rx.log.reflection.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogLevelState extends GenericEntity {
	EntityType<LogLevelState> T = EntityTypes.T(LogLevelState.class);

	Map<String, String> getEffectiveLevels();
	void setEffectiveLevels(Map<String, String> effectiveLevels);

	Map<String, String> getRuntimeLevels();
	void setRuntimeLevels(Map<String, String> runtimeLevels);

	Map<String, String> getPackagedLevels();
	void setPackagedLevels(Map<String, String> packagedLevels);

	Set<String> getKnownLoggerNames();
	void setKnownLoggerNames(Set<String> knownLoggerNames);

	List<LogLevelProfile> getPackagedProfiles();
	void setPackagedProfiles(List<LogLevelProfile> profiles);

	List<LogLevelProfile> getRuntimeProfiles();
	void setRuntimeProfiles(List<LogLevelProfile> profiles);

	List<String> getPackagedProfileCascade();
	void setPackagedProfileCascade(List<String> cascade);

	List<String> getRuntimeProfileCascade();
	void setRuntimeProfileCascade(List<String> cascade);

	List<String> getEffectiveProfileCascade();
	void setEffectiveProfileCascade(List<String> cascade);

	boolean getProfileSupport();
	void setProfileSupport(boolean profileSupport);
}
