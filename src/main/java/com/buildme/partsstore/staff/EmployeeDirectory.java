package com.buildme.partsstore.staff;

import com.buildme.partsstore.model.Employee;
import com.buildme.partsstore.model.Role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Holds the employees who work at the store.
 *
 * Deliberately mirrors the structure of Inventory: a small, single-
 * responsibility class whose queries are expressed declaratively with
 * the Stream API instead of manual loops, and whose lookups return
 * Optional rather than null.
 */
public class EmployeeDirectory {

    private final Map<String, Employee> employees = new LinkedHashMap<>();

    public void addEmployee(Employee employee) {
        employees.put(employee.getId(), employee);
    }

    public Optional<Employee> findById(String id) {
        return Optional.ofNullable(employees.get(id));
    }

    public List<Employee> findByRole(Role role) {
        return employees.values().stream()
                .filter(e -> e.getRole() == role)
                .sorted(Comparator.comparing(Employee::getName))
                .collect(Collectors.toList());
    }

    public List<Employee> allEmployeesSortedByName() {
        return employees.values().stream()
                .sorted(Comparator.comparing(Employee::getName))
                .collect(Collectors.toList());
    }

    public List<Employee> managers() {
        return findByRole(Role.MANAGER);
    }

    public int size() {
        return employees.size();
    }
}
