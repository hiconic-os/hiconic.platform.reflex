// ************
// Types
// ************

import '@dev.hiconic/gm_root-model';
import '@dev.hiconic/platform.reflex_access-configuration-model';
import '@dev.hiconic/gm_gm-core-api';

import { T } from '@dev.hiconic/hc-js-base';

export declare namespace meta {
	const groupId: string;
	const artifactId: string;
	const version: string;
}

export import SmoodAccess = T.hiconic.rx.access.smood.model.configuration.SmoodAccess;

declare module '@dev.hiconic/hc-js-base' {

	namespace T.hiconic.rx.access.smood.model.configuration {

		const SmoodAccess: hc.reflection.EntityType<SmoodAccess>;
		type SmoodAccess = T.hiconic.rx.access.model.configuration.Access &
		  Entity<"hiconic.rx.access.smood.model.configuration.SmoodAccess">;

	}

}
