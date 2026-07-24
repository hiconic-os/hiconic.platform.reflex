// ============================================================================
package hiconic.rx.scripting.groovy.processing;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.resource.Resources;

import tribefire.extension.scripting.deployment.model.GroovyScript;
import tribefire.extension.scripting.model.ScriptCompileError;
import tribefire.extension.scripting.model.ScriptRuntimeError;

public class GroovyScriptingTest {

	@Test
	public void testGroovy() {
		Maybe<String> resultMaybe = evaluate("return param.toLowerCase();", Collections.singletonMap("param", "FoOBAR"));

		if (resultMaybe.isUnsatisfied())
			Assertions.fail("unexpected evaluation problem with reason: " + resultMaybe.whyUnsatisfied().stringify());

		String result = resultMaybe.get();

		Assertions.assertThat(result).withFailMessage("Script did not return expected value").isEqualTo("foobar");
	}

	@Test
	public void testBrokenSyntaxGroovy() {
		Maybe<String> resultMaybe = evaluate("return param.toLowerCase..NONSENSE-SYNTAX..();", Collections.singletonMap("param", "FoOBAR"));

		if (resultMaybe.isUnsatisfiedBy(ScriptCompileError.T))
			return;

		Assertions.fail("ScriptCompileError was expected");
	}

	@Test
	public void testBrokenRuntimeGroovy() {
		Maybe<String> resultMaybe = evaluate("return NONSENSE_PARAM.toLowerCase();", Collections.singletonMap("param", "FoOBAR"));

		if (resultMaybe.isUnsatisfiedBy(ScriptRuntimeError.T))
			return;

		Assertions.fail("ScriptRuntimeError was expected");
	}

	private <R> Maybe<R> evaluate(String code, Map<String, Object> bindings) {
		GroovyEngine engine = new GroovyEngine();

		GroovyScript script = GroovyScript.T.create();
		script.setSource(Resources.createTransient(code));

		return engine.evaluate(script, bindings);
	}

}
