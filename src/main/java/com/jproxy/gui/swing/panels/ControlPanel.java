package com.jproxy.gui.swing.panels;

import com.jproxy.core.ServerController;
import com.jproxy.gui.swing.DashboardFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    private ServerController serverController;
    private StatusPanel statusPanel;
    private LogPanel logPanel;

    private JComboBox<String> modeCombo;
    private JTextField portField;
    private JTextField configField;
    private JButton startButton;
    private JButton stopButton;

    private DashboardFrame dashboardFrame;

    public ControlPanel(ServerController serverController, StatusPanel statusPanel,
            LogPanel logPanel, DashboardFrame dashboardFrame) {
        this.serverController = serverController;
        this.statusPanel = statusPanel;
        this.logPanel = logPanel;
        this.dashboardFrame = dashboardFrame;

        initUI();
    }

    public void setLogPanel(LogPanel logPanel) {
        this.logPanel = logPanel;
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        // Title
        JLabel titleLabel = new JLabel("Server Configuration");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(44, 62, 80));
        add(titleLabel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Mode
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Mode:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        modeCombo = new JComboBox<>(new String[] { "mock", "proxy" });
        formPanel.add(modeCombo, gbc);

        // Port
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        portField = new JTextField("8080", 10);
        formPanel.add(portField, gbc);

        // Config
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Config:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        configField = new JTextField("config/mocks.json", 30);
        formPanel.add(configField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);

        startButton = createStyledButton("Start Server", new Color(52, 152, 219));
        stopButton = createStyledButton("Stop Server", new Color(231, 76, 60));
        updateButtonState(stopButton, false, new Color(231, 76, 60));

        startButton.addActionListener(e -> handleStart());
        stopButton.addActionListener(e -> handleStop());

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(150, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(color.darker());
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(color);
                }
            }
        });

        return button;
    }

    private void handleStart() {
        try {
            int port = Integer.parseInt(portField.getText());
            String mode = (String) modeCombo.getSelectedItem();
            String config = configField.getText();

            serverController.configure(port, mode, config);
            serverController.start();

            updateButtonState(startButton, false, new Color(52, 152, 219));
            updateButtonState(stopButton, true, new Color(231, 76, 60));
            modeCombo.setEnabled(false);
            portField.setEnabled(false);
            configField.setEnabled(false);

            statusPanel.setRunning(true);

            // Set request listener
            if (dashboardFrame != null) {
                serverController.setRequestListener(dashboardFrame);
            }

            if (logPanel != null) {
                logPanel.appendLog("Server started: " + mode + " mode on port " + port);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to start server: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Failed to start server", e);
        }
    }

    private void handleStop() {
        try {
            serverController.stop();

            updateButtonState(startButton, true, new Color(52, 152, 219));
            updateButtonState(stopButton, false, new Color(231, 76, 60));
            modeCombo.setEnabled(true);
            portField.setEnabled(true);
            configField.setEnabled(true);

            statusPanel.setRunning(false);

            if (logPanel != null) {
                logPanel.appendLog("Server stopped");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to stop server: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Failed to stop server", e);
        }
    }

    private void updateButtonState(JButton button, boolean enabled, Color enabledColor) {
        button.setEnabled(enabled);
        if (enabled) {
            button.setBackground(enabledColor);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(189, 195, 199));
            button.setForeground(new Color(127, 140, 141));
        }
    }
}