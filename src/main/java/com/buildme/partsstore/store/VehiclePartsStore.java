package com.buildme.partsstore.store;

import com.buildme.partsstore.inventory.Inventory;
import com.buildme.partsstore.model.Part;
import com.buildme.partsstore.pricing.PricingStrategy;

import java.util.NoSuchElementException;

 // Strategy Pattern - Context class.

public class VehiclePartsStore {

    private final Inventory inventory;
    private PricingStrategy pricingStrategy;

    public VehiclePartsStore(Inventory inventory, PricingStrategy pricingStrategy) {
        this.inventory = inventory;
        this.pricingStrategy = pricingStrategy;
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public double quote(String partId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Part part = inventory.findById(partId)
                .orElseThrow(() -> new NoSuchElementException("Unknown part id: " + partId));
        return pricingStrategy.calculatePrice(part, quantity);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
