package hiconic.rx.platform.loading;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import com.braintribe.wire.api.space.ContractResolution;
import com.braintribe.wire.api.space.ContractSpaceResolver;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxContractSpaceResolverConfigurator;

public class RxConfigurableContractSpaceResolver implements RxContractSpaceResolverConfigurator, ContractSpaceResolver {

	private List<ContractSpaceResolver> contractSpaceResolvers = emptyList();

	@Override
	public synchronized void addResolver(ContractSpaceResolver resolver) {
		List<ContractSpaceResolver> newList = new ArrayList<>(contractSpaceResolvers);
		newList.add(resolver);

		contractSpaceResolvers = newList;
	}

	@Override
	public ContractResolution resolveContractSpace(Class<? extends WireSpace> contractSpaceClass) {
		for (ContractSpaceResolver resolver : contractSpaceResolvers) {
			ContractResolution resolution = resolver.resolveContractSpace(contractSpaceClass);

			if (resolution != null)
				return resolution;
		}

		return null;
	}

}
