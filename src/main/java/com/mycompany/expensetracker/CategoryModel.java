package com.mycompany.expensetracker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CategoryModel {
    private static final Logger logger = Logger.getLogger(CategoryModel.class.getName());
    private String name;
    private String type; // "Income" or "Expense"

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

    // In-memory cache
    private static final List<CategoryModel> categories = new ArrayList<>();

    /**
     * Loads categories from Firestore. If the collection is empty, seeds it with defaults.
     */
    public static synchronized void loadFromFirestore(Firestore db) throws Exception {
        ApiFuture<QuerySnapshot> future = db.collection("categories").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            logger.warning("No categories found in Firestore 'categories' collection.");
        }

        categories.clear();
        for (QueryDocumentSnapshot doc : documents) {
            String name = doc.getString("name");
            String type = doc.getString("type");
            if (name != null && type != null) {
                categories.add(new CategoryModel(name, type));
            }
        }
        logger.info("Loaded " + categories.size() + " categories from Firestore.");
    }

    public static synchronized List<CategoryModel> getExpenseCategories() {
        List<CategoryModel> list = new ArrayList<>();
        for (CategoryModel c : categories) {
            if ("Expense".equalsIgnoreCase(c.getType())) {
                list.add(c);
            }
        }
        return list;
    }

    public static synchronized List<CategoryModel> getIncomeCategories() {
        List<CategoryModel> list = new ArrayList<>();
        for (CategoryModel c : categories) {
            if ("Income".equalsIgnoreCase(c.getType())) {
                list.add(c);
            }
        }
        return list;
    }

    public static synchronized void addCategoryLocal(String name, String type) {
        if (name == null || type == null) return;
        String norm = name.trim();
        if (norm.isEmpty()) return;
        for (CategoryModel c : categories) {
            if (c.getName().equalsIgnoreCase(norm) && c.getType().equalsIgnoreCase(type)) return;
        }
        categories.add(new CategoryModel(norm, type));
    }

    public static void addExpenseCategory(String name) {
        addCategoryLocal(name, "Expense");
    }

    public static void addIncomeCategory(String name) {
        addCategoryLocal(name, "Income");
    }

    public static List<String> getAllCategoryNames() {
        List<String> names = new ArrayList<>();
        for (CategoryModel c : categories) {
            if (!names.contains(c.getName())) {
                names.add(c.getName());
            }
        }
        return names;
    }

    @Override
    public String toString() {
        return name;
    }
}
