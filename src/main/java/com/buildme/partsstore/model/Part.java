package com.buildme.partsstore.model;

import java.util.Objects;

/*

 ... Represents a single vehicle part held in the store's inventory.
 ...The class is deliberately immutable (all fields final, no setters).
 ...This avoids the "Data Class" / "Primitive Obsession" bad smells and
 ...removes an entire category of bugs caused by shared mutable state -
 ...a part's price or stock level cannot be silently changed from
 ... elsewhere in the codebase.
*/

public final class Part {

    private final String id;
    private final String name;
    private final Category category;
    private final double unitPrice;
    private final int quantityInStock;

    public Part(String id, String name, Category category, double unitPrice, int quantityInStock) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Part id must not be blank");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (quantityInStock < 0) {
            throw new IllegalArgumentException("Quantity in stock cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.quantityInStock = quantityInStock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Part)) return false;
        Part part = (Part) o;
        return id.equals(part.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Part{id='%s', name='%s', category=%s, unitPrice=%.2f, stock=%d}",
                id, name, category, unitPrice, quantityInStock);
    }
}
