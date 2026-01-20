/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.expensetracker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.awt.Color;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 *
 * @author sirh9
 */
public class AddExpense extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AddExpense.class.getName());
    private String currentUserId;
    private String currentUserName;
    
    /**
     * Creates new form AddExpense
     * @param userId
     * @param userName
     */
    public AddExpense(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initComponents();
        
   
        setLocationRelativeTo(null); 
        applyThemeColors();
        setupCategories(); 
        setupInputPlaceholders();
        setupNavigationListeners();
        

        profileName.setText("Welcome, " + userName);
        date.setDate(new Date()); 
    }
    

    public AddExpense() {
        initComponents();
        setLocationRelativeTo(null);
        applyThemeColors();
        setupInputPlaceholders();
        setupNavigationListeners();
        setupCategories();
        profileName.setText("Design View");
    }



    private void setupCategories() {
        categoryMenu.removeAllItems();
        // Use CategoryModel for expense categories
        for (CategoryModel category : CategoryModel.getExpenseCategories()) {
            categoryMenu.addItem(category.getName());
        }
        categoryMenu.setSelectedIndex(0);
    }
    
    private void applyThemeColors() {
        Color LIGHT_TEXT = Color.WHITE;
        Color ACCENT_COLOR_EXPENSE = new Color(204, 0, 51); 
        Color DARK_BUTTON_BG = new Color(51, 51, 51);
        Color INACTIVE_COLOR = new Color(0, 153, 255); 


        JButton[] buttons = {addIncome, addExpense, transactions, monthlyReport, charts, budget};
        for (JButton btn : buttons) {
            btn.setBackground(DARK_BUTTON_BG);
            btn.setForeground(LIGHT_TEXT);
        }
        

        addExpense.setBackground(ACCENT_COLOR_EXPENSE);
        addIncome.setBackground(INACTIVE_COLOR); 
        

        expensePanel.setBackground(new Color(255, 255, 255, 200));
        heading.setForeground(Color.BLACK);
        categoryLabel.setForeground(Color.BLACK);
        dateLabel.setForeground(Color.BLACK);
        

        saveButton.setBackground(ACCENT_COLOR_EXPENSE); 
        saveButton.setForeground(LIGHT_TEXT);
        cancelButton.setBackground(new Color(200, 200, 200));
        cancelButton.setForeground(Color.BLACK);
    }
    
    private void setupInputPlaceholders() {

        setupPlaceholder(amountText, "Amount...");
        

        descriptionArea.setText("Description (e.g., monthly rent payment)");
        descriptionArea.setForeground(Color.GRAY);
        
        descriptionArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (descriptionArea.getText().equals("Description (e.g., monthly rent payment)")) {
                    descriptionArea.setText("");
                    descriptionArea.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (descriptionArea.getText().trim().isEmpty()) {
                    descriptionArea.setText("Description (e.g., monthly rent payment)");
                    descriptionArea.setForeground(Color.GRAY);
                }
            }
        });
    }

    private void setupPlaceholder(final javax.swing.JTextField textField, final String placeholder) {
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);
        
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
    }
    
    private void setupNavigationListeners() {

        cancelButton.addActionListener(e -> navigateToHome());
        logo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigateToHome();
            }
        });
        

        addIncome.addActionListener(e -> navigateToScreen(new AddIncome(currentUserId, currentUserName)));
        addExpense.addActionListener(e -> {});
        transactions.addActionListener(e -> navigateToScreen(new Transactions(currentUserId, currentUserName))); 
        monthlyReport.addActionListener(e -> navigateToScreen(new MonthlyReport(currentUserId, currentUserName))); 
        charts.addActionListener(e -> navigateToScreen(new Charts(currentUserId, currentUserName))); 
        budget.addActionListener(e -> navigateToScreen(new Budget(currentUserId, currentUserName))); 
    }

    private void navigateToHome() {
        this.dispose();
        new HomePage(currentUserId, currentUserName).setVisible(true);
    }

    private void navigateToScreen(JFrame targetScreen) {
        this.dispose();
        targetScreen.setVisible(true);
    }



    private void insertExpenseTransaction() {
        String amountStr = amountText.getText().trim();
        String category = (String) categoryMenu.getSelectedItem();
        Date transactionDate = date.getDate();
        String description = descriptionArea.getText().trim();
        

        
        if (amountStr.isEmpty() || amountStr.equals("Amount...")) {
            JOptionPane.showMessageDialog(this, "Please enter the expense amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        
        final double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        final String finalCategory = category;
        final Date finalTransactionDate = transactionDate;
        
        if (finalCategory == null || finalCategory.isEmpty() || finalCategory.equals("Category")) {
            JOptionPane.showMessageDialog(this, "Please select or enter an expense category.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (finalTransactionDate == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid date.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if date is in the future
        Date today = new Date();
        if (finalTransactionDate.after(today)) {
            JOptionPane.showMessageDialog(this, "Cannot select a future date. Please select today or a past date.", "Invalid Date", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        final String finalDescription = description.equals("Description (e.g., monthly rent payment)") ? "General Expense" : description;
        
        // Check available balance before allowing expense
        saveButton.setEnabled(false); // Prevent double clicks
        LoadingDialog.showLoading(this, "Checking balance...");
        
        SwingWorker<Boolean, Void> balanceChecker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Firestore db = FirebaseService.getFirestore();
                ApiFuture<QuerySnapshot> future = db.collection("transactions")
                        .whereEqualTo("user_id", currentUserId)
                        .get();
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                
                double totalIncome = 0;
                double totalExpense = 0;
                
                for (QueryDocumentSnapshot doc : documents) {
                    String type = doc.getString("type");
                    Double amt = doc.getDouble("amount");
                    if (type != null && amt != null) {
                        if ("Income".equalsIgnoreCase(type)) {
                            totalIncome += amt;
                        } else if ("Expense".equalsIgnoreCase(type)) {
                            totalExpense += amt;
                        }
                    }
                }
                
                double availableBalance = totalIncome - totalExpense;
                return availableBalance > 0;
            }
            
            @Override
            protected void done() {
                try {
                    boolean hasBalance = get();
                    if (!hasBalance) {
                        JOptionPane.showMessageDialog(AddExpense.this,
                            "Insufficient funds! You cannot add an expense when your available balance is zero or negative.\n" +
                            "Please add income first.",
                            "Insufficient Balance",
                            JOptionPane.WARNING_MESSAGE);
                        saveButton.setEnabled(true);
                    } else {
                        // Proceed with expense insertion
                        LoadingDialog.hideLoading();
                        proceedWithExpenseInsertion(amount, finalCategory, finalTransactionDate, finalDescription);
                    }
                } catch (Exception ex) {
                    LoadingDialog.hideLoading();
                    logger.log(Level.SEVERE, "Error checking balance", ex);
                    saveButton.setEnabled(true);
                    JOptionPane.showMessageDialog(AddExpense.this,
                        "Error checking balance: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        balanceChecker.execute();
    }
    
    
    private void proceedWithExpenseInsertion(double amount, String category, Date transactionDate, String finalDescription) {
        LoadingDialog.showLoading(this, "Saving expense...");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Firestore db = FirebaseService.getFirestore();
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = sdf.format(transactionDate);

                Map<String, Object> data = new HashMap<>();
                data.put("user_id", currentUserId);
                data.put("amount", amount);
                data.put("type", "Expense");
                data.put("category", category);
                data.put("description", finalDescription);
                data.put("transaction_date", dateString);

                ApiFuture<DocumentReference> future = db.collection("transactions").add(data);
                future.get(); // Wait for completion
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    JOptionPane.showMessageDialog(AddExpense.this, "Expense recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                    amountText.setText("");
                    setupPlaceholder(amountText, "Amount..."); 
                    descriptionArea.setText("");
                    setupInputPlaceholders();
                    date.setDate(new Date());
                    categoryMenu.setSelectedIndex(0);
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(AddExpense.this, "Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    logger.log(Level.SEVERE, "Expense insert error", ex);
                } finally {
                    LoadingDialog.hideLoading();
                    saveButton.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        profileName = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
        expensePanel = new javax.swing.JPanel();
        heading = new javax.swing.JLabel();
        amountText = new javax.swing.JTextField();
        saveButton = new javax.swing.JButton();
        categoryMenu = new javax.swing.JComboBox<>();
        categoryLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionArea = new javax.swing.JTextArea();
        cancelButton = new javax.swing.JButton();
        date = new com.toedter.calendar.JDateChooser();
        dateLabel = new javax.swing.JLabel();
        sidebar = new javax.swing.JPanel();
        addIncome = new javax.swing.JButton();
        addExpense = new javax.swing.JButton();
        transactions = new javax.swing.JButton();
        monthlyReport = new javax.swing.JButton();
        charts = new javax.swing.JButton();
        budget = new javax.swing.JButton();
        bgImage = new javax.swing.JLabel();

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

        expensePanel.setOpaque(false);

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Add Expense");

        amountText.setBackground(new java.awt.Color(255, 255, 255));
        amountText.setForeground(new java.awt.Color(0, 0, 0));
        amountText.setText("Amount...");
        amountText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                amountTextActionPerformed(evt);
            }
        });

        saveButton.setBackground(new java.awt.Color(0, 153, 255));
        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        categoryMenu.setEditable(true);
        categoryMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {  }));
        categoryMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categoryMenuActionPerformed(evt);
            }
        });

        categoryLabel.setBackground(new java.awt.Color(102, 102, 102));
        categoryLabel.setText("Category: ");

        descriptionArea.setBackground(new java.awt.Color(255, 255, 255));
        descriptionArea.setColumns(20);
        descriptionArea.setForeground(new java.awt.Color(0, 0, 0));
        descriptionArea.setRows(5);
        descriptionArea.setText("Description");
        jScrollPane1.setViewportView(descriptionArea);

        cancelButton.setText("Cancel");

        dateLabel.setText("Date:");

        javax.swing.GroupLayout expensePanelLayout = new javax.swing.GroupLayout(expensePanel);
        expensePanel.setLayout(expensePanelLayout);
        expensePanelLayout.setHorizontalGroup(
            expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(expensePanelLayout.createSequentialGroup()
                .addGroup(expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(expensePanelLayout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(heading))
                    .addGroup(expensePanelLayout.createSequentialGroup()
                        .addGap(78, 78, 78)
                        .addGroup(expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(amountText, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                            .addComponent(jScrollPane1)
                            .addGroup(expensePanelLayout.createSequentialGroup()
                                .addGroup(expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, expensePanelLayout.createSequentialGroup()
                                        .addComponent(dateLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, expensePanelLayout.createSequentialGroup()
                                        .addComponent(categoryLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(categoryMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(108, 108, 108)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, expensePanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(cancelButton)
                .addGap(43, 43, 43)
                .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(102, 102, 102))
        );
        expensePanelLayout.setVerticalGroup(
            expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(expensePanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(heading)
                .addGap(18, 18, 18)
                .addComponent(amountText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(categoryMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(categoryLabel))
                .addGap(12, 12, 12)
                .addGroup(expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dateLabel))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(expensePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveButton)
                    .addComponent(cancelButton))
                .addContainerGap(46, Short.MAX_VALUE))
        );

        getContentPane().add(expensePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 80, 390, 340));

        sidebar.setOpaque(false);

        addIncome.setText("Add Income");
        addIncome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addIncomeActionPerformed(evt);
            }
        });

        addExpense.setText("Add Expense");
        addExpense.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addExpenseActionPerformed(evt);
            }
        });

        transactions.setText("Transactions");

        monthlyReport.setText("Monthly Report");

        charts.setText("Charts");

        budget.setText("Budget");

        javax.swing.GroupLayout sidebarLayout = new javax.swing.GroupLayout(sidebar);
        sidebar.setLayout(sidebarLayout);
        sidebarLayout.setHorizontalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidebarLayout.createSequentialGroup()
                .addGroup(sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sidebarLayout.createSequentialGroup()
                        .addGap(0, 1, Short.MAX_VALUE)
                        .addGroup(sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(monthlyReport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(transactions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(addExpense, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(addIncome, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(charts, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(budget, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        sidebarLayout.setVerticalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidebarLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(addIncome)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(addExpense)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(transactions)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(monthlyReport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(charts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(budget)
                .addGap(41, 41, 41))
        );

        getContentPane().add(sidebar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 90, 120, 240));

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1015, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void amountTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_amountTextActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_amountTextActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
       insertExpenseTransaction(); // TODO add your handling code here:
    }//GEN-LAST:event_saveButtonActionPerformed

    private void categoryMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categoryMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_categoryMenuActionPerformed

    private void addIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIncomeActionPerformed
        navigateToScreen(new AddIncome(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addIncomeActionPerformed

    private void addExpenseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpenseActionPerformed
        navigateToScreen(new AddExpense(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addExpenseActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AddExpense("testUser123", "TestUser").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JTextField amountText;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel categoryLabel;
    private javax.swing.JComboBox<String> categoryMenu;
    private javax.swing.JButton charts;
    private com.toedter.calendar.JDateChooser date;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JTextArea descriptionArea;
    private javax.swing.JPanel expensePanel;
    private javax.swing.JLabel heading;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JLabel profileName;
    private javax.swing.JButton saveButton;
    private javax.swing.JPanel sidebar;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
