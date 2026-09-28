// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.module.api.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import hiconic.rx.module.api.wire.RxModule;

/** Describes the RX modules contributed by an artifact without loading them. */
@Retention(CLASS)
@Target(PACKAGE)
public @interface RxArtifact {

	Class<? extends RxModule<?>>[] modules() default {};

	OptionalRxModule[] optionalModules() default {};
}
