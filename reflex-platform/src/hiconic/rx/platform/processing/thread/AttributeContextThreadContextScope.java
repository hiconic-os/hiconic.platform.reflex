package hiconic.rx.platform.processing.thread;

import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.thread.api.ThreadContextScope;
import com.braintribe.utils.collection.impl.AttributeContexts;

public class AttributeContextThreadContextScope implements ThreadContextScope {
	
	public static final Supplier<ThreadContextScope> SUPPLIER = () -> new AttributeContextThreadContextScope();

	private AttributeContext callerContext = AttributeContexts.peek();
	
	@Override
	public void push() {
		AttributeContexts.push(callerContext);
	}

	@Override
	public void pop() {
		AttributeContexts.pop();
	}
}
