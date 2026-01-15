package com.mycompany.expensetracker;

import java.util.ArrayList;
import java.util.List;

public class CategoryModel {
    private String name;
    private String type; // "Income" or "Expense" (or "Both" if applicable)

    public CategoryModel() {
    }

    public CategoryModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Static source of truth
    public static List<CategoryModel> getExpenseCategories() {
        List<CategoryModel> list = new ArrayList<>();
        String[] categories = {
            "Groceries", "Rent", "Utilities", "Transportation", "Entertainment",
            "Health", "Shopping", "Dining Out", "Education", "Travel", "Others"
        };
        for (String c : categories) {
            list.add(new CategoryModel(c, "Expense"));
        }
        return list;
    }
    
    public static List<CategoryModel> getIncomeCategories() {
        List<CategoryModel> list = new ArrayList<>();
        String[] categories = {
            "Salary", "Business", "Gift", "Others"
        };
        for (String c : categories) {
            list.add(new CategoryModel(c, "Income"));
        }
        return list;
    }

    public static List<CategoryModel> getAllCategories() {
        List<CategoryModel> list = new ArrayList<>();
        list.addAll(getIncomeCategories());
        // Avoid duplicates if "Others" is in both, but technically they are distinct types
        // However for a filter list "All", we might want just unique names or allow both.
        // Transactions.java filter list currently mixes them.
        
        // Let's add Expense categories that aren't already added (just in case of name collision, though Types differ)
        for (CategoryModel c : getExpenseCategories()) {
             // specific logic if needed, for now just add all
             list.add(c);
        }
        return list;
    }
    
    /**
     * Returns a list of unique category names for filtering purposes.
     */
    public static List<String> getAllCategoryNames() {
        List<String> names = new ArrayList<>();
        // Add Income categories
        for (CategoryModel c : getIncomeCategories()) {
            if (!names.contains(c.getName())) names.add(c.getName());
        }
        // Add Expense categories
        for (CategoryModel c : getExpenseCategories()) {
            if (!names.contains(c.getName())) names.add(c.getName());
        }
        return names;
    }

    @Override
    public String toString() {
        return name; // Useful for JComboBox
    }
}
