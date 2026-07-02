package com.buildme.partsstore.pricing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*

 ..Metadata annotation attached to concrete PricingStrategy implementations.
 ..This annotation is read at runtime via reflection by StrategyRegistry
 .. (see com.buildme.partsstore.reflection.StrategyRegistry) to register a
 ..human-readable key and description for each strategy without having to
 .. hard-code that information anywhere else in the codebase.

*/

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StrategyInfo {

//     Short, unique key used to look the strategy up at runtime, e.g. "BULK_DISCOUNT".
    String name();

//    Human-readable description of what the strategy does.
    String description();
}
