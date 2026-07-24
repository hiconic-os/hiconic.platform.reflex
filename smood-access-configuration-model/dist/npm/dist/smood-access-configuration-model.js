import '@dev.hiconic/gm_root-model';
import '@dev.hiconic/platform.reflex_access-configuration-model';
import '@dev.hiconic/gm_gm-core-api';

import {T, hc} from '@dev.hiconic/hc-js-base';

export const meta = {
	groupId: "hiconic.platform.reflex",
	artifactId: "smood-access-configuration-model",
	version: "1.0.1",
}

function modelAssembler($, P, _) {
//JSE version=4.0
//BEGIN_TYPES
P.a=$.T("com.braintribe.model.meta.GmMetaModel");
P.b=$.T("com.braintribe.model.meta.GmEntityType");
//END_TYPES
P.c=$.P(P.a,'name');P.d=$.P(P.a,'types');P.e=$.P(P.a,'version');P.f=$.P(P.b,'globalId');P.g=$.P(P.b,'isAbstract');P.h=$.P(P.b,'superTypes');P.i=$.P(P.b,'typeSignature');
P.j=$.C(P.a);P.k=$.C(P.b);P.l=$.C(P.b);
_=P.j;
$.s(_,P.c,"hiconic.platform.reflex:smood-access-configuration-model");
$.s(_,P.d,$.S([P.k]));
$.s(_,P.e,"1.0.1");
_=P.k;
$.s(_,P.f,"type:hiconic.rx.access.smood.model.configuration.SmoodAccess");
$.s(_,P.g,$.n);
$.s(_,P.h,$.L([P.l]));
$.s(_,P.i,"hiconic.rx.access.smood.model.configuration.SmoodAccess");
_=P.l;
$.s(_,P.g,$.n);
$.s(_,P.i,"hiconic.rx.access.model.configuration.Access");
return P.j;
[760];
}

hc.reflection.internal.ensureModel(modelAssembler)

export const SmoodAccess = T.hiconic.rx.access.smood.model.configuration.SmoodAccess;
