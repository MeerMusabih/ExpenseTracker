/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.expensetracker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.table.*;

/**
 *
 * @author sirh9
 */
public class Transactions extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Transactions.class.getName());
    private String currentUserId;
    private String currentUserName; 
    private DefaultTableModel tableModel;

    // Card Components
    private javax.swing.JPanel cardsPanel;
    private javax.swing.JPanel incomeCard;
    private javax.swing.JLabel incomeTitle;
    private javax.swing.JLabel incomeAmount;
    private javax.swing.JPanel expenseCard;
    private javax.swing.JLabel expenseTitle;
    private javax.swing.JLabel expenseAmount;
    private javax.swing.JPanel remainCard;
    private javax.swing.JLabel remainTitle;
    private javax.swing.JLabel remainAmount;
    
    /**
     * Creates new form ShowTransactions
     * @param userId
     * @param userName
     */
    public Transactions(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initComponents();
        
 
        setLocationRelativeTo(null); 
        setupCards(); // Add cards
        applyThemeColors();
        setupFilterMenu(); 
        setupTable(); 
        setupInputPlaceholders();
        setupNavigationListeners();
        this.tableModel = (javax.swing.table.DefaultTableModel) transactionTable.getModel();
        
        profileName.setText("Welcome, " + userName);
        
 
        loadTransactions(null, "All");
    }
    
 
    public Transactions() {
        initComponents();
        setLocationRelativeTo(null);
        setupCards();
        applyThemeColors();
        setupFilterMenu();
        setupTable();
        setupInputPlaceholders();
        profileName.setText("Design View");
    }

 

    private void applyThemeColors() {
        Color LIGHT_TEXT = Color.WHITE;
        Color ACCENT_COLOR = new Color(0, 153, 255); // Blue
        Color DARK_BUTTON_BG = new Color(51, 51, 51);

        // Card Styles
        if (incomeCard != null) incomeCard.setBackground(new Color(40, 90, 40)); 
        if (expenseCard != null) expenseCard.setBackground(new Color(110, 40, 40));
        if (remainCard != null) remainCard.setBackground(new Color(60, 60, 60));
        
        if (incomeTitle != null) incomeTitle.setForeground(LIGHT_TEXT);
        if (incomeAmount != null) incomeAmount.setForeground(LIGHT_TEXT);
        if (expenseTitle != null) expenseTitle.setForeground(LIGHT_TEXT);
        if (expenseAmount != null) expenseAmount.setForeground(LIGHT_TEXT);
        if (remainTitle != null) remainTitle.setForeground(LIGHT_TEXT);
        if (remainAmount != null) remainAmount.setForeground(LIGHT_TEXT);


        JButton[] buttons = {addIncome, addExpense, transactions, monthlyReport, charts, budget};
        for (JButton btn : buttons) {
            btn.setBackground(DARK_BUTTON_BG);
            btn.setForeground(LIGHT_TEXT);
        }
        

        transactions.setBackground(ACCENT_COLOR);
        

        transactionPanel.setBackground(new Color(255, 255, 255, 200));
        heading.setForeground(Color.BLACK);
        filterLabel.setForeground(Color.BLACK);
        

        transactionTable.setBackground(Color.WHITE);
        transactionTable.setForeground(Color.BLACK);
        transactionTable.getTableHeader().setBackground(new Color(200, 200, 200));
        transactionTable.getTableHeader().setForeground(Color.BLACK);
        jScrollPane1.getViewport().setBackground(Color.WHITE); 
    }
    
    private void setupTable() {

        String[] columnNames = {"ID", "Date", "Type", "Category", "Description", "Amount"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {

                if (columnIndex == 5) return Double.class; 
                return super.getColumnClass(columnIndex);
            }
        };
        transactionTable.setModel(tableModel);


        transactionTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                

                String type = (String) table.getModel().getValueAt(row, 2); 
                
                if (!isSelected) {
                    if ("Income".equals(type)) {
                        c.setBackground(new Color(204, 255, 204));
                    } else if ("Expense".equals(type)) {
                        c.setBackground(new Color(255, 204, 204));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else {

                    c.setBackground(table.getSelectionBackground());
                }
                

                if (column == 5 && value instanceof Double) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    c.setForeground(Color.BLACK);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                
                return c;
            }
        });
        

        transactionTable.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(80); // Date
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(70); // Type
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Category
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Description
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Amount
    }
    
    private void setupFilterMenu() {
        filterMenu.removeAllItems();

        filterMenu.addItem("All");
        filterMenu.addItem("Income");
        filterMenu.addItem("Expense");
        
        
        // Standard categories from CategoryModel
        for (String c : CategoryModel.getAllCategoryNames()) {
            filterMenu.addItem(c);
        }
        
        filterMenu.addActionListener(this::filterMenuActionPerformed);
    }

    private void setupInputPlaceholders() {
        setupPlaceholder(search, "Search by Description or Category...");
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearchAndFilter();
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
    
    private void setupCards() {
        // Initialize components
        cardsPanel = new javax.swing.JPanel();
        incomeCard = new javax.swing.JPanel();
        incomeTitle = new javax.swing.JLabel();
        incomeAmount = new javax.swing.JLabel();
        expenseCard = new javax.swing.JPanel();
        expenseTitle = new javax.swing.JLabel();
        expenseAmount = new javax.swing.JLabel();
        remainCard = new javax.swing.JPanel();
        remainTitle = new javax.swing.JLabel();
        remainAmount = new javax.swing.JLabel();

        // Configure Layouts & Text
        cardsPanel.setOpaque(false);
        cardsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));

        // Income Card
        incomeCard.setPreferredSize(new java.awt.Dimension(150, 100));
        incomeCard.setLayout(new java.awt.GridLayout(2, 1));
        incomeTitle.setText(" Total Income");
        incomeAmount.setText(" Rs. 0.00");
        incomeAmount.setFont(new java.awt.Font("Segoe UI", 0, 18));
        incomeCard.add(incomeTitle);
        incomeCard.add(incomeAmount);

        // Expense Card
        expenseCard.setPreferredSize(new java.awt.Dimension(150, 100));
        expenseCard.setLayout(new java.awt.GridLayout(2, 1));
        expenseTitle.setText(" Total Expense");
        expenseAmount.setText(" Rs. 0.00");
        expenseAmount.setFont(new java.awt.Font("Segoe UI", 0, 18));
        expenseCard.add(expenseTitle);
        expenseCard.add(expenseAmount);

        // Remain Card
        remainCard.setPreferredSize(new java.awt.Dimension(180, 100));
        remainCard.setLayout(new java.awt.GridLayout(2, 1));
        remainTitle.setText(" Overall Savings");
        remainAmount.setText(" Rs. 0.00");
        remainAmount.setFont(new java.awt.Font("Segoe UI", 0, 18));
        remainCard.add(remainTitle);
        remainCard.add(remainAmount);

        // Add cards to panel
        cardsPanel.add(incomeCard);
        cardsPanel.add(expenseCard);
        cardsPanel.add(remainCard);

        // Add panel to frame
        getContentPane().add(cardsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 80, 630, 110));
        
        // Adjust transactionPanel
        if (transactionPanel != null) {
            getContentPane().remove(transactionPanel);
            getContentPane().add(transactionPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 200, 630, 400));
        }

        // Adjust bgImage z-order (ensure it's at back)
         if (bgImage != null) {
            getContentPane().remove(bgImage);
            getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1070, 750));
        }
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
        transactions.addActionListener(e -> {}); 
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
    


    private void loadTransactions(String searchTerm, String filterBy) {
        
        if (tableModel == null) {
            tableModel = (DefaultTableModel) transactionTable.getModel();
        }
        
        tableModel.setRowCount(0); 
        
        // SwingWorker for background loading
        SwingWorker<List<QueryDocumentSnapshot>, Void> worker = new SwingWorker<List<QueryDocumentSnapshot>, Void>() {
            @Override
            protected List<QueryDocumentSnapshot> doInBackground() throws Exception {
                Firestore db = FirebaseService.getFirestore();
                // Get all transactions for user
                ApiFuture<QuerySnapshot> future = db.collection("transactions")
                        .whereEqualTo("user_id", currentUserId)
                        .get();
                return future.get().getDocuments();
            }

            @Override
            protected void done() {
                try {
                    List<QueryDocumentSnapshot> documents = get();
                    List<TransactionModel> txList = new ArrayList<>();
                    
                    double totalIncome = 0;
                    double totalExpense = 0;
                    
                    // Client-side processing
                    for (QueryDocumentSnapshot doc : documents) {
                        TransactionModel tx = new TransactionModel();
                        tx.setId(doc.getId());
                        if (doc.contains("amount")) tx.setAmount(doc.getDouble("amount"));
                        if (doc.contains("type")) tx.setType(doc.getString("type"));
                        if (doc.contains("category")) tx.setCategory(doc.getString("category"));
                        if (doc.contains("description")) tx.setDescription(doc.getString("description"));
                        
                        // Calculate totals
                        if (tx.getAmount() > 0) {
                            if ("Income".equalsIgnoreCase(tx.getType())) {
                                totalIncome += tx.getAmount();
                            } else if ("Expense".equalsIgnoreCase(tx.getType())) {
                                totalExpense += tx.getAmount();
                            }
                        }

                        String dateStr = doc.getString("transaction_date");
                        if (dateStr != null) {
                            tx.setDate(dateStr);
                        }
                        
                        txList.add(tx);
                    }

                    // Update Card UI
                    final double fIncome = totalIncome;
                    final double fExpense = totalExpense;
                    final double fSavings = totalIncome - totalExpense;
                    
                    incomeAmount.setText(String.format("Rs. %.2f", fIncome));
                    expenseAmount.setText(String.format("Rs. %.2f", fExpense));
                    remainAmount.setText(String.format("Rs. %.2f", fSavings));
                    
                    if (fSavings < 0) {
                        remainAmount.setForeground(new Color(255, 100, 100));
                    } else {
                        remainAmount.setForeground(new Color(100, 255, 100));
                    }
                    
                    // Filtering
                    List<TransactionModel> filtered = new ArrayList<>();
                    for (TransactionModel tx : txList) {
                        boolean matchesFilter = true;
                        if (filterBy != null && !filterBy.equals("All")) {
                            if (filterBy.equals("Income") || filterBy.equals("Expense")) {
                                if (!filterBy.equals(tx.getType())) matchesFilter = false;
                            } else {
                                if (!filterBy.equals(tx.getCategory())) matchesFilter = false;
                            }
                        }
                        
                        if (matchesFilter) {
                             if (searchTerm != null && !searchTerm.isEmpty()) {
                                 String lowerSearch = searchTerm.toLowerCase();
                                 String desc = tx.getDescription() != null ? tx.getDescription().toLowerCase() : "";
                                 String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";
                                 if (!desc.contains(lowerSearch) && !cat.contains(lowerSearch)) {
                                     matchesFilter = false;
                                 }
                             }
                        }
                        
                        if (matchesFilter) filtered.add(tx);
                    }
                    
                    // Sorting (Date desc)
                    Collections.sort(filtered, (t1, t2) -> {
                        if (t1.getDate() == null || t2.getDate() == null) return 0;
                        return t2.getDate().compareTo(t1.getDate());
                    });
                    
                    // Populate Table
                    for (TransactionModel tx : filtered) {
                        tableModel.addRow(new Object[]{
                            tx.getId(), // ID
                            tx.getDate(), // Date
                            tx.getType(), // Type
                            tx.getCategory(), // Category
                            tx.getDescription(), // Description
                            tx.getAmount() // Amount
                        });
                    }
                    
                    if (tableModel.getRowCount() == 0) {
                        tableModel.addRow(new Object[]{"", "", "", "No Transactions Found", "", ""});
                    }
                    
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error fetching transactions.", ex);
                     JOptionPane.showMessageDialog(Transactions.this, "Error loading transactions: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void performSearchAndFilter() {
        String filter = (String) filterMenu.getSelectedItem();
        String searchTxt = search.getText();
        
        // Handle placeholder text
        if (searchTxt.equals("Search by Description or Category...")) {
            searchTxt = null;
        }

        loadTransactions(searchTxt, filter);
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
        transactionPanel = new javax.swing.JPanel();
        heading = new javax.swing.JLabel();
        search = new javax.swing.JTextField();
        filterMenu = new javax.swing.JComboBox<>();
        filterLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        transactionTable = new javax.swing.JTable();
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

        transactionPanel.setOpaque(false);

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Transactions");

        search.setBackground(new java.awt.Color(255, 255, 255));
        search.setForeground(new java.awt.Color(0, 0, 0));
        search.setText("Search...");
        search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        filterMenu.setEditable(true);
        filterMenu.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Salary", "Business", "Gift", "Others" }));
        filterMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterMenuActionPerformed(evt);
            }
        });

        filterLabel.setBackground(new java.awt.Color(102, 102, 102));
        filterLabel.setText("Filter By:");

        transactionTable.setModel(new javax.swing.table.DefaultTableModel(
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
        transactionTable.setShowHorizontalLines(true);
        transactionTable.setShowVerticalLines(true);
        jScrollPane1.setViewportView(transactionTable);

        javax.swing.GroupLayout transactionPanelLayout = new javax.swing.GroupLayout(transactionPanel);
        transactionPanel.setLayout(transactionPanelLayout);
        transactionPanelLayout.setHorizontalGroup(
            transactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transactionPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(heading)
                .addGap(194, 194, 194))
            .addGroup(transactionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(transactionPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(transactionPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 613, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(11, Short.MAX_VALUE))))
        );
        transactionPanelLayout.setVerticalGroup(
            transactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transactionPanelLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(heading)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(transactionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(filterMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(filterLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        getContentPane().add(transactionPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 80, 630, 470));

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

        getContentPane().add(sidebar, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 90, 120, 230));

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1070, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchActionPerformed

    private void filterMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterMenuActionPerformed
       performSearchAndFilter(); // TODO add your handling code here:
    }//GEN-LAST:event_filterMenuActionPerformed

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


        java.awt.EventQueue.invokeLater(() -> new Transactions("testUser123", "TestUser").setVisible(true));
    
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JButton charts;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JComboBox<String> filterMenu;
    private javax.swing.JLabel heading;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JLabel profileName;
    private javax.swing.JTextField search;
    private javax.swing.JPanel sidebar;
    private javax.swing.JPanel transactionPanel;
    private javax.swing.JTable transactionTable;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
