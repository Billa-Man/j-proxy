package com.jproxy.gui.swing;

import com.jproxy.core.MetricsCollector;
import com.jproxy.core.RequestListener;
import com.jproxy.core.ServerController;
import com.jproxy.gui.swing.panels.*;
import com.jproxy.handler.ProxyHandler;
import com.jproxy.gui.swing.models.RequestRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DashboardFrame extends JFrame implements RequestListener {
    private static final Logger logger = LoggerFactory.getLogger(DashboardFrame.class);

    private ServerController serverController;

    // Panels
    private ControlPanel controlPanel;
    private MetricsPanel metricsPanel;
    private CacheMetricsPanel cacheMetricsPanel;
    private ChartPanel chartPanel;
    private LogPanel logPanel;
    private StatusPanel statusPanel;
    private RequestTablePanel requestTablePanel;

    public DashboardFrame() {
        serverController = new ServerController();
        initUI();
        startUpdateTimer();
        logger.info("Dashboard initialized");
    }

    private void initUI() {
        setTitle("J-Proxy Dashboard v1.0.0");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Failed to set system look and feel", e);
        }

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));

        // Top: Status bar
        statusPanel = new StatusPanel();
        mainPanel.add(statusPanel, BorderLayout.NORTH);

        // Center: Main content
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);

        // Control panel
        controlPanel = new ControlPanel(serverController, statusPanel, logPanel, this);
        centerPanel.add(controlPanel, BorderLayout.NORTH);

        // Metrics and chart
        JPanel metricsAndChartPanel = new JPanel(new BorderLayout(10, 10));
        metricsAndChartPanel.setOpaque(false);

        // Metrics panels side by side
        JPanel metricsContainer = new JPanel(new GridLayout(1, 2, 10, 0));
        metricsContainer.setOpaque(false);

        metricsPanel = new MetricsPanel();
        cacheMetricsPanel = new CacheMetricsPanel();

        metricsContainer.add(metricsPanel);
        metricsContainer.add(cacheMetricsPanel);

        metricsAndChartPanel.add(metricsContainer, BorderLayout.NORTH);

        // Chart
        chartPanel = new ChartPanel();
        metricsAndChartPanel.add(chartPanel, BorderLayout.CENTER);

        // Add request table
        requestTablePanel = new RequestTablePanel();

        // Combine chart and request table
        JPanel chartAndTablePanel = new JPanel(new GridLayout(2, 1, 0, 10));
        chartAndTablePanel.setOpaque(false);
        chartAndTablePanel.add(chartPanel);
        chartAndTablePanel.add(requestTablePanel);

        metricsAndChartPanel.add(chartAndTablePanel, BorderLayout.CENTER);

        centerPanel.add(metricsAndChartPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Logs
        logPanel = new LogPanel();
        mainPanel.add(logPanel, BorderLayout.SOUTH);

        // Update control panel reference to log panel
        controlPanel.setLogPanel(logPanel);

        add(mainPanel);

        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
    }

    private void startUpdateTimer() {
        Timer timer = new Timer(1000, e -> updateMetrics());
        timer.start();
    }

    private void updateMetrics() {
        if (serverController.isRunning()) {
            MetricsCollector metrics = serverController.getMetricsCollector();
            metricsPanel.updateMetrics(metrics);
            chartPanel.updateChart(metrics);

            // Only update cache metrics in proxy mode
            if ("proxy".equalsIgnoreCase(serverController.getMode())) {
                ProxyHandler proxyHandler = serverController.getProxyHandler();
                cacheMetricsPanel.updateMetrics(proxyHandler);
            } else {
                cacheMetricsPanel.updateMetrics(null);
            }
        }
    }

    private void handleClose() {
        if (serverController.isRunning()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Server is still running. Stop server and exit?",
                    "Confirm Exit",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                serverController.stop();
                dispose();
                System.exit(0);
            }
        } else {
            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame();
            frame.setVisible(true);
        });
    }

    @Override
    public void onRequest(String method, String path, int statusCode, long duration, boolean fromCache) {
        RequestRecord record = new RequestRecord(method, path, statusCode, duration, fromCache);
        requestTablePanel.addRequest(record);
        logPanel.appendLog(String.format("%s %s -> %d (%dms)", method, path, statusCode, duration));
    }

}