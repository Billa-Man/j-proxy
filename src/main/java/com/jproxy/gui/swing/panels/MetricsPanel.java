package com.jproxy.gui.swing.panels;

import com.jproxy.core.MetricsCollector;

import javax.swing.*;
import java.awt.*;

public class MetricsPanel extends JPanel {
    
    private JLabel totalRequestsLabel;
    private JLabel successRateLabel;
    private JLabel avgResponseLabel;
    private JLabel uptimeLabel;
    
    public MetricsPanel() {
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Title
        JLabel titleLabel = new JLabel("Request Metrics");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(44, 62, 80));
        add(titleLabel, BorderLayout.NORTH);
        
        // Metrics grid
        JPanel metricsGrid = new JPanel(new GridLayout(4, 2, 10, 10));
        metricsGrid.setOpaque(false);
        
        // Total Requests
        metricsGrid.add(createMetricLabel("Total Requests:"));
        totalRequestsLabel = createValueLabel("0");
        metricsGrid.add(totalRequestsLabel);
        
        // Success Rate
        metricsGrid.add(createMetricLabel("Success Rate:"));
        successRateLabel = createValueLabel("0%");
        metricsGrid.add(successRateLabel);
        
        // Avg Response
        metricsGrid.add(createMetricLabel("Avg Response:"));
        avgResponseLabel = createValueLabel("0 ms");
        metricsGrid.add(avgResponseLabel);
        
        // Uptime
        metricsGrid.add(createMetricLabel("Uptime:"));
        uptimeLabel = createValueLabel("00:00:00");
        metricsGrid.add(uptimeLabel);
        
        add(metricsGrid, BorderLayout.CENTER);
    }
    
    private JLabel createMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(127, 140, 141));
        return label;
    }
    
    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(new Color(44, 62, 80));
        return label;
    }
    
    public void updateMetrics(MetricsCollector metrics) {
        if (metrics == null) {
            totalRequestsLabel.setText("0");
            successRateLabel.setText("0%");
            avgResponseLabel.setText("0 ms");
            uptimeLabel.setText("00:00:00");
            return;
        }
        
        totalRequestsLabel.setText(String.valueOf(metrics.getTotalRequests()));
        successRateLabel.setText(String.format("%.1f%%", metrics.getSuccessRate()));
        avgResponseLabel.setText(String.format("%.0f ms", metrics.getAverageResponseTime()));
        
        long uptime = metrics.getUptimeSeconds();
        uptimeLabel.setText(formatUptime(uptime));
    }
    
    private String formatUptime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}