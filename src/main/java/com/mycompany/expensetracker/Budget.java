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
import java.util.logging.Level;
import javax.swing.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 *
 * @author sirh9
 */
public class Budget extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Budget.class.getName());
    private String currentUserId;
    private String currentUserName; 
    
    private final YearMonth currentMonth = YearMonth.now();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
    private final String currentMonthYear = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    
    /**
     * Creates new form Budget
     * @param userId
     * @param userName
     */
 public Budget(String userId, String userName) {
        this.currentUserId = userId;
        this.currentUserName = userName;
        initComponents();
        
      
        setLocationRelativeTo(null); 
        applyThemeColors();
        setupNavigationListeners();
        
        profileName.setText("Welcome, " + userName);
        
        loadBudgets();
    }
    
    public Budget() {
        initComponents();
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
        addBudgetButton.setForeground(LIGHT_TEXT);
        
        budgetsPanel.setOpaque(false);
        budgetCards.setOpaque(false);
        jScrollPane1.setOpaque(false);
        jScrollPane1.getViewport().setOpaque(false);
        
        budgetCards.removeAll();

        budgetCards.setLayout(new BoxLayout(budgetCards, BoxLayout.Y_AXIS)); 

        heading.setText(currentMonth.format(monthFormatter) + " Budgets");
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
        charts.addActionListener(e -> navigateToScreen(new Charts(currentUserId, currentUserName))); 
        budget.addActionListener(e -> {});
        addBudgetButton.addActionListener(e -> navigateToScreen(new AddBudget(currentUserId,currentUserName)));
    }

    private void navigateToHome() {
        this.dispose();
        new HomePage(currentUserId, currentUserName).setVisible(true);
    }

    private void navigateToScreen(JFrame targetScreen) {
        this.dispose();
        targetScreen.setVisible(true);
    }
    
    private void editBudget(String category, double targetAmount) {
        this.dispose();
        new AddBudget(currentUserId, currentUserName, category, targetAmount).setVisible(true);
    }
    
    private void deleteBudget(String category) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the budget for \"" + category + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Firestore db = FirebaseService.getFirestore();
                    String docId = currentUserId + "_" + currentMonthYear + "_" + category;
                    db.collection("budgets").document(docId).delete().get();
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(Budget.this,
                            "Budget for \"" + category + "\" deleted successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        loadBudgets(); // Refresh the display
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error deleting budget", ex);
                        JOptionPane.showMessageDialog(Budget.this,
                            "Error deleting budget: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void loadBudgets() {
        budgetCards.removeAll();
        if (currentUserId == null) return;

        SwingWorker<Void, JPanel> worker = new SwingWorker<Void, JPanel>() {
            @Override
            protected Void doInBackground() throws Exception {
                Firestore db = FirebaseService.getFirestore();
                
                // 1. Fetch Budgets
                ApiFuture<QuerySnapshot> budgetFuture = db.collection("budgets")
                        .whereEqualTo("user_id", currentUserId)
                        .whereEqualTo("month_year", currentMonthYear)
                        .get();
                List<QueryDocumentSnapshot> budgetDocs = budgetFuture.get().getDocuments();
                
                Map<String, Double> budgetTargets = new HashMap<>();
                for (QueryDocumentSnapshot doc : budgetDocs) {
                    budgetTargets.put(doc.getString("category"), doc.getDouble("target_amount"));
                }
                
                // 2. Fetch Transactions for Spending
                ApiFuture<QuerySnapshot> transFuture = db.collection("transactions")
                        .whereEqualTo("user_id", currentUserId)
                        .get();
                List<QueryDocumentSnapshot> transDocs = transFuture.get().getDocuments();
                
                Map<String, Double> currentSpending = new HashMap<>();
                String monthPrefix = currentMonthYear; // yyyy-MM
                
                for (QueryDocumentSnapshot doc : transDocs) {
                    String tDate = doc.getString("transaction_date");
                    String type = doc.getString("type");
                    // We need 'Expense' only
                    if (tDate != null && tDate.startsWith(monthPrefix) && "Expense".equalsIgnoreCase(type)) {
                         String cat = doc.getString("category");
                         double amount = 0.0;
                         if (doc.contains("amount")) amount = doc.getDouble("amount");
                         
                         if (cat != null) {
                             currentSpending.put(cat, currentSpending.getOrDefault(cat, 0.0) + amount);
                         }
                    }
                }
                
                // 3. Iterate through ALL expense categories from CategoryModel
                for (CategoryModel category : CategoryModel.getExpenseCategories()) {
                    String categoryName = category.getName();
                    double target = budgetTargets.getOrDefault(categoryName, 0.0);
                    double spent = currentSpending.getOrDefault(categoryName, 0.0);
                    publish(createBudgetCard(categoryName, target, spent));
                }
                
                return null;
            }

            @Override
            protected void process(List<JPanel> chunks) {
                for (JPanel card : chunks) {
                    budgetCards.add(card);
                    budgetCards.add(Box.createVerticalStrut(10));
                }
            }

            @Override
            protected void done() {
                if (budgetCards.getComponentCount() == 0) {
                     budgetCards.add(new JLabel("No categories available. Please check CategoryModel."));
                }
                budgetCards.revalidate();
                budgetCards.repaint();
            }
        };
        worker.execute();
    }
    
    private void goToAddBudget(){
        navigateToScreen(new AddBudget(currentUserId,currentUserName));
    }
     
    private JPanel createBudgetCard(String category, double targetAmount, double spentAmount) {

    JPanel card = new JPanel(null);
    card.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(100, 100, 100)));
    card.setBackground(new Color(40, 40, 40, 180));

    // ★ FULL WIDTH FIX (DO NOT REMOVE)
    card.setPreferredSize(new Dimension(Integer.MAX_VALUE, 130));
    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
    card.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel categoryLabel = new JLabel(category);
    categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
    categoryLabel.setForeground(Color.WHITE);
    categoryLabel.setBounds(15, 10, 250, 25);
    card.add(categoryLabel);

    JLabel targetLabel = new JLabel("Target:");
    targetLabel.setForeground(new Color(200, 200, 200));
    targetLabel.setBounds(15, 45, 60, 20);
    card.add(targetLabel);

    JLabel targetAmountLabel;
    if (targetAmount > 0) {
        targetAmountLabel = new JLabel(String.format("Rs %,.2f", targetAmount));
        targetAmountLabel.setForeground(new Color(0, 255, 255));
    } else {
        targetAmountLabel = new JLabel("No budget set");
        targetAmountLabel.setForeground(new Color(150, 150, 150));
    }
    targetAmountLabel.setBounds(75, 45, 150, 20);
    card.add(targetAmountLabel);

    JLabel spentLabel = new JLabel("Spent:");
    spentLabel.setForeground(new Color(200, 200, 200));
    spentLabel.setBounds(240, 45, 60, 20);
    card.add(spentLabel);

    JLabel spentAmountLabel = new JLabel(String.format("Rs %,.2f", spentAmount));
    spentAmountLabel.setBounds(295, 45, 150, 20);
    spentAmountLabel.setForeground(
        targetAmount > 0 && spentAmount > targetAmount
            ? new Color(255, 100, 100)
            : new Color(100, 255, 100)
    );
    card.add(spentAmountLabel);

    JProgressBar progressBar = new JProgressBar(0, 100);
    int progressPercent = targetAmount > 0
            ? (int) Math.min(100, (spentAmount / targetAmount) * 100)
            : 0;

    progressBar.setValue(progressPercent);
    progressBar.setStringPainted(true);
    progressBar.setBounds(15, 75, 900, 20); // wide enough for full panel
    card.add(progressBar);

    if (targetAmount > 0) {
        JButton editButton = new JButton("Edit");
        editButton.setBounds(700, 103, 60, 22);
        editButton.addActionListener(e -> editBudget(category, targetAmount));
        card.add(editButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.setBounds(770, 103, 70, 22);
        deleteButton.setBackground(new Color(220, 50, 50));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> deleteBudget(category));
        card.add(deleteButton);
    }

    return card;
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        budgetsPanel = new javax.swing.JPanel();
        addBudgetButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        budgetCards = new javax.swing.JPanel();
        card1 = new javax.swing.JPanel();
        card1Label = new javax.swing.JLabel();
        card1targetLabel = new javax.swing.JLabel();
        card1targetAmount = new javax.swing.JTextField();
        card1spentLabel = new javax.swing.JLabel();
        card1spentAmount = new javax.swing.JTextField();
        card1progress = new javax.swing.JProgressBar();
        card2 = new javax.swing.JPanel();
        card2Label = new javax.swing.JLabel();
        card2targetLabel = new javax.swing.JLabel();
        card2targetAmount = new javax.swing.JTextField();
        card2spentLabel = new javax.swing.JLabel();
        card2spentAmount = new javax.swing.JTextField();
        card2progress = new javax.swing.JProgressBar();
        card3 = new javax.swing.JPanel();
        card3Label = new javax.swing.JLabel();
        card3targetLabel = new javax.swing.JLabel();
        card3targetAmount = new javax.swing.JTextField();
        card3spentLabel = new javax.swing.JLabel();
        card3spentAmount = new javax.swing.JTextField();
        card3progress = new javax.swing.JProgressBar();
        heading = new javax.swing.JLabel();
        logo = new javax.swing.JLabel();
        profileName = new javax.swing.JLabel();
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

        budgetsPanel.setOpaque(false);

        addBudgetButton.setBackground(new java.awt.Color(0, 153, 255));
        addBudgetButton.setText("Add");
        addBudgetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBudgetButtonActionPerformed(evt);
            }
        });

        jScrollPane1.setOpaque(false);

        budgetCards.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 255, 255)));
        budgetCards.setOpaque(false);

        card1.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 255, 255)));
        card1.setOpaque(false);

        card1Label.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        card1Label.setForeground(new java.awt.Color(255, 255, 255));
        card1Label.setText("Entertainment");

        card1targetLabel.setText("Target:");

        card1targetAmount.setText("0.00");
        card1targetAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                card1targetAmountActionPerformed(evt);
            }
        });

        card1spentLabel.setText("Spent:");

        card1spentAmount.setText("0.00");
        card1spentAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                card1spentAmountActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout card1Layout = new javax.swing.GroupLayout(card1);
        card1.setLayout(card1Layout);
        card1Layout.setHorizontalGroup(
            card1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(card1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(card1Label)
                    .addGroup(card1Layout.createSequentialGroup()
                        .addComponent(card1targetLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(card1targetAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(card1spentLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(card1spentAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(card1Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(card1progress, javax.swing.GroupLayout.PREFERRED_SIZE, 407, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        card1Layout.setVerticalGroup(
            card1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(card1Label)
                .addGap(28, 28, 28)
                .addGroup(card1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(card1targetLabel)
                    .addComponent(card1targetAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(card1spentLabel)
                    .addComponent(card1spentAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(card1progress, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        card2.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 255, 255)));
        card2.setOpaque(false);

        card2Label.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        card2Label.setForeground(new java.awt.Color(255, 255, 255));
        card2Label.setText("Groceries");

        card2targetLabel.setText("Target:");

        card2targetAmount.setText("0.00");
        card2targetAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                card2targetAmountActionPerformed(evt);
            }
        });

        card2spentLabel.setText("Spent:");

        card2spentAmount.setText("0.00");
        card2spentAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                card2spentAmountActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout card2Layout = new javax.swing.GroupLayout(card2);
        card2.setLayout(card2Layout);
        card2Layout.setHorizontalGroup(
            card2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(card2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(card2Label)
                    .addGroup(card2Layout.createSequentialGroup()
                        .addComponent(card2targetLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(card2targetAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(card2spentLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(card2spentAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(card2Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(card2progress, javax.swing.GroupLayout.PREFERRED_SIZE, 407, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        card2Layout.setVerticalGroup(
            card2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(card2Label)
                .addGap(28, 28, 28)
                .addGroup(card2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(card2targetLabel)
                    .addComponent(card2targetAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(card2spentLabel)
                    .addComponent(card2spentAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(card2progress, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        card3.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 255, 255)));
        card3.setOpaque(false);

        card3Label.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        card3Label.setForeground(new java.awt.Color(255, 255, 255));
        card3Label.setText("Dining Out");

        card3targetLabel.setText("Target:");

        card3targetAmount.setText("0.00");
        card3targetAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                card3targetAmountActionPerformed(evt);
            }
        });

        card3spentLabel.setText("Spent:");

        card3spentAmount.setText("0.00");
        card3spentAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                card3spentAmountActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout card3Layout = new javax.swing.GroupLayout(card3);
        card3.setLayout(card3Layout);
        card3Layout.setHorizontalGroup(
            card3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(card3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(card3Label)
                    .addGroup(card3Layout.createSequentialGroup()
                        .addComponent(card3targetLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(card3targetAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(card3spentLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(card3spentAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(card3Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(card3progress, javax.swing.GroupLayout.PREFERRED_SIZE, 407, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        card3Layout.setVerticalGroup(
            card3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(card3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(card3Label)
                .addGap(28, 28, 28)
                .addGroup(card3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(card3targetLabel)
                    .addComponent(card3targetAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(card3spentLabel)
                    .addComponent(card3spentAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(card3progress, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout budgetCardsLayout = new javax.swing.GroupLayout(budgetCards);
        budgetCards.setLayout(budgetCardsLayout);
        budgetCardsLayout.setHorizontalGroup(
            budgetCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(card1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(card3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(card2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        budgetCardsLayout.setVerticalGroup(
            budgetCardsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(budgetCardsLayout.createSequentialGroup()
                .addComponent(card1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(card3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(card2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 37, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(budgetCards);

        heading.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        heading.setText("Budgets");

        javax.swing.GroupLayout budgetsPanelLayout = new javax.swing.GroupLayout(budgetsPanel);
        budgetsPanel.setLayout(budgetsPanelLayout);
        budgetsPanelLayout.setHorizontalGroup(
            budgetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(budgetsPanelLayout.createSequentialGroup()
                .addContainerGap(206, Short.MAX_VALUE)
                .addComponent(heading)
                .addGap(93, 93, 93)
                .addComponent(addBudgetButton)
                .addContainerGap())
            .addGroup(budgetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE))
        );
        budgetsPanelLayout.setVerticalGroup(
            budgetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(budgetsPanelLayout.createSequentialGroup()
                .addGroup(budgetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(budgetsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(heading))
                    .addGroup(budgetsPanelLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(addBudgetButton)))
                .addContainerGap(411, Short.MAX_VALUE))
            .addGroup(budgetsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, budgetsPanelLayout.createSequentialGroup()
                    .addContainerGap(52, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 402, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        getContentPane().add(budgetsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 80, 470, 460));

        logo.setFont(new java.awt.Font("Imprint MT Shadow", 1, 48)); // NOI18N
        logo.setForeground(new java.awt.Color(255, 255, 255));
        logo.setText("SpendWise ");
        getContentPane().add(logo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, -1, -1));

        profileName.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        profileName.setForeground(new java.awt.Color(255, 255, 255));
        profileName.setText("Meer Musabih Saleem");
        getContentPane().add(profileName, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 40, -1, -1));

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

        bgImage.setIcon(new javax.swing.ImageIcon("C:\\Users\\sirh9\\Downloads\\nasa waly bohot khatarnak hn.png")); // NOI18N
        getContentPane().add(bgImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(-320, -80, 1015, 670));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addBudgetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBudgetButtonActionPerformed
       
        goToAddBudget();
        this.dispose();
    }//GEN-LAST:event_addBudgetButtonActionPerformed

    private void card1targetAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_card1targetAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_card1targetAmountActionPerformed

    private void card1spentAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_card1spentAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_card1spentAmountActionPerformed

    private void addIncomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIncomeActionPerformed
        navigateToScreen(new AddIncome(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addIncomeActionPerformed

    private void addExpenseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpenseActionPerformed
        navigateToScreen(new AddExpense(currentUserId, currentUserName));// TODO add your handling code here:
    }//GEN-LAST:event_addExpenseActionPerformed

    private void card2targetAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_card2targetAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_card2targetAmountActionPerformed

    private void card2spentAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_card2spentAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_card2spentAmountActionPerformed

    private void card3targetAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_card3targetAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_card3targetAmountActionPerformed

    private void card3spentAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_card3spentAmountActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_card3spentAmountActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Budget("testUser123", "TestUser").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addBudgetButton;
    private javax.swing.JButton addExpense;
    private javax.swing.JButton addIncome;
    private javax.swing.JLabel bgImage;
    private javax.swing.JButton budget;
    private javax.swing.JPanel budgetCards;
    private javax.swing.JPanel budgetsPanel;
    private javax.swing.JPanel card1;
    private javax.swing.JLabel card1Label;
    private javax.swing.JProgressBar card1progress;
    private javax.swing.JTextField card1spentAmount;
    private javax.swing.JLabel card1spentLabel;
    private javax.swing.JTextField card1targetAmount;
    private javax.swing.JLabel card1targetLabel;
    private javax.swing.JPanel card2;
    private javax.swing.JLabel card2Label;
    private javax.swing.JProgressBar card2progress;
    private javax.swing.JTextField card2spentAmount;
    private javax.swing.JLabel card2spentLabel;
    private javax.swing.JTextField card2targetAmount;
    private javax.swing.JLabel card2targetLabel;
    private javax.swing.JPanel card3;
    private javax.swing.JLabel card3Label;
    private javax.swing.JProgressBar card3progress;
    private javax.swing.JTextField card3spentAmount;
    private javax.swing.JLabel card3spentLabel;
    private javax.swing.JTextField card3targetAmount;
    private javax.swing.JLabel card3targetLabel;
    private javax.swing.JButton charts;
    private javax.swing.JLabel heading;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo;
    private javax.swing.JButton monthlyReport;
    private javax.swing.JLabel profileName;
    private javax.swing.JPanel sidebar;
    private javax.swing.JButton transactions;
    // End of variables declaration//GEN-END:variables
}
