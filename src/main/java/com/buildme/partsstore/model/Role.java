package com.buildme.partsstore.model;

/**
 * Represents an employee's role within the store.
 * As with Category, using an enum here avoids Primitive Obsession -
 * a role is not a free-text String that could be mistyped or left
 * inconsistent across the codebase.
 */
public enum Role {
    MANAGER,
    SALES_ASSISTANT,
    CASHIER,
    WAREHOUSE_STAFF
}
