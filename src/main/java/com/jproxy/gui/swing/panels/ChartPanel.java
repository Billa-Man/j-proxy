package com.jproxy.gui.swing.panels;

import com.jproxy.core.MetricsCollector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChartPanel extends JPanel {
    
    private List<Long> dataPoints;
    private int maxDataPoints = 300;
    private long lastRequestCount = 0;
    
    public ChartPanel() {
        dataPoints = new ArrayList<>();
        initUI();
    }
    
    private void initUI() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        setPreferredSize(new Dimension(0, 250));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int padding = 40;
        
        // Title
        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2d.setColor(new Color(44, 62, 80));
        g2d.drawString("Request Activity", padding, 25);
        
        if (dataPoints.isEmpty()) {
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(Color.GRAY);
            g2d.drawString("No data yet...", width / 2 - 40, height / 2);
            return;
        }
        
        // Draw axes
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawLine(padding, height - padding, width - padding, height - padding); // X-axis
        g2d.drawLine(padding, padding, padding, height - padding); // Y-axis
        
        // Draw data
        long maxValue = dataPoints.stream().max(Long::compare).orElse(1L);
        if (maxValue == 0) maxValue = 1;
        
        g2d.setColor(new Color(52, 152, 219));
        g2d.setStroke(new BasicStroke(2));
        
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding - 20;
        
        for (int i = 1; i < dataPoints.size(); i++) {
            int x1 = padding + (int) ((double) (i - 1) / maxDataPoints * chartWidth);
            int y1 = height - padding - (int) ((double) dataPoints.get(i - 1) / maxValue * chartHeight);
            int x2 = padding + (int) ((double) i / maxDataPoints * chartWidth);
            int y2 = height - padding - (int) ((double) dataPoints.get(i) / maxValue * chartHeight);
            
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        // Draw labels
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.setColor(Color.GRAY);
        g2d.drawString("Time (seconds)", width / 2 - 40, height - 10);
        g2d.drawString("Total Requests", 5, padding + 10);
    }
    
    public void updateChart(MetricsCollector metrics) {
        if (metrics == null) {
            return;
        }
        
        long currentCount = metrics.getTotalRequests();
        dataPoints.add(currentCount);
        
        if (dataPoints.size() > maxDataPoints) {
            dataPoints.remove(0);
        }
        
        repaint();
    }
    
    public void clear() {
        dataPoints.clear();
        lastRequestCount = 0;
        repaint();
    }
}