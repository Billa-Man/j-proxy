package com.jproxy.gui.swing.panels;

import com.jproxy.gui.swing.models.RequestRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RequestTablePanel extends JPanel {
    
    private DefaultTableModel tableModel;
    private JTable requestTable;
    private JLabel countLabel;
    private List<RequestRecord> records;
    private int maxRecords = 100;
    
    public RequestTablePanel() {
        records = new ArrayList<>();
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        setPreferredSize(new Dimension(0, 300));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Recent Requests");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(44, 62, 80));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        countLabel = new JLabel("0 requests");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(new Color(127, 140, 141));
        headerPanel.add(countLabel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Time", "Method", "Path", "Status", "Duration", "Cache"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        requestTable = new JTable(tableModel);
        requestTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        requestTable.setRowHeight(25);
        requestTable.setShowGrid(true);
        requestTable.setGridColor(new Color(240, 240, 240));
        requestTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        requestTable.getTableHeader().setBackground(new Color(236, 240, 241));
        
        // Column widths
        requestTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Time
        requestTable.getColumnModel().getColumn(1).setPreferredWidth(70);  // Method
        requestTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Path
        requestTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Status
        requestTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Duration
        requestTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Cache
        
        // Custom cell renderer for status codes
        requestTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCodeRenderer());
        
        JScrollPane scrollPane = new JScrollPane(requestTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with clear button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setOpaque(false);
        
        JButton clearButton = new JButton("Clear History");
        clearButton.setBackground(new Color(52, 152, 219));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("SansSerif", Font.BOLD, 11));
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearRecords());
        
        bottomPanel.add(clearButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void addRequest(RequestRecord record) {
        SwingUtilities.invokeLater(() -> {
            records.add(0, record); // Add to beginning
            
            if (records.size() > maxRecords) {
                records.remove(records.size() - 1);
            }
            
            // Add row to table
            Object[] row = {
                record.getTimestamp(),
                record.getMethod(),
                record.getPath(),
                record.getStatusCode(),
                record.getDuration() + " ms",
                record.isFromCache() ? "HIT" : "MISS"
            };
            
            tableModel.insertRow(0, row);
            
            if (tableModel.getRowCount() > maxRecords) {
                tableModel.removeRow(tableModel.getRowCount() - 1);
            }
            
            countLabel.setText(records.size() + " request" + (records.size() != 1 ? "s" : ""));
        });
    }
    
    public void clearRecords() {
        records.clear();
        tableModel.setRowCount(0);
        countLabel.setText("0 requests");
    }
    
    // Custom renderer for status codes with colors
    private static class StatusCodeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected && value instanceof Integer) {
                int status = (Integer) value;
                if (status >= 200 && status < 300) {
                    c.setForeground(new Color(46, 204, 113)); // Green
                } else if (status >= 300 && status < 400) {
                    c.setForeground(new Color(52, 152, 219)); // Blue
                } else if (status >= 400 && status < 500) {
                    c.setForeground(new Color(230, 126, 34)); // Orange
                } else {
                    c.setForeground(new Color(231, 76, 60)); // Red
                }
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                c.setForeground(table.getForeground());
            }
            
            return c;
        }
    }
}