package com.mycompany.expensetracker;


import javax.swing.SwingUtilities;


public class ExpenseTracker {

   public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Firebase initialization is handled lazily by FirebaseService when needed.
            // We can directly launch the Login screen.
            try {
                 Login loginScreen = new Login();
                 loginScreen.setVisible(true);
            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(
                        null, 
                        "An error occurred launching the application: " + e.getMessage(),
                        "Launch Error", 
                        javax.swing.JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        });
    }
}
