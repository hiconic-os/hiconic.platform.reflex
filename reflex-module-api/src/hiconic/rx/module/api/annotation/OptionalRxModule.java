// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.module.api.annotation;

import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import hiconic.rx.module.api.wire.RxModule;

/** An RX module which is activated only when required or when all activation dependencies are present. */
@Retention(CLASS)
@Target({})
public @interface OptionalRxModule {

	Class<? extends RxModule<?>> module();

	Class<? extends RxModule<?>>[] activationDependencies() default {};
}
