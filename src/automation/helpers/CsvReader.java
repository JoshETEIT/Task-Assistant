package automation.helpers;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class CsvReader {
    public static final int DEFAULT_SKIP_LINES = 1;

    // Backward-compatible read()
    public static <T> List<T> read(String filePath, Function<String[], T> mapper) throws IOException {
        return read(filePath, mapper, DEFAULT_SKIP_LINES);
    }

    public static <T> List<T> read(String filePath, Function<String[], T> mapper, int skipLines)
            throws IOException {
        return read(filePath, mapper, skipLines, 0);
    }

    // Overload: allows selecting a specific table (index-based)
    public static <T> List<T> read(String filePath, Function<String[], T> mapper, int skipLines, int tableIndex)
            throws IOException {

        List<List<String[]>> allTables = parseAllTables(filePath, skipLines);

        if (allTables.isEmpty()) return Collections.emptyList();

        if (tableIndex < 0 || tableIndex >= allTables.size()) {
            System.out.printf("⚠️ Requested table index %d not found in '%s'. Returning empty list.%n",
                    tableIndex, filePath);
            return Collections.emptyList();
        }

        List<T> mapped = new ArrayList<>();
        for (String[] row : allTables.get(tableIndex)) {
            mapped.add(mapper.apply(row));
        }
        return mapped;
    }

    // Returns all parsed tables (each table is a list of String[] rows)
    private static List<List<String[]>> parseAllTables(String filePath, int skipLines) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String l;
            while ((l = br.readLine()) != null) {
                lines.add(l);
            }
        }

        List<List<String[]>> tables = new ArrayList<>();
        List<String[]> currentTable = new ArrayList<>();
        tables.add(currentTable);

        int lineIndex = 0;
        int skipped = 0;

        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) {
                lineIndex++;
                continue;
            }

            // Apply global skip lines
            if (skipped < skipLines) {
                skipped++;
                lineIndex++;
                continue;
            }

            // Handle tagged rows (#Table, #Title, #Anything)
            if (line.startsWith("#")) {
                // Start new table when hitting a tag like #Table or #Title
                if (line.regionMatches(true, 0, "#table", 0, 6)
                        || line.regionMatches(true, 0, "#title", 0, 6)) {
                    currentTable = new ArrayList<>();
                    tables.add(currentTable);
                }
                // ⚠️ Always skip the entire tag line itself
                lineIndex++;
                continue;
            }

            // Normal CSV line
            currentTable.add(parseCsvLine(line));
            lineIndex++;
        }

        // Clean up empty tables
        tables.removeIf(List::isEmpty);

        // Ensure at least one table exists
        if (tables.isEmpty()) {
            tables.add(new ArrayList<>());
        }

        return tables;
    }


    // CSV parsing that respects quoted values
    private static String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}
