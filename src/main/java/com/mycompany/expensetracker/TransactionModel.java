package com.mycompany.expensetracker;

public class TransactionModel {
    private String id;
    private String userId;
    private double amount;
    private String type; 
    private String category;
    private String description;
    private String date; 

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

    // Firestore documents sometimes use snake_case field names. Provide setters/getters
    // that the Firestore mapper can use to populate the same internal fields.
    public void setUser_id(String user_id) { this.userId = user_id; }
    public String getUser_id() { return this.userId; }

    public void setTransaction_date(String transaction_date) { this.date = transaction_date; }
    public String getTransaction_date() { return this.date; }
}
