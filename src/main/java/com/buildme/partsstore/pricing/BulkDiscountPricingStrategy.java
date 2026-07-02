package com.buildme.partsstore.pricing;

import com.buildme.partsstore.model.Part;

/*

 - Strategy Pattern - Concrete Strategy.
  -Applies tiered discounts to reward customers who buy in bulk:
    - 10 to 19 units: 10% off
    - 20+ units:      15% off
*/

@StrategyInfo(name = "BULK_DISCOUNT",
        description = "Applies tiered discounts for bulk purchases of 10 or more units.")
public class BulkDiscountPricingStrategy implements PricingStrategy {

    private static final int TIER_ONE_THRESHOLD = 10;
    private static final int TIER_TWO_THRESHOLD = 20;
    private static final double TIER_ONE_DISCOUNT = 0.10;
    private static final double TIER_TWO_DISCOUNT = 0.15;

    @Override
    public double calculatePrice(Part part, int quantity) {
        double base = part.getUnitPrice() * quantity;
        double discountRate = resolveDiscountRate(quantity);
        return base * (1 - discountRate);
    }

    private double resolveDiscountRate(int quantity) {
        if (quantity >= TIER_TWO_THRESHOLD) {
            return TIER_TWO_DISCOUNT;
        }
        if (quantity >= TIER_ONE_THRESHOLD) {
            return TIER_ONE_DISCOUNT;
        }
        return 0.0;
    }
}
