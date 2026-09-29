package hiconic.rx.openapi.v3.processing;

import java.util.ArrayList;
import java.util.List;

import hiconic.rx.openapi.v3.api.OpenapiDescriptionResolver;
import hiconic.rx.openapi.v3.api.OpenapiDescriptionResolverRegistry;

public class OpenapiDescriptionResolverRegistryImpl implements OpenapiDescriptionResolverRegistry {

	public record ResolverEntry(String name, OpenapiDescriptionResolver resolver) {}

	private final List<ResolverEntry> resolvers = new ArrayList<>();

	@Override
	public void registerDescriptionResolver(String name, OpenapiDescriptionResolver resolver) {
		synchronized (resolvers) {
			resolvers.add(new ResolverEntry(name, resolver));
		}
	}

	public List<ResolverEntry> getResolvers() {
		synchronized (resolvers) {
			return new ArrayList<>(resolvers);
		}
	}

	@Override
	public void orderDescriptionResolvers(String... names) {
		synchronized (resolvers) {
			if (names == null || names.length < 2)
				return;

			for (int i = 0; i < names.length - 1; i++) {
				String beforeName = names[i];
				String afterName = names[i + 1];
				if (beforeName == null || afterName == null || beforeName.equals(afterName))
					continue;

				int beforeIndex = indexOf(beforeName);
				int afterIndex = indexOf(afterName);
				if (beforeIndex < 0 || afterIndex < 0 || beforeIndex < afterIndex)
					continue;

				ResolverEntry afterEntry = resolvers.remove(afterIndex);
				resolvers.add(beforeIndex, afterEntry);
			}
		}
	}

	private int indexOf(String name) {
		for (int i = 0; i < resolvers.size(); i++) {
			if (resolvers.get(i).name().equals(name))
				return i;
		}
		return -1;
	}
}
