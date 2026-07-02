package com.buildme.partsstore.pricing;

import com.buildme.partsstore.model.Part;

/*
 Strategy Pattern - Concrete Strategy.
  Flat 30% discount, but only applicable to parts that are already low in
  stock (fewer than LOW_STOCK_THRESHOLD units) - it exists to help clear
  ageing stock, not to be used as a general-purpose discount.
*/

@StrategyInfo(name = "CLEARANCE",
        description = "Flat 30% discount for clearing low-stock parts (stock below 5 units).")
public class ClearancePricingStrategy implements PricingStrategy {

    private static final double CLEARANCE_DISCOUNT = 0.30;
    private static final int LOW_STOCK_THRESHOLD = 5;

    @Override
    public double calculatePrice(Part part, int quantity) {
        if (part.getQuantityInStock() >= LOW_STOCK_THRESHOLD) {
            throw new IllegalStateException(
                    "Clearance pricing only applies to low-stock parts (stock < "
                            + LOW_STOCK_THRESHOLD + "). '" + part.getName()
                            + "' has " + part.getQuantityInStock() + " units in stock.");
        }
        return part.getUnitPrice() * quantity * (1 - CLEARANCE_DISCOUNT);
    }
}
