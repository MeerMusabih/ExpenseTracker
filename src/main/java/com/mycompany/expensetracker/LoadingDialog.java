package com.mycompany.expensetracker;

import javax.swing.*;
import java.awt.*;

public class LoadingDialog extends JDialog {

    private Frame parentFrame;
    private static LoadingDialog currentDialog;

    private LoadingDialog(Frame parent, String message) {
        super(parent, "Please Wait", true);
        this.parentFrame = parent;
        initComponents(message);
    }

    private void initComponents(String message) {
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(0, 153, 255), 2));

        JLabel label = new JLabel(message);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.NORTH);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 20));
        panel.add(progressBar, BorderLayout.CENTER);

        add(panel);
        pack();
        setLocationRelativeTo(parentFrame);
    }

    public static void showLoading(Frame parent, String message) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = new LoadingDialog(parent, message);
        
        // Use a separate thread to show the dialog so it doesn't block
        SwingUtilities.invokeLater(() -> {
            if (currentDialog != null) {
                currentDialog.setVisible(true);
            }
        });
    }

    public static void hideLoading() {
        if (currentDialog != null) {
            currentDialog.setVisible(false);
            currentDialog.dispose();
            currentDialog = null;
        }
    }
}
