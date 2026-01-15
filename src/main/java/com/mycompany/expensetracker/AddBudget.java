/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.expensetracker;

import com.google.cloud.firestore.Firestore;
import java.awt.Color;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import javax.swing.*;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author sirh9
 */
public class AddBudget extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AddBudget.class.getName());

    private String currentUserId;
    private String currentUserName;
    String monthYear;
    private boolean isEditMode = false;
    private String editCategory;

    /**
     * Creates new form AddBudget
     */
    public AddBudget(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        this.monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        initComponents();
        addBudegtPanel.setSize(380, 280);
addBudegtPanel.setPreferredSize(new java.awt.Dimension(380, 280));
addBudegtPanel.setMinimumSize(new java.awt.Dimension(380, 280));
addBudegtPanel.setMaximumSize(new java.awt.Dimension(380, 280));


        setLocationRelativeTo(null);
        applyThemeColors();

        profileName.setText("Welcome, " + userName);
        setupActionListeners();
    }

    // Constructor for edit mode
    public AddBudget(String userId, String userName, String category, double targetAmount) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        this.monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        this.isEditMode = true;
        this.editCategory = category;

        initComponents();
        addBudegtPanel.setSize(380, 280);
addBudegtPanel.setPreferredSize(new java.awt.Dimension(380, 280));
addBudegtPanel.setMinimumSize(new java.awt.Dimension(380, 280));
addBudegtPanel.setMaximumSize(new java.awt.Dimension(380, 280));


        setLocationRelativeTo(null);
        applyThemeColors();

        profileName.setText("Welcome, " + userName);
        heading.setText("Edit Budget");

        nameText.setText(category);
        nameText.setEnabled(false);
        amountField.setText(String.valueOf(targetAmount));
        addButton.setText("Update");

        setupActionListeners();
    }

    public AddBudget() {
        initComponents();
        addBudegtPanel.setSize(380, 280);
addBudegtPanel.setPreferredSize(new java.awt.Dimension(380, 280));
addBudegtPanel.setMinimumSize(new java.awt.Dimension(380, 280));
addBudegtPanel.setMaximumSize(new java.awt.Dimension(380, 280));

        
        setLocationRelativeTo(null);
        applyThemeColors();
        profileName.setText("Design View");
    }

    private void applyThemeColors() {
        Color LIGHT_TEXT = Color.WHITE;
        Color ACCENT_COLOR = new Color(0, 153, 255);
        Color DARK_BUTTON_BG = new Color(51, 51, 51);

        JButton[] buttons = {addIncome, addExpense, transactions, monthlyReport, charts, budget};
        for (JButton btn : buttons) {
            btn.setBackground(DARK_BUTTON_BG);
            btn.setForeground(LIGHT_TEXT);
        }

        budget.setBackground(ACCENT_COLOR);
        heading.setForeground(Color.BLACK);
        addButton.setBackground(ACCENT_COLOR);
        addButton.setForeground(LIGHT_TEXT);
    }

    private void setupActionListeners() {

        addButton.addActionListener(e -> addBudget());
        cancelButton.addActionListener(e -> navigateToScreen(new Budget(currentUserId, currentUserName)));

        addIncome.addActionListener(e -> navigateToScreen(new AddIncome(currentUserId, currentUserName)));
        addExpense.addActionListener(e -> navigateToScreen(new AddExpense(currentUserId, currentUserName)));
        transactions.addActionListener(e -> navigateToScreen(new Transactions(currentUserId, currentUserName)));
        monthlyReport.addActionListener(e -> navigateToScreen(new MonthlyReport(currentUserId, currentUserName)));
        charts.addActionListener(e -> navigateToScreen(new Charts(currentUserId, currentUserName)));
        budget.addActionListener(e -> navigateToScreen(new Budget(currentUserId, currentUserName)));
    }

    private void navigateToScreen(JFrame targetScreen) {
        this.dispose();
        targetScreen.setVisible(true);
    }

    private void addBudget() {

        String category = nameText.getText().trim();
        String amountStr = amountField.getText().trim();
        double amount;

        if (category.isEmpty() || category.equalsIgnoreCase("Name..")) {
            JOptionPane.showMessageDialog(this, "Please enter a category name.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (amountStr.isEmpty() || amountStr.equals("Target..")) {
            JOptionPane.showMessageDialog(this, "Please enter a budget amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Budget amount must be positive.", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid amount entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {

                Firestore db = FirebaseService.getFirestore();

                Map<String, Object> data = new HashMap<>();
                data.put("user_id", currentUserId);
                data.put("category", category);
                data.put("target_amount", amount);
                data.put("month_year", monthYear);

                // Composite ID ensures update if already present
                String docId = currentUserId + "_" + monthYear + "_" + category.toLowerCase();

                db.collection("budgets").document(docId).set(data).get();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            AddBudget.this,
                            "Budget saved successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    navigateToScreen(new Budget(currentUserId, currentUserName));
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error saving budget.", ex);
                    JOptionPane.showMessageDialog(
                            AddBudget.this,
                            "Error saving budget.",
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        profileName = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
        addBudegtPanel = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        nameText = new javax.swing.JTextField();
        amountField = new javax.swing.JTextField();
        heading = new javax.swing.JLabel();
        sidebar = new javax.swing.JPanel();
        addIncome = new javax.swing.JButton();
        addExpense = new javax.swing.JButton();
        transactions = new javax.swing.JButton();
        monthlyReport = new javax.swing.JButton();
        charts = new javax.swing.JButton();
        budget = new javax.swing.JButton();
        targetText = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        profileName.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        profileName.setForeground(new java.awt.Color(255, 255, 255));
        profileName.setText("Meer Musabih Saleem");
        getContentPane().add(profileName, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 20, -1, -1));

        logo.setFont(new java.awt.Font("Imprint MT Shadow", 1, 48)); // NOI18N
        logo.setForeground(new java.awt.Color(255, 255, 255));
        logo.setText("SpendWise ");
        getContentPane().add(logo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, -1, -1));

        addBudegtPanel.setOpaque(false);

        addButton.setBackground(new java.awt.Color(0, 153, 255));
        addButton.setText("Add");

        cancelButton.setText("Cancel");

        nameText.setText("Name..");
        amountField.setText("Target..");

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Add Budget");

        javax.swing.GroupLayout addBudegtPanelLayout = new javax.swing.GroupLayout(addBudegtPanel);
        addBudegtPanel.setLayout(addBudegtPanelLayout);
        addBudegtPanelLayout.setHorizontalGroup(
            addBudegtPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addBudegtPanelLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(addBudegtPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(amountField, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        addBudegtPanelLayout.setVerticalGroup(
            addBudegtPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addBudegtPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(heading)
                .addGap(57, 57, 57)
                .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(amountField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addGroup(addBudegtPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addButton)
                    .addComponent(cancelButton))
                .addGap(50, 50, 50))
        );

        getContentPane().add(addBudegtPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 80, 380, 280));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new AddBudget("testUser123", "TestUser").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel addBudegtPanel;
    private javax.swing.JButton addButton;
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JTextField amountField;
    private javax.swing.JButton budget;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton charts;
    private javax.swing.JLabel heading;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JTextField nameText;
    private javax.swing.JLabel profileName;
    private javax.swing.JPanel sidebar;
    private javax.swing.JLabel targetText;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
