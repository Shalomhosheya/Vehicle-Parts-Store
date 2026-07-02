package com.buildme.partsstore.pricing;

import com.buildme.partsstore.model.Part;


/*
  Strategy Pattern - Concrete Strategy.
  Standard pricing: quantity multiplied by the part's unit price, no discount.
*/
@StrategyInfo(name = "REGULAR", description = "Standard price with no discount applied.")
public class RegularPricingStrategy implements PricingStrategy {

    @Override
    public double calculatePrice(Part part, int quantity) {
        return part.getUnitPrice() * quantity;
    }
}
