package com.jproxy.gui.swing.panels;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel {
    
    private JLabel statusLabel;
    private JPanel statusIndicator;
    
    public StatusPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(44, 62, 80));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Title
        JLabel titleLabel = new JLabel("J-Proxy Dashboard");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.WEST);
        
        // Status indicator
        JPanel statusContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statusContainer.setOpaque(false);
        
        statusIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.fillOval(0, 0, getWidth(), getHeight());
            }
        };
        statusIndicator.setPreferredSize(new Dimension(16, 16));
        statusIndicator.setBackground(Color.RED);
        
        statusLabel = new JLabel("Stopped");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);
        
        statusContainer.add(statusIndicator);
        statusContainer.add(statusLabel);
        
        add(statusContainer, BorderLayout.EAST);
    }
    
    public void setRunning(boolean running) {
        if (running) {
            statusIndicator.setBackground(new Color(46, 204, 113));
            statusLabel.setText("Running");
        } else {
            statusIndicator.setBackground(new Color(231, 76, 60));
            statusLabel.setText("Stopped");
        }
        repaint();
    }
}