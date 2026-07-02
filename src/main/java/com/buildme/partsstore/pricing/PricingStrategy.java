package com.buildme.partsstore.pricing;

import com.buildme.partsstore.model.Part;

/*

  Strategy Pattern - Strategy interface.
  Declared as a @FunctionalInterface so it can be implemented either as a
  traditional named class (see RegularPricingStrategy, BulkDiscountPricingStrategy,
  ClearancePricingStrategy) or as an inline lambda expression at the call site.
  This dual usage is what lets the Strategy pattern and functional programming
  style work together in this project.
*/

@FunctionalInterface
public interface PricingStrategy {

//
//      Calculates the total price for purchasing {@code quantity} units of
//      {@code part} under this strategy's pricing rules.
//
    double calculatePrice(Part part, int quantity);
}
