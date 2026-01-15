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
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.*;

/**
 *
 * @author sirh9
 */
public class Charts extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Charts.class.getName());
    private String currentUserId;
    private String currentUserName; 
    
    private DefaultCategoryDataset barChartDataset;
    private DefaultPieDataset pieChartDataset;
    private TimeSeriesCollection lineChartDataset;
    /**
     * Creates new form Charts
     * @param userId 
     * @param userName 
     */
   public Charts(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initComponents();
        
    
        setLocationRelativeTo(null); 
        applyThemeColors();
        setupNavigationListeners();
        
        profileName.setText("Welcome, " + userName);
        
    
        loadChartData();
        renderCharts();
    }
    
    
    public Charts() {
        initComponents();
        setLocationRelativeTo(null);
        applyThemeColors();
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
        
    
        charts.setBackground(ACCENT_COLOR);
        
    
        heading.setForeground(Color.BLACK);
        
        Color CHART_BG = new Color(255, 255, 255, 200); 
        barChartPanel.setBackground(CHART_BG);
        lineChartPanel.setBackground(CHART_BG);
        pieChartPanel.setBackground(CHART_BG);

    
        barChartPlaceholder.setVisible(false);
        lineChartPlaceholder.setVisible(false);
        pieChartPlaceholder.setVisible(false);
        
    
        barChartPanel.setLayout(new java.awt.BorderLayout());
        lineChartPanel.setLayout(new java.awt.BorderLayout());
        pieChartPanel.setLayout(new java.awt.BorderLayout());
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
        monthlyReport.addActionListener(e -> navigateToScreen(new MonthlyReport(currentUserId, currentUserName))); 
        charts.addActionListener(e -> {});
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
    
    private void loadChartData() {
        if (currentUserId == null || currentUserId.isEmpty()) return;

        barChartDataset = new DefaultCategoryDataset();
        pieChartDataset = new DefaultPieDataset();
        lineChartDataset = new TimeSeriesCollection();
        
        SwingWorker<List<QueryDocumentSnapshot>, Void> worker = new SwingWorker<List<QueryDocumentSnapshot>, Void>() {
            @Override
            protected List<QueryDocumentSnapshot> doInBackground() throws Exception {
                Firestore db = FirebaseService.getFirestore();
                ApiFuture<QuerySnapshot> future = db.collection("transactions")
                        .whereEqualTo("user_id", currentUserId)
                        .get();
                return future.get().getDocuments();
            }

            @Override
            protected void done() {
                try {
                    List<QueryDocumentSnapshot> docs = get();
                    
                    // Maps for aggregation
                    Map<String, Double> expensesByCategory = new HashMap<>();
                    Map<String, Map<String, Double>> monthTypeTotal = new TreeMap<>();
                    
                    // Sort docs by date for Line Chart
                    List<QueryDocumentSnapshot> sortedDocs = new ArrayList<>(docs);
                    sortedDocs.sort((d1, d2) -> {
                        String date1 = d1.getString("transaction_date");
                        String date2 = d2.getString("transaction_date");
                        if (date1 == null) return -1;
                        if (date2 == null) return 1;
                        return date1.compareTo(date2);
                    });
                    
                    TimeSeries balanceSeries = new TimeSeries("Net Balance");
                    double runningBalance = 0.0;
                    
                    LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
                    String sixMonthsAgoStr = sixMonthsAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                    for (QueryDocumentSnapshot doc : sortedDocs) {
                        String tDate = doc.getString("transaction_date");
                        String type = doc.getString("type");
                        double amount = 0.0;
                        if (doc.contains("amount")) amount = doc.getDouble("amount");
                        
                        // Pie Chart Data
                        if ("Expense".equalsIgnoreCase(type)) {
                            String cat = doc.getString("category");
                            if (cat != null) {
                                expensesByCategory.put(cat, expensesByCategory.getOrDefault(cat, 0.0) + amount);
                            }
                        }
                        
                        // Bar Chart Data (last 6 months)
                        if (tDate != null && tDate.compareTo(sixMonthsAgoStr) >= 0) {
                            String monthKey = tDate.substring(0, 7); // yyyy-MM
                            monthTypeTotal.putIfAbsent(monthKey, new HashMap<>());
                            monthTypeTotal.get(monthKey).put(type, monthTypeTotal.get(monthKey).getOrDefault(type, 0.0) + amount);
                        }
                        
                        // Line Chart Data
                        if ("Income".equalsIgnoreCase(type)) {
                            runningBalance += amount;
                        } else {
                            runningBalance -= amount;
                        }
                        if (tDate != null) {
                            try {
                                LocalDate ld = LocalDate.parse(tDate);
                                balanceSeries.addOrUpdate(new Day(ld.getDayOfMonth(), ld.getMonthValue(), ld.getYear()), runningBalance);
                            } catch (Exception e) {}
                        }
                    }
                    
                    // Populate Pie
                    expensesByCategory.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(5)
                        .forEach(e -> pieChartDataset.setValue(e.getKey(), e.getValue()));
                        
                    // Populate Bar
                    for (Map.Entry<String, Map<String, Double>> entry : monthTypeTotal.entrySet()) {
                        String monthKey = entry.getKey();
                        try {
                            LocalDate ld = LocalDate.parse(monthKey + "-01");
                            String displayMonth = ld.format(DateTimeFormatter.ofPattern("MMM ''yy"));
                            
                            Double inc = entry.getValue().getOrDefault("Income", 0.0);
                            Double exp = entry.getValue().getOrDefault("Expense", 0.0);
                            
                            barChartDataset.addValue(inc, "Income", displayMonth);
                            barChartDataset.addValue(exp, "Expense", displayMonth);
                        } catch (Exception e) {}
                    }
                    
                    // Populate Line
                    lineChartDataset.addSeries(balanceSeries);
                    
                    renderCharts();
                    
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error loading charts.", ex);
                }
            }
        };
        worker.execute();
    }

    private void renderCharts() {

        JFreeChart pieChart = ChartFactory.createPieChart(
            "Top Expense Categories", 
            pieChartDataset, 
            true, // legend
            true, // tooltips
            false // urls
        );
        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setSectionOutlinesVisible(false);
        piePlot.setLabelBackgroundPaint(Color.WHITE);
        
        ChartPanel pieChartComponent = new ChartPanel(pieChart);
        pieChartPanel.removeAll();
        pieChartPanel.add(pieChartComponent, java.awt.BorderLayout.CENTER);
        
      
        JFreeChart barChart = ChartFactory.createBarChart(
            "Last 6 Months Income vs. Expense", 
            "Month", 
            "Amount (Rs)", 
            barChartDataset, 
            PlotOrientation.VERTICAL, 
            true, // legend
            true, // tooltips
            false // urls
        );
        CategoryPlot barPlot = barChart.getCategoryPlot();
        barPlot.setBackgroundPaint(Color.lightGray);
        
        BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
        renderer.setSeriesPaint(0, new Color(0, 153, 51)); // Income (Green)
        renderer.setSeriesPaint(1, new Color(204, 0, 51)); // Expense (Red)
        
        ChartPanel barChartComponent = new ChartPanel(barChart);
        barChartPanel.removeAll();
        barChartPanel.add(barChartComponent, java.awt.BorderLayout.CENTER);

       
        JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
            "Cumulative Net Balance Trend", 
            "Date", 
            "Net Balance (Rs)", 
            lineChartDataset, 
            true, // legend
            true, // tooltips
            false // urls
        );
        XYPlot linePlot = lineChart.getXYPlot();
        linePlot.setBackgroundPaint(Color.lightGray);
        linePlot.setDomainGridlinesVisible(true);
        linePlot.setRangeGridlinePaint(Color.BLACK);
        
        ChartPanel lineChartComponent = new ChartPanel(lineChart);
        lineChartPanel.removeAll();
        lineChartPanel.add(lineChartComponent, java.awt.BorderLayout.CENTER);

       
        pieChartPanel.revalidate();
        pieChartPanel.repaint();
        barChartPanel.revalidate();
        barChartPanel.repaint();
        lineChartPanel.revalidate();
        lineChartPanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        heading = new javax.swing.JLabel();
        profileName = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
        barChartPanel = new javax.swing.JPanel();
        barChartPlaceholder = new javax.swing.JLabel();
        lineChartPanel = new javax.swing.JPanel();
        lineChartPlaceholder = new javax.swing.JLabel();
        pieChartPanel = new javax.swing.JPanel();
        pieChartPlaceholder = new javax.swing.JLabel();
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

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Charts");
        getContentPane().add(heading, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 90, -1, -1));

        profileName.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        profileName.setForeground(new java.awt.Color(255, 255, 255));
        profileName.setText("Meer Musabih Saleem");
        getContentPane().add(profileName, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 20, -1, -1));

        logo.setFont(new java.awt.Font("Imprint MT Shadow", 1, 48)); // NOI18N
        logo.setForeground(new java.awt.Color(255, 255, 255));
        logo.setText("SpendWise ");
        getContentPane().add(logo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, -1, -1));

        barChartPlaceholder.setText("bar chart for monthly");

        javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
        barChartPanel.setLayout(barChartPanelLayout);
        barChartPanelLayout.setHorizontalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, barChartPanelLayout.createSequentialGroup()
                .addContainerGap(58, Short.MAX_VALUE)
                .addComponent(barChartPlaceholder)
                .addGap(79, 79, 79))
        );
        barChartPanelLayout.setVerticalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(barChartPanelLayout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(barChartPlaceholder)
                .addContainerGap(147, Short.MAX_VALUE))
        );

        getContentPane().add(barChartPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 140, 250, 230));

        lineChartPlaceholder.setText("line chart for balance");

        javax.swing.GroupLayout lineChartPanelLayout = new javax.swing.GroupLayout(lineChartPanel);
        lineChartPanel.setLayout(lineChartPanelLayout);
        lineChartPanelLayout.setHorizontalGroup(
            lineChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lineChartPanelLayout.createSequentialGroup()
                .addContainerGap(201, Short.MAX_VALUE)
                .addComponent(lineChartPlaceholder)
                .addGap(198, 198, 198))
        );
        lineChartPanelLayout.setVerticalGroup(
            lineChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lineChartPanelLayout.createSequentialGroup()
                .addContainerGap(82, Short.MAX_VALUE)
                .addComponent(lineChartPlaceholder)
                .addGap(82, 82, 82))
        );

        getContentPane().add(lineChartPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 400, 510, 180));

        pieChartPlaceholder.setText("pie chart for category");

        javax.swing.GroupLayout pieChartPanelLayout = new javax.swing.GroupLayout(pieChartPanel);
        pieChartPanel.setLayout(pieChartPanelLayout);
        pieChartPanelLayout.setHorizontalGroup(
            pieChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pieChartPanelLayout.createSequentialGroup()
                .addContainerGap(31, Short.MAX_VALUE)
                .addComponent(pieChartPlaceholder)
                .addGap(96, 96, 96))
        );
        pieChartPanelLayout.setVerticalGroup(
            pieChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pieChartPanelLayout.createSequentialGroup()
                .addGap(76, 76, 76)
                .addComponent(pieChartPlaceholder)
                .addContainerGap(138, Short.MAX_VALUE))
        );

        getContentPane().add(pieChartPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 140, 240, 230));

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
        transactions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transactionsActionPerformed(evt);
            }
        });

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

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1015, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIncomeActionPerformed
        navigateToScreen(new AddIncome(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addIncomeActionPerformed

    private void addExpenseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpenseActionPerformed
        navigateToScreen(new AddExpense(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addExpenseActionPerformed

    private void transactionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transactionsActionPerformed
      // TODO add your handling code here:
    }//GEN-LAST:event_transactionsActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Charts("testUser123", "TestUser").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JPanel barChartPanel;
    private javax.swing.JLabel barChartPlaceholder;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JButton charts;
    private javax.swing.JLabel heading;
    private javax.swing.JPanel lineChartPanel;
    private javax.swing.JLabel lineChartPlaceholder;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JPanel pieChartPanel;
    private javax.swing.JLabel pieChartPlaceholder;
    private javax.swing.JLabel profileName;
    private javax.swing.JPanel sidebar;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
