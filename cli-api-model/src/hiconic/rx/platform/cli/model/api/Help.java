package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

@PositionalArguments({"type"})
@Description("Prints details for a specified type (e.g. command).")
public interface Help extends CliRequest {
	EntityType<Help> T = EntityTypes.T(Help.class);

	@Alias("t")
	@Description("The type (e.g. command) for which help should be printed.")
	String getType();
	void setType(String type);
	
	@Alias("d")
	@Description("Includes deprecated types/properties.")
	boolean getDeprecated();
	void setDeprecated(boolean deprecated);
	
	@Alias("u")
	@Description("Includes up-to-date types/properties.")
	@Initializer("true")
	boolean getUpToDate();
	void setUpToDate(boolean upToDate);
	
	@Alias("m")
	@Description("Includes mandatory properties.")
	@Initializer("true")
	boolean getMandatory();
	void setMandatory(boolean mandatory);
	
	@Alias("o")
	@Description("Includes optional properties.")
	@Initializer("true")
	boolean getOptional();
	void setOptional(boolean optional);
	
	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}
