package com.jproxy.gui.swing.panels;

import com.jproxy.core.Cache;
import com.jproxy.handler.ProxyHandler;

import javax.swing.*;
import java.awt.*;

public class CacheMetricsPanel extends JPanel {
    
    private JLabel cacheHitsLabel;
    private JLabel cacheMissesLabel;
    private JLabel hitRateLabel;
    private JLabel cacheSizeLabel;
    
    public CacheMetricsPanel() {
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
        JLabel titleLabel = new JLabel("Cache Metrics");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(44, 62, 80));
        add(titleLabel, BorderLayout.NORTH);
        
        // Metrics grid
        JPanel metricsGrid = new JPanel(new GridLayout(4, 2, 10, 10));
        metricsGrid.setOpaque(false);
        
        // Cache Hits
        metricsGrid.add(createMetricLabel("Cache Hits:"));
        cacheHitsLabel = createValueLabel("N/A");
        metricsGrid.add(cacheHitsLabel);
        
        // Cache Misses
        metricsGrid.add(createMetricLabel("Cache Misses:"));
        cacheMissesLabel = createValueLabel("N/A");
        metricsGrid.add(cacheMissesLabel);
        
        // Hit Rate
        metricsGrid.add(createMetricLabel("Hit Rate:"));
        hitRateLabel = createValueLabel("N/A");
        metricsGrid.add(hitRateLabel);
        
        // Cache Size
        metricsGrid.add(createMetricLabel("Cache Size:"));
        cacheSizeLabel = createValueLabel("N/A");
        metricsGrid.add(cacheSizeLabel);
        
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
    
    public void updateMetrics(ProxyHandler proxyHandler) {
        if (proxyHandler == null) {
            cacheHitsLabel.setText("N/A");
            cacheMissesLabel.setText("N/A");
            hitRateLabel.setText("N/A");
            cacheSizeLabel.setText("N/A");
            return;
        }
        
        Cache.CacheStats stats = proxyHandler.getCacheStats();
        if (stats != null) {
            cacheHitsLabel.setText(String.valueOf(stats.getHits()));
            cacheMissesLabel.setText(String.valueOf(stats.getMisses()));
            hitRateLabel.setText(String.format("%.1f%%", stats.getHitRate()));
            cacheSizeLabel.setText(String.format("%d/%d", stats.getSize(), stats.getMaxSize()));
        } else {
            cacheHitsLabel.setText("0");
            cacheMissesLabel.setText("0");
            hitRateLabel.setText("0%");
            cacheSizeLabel.setText("0/0");
        }
    }
}