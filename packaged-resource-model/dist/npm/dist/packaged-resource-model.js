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
P.b=$.T("com.braintribe.model.meta.GmEntityType");
P.c=$.T("com.braintribe.model.meta.GmEnumType");
P.d=$.T("com.braintribe.model.meta.GmProperty");
P.e=$.T("com.braintribe.model.meta.GmEnumConstant");
P.f=$.T("com.braintribe.model.generic.value.EnumReference");
P.g=$.T("com.braintribe.model.meta.GmStringType");
//END_TYPES
P.h=$.P(P.a,'name');P.i=$.P(P.a,'types');P.j=$.P(P.a,'version');P.k=$.P(P.b,'globalId');P.l=$.P(P.b,'isAbstract');P.m=$.P(P.b,'properties');P.n=$.P(P.b,'superTypes');
P.o=$.P(P.b,'typeSignature');P.p=$.P(P.c,'constants');P.q=$.P(P.c,'globalId');P.r=$.P(P.c,'typeSignature');P.s=$.P(P.d,'declaringType');P.t=$.P(P.d,'globalId');P.u=$.P(P.d,'initializer');
P.v=$.P(P.d,'name');P.w=$.P(P.d,'nullable');P.x=$.P(P.d,'type');P.y=$.P(P.e,'declaringType');P.z=$.P(P.e,'globalId');P.A=$.P(P.e,'name');P.B=$.P(P.f,'constant');
P.C=$.P(P.f,'globalId');P.D=$.P(P.f,'typeSignature');P.E=$.P(P.g,'typeSignature');
P.F=$.C(P.a);P.G=$.C(P.b);P.H=$.C(P.c);P.I=$.C(P.d);P.J=$.C(P.d);P.K=$.C(P.b);P.L=$.C(P.e);P.M=$.C(P.e);P.N=$.C(P.f);P.O=$.C(P.g);
_=P.F;
$.s(_,P.h,"hiconic.platform.reflex:packaged-resource-model");
$.s(_,P.i,$.S([P.G,P.H]));
$.s(_,P.j,"1.0.1");
_=P.G;
$.s(_,P.k,"type:hiconic.rx.resource.model.packaged.PackagedResourceSource");
$.s(_,P.l,$.n);
$.s(_,P.m,$.L([P.I,P.J]));
$.s(_,P.n,$.L([P.K]));
$.s(_,P.o,"hiconic.rx.resource.model.packaged.PackagedResourceSource");
_=P.H;
$.s(_,P.p,$.L([P.L,P.M]));
$.s(_,P.q,"type:hiconic.rx.resource.model.packaged.PackagedResourceNamespace");
$.s(_,P.r,"hiconic.rx.resource.model.packaged.PackagedResourceNamespace");
_=P.I;
$.s(_,P.s,P.G);
$.s(_,P.t,"property:hiconic.rx.resource.model.packaged.PackagedResourceSource/namespace");
$.s(_,P.u,P.N);
$.s(_,P.v,"namespace");
$.s(_,P.w,$.y);
$.s(_,P.x,P.H);
_=P.J;
$.s(_,P.s,P.G);
$.s(_,P.t,"property:hiconic.rx.resource.model.packaged.PackagedResourceSource/path");
$.s(_,P.v,"path");
$.s(_,P.w,$.y);
$.s(_,P.x,P.O);
_=P.K;
$.s(_,P.l,$.n);
$.s(_,P.o,"com.braintribe.model.resource.source.ResourceSource");
_=P.L;
$.s(_,P.y,P.H);
$.s(_,P.z,"enum:hiconic.rx.resource.model.packaged.PackagedResourceNamespace/resources");
$.s(_,P.A,"resources");
_=P.M;
$.s(_,P.y,P.H);
$.s(_,P.z,"enum:hiconic.rx.resource.model.packaged.PackagedResourceNamespace/publicResources");
$.s(_,P.A,"publicResources");
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
