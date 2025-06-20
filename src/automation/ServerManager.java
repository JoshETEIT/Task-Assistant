package automation;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jasypt.util.text.BasicTextEncryptor;

public class ServerManager {
    private static final String CSV_PATH = "servers.csv";
    private static final String ENCRYPTION_PASSWORD = "test-automation-key-123"; // Should be more secure in production
    private List<Server> servers;
    private BasicTextEncryptor encryptor;

    public static class Server {
        private String name;
        private String url;
        private String username;
        private String password;

        public Server(String name, String url, String username, String password) {
            this.name = name;
            this.url = url;
            this.username = username;
            this.password = password;
        }

        public String[] toRow() {
            return new String[]{name, url, username, "Select", "Edit", "Delete"};
        }

        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    public ServerManager() {
        this.encryptor = new BasicTextEncryptor();
        this.encryptor.setPassword(ENCRYPTION_PASSWORD);
        this.servers = loadServersFromCSV();
    }

    public List<Server> getServers() {
        return new ArrayList<>(servers);
    }

    public void addOrUpdateServer(int index, Server server) {
        if (index >= servers.size()) {
            servers.add(server);
        } else {
            servers.set(index, server);
        }
        saveServersToCSV();
    }

    public void removeServer(int index) {
        if (index < servers.size()) {
            servers.remove(index);
            saveServersToCSV();
        }
    }

    private List<Server> loadServersFromCSV() {
        List<Server> servers = new ArrayList<>();
        File file = new File(CSV_PATH);

        if (!file.exists()) {
            return servers;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] f = parseCSVLine(line);
                if (f.length >= 4) {
                    String decryptedPassword = f[3].isEmpty() ? "" : encryptor.decrypt(f[3]);
                    servers.add(new Server(f[0], f[1], f[2], decryptedPassword));
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading CSV: " + e.getMessage());
        }

        return servers;
    }

    private void saveServersToCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_PATH))) {
            pw.println("Name,URL,Username,Password");
            for (Server s : servers) {
                pw.printf("%s,%s,%s,%s%n",
                        escapeCSV(s.getName()),
                        escapeCSV(s.getUrl()),
                        escapeCSV(s.getUsername()),
                        escapeCSV(encryptor.encrypt(s.getPassword())));
            }
        } catch (IOException e) {
            System.out.println("Failed to save CSV: " + e.getMessage());
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().replace("\"\"", "\""));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        values.add(sb.toString().replace("\"\"", "\""));
        return values.toArray(new String[0]);
    }
}