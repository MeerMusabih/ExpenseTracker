/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.expensetracker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.awt.Color;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.*;
import org.jfree.data.general.*;

/**
 *
 * @author sirh9
 */
public class HomePage extends javax.swing.JFrame {
    
    private static final Logger logger = Logger.getLogger(HomePage.class.getName());
    private String currentUserId; // Changed to String for Firebase
    private String currentUserName;
    
    /**
     * Creates new form HomePage
     * @param userId
     * @param username
     */
    public HomePage(String userId, String username) {
        this.currentUserId = userId;
        this.currentUserName = username;
        initComponents();
        
        setLocationRelativeTo(null); 
        
        profileName.setText("Welcome, " + username);
        
        applyThemeColors();
        
        loadDashboardData();
        
        setupNavigationListeners();
    }

    // Default constructor for design view / testing
    public HomePage() {
        initComponents();
        applyThemeColors();
        setLocationRelativeTo(null);
    }
    
    private void applyThemeColors() {

        Color DARK_CARD = new Color(51, 51, 51);
        Color LIGHT_TEXT = Color.WHITE;
        
        IncomeCard.setBackground(new Color(40, 90, 40)); // Dark Green
        ExpenseCard.setBackground(new Color(110, 40, 40)); // Dark Red
        RemainCard.setBackground(new Color(60, 60, 60)); // Dark Gray
        
        incomeTitle.setForeground(LIGHT_TEXT);
        incomeAmount.setForeground(LIGHT_TEXT);
        expenseTitle.setForeground(LIGHT_TEXT);
        expenseAmount.setForeground(LIGHT_TEXT);
        remainTitle.setForeground(LIGHT_TEXT);
        remainAmount.setForeground(LIGHT_TEXT);
        
        chartsPanel.setBackground(new Color(30, 30, 30));
        placeHolder.setForeground(LIGHT_TEXT);
        
        JButton[] buttons = {addIncome, addExpense, transactions, monthlyReport, charts, budget};
        for (JButton btn : buttons) {
            btn.setBackground(DARK_CARD);
            btn.setForeground(LIGHT_TEXT);
        }
        
        RecentTransactions.setBackground(new Color(40, 40, 40));
        RecentTransactions.setForeground(LIGHT_TEXT);
        jScrollPane1.getViewport().setBackground(new Color(40, 40, 40));
    }
    
    private void setupNavigationListeners() {
        addIncome.addActionListener(e -> navigateToScreen(new AddIncome(currentUserId, currentUserName))); 
        addExpense.addActionListener(e -> navigateToScreen(new AddExpense(currentUserId, currentUserName)));
        addExpenseFAB.addActionListener(e -> navigateToScreen(new AddExpense(currentUserId, currentUserName)));
        
        transactions.addActionListener(e -> navigateToScreen(new Transactions(currentUserId, currentUserName)));
        monthlyReport.addActionListener(e -> navigateToScreen(new MonthlyReport(currentUserId, currentUserName)));
        charts.addActionListener(e -> navigateToScreen(new Charts(currentUserId, currentUserName))); 
        budget.addActionListener(e -> navigateToScreen(new Budget(currentUserId, currentUserName))); 
        
        profileName.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (JOptionPane.showConfirmDialog(null, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    dispose();
                    new Login().setVisible(true);
                }
            }
        });
    }
    
    private void navigateToScreen(JFrame targetScreen) {
        this.dispose();
        targetScreen.setVisible(true);
    }
    
    private void loadDashboardData() {
        if (currentUserId == null || currentUserId.isEmpty()) return;
        
        // Run in background to avoid freezing UI
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                loadSummaryCards();
                loadRecentTransactions();
                loadCategoryChartData();
                return null;
            }
        };
        worker.execute();
    }

    private void loadCategoryChartData() {
        try {
            // Get current month string YYYY-MM
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
            String currentYearMonth = sdf.format(new java.util.Date());

            CollectionReference transactionsRef = FirebaseService.getFirestore().collection("transactions");
            Query query = transactionsRef.whereEqualTo("user_id", currentUserId)
                                         .whereEqualTo("type", "Expense");
            
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            DefaultPieDataset dataset = new DefaultPieDataset();
            Map<String, Double> categoryTotals = new HashMap<>();
            boolean hasData = false;

            for (QueryDocumentSnapshot doc : documents) {
                String date = doc.getString("transaction_date");
                if (date != null && date.startsWith(currentYearMonth)) {
                    String category = doc.getString("category");
                    Double amount = doc.getDouble("amount");
                    if (category != null && amount != null) {
                        categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                        hasData = true;
                    }
                }
            }

            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            final boolean finalHasData = hasData;

            SwingUtilities.invokeLater(() -> {
                chartsPanel.removeAll();
                chartsPanel.setLayout(new java.awt.BorderLayout());
                
                if (finalHasData) {
                    JFreeChart chart = createCategoryPieChart(dataset);
                    ChartPanel chartPanel = new ChartPanel(chart);
                    chartPanel.setMouseWheelEnabled(true);
                    chartsPanel.add(chartPanel, java.awt.BorderLayout.CENTER);
                    placeHolder.setText(""); 
                } else {
                    JLabel noDataLabel = new JLabel("No Expenses This Month", SwingConstants.CENTER);
                    noDataLabel.setForeground(Color.LIGHT_GRAY);
                    noDataLabel.setFont(new java.awt.Font("Segoe UI", 1, 14));
                    chartsPanel.add(noDataLabel, java.awt.BorderLayout.CENTER);
                    placeHolder.setText(""); 
                }
                chartsPanel.revalidate();
                chartsPanel.repaint();
            });

        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.SEVERE, "Failed to load category chart data.", ex);
        }
    }

    private JFreeChart createCategoryPieChart(PieDataset dataset) {
        JFreeChart chart = ChartFactory.createPieChart(
            "Monthly Expenses by Category",
            dataset,                       
            true,                          
            true,
            false
        );

        chart.setBackgroundPaint(new Color(30, 30, 30));
        chart.getTitle().setPaint(Color.WHITE);
        
        org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelBackgroundPaint(Color.WHITE);
        plot.setLabelPaint(Color.BLACK);
        plot.setOutlineVisible(false); 
        plot.setOutlinePaint(new Color(30, 30, 30));
        plot.setShadowPaint(null);
        plot.setBackgroundPaint(new Color(30, 30, 30));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(new Color(30, 30, 30));
            chart.getLegend().setItemPaint(Color.WHITE);
        }
        
        return chart;
    }

    private void loadSummaryCards() {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
            String currentYearMonth = sdf.format(new java.util.Date());

            CollectionReference transactionsRef = FirebaseService.getFirestore().collection("transactions");
            Query query = transactionsRef.whereEqualTo("user_id", currentUserId);
            
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            double income = 0;
            double expense = 0;

            for (QueryDocumentSnapshot doc : documents) {
                String date = doc.getString("transaction_date");
                if (date != null && date.startsWith(currentYearMonth)) {
                    String type = doc.getString("type");
                    Double amount = doc.getDouble("amount");
                    if (type != null && amount != null) {
                        if ("Income".equalsIgnoreCase(type)) {
                            income += amount;
                        } else if ("Expense".equalsIgnoreCase(type)) {
                            expense += amount;
                        }
                    }
                }
            }

            double balance = income - expense;
            final double fIncome = income;
            final double fExpense = expense;
            final double fBalance = balance;

            SwingUtilities.invokeLater(() -> {
                incomeAmount.setText(String.format("Rs. %.2f", fIncome));
                expenseAmount.setText(String.format("Rs. %.2f", fExpense));
                remainAmount.setText(String.format("Rs. %.2f", fBalance));
                
                if (fBalance < 0) {
                    remainAmount.setForeground(new Color(255, 100, 100)); // Light Red
                } else {
                    remainAmount.setForeground(new Color(100, 255, 100)); // Light Green
                }
            });

        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.SEVERE, "Failed to load summary data", ex);
        }
    }
    
    private void loadRecentTransactions() {
        try {
            CollectionReference transactionsRef = FirebaseService.getFirestore().collection("transactions");
            // Note: Composite index needed for querying user_id AND ordering by date
            // For now, simpler approach: get all user transactions, sort in memory
            Query query = transactionsRef.whereEqualTo("user_id", currentUserId);
            
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            
            // Convert to TransactionModel objects to sort
            java.util.List<TransactionModel> txList = new java.util.ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                TransactionModel tm = doc.toObject(TransactionModel.class);
                tm.setId(doc.getId());
                txList.add(tm);
            }
            
            // Sort by date descending
            txList.sort((t1, t2) -> {
                String d1 = t1.getDate() != null ? t1.getDate() : "";
                String d2 = t2.getDate() != null ? t2.getDate() : "";
                return d2.compareTo(d1); // Descending
            });
            
            // Limit to 15
            int limit = Math.min(txList.size(), 15);
            List<TransactionModel> recent = txList.subList(0, limit);

            DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Type", "Category", "Description", "Amount"}, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            for (TransactionModel tx : recent) {
                 model.addRow(new Object[]{tx.getType(), tx.getCategory(), tx.getDescription(), String.format("Rs. %.2f", tx.getAmount())});
            }

            SwingUtilities.invokeLater(() -> RecentTransactions.setModel(model));

        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.SEVERE, "Failed to load recent transactions", ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidebar = new javax.swing.JPanel();
        addIncome = new javax.swing.JButton();
        addExpense = new javax.swing.JButton();
        transactions = new javax.swing.JButton();
        monthlyReport = new javax.swing.JButton();
        charts = new javax.swing.JButton();
        budget = new javax.swing.JButton();
        addExpenseFAB = new javax.swing.JButton();
        homePageCards = new javax.swing.JPanel();
        IncomeCard = new javax.swing.JPanel();
        incomeTitle = new javax.swing.JLabel();
        incomeAmount = new javax.swing.JLabel();
        ExpenseCard = new javax.swing.JPanel();
        expenseTitle = new javax.swing.JLabel();
        expenseAmount = new javax.swing.JLabel();
        RemainCard = new javax.swing.JPanel();
        remainTitle = new javax.swing.JLabel();
        remainAmount = new javax.swing.JLabel();
        profileName = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        RecentTransactions = new javax.swing.JTable();
        chartsPanel = new javax.swing.JPanel();
        placeHolder = new javax.swing.JLabel();
        bgImage = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

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

        addExpenseFAB.setBackground(new java.awt.Color(0, 153, 255));
        addExpenseFAB.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        addExpenseFAB.setText("+");
        addExpenseFAB.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        addExpenseFAB.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        addExpenseFAB.setFocusPainted(false);
        addExpenseFAB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        addExpenseFAB.setMaximumSize(new java.awt.Dimension(60, 60));
        addExpenseFAB.setMinimumSize(new java.awt.Dimension(60, 60));
        addExpenseFAB.setPreferredSize(new java.awt.Dimension(60, 60));
        addExpenseFAB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addExpenseFABActionPerformed(evt);
            }
        });
        getContentPane().add(addExpenseFAB, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 490, 60, 60));

        homePageCards.setOpaque(false);

        IncomeCard.setBackground(new java.awt.Color(51, 51, 51));

        incomeTitle.setText("Total Income");

        incomeAmount.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        incomeAmount.setText("Rs. 0.00");

        javax.swing.GroupLayout IncomeCardLayout = new javax.swing.GroupLayout(IncomeCard);
        IncomeCard.setLayout(IncomeCardLayout);
        IncomeCardLayout.setHorizontalGroup(
            IncomeCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(IncomeCardLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(incomeTitle)
                .addContainerGap(64, Short.MAX_VALUE))
            .addComponent(incomeAmount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        IncomeCardLayout.setVerticalGroup(
            IncomeCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(IncomeCardLayout.createSequentialGroup()
                .addComponent(incomeTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 62, Short.MAX_VALUE)
                .addComponent(incomeAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        ExpenseCard.setBackground(new java.awt.Color(102, 102, 102));

        expenseTitle.setText("Total Expense");

        expenseAmount.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        expenseAmount.setText("Rs. 0.00");

        javax.swing.GroupLayout ExpenseCardLayout = new javax.swing.GroupLayout(ExpenseCard);
        ExpenseCard.setLayout(ExpenseCardLayout);
        ExpenseCardLayout.setHorizontalGroup(
            ExpenseCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ExpenseCardLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expenseTitle)
                .addContainerGap(69, Short.MAX_VALUE))
            .addComponent(expenseAmount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        ExpenseCardLayout.setVerticalGroup(
            ExpenseCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ExpenseCardLayout.createSequentialGroup()
                .addComponent(expenseTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                .addComponent(expenseAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        RemainCard.setBackground(new java.awt.Color(153, 153, 153));

        remainTitle.setForeground(new java.awt.Color(0, 0, 0));
        remainTitle.setText("Remaining Balance");

        remainAmount.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        remainAmount.setForeground(new java.awt.Color(0, 0, 0));
        remainAmount.setText("Rs. 0.00");

        javax.swing.GroupLayout RemainCardLayout = new javax.swing.GroupLayout(RemainCard);
        RemainCard.setLayout(RemainCardLayout);
        RemainCardLayout.setHorizontalGroup(
            RemainCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RemainCardLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(remainTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(RemainCardLayout.createSequentialGroup()
                .addComponent(remainAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        RemainCardLayout.setVerticalGroup(
            RemainCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RemainCardLayout.createSequentialGroup()
                .addComponent(remainTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addComponent(remainAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout homePageCardsLayout = new javax.swing.GroupLayout(homePageCards);
        homePageCards.setLayout(homePageCardsLayout);
        homePageCardsLayout.setHorizontalGroup(
            homePageCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(homePageCardsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(IncomeCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ExpenseCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(RemainCard, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(11, Short.MAX_VALUE))
        );
        homePageCardsLayout.setVerticalGroup(
            homePageCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(homePageCardsLayout.createSequentialGroup()
                .addGroup(homePageCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(IncomeCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ExpenseCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(RemainCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        getContentPane().add(homePageCards, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 90, 470, 120));

        profileName.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        profileName.setForeground(new java.awt.Color(255, 255, 255));
        profileName.setText("Meer Musabih Saleem");
        getContentPane().add(profileName, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 20, -1, -1));

        logo.setFont(new java.awt.Font("Imprint MT Shadow", 1, 48)); // NOI18N
        logo.setForeground(new java.awt.Color(255, 255, 255));
        logo.setText("SpendWise ");
        getContentPane().add(logo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, -1, -1));

        RecentTransactions.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(RecentTransactions);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 240, 220, 330));

        placeHolder.setText("Placeholder for Chart");

        javax.swing.GroupLayout chartsPanelLayout = new javax.swing.GroupLayout(chartsPanel);
        chartsPanel.setLayout(chartsPanelLayout);
        chartsPanelLayout.setHorizontalGroup(
            chartsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, chartsPanelLayout.createSequentialGroup()
                .addContainerGap(43, Short.MAX_VALUE)
                .addComponent(placeHolder)
                .addGap(75, 75, 75))
        );
        chartsPanelLayout.setVerticalGroup(
            chartsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chartsPanelLayout.createSequentialGroup()
                .addGap(64, 64, 64)
                .addComponent(placeHolder)
                .addContainerGap(150, Short.MAX_VALUE))
        );

        getContentPane().add(chartsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 240, 230, 230));

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1015, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addExpenseFABActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpenseFABActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addExpenseFABActionPerformed

    private void addIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIncomeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addIncomeActionPerformed

    private void addExpenseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpenseActionPerformed
        // TODO add your handling code here:
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
        java.awt.EventQueue.invokeLater(() -> {
             // FOR TESTING: Pass dummy ID and Username
             new HomePage("TestUser", "TestUser").setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ExpenseCard;
    private javax.swing.JPanel IncomeCard;
    private javax.swing.JTable RecentTransactions;
    private javax.swing.JPanel RemainCard;
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addExpenseFAB;
    private javax.swing.JButton addIncome;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JButton charts;
    private javax.swing.JPanel chartsPanel;
    private javax.swing.JLabel expenseAmount;
    private javax.swing.JLabel expenseTitle;
    private javax.swing.JPanel homePageCards;
    private javax.swing.JLabel incomeAmount;
    private javax.swing.JLabel incomeTitle;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JLabel placeHolder;
    private javax.swing.JLabel profileName;
    private javax.swing.JLabel remainAmount;
    private javax.swing.JLabel remainTitle;
    private javax.swing.JPanel sidebar;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
