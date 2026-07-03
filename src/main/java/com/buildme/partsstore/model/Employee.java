package com.buildme.partsstore.model;

import java.util.Objects;

/**
 * Represents a store employee.
 *
 * Deliberately immutable and self-validating, for the same reasons as
 * Part: all fields are private and final, there are no setters, and
 * invariants (a non-blank id/name, a non-negative discount rate) are
 * checked once in the constructor rather than being left for callers
 * to enforce.
 */
public final class Employee {

    private final String id;
    private final String name;
    private final Role role;
    private final double staffDiscountRate;

    public Employee(String id, String name, Role role, double staffDiscountRate) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Employee id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Employee name must not be blank");
        }
        if (staffDiscountRate < 0 || staffDiscountRate > 1) {
            throw new IllegalArgumentException("Staff discount rate must be between 0 and 1");
        }
        this.id = id;
        this.name = name;
        this.role = role;
        this.staffDiscountRate = staffDiscountRate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public double getStaffDiscountRate() {
        return staffDiscountRate;
    }

    /** Only managers are authorised to apply clearance pricing. */
    public boolean canAuthoriseClearance() {
        return role == Role.MANAGER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee employee = (Employee) o;
        return id.equals(employee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Employee{id='%s', name='%s', role=%s, staffDiscountRate=%.2f}",
                id, name, role, staffDiscountRate);
    }
}
