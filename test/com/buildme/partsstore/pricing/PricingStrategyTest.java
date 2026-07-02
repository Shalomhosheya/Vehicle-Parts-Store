package com.buildme.partsstore.pricing;

import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Part;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for the concrete PricingStrategy implementations,
 * and for the functional-interface / lambda usage of PricingStrategy.
 */
public class PricingStrategyTest {

    private final Part highStockPart = new Part("P1", "Test Part", Category.ENGINE, 100.0, 40);
    private final Part lowStockPart = new Part("P2", "Rare Part", Category.ENGINE, 100.0, 2);

    @Test
    public void regularStrategy_appliesNoDiscount() {
        PricingStrategy strategy = new RegularPricingStrategy();
        assertEquals(500.0, strategy.calculatePrice(highStockPart, 5), 0.001);
    }

    @Test
    public void bulkDiscount_belowThreshold_appliesNoDiscount() {
        PricingStrategy strategy = new BulkDiscountPricingStrategy();
        assertEquals(500.0, strategy.calculatePrice(highStockPart, 5), 0.001);
    }

    @Test
    public void bulkDiscount_tierOne_applies10PercentOff() {
        PricingStrategy strategy = new BulkDiscountPricingStrategy();
        // 10 units * 100.0 = 1000, 10% off = 900
        assertEquals(900.0, strategy.calculatePrice(highStockPart, 10), 0.001);
    }

    @Test
    public void bulkDiscount_tierTwo_applies15PercentOff() {
        PricingStrategy strategy = new BulkDiscountPricingStrategy();
        // 20 units * 100.0 = 2000, 15% off = 1700
        assertEquals(1700.0, strategy.calculatePrice(highStockPart, 20), 0.001);
    }

    @Test
    public void clearance_onLowStockPart_applies30PercentOff() {
        PricingStrategy strategy = new ClearancePricingStrategy();
        // 2 units * 100.0 = 200, 30% off = 140
        assertEquals(140.0, strategy.calculatePrice(lowStockPart, 2), 0.001);
    }

    @Test
    public void clearance_onHighStockPart_throwsIllegalStateException() {
        PricingStrategy strategy = new ClearancePricingStrategy();
        assertThrows(IllegalStateException.class,
                () -> strategy.calculatePrice(highStockPart, 2));
    }

    @Test
    public void pricingStrategy_asLambda_isUsableLikeAnyOtherStrategy() {
        // Functional Programming: PricingStrategy is a functional interface,
        // so it can be implemented inline as a lambda without a named class.
        PricingStrategy flat20PercentOff = (part, qty) -> part.getUnitPrice() * qty * 0.80;
        assertEquals(80.0, flat20PercentOff.calculatePrice(highStockPart, 1), 0.001);
    }
}
