package automation.ui;

import automation.ServerManager;
import automation.TestSuite;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

public class ServerUI {
    private final ServerManager serverManager;
    private JFrame frame;

    public ServerUI(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public void showServerTable(boolean runAddLead, boolean runIronmongeryImport, 
                               boolean runGlassImport, boolean runUploadImages) {
        frame = AutomationUI.createMainFrame("Server Management", 900, 500);
        
        String[] columns = {"Name", "URL", "Username", "Select", "Edit", "Delete"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override 
            public boolean isCellEditable(int row, int column) {
                return column >= 3;
            }
        };

        JTable table = configureTable(model);
        populateTable(model, table, runAddLead, runIronmongeryImport, runGlassImport, runUploadImages);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.setOpaque(false);
        contentPanel.add(AutomationUI.createLabel("Server Management Panel"), BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JTable configureTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.setRowHeight(36);
        table.setFont(AutomationUI.BODY_FONT);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createEmptyBorder());
        
        JTableHeader header = table.getTableHeader();
        header.setFont(AutomationUI.TITLE_FONT);
        header.setBackground(AutomationUI.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        header.setOpaque(true);
        
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                
                if (isSelected) {
                    setBackground(new Color(
                        AutomationUI.PRIMARY_COLOR.getRed(),
                        AutomationUI.PRIMARY_COLOR.getGreen(),
                        AutomationUI.PRIMARY_COLOR.getBlue(),
                        150
                    ));
                    setForeground(Color.WHITE);
                } else {
                    setForeground(AutomationUI.TEXT_COLOR);
                }
                
                return this;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        return table;
    }

    private void populateTable(DefaultTableModel model, JTable table, 
            boolean runAddLead, boolean runIronmongeryImport, 
            boolean runGlassImport, boolean runUploadImages) {
        List<ServerManager.Server> servers = serverManager.getServers();

        for (ServerManager.Server s : servers) {
            model.addRow(s.toRow());
        }
        model.addRow(new Object[]{"", "", "", "", "Add", ""});

        addActionButtons(table, servers, runAddLead, runIronmongeryImport, runGlassImport, runUploadImages);
    }

    private void addActionButtons(JTable table, List<ServerManager.Server> servers,
                   boolean runAddLead, boolean runIronmongeryImport, 
                   boolean runGlassImport, boolean runUploadImages) {
        addButtonColumn(table, 3, "Select", row -> {
            if (row >= servers.size()) return;
            frame.dispose();
            new Thread(() -> 
                TestSuite.runSeleniumTest(
                    servers.get(row), 
                    runAddLead, 
                    runIronmongeryImport,
                    runGlassImport,
                    runUploadImages
                )
            ).start();
        }, servers.size());

        addButtonColumn(table, 4, "Edit", row -> {
            ServerManager.Server existing = row < servers.size() ? servers.get(row) : null;
            showEditDialog(existing, row, runAddLead, runIronmongeryImport, runGlassImport, runUploadImages);
        }, -1);

        addButtonColumn(table, 5, "Delete", row -> {
            if (row >= servers.size()) return;
            int confirm = AutomationUI.showOptionDialog(
                frame, 
                "Delete this server?", 
                "Confirm Delete", 
                new String[]{"Delete", "Cancel"}
            );
            if (confirm == 0) {
                serverManager.removeServer(row);
                frame.dispose();
                showServerTable(runAddLead, runIronmongeryImport, runGlassImport, runUploadImages);
            }
        }, servers.size());
    }

    private void addButtonColumn(JTable table, int colIndex, String label,
                               IntConsumer onClick, int disableRowIndex) {
        table.getColumnModel().getColumn(colIndex).setCellRenderer((tbl, val, sel, foc, row, col) -> {
            JButton btn = AutomationUI.createButton(label);
            if (disableRowIndex != -1 && row >= disableRowIndex && !label.equals("Edit")) {
                btn.setEnabled(false);
                btn.setBackground(Color.LIGHT_GRAY);
            }
            return btn;
        });

        table.getColumnModel().getColumn(colIndex).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public Component getTableCellEditorComponent(JTable tbl, Object val, boolean sel, int row, int col) {
                JButton btn = AutomationUI.createButton(label);
                if (disableRowIndex != -1 && row >= disableRowIndex && !label.equals("Edit")) {
                    btn.setEnabled(false);
                    btn.setBackground(Color.LIGHT_GRAY);
                } else {
                    btn.addActionListener(e -> {
                        onClick.accept(row);
                        fireEditingStopped();
                    });
                }
                return btn;
            }

            @Override
            public Object getCellEditorValue() {
                return label;
            }
        });
    }

    private void showEditDialog(ServerManager.Server server, int rowIndex,
            boolean runAddLead, boolean runIronmongeryImport, 
            boolean runGlassImport, boolean runUploadImages) {
        JDialog dialog = AutomationUI.createStyledDialog(
            server == null ? "Add New Server" : "Edit Server", 
            400, 
            300
        );
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(AutomationUI.DIALOG_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        
        gbc.gridy = 0;
        panel.add(AutomationUI.createLabel("Server Name:"), gbc);
        gbc.gridy++;
        panel.add(AutomationUI.createLabel("Server URL:"), gbc);
        gbc.gridy++;
        panel.add(AutomationUI.createLabel("Username:"), gbc);
        gbc.gridy++;
        panel.add(AutomationUI.createLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JTextField nameField = new JTextField(server == null ? "" : server.getName(), 20);
        nameField.setFont(AutomationUI.BODY_FONT);
        nameField.setOpaque(true);
        panel.add(nameField, gbc);
        
        gbc.gridy++;
        JTextField urlField = new JTextField(server == null ? "" : server.getUrl(), 20);
        urlField.setFont(AutomationUI.BODY_FONT);
        urlField.setOpaque(true);
        panel.add(urlField, gbc);
        
        gbc.gridy++;
        JTextField userField = new JTextField(server == null ? "" : server.getUsername(), 20);
        userField.setFont(AutomationUI.BODY_FONT);
        userField.setOpaque(true);
        panel.add(userField, gbc);
        
        gbc.gridy++;
        JPasswordField passField = new JPasswordField(server == null ? "" : server.getPassword(), 20);
        passField.setFont(AutomationUI.BODY_FONT);
        passField.setOpaque(true);
        panel.add(passField, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton saveButton = AutomationUI.createButton("Save");
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String url = urlField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();

            if (!name.isEmpty() && !url.isEmpty() && !user.isEmpty()) {
                serverManager.addOrUpdateServer(
                    rowIndex, 
                    new ServerManager.Server(name, url, user, pass)
                );
                dialog.dispose();
                frame.dispose();
                showServerTable(runAddLead, runIronmongeryImport, runGlassImport, runUploadImages);
            } else {
                AutomationUI.showMessageDialog(
                    dialog, 
                    "Please fill all required fields", 
                    "Validation Error", 
                    JOptionPane.WARNING_MESSAGE
                );
            }
        });
        
        JButton cancelButton = AutomationUI.createButton("Cancel");
        cancelButton.setBackground(Color.GRAY);
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}