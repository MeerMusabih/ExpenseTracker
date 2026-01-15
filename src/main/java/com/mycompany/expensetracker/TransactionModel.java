package com.mycompany.expensetracker;

public class TransactionModel {
    private String id;
    private String userId;
    private double amount;
    private String type; // "Income" or "Expense"
    private String category;
    private String description;
    private String date; // Stored as "yyyy-MM-dd"

    public TransactionModel() {}

    public TransactionModel(String userId, double amount, String type, String category, String description, String date) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
