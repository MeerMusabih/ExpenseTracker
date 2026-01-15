/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.expensetracker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author sirh9
 */
public class MonthlyReport extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MonthlyReport.class.getName());
    private String currentUserId;
    private String currentUserName; 
    private DefaultTableModel tableModel;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("Rs #,##0.00");

    /**
     * Creates new form MonthlyReport
     * @param userId
     * @param userName
     */
   public MonthlyReport(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initComponents();
        
        setLocationRelativeTo(null); 
        applyThemeColors();
        setupMonthMenu(); 
        setupTable(); 
        setupNavigationListeners();
        
        profileName.setText("Welcome, " + userName);
        
        loadReportData();
    }
    
    public MonthlyReport() {
        initComponents();
        setLocationRelativeTo(null);
        applyThemeColors();
        setupMonthMenu();
        setupTable();
        profileName.setText("Design View");
    }

    private void applyThemeColors() {
        Color LIGHT_TEXT = Color.WHITE;
        Color ACCENT_COLOR = new Color(0, 153, 255); // Blue
        Color DARK_BUTTON_BG = new Color(51, 51, 51);
        Color INCOME_GREEN = new Color(0, 153, 51);
        Color EXPENSE_RED = new Color(204, 0, 51);
        Color SAVINGS_BLUE = new Color(0, 102, 204);

        JButton[] buttons = {addIncome, addExpense, transactions, monthlyReport, charts, budget};
        for (JButton btn : buttons) {
            btn.setBackground(DARK_BUTTON_BG);
            btn.setForeground(LIGHT_TEXT);
        }
        
        monthlyReport.setBackground(ACCENT_COLOR);
        exportButton.setBackground(ACCENT_COLOR);
        exportButton.setForeground(LIGHT_TEXT);
        
        incomeCard.setBackground(INCOME_GREEN);
        expenseCard.setBackground(EXPENSE_RED);
        savingsCard.setBackground(SAVINGS_BLUE);
        
        JLabel[] cardLabels = {incomeLabel, expenseLabel, savingsLabel, incomeAmount, expenseAmount, savingsAmount};
        for (JLabel label : cardLabels) {
            label.setForeground(LIGHT_TEXT);
        }
        
        reportCards.setOpaque(true);
        reportCards.setBackground(new Color(255, 255, 255, 150)); 
        heading.setForeground(Color.BLACK);
        monthLabel.setForeground(Color.BLACK);
        
        reportTable.setBackground(Color.WHITE);
        reportTable.setForeground(Color.BLACK);
        reportTable.getTableHeader().setBackground(new Color(200, 200, 200));
        reportTable.getTableHeader().setForeground(Color.BLACK);
        jScrollPane1.getViewport().setBackground(Color.WHITE); 
    }
    
    private void setupMonthMenu() {
        monthMenu.removeAllItems();
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            monthMenu.addItem(month.format(monthYearFormatter));
        }
        
        monthMenu.setSelectedItem(today.format(monthYearFormatter));
        
        monthMenu.addActionListener(this::monthMenuActionPerformed);
    }
    
    private void setupTable() {
        String[] columnNames = {"Category", "Total Expense"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        reportTable.setModel(tableModel);

        reportTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Category
        reportTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Amount
    }
    
    private void setupNavigationListeners() {
        logo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigateToHome();
            }
        });
        
        addIncome.addActionListener(e -> navigateToScreen(new AddIncome(currentUserId, currentUserName)));
        addExpense.addActionListener(e -> navigateToScreen(new AddExpense(currentUserId, currentUserName)));
        transactions.addActionListener(e -> navigateToScreen(new Transactions(currentUserId, currentUserName))); 
        monthlyReport.addActionListener(e -> {}); 
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
    
    private void monthMenuActionPerformed(java.awt.event.ActionEvent evt) {                                        
        loadReportData();
    } 

    private void loadReportData() {
        String selectedMonthYear = (String) monthMenu.getSelectedItem();
        if (selectedMonthYear == null) return;
        if (currentUserId == null) return;

        LocalDate date = LocalDate.parse("01 " + selectedMonthYear, DateTimeFormatter.ofPattern("dd MMM yyyy"));
        LocalDate startDate = date.withDayOfMonth(1);
        LocalDate endDate = date.withDayOfMonth(date.lengthOfMonth());
        
        String startStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        SwingWorker<List<QueryDocumentSnapshot>, Void> worker = new SwingWorker<List<QueryDocumentSnapshot>, Void>() {
            @Override
            protected List<QueryDocumentSnapshot> doInBackground() throws Exception {
                Firestore db = FirebaseService.getFirestore();
                // Fetch all user transactions
                ApiFuture<QuerySnapshot> future = db.collection("transactions")
                        .whereEqualTo("user_id", currentUserId)
                        .get();
                return future.get().getDocuments();
            }

            @Override
            protected void done() {
                try {
                    List<QueryDocumentSnapshot> docs = get();
                    double totalIncome = 0.0;
                    double totalExpense = 0.0;
                    Map<String, Double> categoryExpenseMap = new HashMap<>();
                    
                    for (QueryDocumentSnapshot doc : docs) {
                        String tDate = doc.getString("transaction_date");
                        if (tDate == null || tDate.compareTo(startStr) < 0 || tDate.compareTo(endStr) > 0) {
                            continue;
                        }
                        
                        String type = doc.getString("type");
                        double amount = 0.0;
                        if (doc.contains("amount")) amount = doc.getDouble("amount");
                        
                        if ("Income".equalsIgnoreCase(type)) {
                            totalIncome += amount;
                        } else if ("Expense".equalsIgnoreCase(type)) {
                            totalExpense += amount;
                            String cat = doc.getString("category");
                            if (cat != null) {
                                categoryExpenseMap.put(cat, categoryExpenseMap.getOrDefault(cat, 0.0) + amount);
                            }
                        }
                    }
                    
                    double totalSavings = totalIncome - totalExpense;
                    
                    incomeAmount.setText(CURRENCY_FORMAT.format(totalIncome));
                    expenseAmount.setText(CURRENCY_FORMAT.format(totalExpense));
                    savingsAmount.setText(CURRENCY_FORMAT.format(totalSavings));
                    
                    if (totalSavings < 0) {
                        savingsCard.setBackground(new Color(153, 0, 0)); 
                    } else {
                        savingsCard.setBackground(new Color(0, 102, 204));
                    }
                    
                    tableModel.setRowCount(0);
                    // Sort categories by amount desc
                    categoryExpenseMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .forEach(entry -> {
                            tableModel.addRow(new Object[]{
                                entry.getKey(),
                                CURRENCY_FORMAT.format(entry.getValue())
                            });
                        });
                        
                    if (tableModel.getRowCount() == 0) {
                        tableModel.addRow(new Object[]{"No expenses recorded for this month.", ""});
                    }

                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error fetching monthly report data.", ex);
                    JOptionPane.showMessageDialog(MonthlyReport.this, "Error loading report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

        exportButton = new javax.swing.JButton();
        monthMenu = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        reportTable = new javax.swing.JTable();
        monthLabel = new javax.swing.JLabel();
        selectButton = new javax.swing.JButton();
        heading = new javax.swing.JLabel();
        reportCards = new javax.swing.JPanel();
        incomeCard = new javax.swing.JPanel();
        incomeLabel = new javax.swing.JLabel();
        incomeAmount = new javax.swing.JLabel();
        expenseCard = new javax.swing.JPanel();
        expenseLabel = new javax.swing.JLabel();
        expenseAmount = new javax.swing.JLabel();
        savingsCard = new javax.swing.JPanel();
        savingsLabel = new javax.swing.JLabel();
        savingsAmount = new javax.swing.JLabel();
        profileName = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
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

        exportButton.setBackground(new java.awt.Color(0, 153, 255));
        exportButton.setText("Export");
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        getContentPane().add(exportButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 170, -1, -1));

        monthMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        getContentPane().add(monthMenu, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 170, -1, -1));

        reportTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(reportTable);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 330, -1, 250));

        monthLabel.setText("Select Month:");
        getContentPane().add(monthLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 170, -1, -1));

        selectButton.setText("select");
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectButtonActionPerformed(evt);
            }
        });
        getContentPane().add(selectButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 170, -1, -1));

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Monthly Report");
        getContentPane().add(heading, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 100, -1, -1));

        reportCards.setOpaque(false);

        incomeCard.setBackground(new java.awt.Color(51, 51, 51));

        incomeLabel.setText("Total Income");

        incomeAmount.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        incomeAmount.setText("Rs. 0.00");

        javax.swing.GroupLayout incomeCardLayout = new javax.swing.GroupLayout(incomeCard);
        incomeCard.setLayout(incomeCardLayout);
        incomeCardLayout.setHorizontalGroup(
            incomeCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(incomeCardLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(incomeLabel)
                .addContainerGap(64, Short.MAX_VALUE))
            .addComponent(incomeAmount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        incomeCardLayout.setVerticalGroup(
            incomeCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(incomeCardLayout.createSequentialGroup()
                .addComponent(incomeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 62, Short.MAX_VALUE)
                .addComponent(incomeAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        expenseCard.setBackground(new java.awt.Color(102, 102, 102));

        expenseLabel.setText("Total Expense");

        expenseAmount.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        expenseAmount.setText("Rs. 0.00");

        javax.swing.GroupLayout expenseCardLayout = new javax.swing.GroupLayout(expenseCard);
        expenseCard.setLayout(expenseCardLayout);
        expenseCardLayout.setHorizontalGroup(
            expenseCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(expenseCardLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expenseLabel)
                .addContainerGap(69, Short.MAX_VALUE))
            .addComponent(expenseAmount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        expenseCardLayout.setVerticalGroup(
            expenseCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(expenseCardLayout.createSequentialGroup()
                .addComponent(expenseLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addComponent(expenseAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        savingsCard.setBackground(new java.awt.Color(153, 153, 153));

        savingsLabel.setForeground(new java.awt.Color(0, 0, 0));
        savingsLabel.setText("Savings");

        savingsAmount.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        savingsAmount.setForeground(new java.awt.Color(0, 0, 0));
        savingsAmount.setText("Rs. 0.00");

        javax.swing.GroupLayout savingsCardLayout = new javax.swing.GroupLayout(savingsCard);
        savingsCard.setLayout(savingsCardLayout);
        savingsCardLayout.setHorizontalGroup(
            savingsCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(savingsCardLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(savingsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(savingsCardLayout.createSequentialGroup()
                .addComponent(savingsAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        savingsCardLayout.setVerticalGroup(
            savingsCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(savingsCardLayout.createSequentialGroup()
                .addComponent(savingsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addComponent(savingsAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout reportCardsLayout = new javax.swing.GroupLayout(reportCards);
        reportCards.setLayout(reportCardsLayout);
        reportCardsLayout.setHorizontalGroup(
            reportCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(reportCardsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(incomeCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(expenseCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(savingsCard, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        reportCardsLayout.setVerticalGroup(
            reportCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(reportCardsLayout.createSequentialGroup()
                .addGroup(reportCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(incomeCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(expenseCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(savingsCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        getContentPane().add(reportCards, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 200, 470, 120));

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

        getContentPane().add(sidebar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 90, 120, 240));

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1015, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
       String monthYear = (String) monthMenu.getSelectedItem();
        if (monthYear == null) return;
        
        String filename = "Monthly_Report_" + monthYear.replace(" ", "_") + ".csv";
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filename));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (FileWriter writer = new FileWriter(file)) {
            
                writer.append("Monthly Report Summary:,").append(monthYear).append("\n");
                writer.append("Total Income:,").append(incomeAmount.getText()).append("\n");
                writer.append("Total Expense:,").append(expenseAmount.getText()).append("\n");
                writer.append("Net Savings:,").append(savingsAmount.getText()).append("\n\n");
                
            
                writer.append("Expense Breakdown:\n");
                
            
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.append(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) writer.append(",");
                }
                writer.append("\n");
                
            
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.append(tableModel.getValueAt(i, j).toString());
                        if (j < tableModel.getColumnCount() - 1) writer.append(",");
                    }
                    writer.append("\n");
                }
                
                JOptionPane.showMessageDialog(this, "Report exported successfully to:\n" + file.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
                
            
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error exporting CSV file.", ex);
                JOptionPane.showMessageDialog(this, "Error writing file: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        } // TODO add your handling code here:
    }//GEN-LAST:event_exportButtonActionPerformed

    private void addIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIncomeActionPerformed
        navigateToScreen(new AddIncome(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addIncomeActionPerformed

    private void addExpenseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpenseActionPerformed
        navigateToScreen(new AddExpense(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addExpenseActionPerformed

    private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed
        loadReportData();        // TODO add your handling code here:
    }//GEN-LAST:event_selectButtonActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new MonthlyReport("testUser123", "TestUser").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JButton charts;
    private javax.swing.JLabel expenseAmount;
    private javax.swing.JPanel expenseCard;
    private javax.swing.JLabel expenseLabel;
    private javax.swing.JButton exportButton;
    private javax.swing.JLabel heading;
    private javax.swing.JLabel incomeAmount;
    private javax.swing.JPanel incomeCard;
    private javax.swing.JLabel incomeLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo;
    private javax.swing.JLabel monthLabel;
    private javax.swing.JComboBox<String> monthMenu;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JLabel profileName;
    private javax.swing.JPanel reportCards;
    private javax.swing.JTable reportTable;
    private javax.swing.JLabel savingsAmount;
    private javax.swing.JPanel savingsCard;
    private javax.swing.JLabel savingsLabel;
    private javax.swing.JButton selectButton;
    private javax.swing.JPanel sidebar;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
