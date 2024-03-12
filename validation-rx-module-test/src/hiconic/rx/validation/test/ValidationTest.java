package hiconic.rx.validation.test;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;

import hiconic.rx.demo.model.api.ReverseText;
import hiconic.rx.test.common.AbstractRxTest;

public class ValidationTest extends AbstractRxTest {
	@Test
	public void testValidation() {
		System.out.println(platform.getContract().applicationName());
		
		
		ReverseText reverseText = ReverseText.T.create();
		
		Maybe<String> maybe = reverseText.eval(evaluator).getReasoned();
		
		Assertions.assertThat(maybe.isUnsatisfiedBy(InvalidArgument.T)) //
			.as("Expected validation to reflect missing mandatory property") //
			.isTrue();
	}
}
