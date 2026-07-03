package com.buildme.partsstore;

import com.buildme.partsstore.inventory.Inventory;
import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Employee;
import com.buildme.partsstore.model.Part;
import com.buildme.partsstore.model.Role;
import com.buildme.partsstore.pricing.PricingStrategy;
import com.buildme.partsstore.reflection.StrategyRegistry;
import com.buildme.partsstore.staff.EmployeeDirectory;
import com.buildme.partsstore.store.VehiclePartsStore;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

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
        EmployeeDirectory employeeDirectory = buildSampleEmployeeDirectory();

        System.out.println("=== Vehicle Parts Store ===");
        System.out.println("Total parts in inventory: " + inventory.size());
        System.out.printf("Total inventory value: GBP %.2f%n%n", inventory.totalInventoryValue());

        // --- Functional Programming demo: streams, method references, Optional ---
        System.out.println("--- Low stock parts (stock < 5) ---");
        List<Part> lowStock = inventory.lowStockParts(5);
        lowStock.forEach(p -> System.out.println("  " + p));

        System.out.println("\n--- Brake parts, sorted by name (stream filter + sorted) ---");
        inventory.findByCategory(Category.BRAKES).forEach(p -> System.out.println("  " + p));

        // --- Reflection demo: discover and describe strategies without ever "new"-ing them directly ---
        System.out.println("\n--- Available pricing strategies (via reflection) ---");
        StrategyRegistry registry = new StrategyRegistry();
        for (String key : registry.availableStrategies()) {
            System.out.println("  " + registry.describe(key));
        }

        // --- Strategy Pattern demo: same store, three different pricing behaviours ---
        VehiclePartsStore store = new VehiclePartsStore(inventory, registry.createStrategy("REGULAR"));

        System.out.println("\n--- Pricing the same order under different strategies ---");
        String partId = "P-ENG-001";
        int quantity = 12;

        for (String key : List.of("REGULAR", "BULK_DISCOUNT")) {
            PricingStrategy strategy = registry.createStrategy(key); // instantiated via reflection
            store.setPricingStrategy(strategy);
            double price = store.quote(partId, quantity);
            System.out.printf("  [%s] %d x %s = GBP %.2f%n", key, quantity,
                    inventory.findById(partId).map(Part::getName).orElse("?"), price);
        }

        // Clearance strategy correctly rejects a part that isn't low stock
        store.setPricingStrategy(registry.createStrategy("CLEARANCE"));
        try {
            store.quote(partId, quantity);
        } catch (IllegalStateException ex) {
            System.out.println("  [CLEARANCE] rejected as expected: " + ex.getMessage());
        }

        // Clearance strategy succeeds against genuinely low-stock part
        String lowStockPartId = "P-TYR-003";
        double clearancePrice = store.quote(lowStockPartId, 2);
        System.out.printf("  [CLEARANCE] 2 x %s = GBP %.2f%n",
                inventory.findById(lowStockPartId).map(Part::getName).orElse("?"), clearancePrice);

        // --- Employees demo: EmployeeDirectory mirrors Inventory's structure and Stream usage ---
        System.out.println("\n--- Employees ---");
        employeeDirectory.allEmployeesSortedByName().forEach(e -> System.out.println("  " + e));

        System.out.println("\n--- Managers only (stream filter) ---");
        employeeDirectory.managers().forEach(e -> System.out.println("  " + e));

        // --- Functional Programming demo: an ad-hoc strategy built from an employee's own discount rate ---
        System.out.println("\n--- Staff discount priced from an employee's own record ---");
        Employee cashier = employeeDirectory.findById("E3")
                .orElseThrow(() -> new NoSuchElementException("Unknown employee id"));
        PricingStrategy staffDiscount = (part, qty) ->
                part.getUnitPrice() * qty * (1 - cashier.getStaffDiscountRate());
        store.setPricingStrategy(staffDiscount);
        double staffPrice = store.quote(partId, 3);
        System.out.printf("  [STAFF_DISCOUNT lambda, %s @ %.0f%%] 3 x %s = GBP %.2f%n",
                cashier.getName(), cashier.getStaffDiscountRate() * 100,
                inventory.findById(partId).map(Part::getName).orElse("?"), staffPrice);

        // --- Role-based authorisation demo: only a manager may authorise clearance pricing ---
        System.out.println("\n--- Clearance authorisation (role-based check) ---");
        Employee assistant = employeeDirectory.findById("E2")
                .orElseThrow(() -> new NoSuchElementException("Unknown employee id"));
        Employee manager = employeeDirectory.findById("E1")
                .orElseThrow(() -> new NoSuchElementException("Unknown employee id"));

        for (Employee approver : List.of(assistant, manager)) {
            if (approver.canAuthoriseClearance()) {
                store.setPricingStrategy(registry.createStrategy("CLEARANCE"));
                double approvedPrice = store.quote(lowStockPartId, 2);
                System.out.printf("  %s (%s) authorised clearance: 2 x %s = GBP %.2f%n",
                        approver.getName(), approver.getRole(),
                        inventory.findById(lowStockPartId).map(Part::getName).orElse("?"), approvedPrice);
            } else {
                System.out.printf("  %s (%s) is NOT authorised to apply clearance pricing.%n",
                        approver.getName(), approver.getRole());
            }
        }
        }

    // just some made-up sample data to populate the inventory for the demo
    private static Inventory buildSampleInventory() {
        Inventory inventory = new Inventory();


        // Engine parts
                inventory.addPart(new Part("P-ENG-001", "Alloy Piston Set", Category.ENGINE, 89.99, 40));
                inventory.addPart(new Part("P-ENG-002", "Timing Belt Kit", Category.ENGINE, 54.50, 15));
                inventory.addPart(new Part("P-ENG-003", "Cylinder Head", Category.ENGINE, 220.00, 5));
                inventory.addPart(new Part("P-ENG-004", "Engine Oil Pump", Category.ENGINE, 75.25, 10));
                inventory.addPart(new Part("P-ENG-005", "Spark Plug Set", Category.ENGINE, 18.99, 50));

        // Brake parts
                inventory.addPart(new Part("P-BRK-001", "Ceramic Brake Pads", Category.BRAKES, 32.00, 60));
                inventory.addPart(new Part("P-BRK-002", "Brake Disc Rotor", Category.BRAKES, 45.75, 3));
                inventory.addPart(new Part("P-BRK-003", "Brake Caliper", Category.BRAKES, 85.00, 6));
                inventory.addPart(new Part("P-BRK-004", "Brake Fluid Bottle", Category.BRAKES, 12.50, 25));

        // Electrical parts
                inventory.addPart(new Part("P-ELE-001", "12V Car Battery", Category.ELECTRICAL, 110.00, 8));
                inventory.addPart(new Part("P-ELE-002", "Alternator", Category.ELECTRICAL, 145.20, 2));
                inventory.addPart(new Part("P-ELE-003", "Starter Motor", Category.ELECTRICAL, 130.00, 4));
                inventory.addPart(new Part("P-ELE-004", "Headlight Bulb", Category.ELECTRICAL, 9.99, 100));
                inventory.addPart(new Part("P-ELE-005", "Fuse Kit", Category.ELECTRICAL, 6.50, 75));

        // Body parts
                inventory.addPart(new Part("P-BOD-001", "Wing Mirror (Left)", Category.BODY, 28.99, 4));
                inventory.addPart(new Part("P-BOD-002", "Wing Mirror (Right)", Category.BODY, 28.99, 6));
                inventory.addPart(new Part("P-BOD-003", "Front Bumper", Category.BODY, 150.00, 2));
                inventory.addPart(new Part("P-BOD-004", "Rear Bumper", Category.BODY, 145.00, 3));
                inventory.addPart(new Part("P-BOD-005", "Car Door Handle", Category.BODY, 12.75, 20));

        // Paint / accessories
                inventory.addPart(new Part("P-IOD-001", "Matt Black Paint Bottle", Category.PAINT, 2.99, 4));
                inventory.addPart(new Part("P-IOD-002", "Gloss White Paint", Category.PAINT, 3.50, 10));
                inventory.addPart(new Part("P-IOD-003", "Car Polish Kit", Category.PAINT, 15.00, 12));
                inventory.addPart(new Part("P-IOD-004", "Scratch Remover", Category.PAINT, 8.99, 18));

        // Tyres
                inventory.addPart(new Part("P-TYR-001", "All-Season Tyre 195/65R15", Category.TYRES, 55.00, 10));
                inventory.addPart(new Part("P-TYR-002", "Performance Tyre 225/40R18", Category.TYRES, 95.00, 6));
                inventory.addPart(new Part("P-TYR-003", "All-Season Tyre 205/55R16", Category.TYRES, 65.00, 4));
                inventory.addPart(new Part("P-TYR-004", "Winter Tyre 205/60R16", Category.TYRES, 70.00, 5));

                return inventory;
    }
    private static EmployeeDirectory buildSampleEmployeeDirectory() {
        EmployeeDirectory directory = new EmployeeDirectory();

        directory.addEmployee(new Employee("E1", "Sahan Dias",   Role.MANAGER,         0.20));
        directory.addEmployee(new Employee("E2", "Amila jayathilaka",       Role.SALES_ASSISTANT, 0.10));
        directory.addEmployee(new Employee("E3", "Warun de silva",  Role.CASHIER,         0.10));
        directory.addEmployee(new Employee("E4", "Pasindu Perera",    Role.WAREHOUSE_STAFF, 0.05));
        return directory;
    }

}