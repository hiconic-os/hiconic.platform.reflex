// ************
// Types
// ************

import '@dev.hiconic/gm_resource-model';
import '@dev.hiconic/gm_gm-core-api';

import { T } from '@dev.hiconic/hc-js-base';

export declare namespace meta {
	const groupId: string;
	const artifactId: string;
	const version: string;
}

export import PackagedResourceNamespace = T.hiconic.rx.resource.model.packaged.PackagedResourceNamespace;
export import PackagedResourceSource = T.hiconic.rx.resource.model.packaged.PackagedResourceSource;

declare module '@dev.hiconic/hc-js-base' {

	namespace T.hiconic.rx.resource.model.packaged {

		interface PackagedResourceNamespace extends hc.reflection.EnumBase<PackagedResourceNamespace>, hc.Enum<PackagedResourceNamespace> {}
		const PackagedResourceNamespace: {
			readonly [hc.Symbol.enumType]: hc.reflection.EnumType<PackagedResourceNamespace>,
			readonly resources: PackagedResourceNamespace,
			readonly publicResources: PackagedResourceNamespace,
		}

		const PackagedResourceSource: hc.reflection.EntityType<PackagedResourceSource>;
		type PackagedResourceSource = T.com.braintribe.model.resource.source.ResourceSource &
		  Entity<"hiconic.rx.resource.model.packaged.PackagedResourceSource", {
			namespace: PackagedResourceNamespace;
			path: string;
		}>;

	}

}
