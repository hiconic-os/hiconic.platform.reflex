package hiconic.rx.module.api.config;

import com.braintribe.gm.model.reason.Maybe;

public interface PropertyResolver {
	public String resolve(String name);
	public Maybe<String> resolveReasoned(String name);
}
