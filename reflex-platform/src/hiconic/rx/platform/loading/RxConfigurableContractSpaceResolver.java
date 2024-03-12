package hiconic.rx.platform.loading;

import com.braintribe.wire.api.space.ContractResolution;
import com.braintribe.wire.api.space.ContractSpaceResolver;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxContractSpaceResolverConfigurator;

public class RxConfigurableContractSpaceResolver implements RxContractSpaceResolverConfigurator, ContractSpaceResolver {

	@Override
	public void addResolver(ContractSpaceResolver resolver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ContractResolution resolveContractSpace(Class<? extends WireSpace> contractSpaceClass) {
		// TODO Auto-generated method stub
		return null;
	}

}
