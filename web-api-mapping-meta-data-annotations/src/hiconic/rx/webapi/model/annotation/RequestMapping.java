package hiconic.rx.webapi.model.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import hiconic.rx.webapi.model.meta.BooleanOverride;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;

/**
 * A complete DDRA mapping. Repetition preserves the association between path, method and response options.
 * Complete mappings coexist with explicitly declared fine-grained mappings; a request path prefix applies to both.
 * Inheritable boolean behavior uses {@link BooleanOverride} because Java annotations cannot represent nullable
 * {@link Boolean} members.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(RequestMappings.class)
public @interface RequestMapping {
	String globalId() default "";
	boolean inherited() default false;
	String path();
	HttpRequestMethod method();
	String section() default "";
	String responseProjection() default "";
	BooleanOverride responseAsResourcePayload() default BooleanOverride.INHERIT;
	BooleanOverride responseWithDownloadDialog() default BooleanOverride.INHERIT;
	String responseMimeType() default "";
	BooleanOverride hideSerializedRequest() default BooleanOverride.INHERIT;
	BooleanOverride announceAsMultipart() default BooleanOverride.INHERIT;
	BooleanOverride useSessionEvaluation() default BooleanOverride.INHERIT;
	int entityRecurrenceDepth() default -1;
	String depth() default "";
	BooleanOverride decodingLenience() default BooleanOverride.INHERIT;
}
