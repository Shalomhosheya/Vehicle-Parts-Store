package com.buildme.partsstore.inventory;

import com.buildme.partsstore.model.Category;
import com.buildme.partsstore.model.Part;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Holds the parts currently stocked by the store.
 */
public class Inventory {

    private final Map<String, Part> parts = new LinkedHashMap<>();

    public void addPart(Part part) {
        parts.put(part.getId(), part);
    }


//      Removes the part with the given id.

//      @return true if a part with that id existed and was removed, false otherwise.

    public boolean removePart(String id) {
        return parts.remove(id) != null;
    }

    public Optional<Part> findById(String id) {
        return Optional.ofNullable(parts.get(id));
    }

    public List<Part> findByCategory(Category category) {
        return parts.values().stream()
                .filter(p -> p.getCategory() == category)
                .sorted(Comparator.comparing(Part::getName))
                .collect(Collectors.toList());
    }

    public List<Part> lowStockParts(int threshold) {
        return parts.values().stream()
                .filter(p -> p.getQuantityInStock() < threshold)
                .sorted(Comparator.comparingInt(Part::getQuantityInStock))
                .collect(Collectors.toList());
    }

    public double totalInventoryValue() {
        return parts.values().stream()
                .mapToDouble(p -> p.getUnitPrice() * p.getQuantityInStock())
                .sum();
    }

//     Total inventory value grouped by category - used by the dashboard pie chart.

    public Map<Category, Double> totalValueByCategory() {
        Map<Category, Double> result = new LinkedHashMap<>();
        for (Part p : parts.values()) {
            result.merge(p.getCategory(), p.getUnitPrice() * p.getQuantityInStock(), Double::sum);
        }
        return result;
    }

    public List<Part> allPartsSortedByPrice() {
        return parts.values().stream()
                .sorted(Comparator.comparingDouble(Part::getUnitPrice).reversed())
                .collect(Collectors.toList());
    }

    public List<Part> allParts() {
        return new ArrayList<>(parts.values());
    }

    public boolean containsId(String id) {
        return parts.containsKey(id);
    }

    public int size() {
        return parts.size();
    }
}