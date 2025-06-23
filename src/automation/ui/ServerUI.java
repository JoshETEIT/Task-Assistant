package automation.ui;

import automation.ServerManager;
import automation.TestSuite;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

public class ServerUI extends AutomationUI {
    private final ServerManager serverManager;
    private JFrame frame;

    public ServerUI(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public void showServerTable(boolean runAddLead, boolean runInjectSettings, boolean runUploadImage) {
        frame = new JFrame("Server Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        String[] columns = {"Name", "URL", "Username", "Select", "Edit", "Delete"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 3;
            }
        };

        JTable table = configureTable(model);
        populateTable(model, table, runAddLead, runInjectSettings, runUploadImage);

        frame.add(createTitleLabel(), BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JTable configureTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // ... other table config ...
        return table;
    }

    private void populateTable(DefaultTableModel model, JTable table, 
                             boolean runAddLead, boolean runInjectSettings, boolean runUploadImage) {
        List<ServerManager.Server> servers = serverManager.getServers();
        
        for (ServerManager.Server s : servers) {
            model.addRow(s.toRow());
        }
        model.addRow(new Object[]{"", "", "", "", "Add", ""});

        addActionButtons(table, servers, runAddLead, runInjectSettings, runUploadImage);
    }

    private void addActionButtons(JTable table, List<ServerManager.Server> servers,
                                boolean runAddLead, boolean runInjectSettings, boolean runUploadImage) {
        addButtonColumn(table, 3, "Select", row -> {
            if (row >= servers.size()) return;
            frame.dispose();
            TestSuite.runSeleniumTest(servers.get(row), runAddLead, runInjectSettings, runUploadImage);
        }, servers.size());

        // ... Edit/Delete button handlers ...
    }

    private void addButtonColumn(JTable table, int colIndex, String label,
                               IntConsumer onClick, int disableRowIndex) {
        // ... button column implementation ...
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Server Management Panel", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        return titleLabel;
    }
    
    private void showEditDialog(ServerManager.Server server) {
        JDialog dialog = createBaseDialog("Edit Server", 400, 300);
        // ... dialog setup ...
        dialog.setVisible(true);
    }
}