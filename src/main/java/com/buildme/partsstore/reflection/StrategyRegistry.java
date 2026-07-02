package com.buildme.partsstore.reflection;

import com.buildme.partsstore.pricing.PricingStrategy;
import com.buildme.partsstore.pricing.StrategyInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

//Reflection: this class is responsible for discovering, describing

public class StrategyRegistry {

    private static final List<String> KNOWN_STRATEGY_CLASSES = List.of(
            "com.buildme.partsstore.pricing.RegularPricingStrategy",
            "com.buildme.partsstore.pricing.BulkDiscountPricingStrategy",
            "com.buildme.partsstore.pricing.ClearancePricingStrategy"
    );

    private final Map<String, Class<? extends PricingStrategy>> registry = new LinkedHashMap<>();

    public StrategyRegistry() {
        for (String className : KNOWN_STRATEGY_CLASSES) {
            registerByClassName(className);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerByClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!PricingStrategy.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(className + " does not implement PricingStrategy");
            }
            StrategyInfo info = clazz.getAnnotation(StrategyInfo.class);
            String key = (info != null) ? info.name() : clazz.getSimpleName();
            registry.put(key, (Class<? extends PricingStrategy>) clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Strategy class not found on classpath: " + className, e);
        }
    }

    /*
      Reflectively instantiates the strategy registered under {@code key}
      using its no-argument constructor.
    */
    public PricingStrategy createStrategy(String key) {
        Class<? extends PricingStrategy> clazz = registry.get(key);
        if (clazz == null) {
            throw new NoSuchElementException("No strategy registered under key: " + key);
        }
        try {
            Constructor<? extends PricingStrategy> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to instantiate strategy: " + clazz.getName(), e);
        }
    }

    /*
      Builds a human-readable description of a registered strategy by
      reading its @StrategyInfo annotation and its declared methods -
      both obtained purely through reflection.
    */
    public String describe(String key) {
        Class<? extends PricingStrategy> clazz = registry.get(key);
        if (clazz == null) {
            throw new NoSuchElementException("No strategy registered under key: " + key);
        }
        StrategyInfo info = clazz.getAnnotation(StrategyInfo.class);
        String methodList = Arrays.stream(clazz.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.joining(", "));

        return String.format("%s -> %s : %s [declared methods: %s]",
                key,
                clazz.getSimpleName(),
                info != null ? info.description() : "(no description)",
                methodList);
    }

    public Set<String> availableStrategies() {
        return registry.keySet();
    }
}
