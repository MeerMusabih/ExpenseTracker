/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.expensetracker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import java.awt.Color;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sirh9
 */
public class AddIncome extends javax.swing.JFrame {
    
    private static final Logger logger = Logger.getLogger(AddIncome.class.getName());
    private String currentUserId;
    private String currentUserName;
    
    /**
     * Creates new form AddIncome
     * @param userId
     * @param userName
     */
    public AddIncome(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initComponents();
        
        setLocationRelativeTo(null); 
        
        applyThemeColors();
        setupInputPlaceholders();
        setupSources();
        setupNavigationListeners();

        
        profileName.setText("Welcome, " + userName);
        
        date.setDate(new Date()); 
    }

    private void setupSources() {
        sourceMenu.removeAllItems();
        for (CategoryModel category : CategoryModel.getIncomeCategories()) {
            sourceMenu.addItem(category.getName());
        }
        sourceMenu.setSelectedIndex(0);
    }
    
    public AddIncome() {
        initComponents();
        setLocationRelativeTo(null);
        applyThemeColors();
        setupInputPlaceholders();
        setupNavigationListeners();
        profileName.setText("Design View");
    }
    
    private void applyThemeColors() {
        Color LIGHT_TEXT = Color.WHITE;
        Color ACCENT_COLOR = new Color(0, 153, 255); // Blue
        Color DARK_BUTTON_BG = new Color(51, 51, 51);

        JButton[] buttons = {addIncome, addExpense, transactions, monthlyReport, charts, budget};
        for (JButton btn : buttons) {
            btn.setBackground(DARK_BUTTON_BG);
            btn.setForeground(LIGHT_TEXT);
        }
        
        addIncome.setBackground(ACCENT_COLOR);
        
        incomePanel.setBackground(new Color(255, 255, 255, 200));
        heading.setForeground(Color.BLACK);
        sourceLabel.setForeground(Color.BLACK);
        dateLabel.setForeground(Color.BLACK);
        
        saveButton.setBackground(new Color(0, 153, 76)); 
        saveButton.setForeground(LIGHT_TEXT);
        cancelButton.setBackground(new Color(200, 200, 200));
        cancelButton.setForeground(Color.BLACK);
    }
    
    private void setupInputPlaceholders() {

        setupPlaceholder(amountText, "Amount...");
        
        // Description Text Area (Placeholder)
        descriptionArea.setText("Description (e.g., received January salary)");
        descriptionArea.setForeground(Color.GRAY);
        
        descriptionArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (descriptionArea.getText().equals("Description (e.g., received January salary)")) {
                    descriptionArea.setText("");
                    descriptionArea.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (descriptionArea.getText().trim().isEmpty()) {
                    descriptionArea.setText("Description (e.g., received January salary)");
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
        
        addExpense.addActionListener(e -> navigateToScreen(new AddExpense(currentUserId, currentUserName)));
        addIncome.addActionListener(e -> {}); // Already on this screen
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

    private void insertIncomeTransaction() {
        String amountStr = amountText.getText().trim();
        String source = (String) sourceMenu.getSelectedItem();
        Date transactionDate = date.getDate();
        String description = descriptionArea.getText().trim();
        
        if (amountStr.isEmpty() || amountStr.equals("Amount...")) {
            JOptionPane.showMessageDialog(this, "Please enter the income amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (source == null || source.isEmpty() || source.equals("Source")) {
            JOptionPane.showMessageDialog(this, "Please select or enter an income source.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (transactionDate == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid date.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if date is in the future
        Date today = new Date();
        if (transactionDate.after(today)) {
            JOptionPane.showMessageDialog(this, "Cannot select a future date. Please select today or a past date.", "Invalid Date", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String finalDescription = description.equals("Description (e.g., received January salary)") ? "General Income" : description;

        // Firebase Insertion
        saveButton.setEnabled(false); // Prevent double clicks
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    String dateString = sdf.format(transactionDate);

                    Map<String, Object> data = new HashMap<>();
                    data.put("user_id", currentUserId);
                    data.put("amount", amount);
                    data.put("type", "Income");
                    data.put("category", source); // Source is used as category for Income
                    data.put("description", finalDescription);
                    data.put("transaction_date", dateString);

                    ApiFuture<DocumentReference> result = FirebaseService.getFirestore().collection("transactions").add(data);
                    result.get(); // Wait for completion
                    
                    SwingUtilities.invokeLater(() -> {
                         JOptionPane.showMessageDialog(AddIncome.this, "Income recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                         resetForm();
                    });
                    
                } catch (InterruptedException | ExecutionException ex) {
                    logger.log(Level.SEVERE, "Income insert error", ex);
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(AddIncome.this, "Failed to record income: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                    );
                } finally {
                    SwingUtilities.invokeLater(() -> saveButton.setEnabled(true));
                }
                return null;
            }
        };
        worker.execute();
    }
    
    private void resetForm() {
        amountText.setText("");
        setupPlaceholder(amountText, "Amount..."); 
        descriptionArea.setText("");
        setupInputPlaceholders();
        date.setDate(new Date()); 
        sourceMenu.setSelectedIndex(0);
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
        sidebar = new javax.swing.JPanel();
        addIncome = new javax.swing.JButton();
        addExpense = new javax.swing.JButton();
        transactions = new javax.swing.JButton();
        monthlyReport = new javax.swing.JButton();
        charts = new javax.swing.JButton();
        budget = new javax.swing.JButton();
        incomePanel = new javax.swing.JPanel();
        heading = new javax.swing.JLabel();
        amountText = new javax.swing.JTextField();
        saveButton = new javax.swing.JButton();
        sourceMenu = new javax.swing.JComboBox<>();
        sourceLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionArea = new javax.swing.JTextArea();
        cancelButton = new javax.swing.JButton();
        date = new com.toedter.calendar.JDateChooser();
        dateLabel = new javax.swing.JLabel();
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

        getContentPane().add(sidebar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 90, 120, 220));

        incomePanel.setOpaque(false);

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Add Income");

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

        sourceMenu.setEditable(true);
        sourceMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Salary", "Business", "Gift", "Others" }));
        sourceMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sourceMenuActionPerformed(evt);
            }
        });

        sourceLabel.setBackground(new java.awt.Color(102, 102, 102));
        sourceLabel.setText("Source: ");

        descriptionArea.setBackground(new java.awt.Color(255, 255, 255));
        descriptionArea.setColumns(20);
        descriptionArea.setForeground(new java.awt.Color(0, 0, 0));
        descriptionArea.setRows(5);
        descriptionArea.setText("Description");
        jScrollPane1.setViewportView(descriptionArea);

        cancelButton.setText("Cancel");

        dateLabel.setText("Date:");

        javax.swing.GroupLayout incomePanelLayout = new javax.swing.GroupLayout(incomePanel);
        incomePanel.setLayout(incomePanelLayout);
        incomePanelLayout.setHorizontalGroup(
            incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(incomePanelLayout.createSequentialGroup()
                .addGroup(incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(incomePanelLayout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(heading))
                    .addGroup(incomePanelLayout.createSequentialGroup()
                        .addGap(78, 78, 78)
                        .addGroup(incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(amountText, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                            .addComponent(jScrollPane1)
                            .addGroup(incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(incomePanelLayout.createSequentialGroup()
                                    .addComponent(sourceLabel)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(sourceMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(incomePanelLayout.createSequentialGroup()
                                    .addComponent(dateLabel)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, incomePanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(cancelButton)
                .addGap(43, 43, 43)
                .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(102, 102, 102))
        );
        incomePanelLayout.setVerticalGroup(
            incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(incomePanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(heading)
                .addGap(18, 18, 18)
                .addComponent(amountText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sourceMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sourceLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dateLabel))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(incomePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveButton)
                    .addComponent(cancelButton))
                .addContainerGap(46, Short.MAX_VALUE))
        );

        getContentPane().add(incomePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 80, 390, 340));

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1015, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void amountTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_amountTextActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_amountTextActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        insertIncomeTransaction();// TODO add your handling code here:
    }//GEN-LAST:event_saveButtonActionPerformed

    private void sourceMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sourceMenuActionPerformed

    private void addIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIncomeActionPerformed
        // TODO add your handling code here:
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AddIncome("TestUser", "TestUser").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JTextField amountText;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton charts;
    private com.toedter.calendar.JDateChooser date;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JTextArea descriptionArea;
    private javax.swing.JLabel heading;
    private javax.swing.JPanel incomePanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JLabel profileName;
    private javax.swing.JButton saveButton;
    private javax.swing.JPanel sidebar;
    private javax.swing.JLabel sourceLabel;
    private javax.swing.JComboBox<String> sourceMenu;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
