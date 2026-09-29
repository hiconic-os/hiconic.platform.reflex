package hiconic.rx.openapi.v3.api;

public interface OpenapiDescriptionResolverRegistry {
	void registerDescriptionResolver(String name, OpenapiDescriptionResolver resolver);
	void orderDescriptionResolvers(String... names);
}
