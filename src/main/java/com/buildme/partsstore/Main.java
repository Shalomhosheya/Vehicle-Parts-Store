package com.buildme.partsstore;

import com.buildme.partsstore.inventory.Inventory;
import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Part;
import com.buildme.partsstore.pricing.PricingStrategy;
import com.buildme.partsstore.reflection.StrategyRegistry;
import com.buildme.partsstore.store.VehiclePartsStore;

import java.util.List;
import java.util.Locale;

/*

  -  Main class - this is just where I run everything so I can show it working.
  -  Covers what the assignment asked for:
    - cleaned up the bad smells (check the comments in the other classes for that)
    - refactored the code into separate classes instead of one big mess
   - Strategy pattern for the pricing (PricingStrategy + the different strategy classes)
    - Reflection - StrategyRegistry builds the strategies dynamically instead of me hardcoding "new X()"
    - Functional programming - streams, Optional, lambdas, method references etc
*/

public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.UK);

        Inventory inventory = buildSampleInventory();

        System.out.println("=== Vehicle Parts Store ===");
        System.out.println("Total parts in inventory: " + inventory.size());
        System.out.printf("Total inventory value: GBP %.2f%n%n", inventory.totalInventoryValue());

        // Functional programming bit - using streams/Optional/method refs here
        System.out.println("--- Low stock parts (stock < 5) ---");
        List<Part> lowStock = inventory.lowStockParts(5);
        lowStock.forEach(p -> System.out.println("  " + p));

        System.out.println("\n--- Brake parts, sorted by name (stream filter + sorted) ---");
        inventory.findByCategory(Category.BRAKES).forEach(p -> System.out.println("  " + p));

        // Reflection bit - grabbing the strategies without ever writing "new SomeStrategy()" directly
        System.out.println("\n--- Available pricing strategies (via reflection) ---");
        StrategyRegistry registry = new StrategyRegistry();
        for (String key : registry.availableStrategies()) {
            System.out.println("  " + registry.describe(key));
        }

        // Strategy pattern bit - same store, just swapping the pricing strategy each time
        VehiclePartsStore store = new VehiclePartsStore(inventory, registry.createStrategy("REGULAR"));

        System.out.println("\n--- Pricing the same order under different strategies ---");
        String partId = "P-ENG-001";
        int quantity = 12;

        for (String key : List.of("REGULAR", "BULK_DISCOUNT")) {
            PricingStrategy strategy = registry.createStrategy(key); // built through reflection
            store.setPricingStrategy(strategy);
            double price = store.quote(partId, quantity);
            System.out.printf("  [%s] %d x %s = GBP %.2f%n", key, quantity,
                    inventory.findById(partId).map(Part::getName).orElse("?"), price);
        }

        // this should fail - the part isn't actually low stock so clearance pricing shouldn't apply
        store.setPricingStrategy(registry.createStrategy("CLEARANCE"));
        try {
            store.quote(partId, quantity);
        } catch (IllegalStateException ex) {
            System.out.println("  [CLEARANCE] rejected as expected: " + ex.getMessage());
        }

        // now try it on a part that's actually low on stock - should go through fine
        String lowStockPartId = "P-TYR-003";
        double clearancePrice = store.quote(lowStockPartId, 2);
        System.out.printf("  [CLEARANCE] 2 x %s = GBP %.2f%n",
                inventory.findById(lowStockPartId).map(Part::getName).orElse("?"), clearancePrice);

        // quick example of just passing in a strategy as a lambda instead of a whole class
        System.out.println("\n--- Ad-hoc lambda strategy (no named class required) ---");
        PricingStrategy staffDiscount = (part, qty) -> part.getUnitPrice() * qty * 0.80; // 20% staff discount
        store.setPricingStrategy(staffDiscount);
        double staffPrice = store.quote(partId, 3);
        System.out.printf("  [STAFF_DISCOUNT lambda] 3 x %s = GBP %.2f%n",
                inventory.findById(partId).map(Part::getName).orElse("?"), staffPrice);
    }

    // just some made-up sample data to populate the inventory for the demo
    private static Inventory buildSampleInventory() {
        Inventory inventory = new Inventory();
        inventory.addPart(new Part("P-ENG-001", "Alloy Piston Set", Category.ENGINE, 89.99, 40));
        inventory.addPart(new Part("P-ENG-002", "Timing Belt Kit", Category.ENGINE, 54.50, 15));
        inventory.addPart(new Part("P-BRK-001", "Ceramic Brake Pads", Category.BRAKES, 32.00, 60));
        inventory.addPart(new Part("P-BRK-002", "Brake Disc Rotor", Category.BRAKES, 45.75, 3));
        inventory.addPart(new Part("P-ELE-001", "12V Car Battery", Category.ELECTRICAL, 110.00, 8));
        inventory.addPart(new Part("P-ELE-002", "Alternator", Category.ELECTRICAL, 145.20, 2));
        inventory.addPart(new Part("P-BOD-001", "Wing Mirror (Left)", Category.BODY, 28.99, 4));
        inventory.addPart(new Part("P-TYR-003", "All-Season Tyre 205/55R16", Category.TYRES, 65.00, 4));
        return inventory;
    }
}