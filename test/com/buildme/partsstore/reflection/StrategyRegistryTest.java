package com.buildme.partsstore.reflection;

import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Part;
import com.buildme.partsstore.pricing.PricingStrategy;
import com.buildme.partsstore.pricing.RegularPricingStrategy;
import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Validates that reflection is being used correctly to discover, describe
 * and instantiate PricingStrategy implementations at runtime.
 */
public class StrategyRegistryTest {

    private final Part part = new Part("P1", "Test Part", Category.ENGINE, 50.0, 10);

    @Test
    public void registry_discoversAllThreeKnownStrategies() {
        StrategyRegistry registry = new StrategyRegistry();
        Set<String> keys = registry.availableStrategies();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("REGULAR"));
        assertTrue(keys.contains("BULK_DISCOUNT"));
        assertTrue(keys.contains("CLEARANCE"));
    }

    @Test
    public void createStrategy_reflectivelyInstantiatesCorrectClass() {
        StrategyRegistry registry = new StrategyRegistry();

        PricingStrategy strategy = registry.createStrategy("REGULAR");

        assertTrue("Expected a RegularPricingStrategy instance",
                strategy instanceof RegularPricingStrategy);
    }

    @Test
    public void createStrategy_producesFunctionallyCorrectStrategy() {
        StrategyRegistry registry = new StrategyRegistry();

        PricingStrategy strategy = registry.createStrategy("REGULAR");
        double price = strategy.calculatePrice(part, 4);

        assertEquals(200.0, price, 0.001);
    }

    @Test
    public void createStrategy_withUnknownKey_throwsNoSuchElementException() {
        StrategyRegistry registry = new StrategyRegistry();

        assertThrows(NoSuchElementException.class,
                () -> registry.createStrategy("DOES_NOT_EXIST"));
    }

    @Test
    public void describe_includesAnnotationDrivenDescriptionAndMethodNames() {
        StrategyRegistry registry = new StrategyRegistry();

        String description = registry.describe("BULK_DISCOUNT");

        assertTrue(description.contains("BulkDiscountPricingStrategy"));
        assertTrue(description.contains("tiered discounts"));
        assertTrue(description.contains("calculatePrice"));
    }
}
