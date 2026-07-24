package hiconic.rx.webapi.model.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import hiconic.rx.webapi.model.meta.HttpRequestMethod;

/** A complete DDRA mapping. Repetition preserves the association between path, method and response options. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(RequestMappings.class)
public @interface RequestMapping {
	String globalId() default "";
	String path();
	HttpRequestMethod method();
	String section() default "";
	String responseProjection() default "";
	boolean responseAsResourcePayload() default false;
	boolean responseWithDownloadDialog() default false;
	String responseMimeType() default "";
	boolean hideSerializedRequest() default false;
	boolean announceAsMultipart() default false;
	boolean useSessionEvaluation() default false;
	int entityRecurrenceDepth() default -1;
	String depth() default "";
	boolean decodingLenience() default false;
}
