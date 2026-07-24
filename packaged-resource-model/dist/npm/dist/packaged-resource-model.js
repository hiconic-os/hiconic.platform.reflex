import '@dev.hiconic/gm_resource-model';
import '@dev.hiconic/gm_gm-core-api';

import {T, hc} from '@dev.hiconic/hc-js-base';

export const meta = {
	groupId: "hiconic.platform.reflex",
	artifactId: "packaged-resource-model",
	version: "1.0.1",
}

function modelAssembler($, P, _) {
//JSE version=4.0
//BEGIN_TYPES
P.a=$.T("com.braintribe.model.meta.GmMetaModel");
P.b=$.T("com.braintribe.model.meta.GmEnumType");
P.c=$.T("com.braintribe.model.meta.GmEntityType");
P.d=$.T("com.braintribe.model.meta.GmEnumConstant");
P.e=$.T("com.braintribe.model.meta.GmProperty");
P.f=$.T("com.braintribe.model.generic.value.EnumReference");
P.g=$.T("com.braintribe.model.meta.GmStringType");
//END_TYPES
P.h=$.P(P.a,'name');P.i=$.P(P.a,'types');P.j=$.P(P.a,'version');P.k=$.P(P.b,'constants');P.l=$.P(P.b,'globalId');P.m=$.P(P.b,'typeSignature');P.n=$.P(P.c,'globalId');
P.o=$.P(P.c,'isAbstract');P.p=$.P(P.c,'properties');P.q=$.P(P.c,'superTypes');P.r=$.P(P.c,'typeSignature');P.s=$.P(P.d,'declaringType');P.t=$.P(P.d,'globalId');P.u=$.P(P.d,'name');
P.v=$.P(P.e,'declaringType');P.w=$.P(P.e,'globalId');P.x=$.P(P.e,'initializer');P.y=$.P(P.e,'name');P.z=$.P(P.e,'nullable');P.A=$.P(P.e,'type');P.B=$.P(P.f,'constant');
P.C=$.P(P.f,'globalId');P.D=$.P(P.f,'typeSignature');P.E=$.P(P.g,'typeSignature');
P.F=$.C(P.a);P.G=$.C(P.b);P.H=$.C(P.c);P.I=$.C(P.d);P.J=$.C(P.d);P.K=$.C(P.e);P.L=$.C(P.e);P.M=$.C(P.c);P.N=$.C(P.f);P.O=$.C(P.g);
_=P.F;
$.s(_,P.h,"hiconic.platform.reflex:packaged-resource-model");
$.s(_,P.i,$.S([P.G,P.H]));
$.s(_,P.j,"1.0.1");
_=P.G;
$.s(_,P.k,$.L([P.I,P.J]));
$.s(_,P.l,"type:hiconic.rx.resource.model.packaged.PackagedResourceNamespace");
$.s(_,P.m,"hiconic.rx.resource.model.packaged.PackagedResourceNamespace");
_=P.H;
$.s(_,P.n,"type:hiconic.rx.resource.model.packaged.PackagedResourceSource");
$.s(_,P.o,$.n);
$.s(_,P.p,$.L([P.K,P.L]));
$.s(_,P.q,$.L([P.M]));
$.s(_,P.r,"hiconic.rx.resource.model.packaged.PackagedResourceSource");
_=P.I;
$.s(_,P.s,P.G);
$.s(_,P.t,"enum:hiconic.rx.resource.model.packaged.PackagedResourceNamespace/resources");
$.s(_,P.u,"resources");
_=P.J;
$.s(_,P.s,P.G);
$.s(_,P.t,"enum:hiconic.rx.resource.model.packaged.PackagedResourceNamespace/publicResources");
$.s(_,P.u,"publicResources");
_=P.K;
$.s(_,P.v,P.H);
$.s(_,P.w,"property:hiconic.rx.resource.model.packaged.PackagedResourceSource/namespace");
$.s(_,P.x,P.N);
$.s(_,P.y,"namespace");
$.s(_,P.z,$.y);
$.s(_,P.A,P.G);
_=P.L;
$.s(_,P.v,P.H);
$.s(_,P.w,"property:hiconic.rx.resource.model.packaged.PackagedResourceSource/path");
$.s(_,P.y,"path");
$.s(_,P.z,$.y);
$.s(_,P.A,P.O);
_=P.M;
$.s(_,P.o,$.n);
$.s(_,P.r,"com.braintribe.model.resource.source.ResourceSource");
_=P.N;
$.s(_,P.B,"resources");
$.s(_,P.C,"initializer:property:hiconic.rx.resource.model.packaged.PackagedResourceSource/namespace");
$.s(_,P.D,"hiconic.rx.resource.model.packaged.PackagedResourceNamespace");
_=P.O;
$.s(_,P.E,"string");
return P.F;
[2635];
}

hc.reflection.internal.ensureModel(modelAssembler)

export const PackagedResourceNamespace = T.hiconic.rx.resource.model.packaged.PackagedResourceNamespace;
export const PackagedResourceSource = T.hiconic.rx.resource.model.packaged.PackagedResourceSource;
